#! /usr/bin/ruby

# parse options and command for this ameem
require 'rubygems'
gem 'amee','~> 4.3.1'
gem 'amee-internal', '~> 5.1.0'
require 'active_support/inflector'
require 'amee-internal'
require 'pp'
require 'log4r'
require 'log4r/yamlconfigurator'
require 'find'
require 'yaml'
require 'raspell'

require 'ameem/boolean'
require 'ameem/options'
require 'ameem/folder_tools'
require 'ameem/apitools'
require 'ameem/config_file'
require 'ameem/item_definition'
require 'ameem/data_item'
require 'ameem/return_value_definition'
require 'ameem/xml'


ItemDefinitionFile='itemdef.csv'
DataFile='data.csv'
ReturnValueDefinitionFile='return_values.csv'
AMEEUID='5F5887BCF726'

# Main class for the AMEEM project
# Instances of this class:
# 1. Have information as to the 'target' of the ameem operation
# 2. Have options (via AMEEM#options) which modify that operation
#    * These are loaded from a config file (see config_file.rb)
#    * And/or from command line options (see options.rb)
# 3. Can dispatch the command to various modules which implement the command.
#    * e.g. history.rb implements commands regarding DIVH
class AMEEM

  @@commands=[]

  # Used by child modules to inform ameem of a new ameem subcommand.
  # The dispatcher (AMEEM#exec) will check that the command is registered here
  # before dispatching
  def self.provides *commands
    commands.each do |command|
      @@commands<<command
    end
  end

  include AMEEMOptions
  include FolderTools
  include AMEEMConfigFile
  include AMEEMXML
  include APITools

  module Exceptions
    class Discover < Exception
    end
    class Location < Exception
    end
  end



  # Execute the current ameem subcommand:
  # * The command and arguments are set in options.command
  # * If the command is registered, then the method named in the command is called
  #   this will typically be a method defined on the module which registered it.
  # * If the current AMEE instance has multiple targets, this will create instances for
  #   each one, execute each.
  def exec
    verbose "executing #{options.command}", Log4r::DEBUG
    if options.target.length>1
      options.target.each do |targ|
        clone=AMEEM.new(options)
        clone.options.target=[targ]
        verbose "recursed to #{targ}"
        begin
          clone.exec
        rescue AMEE::NotFound,AMEE::UnknownError,AMEE::BadData,Errno::ENOENT,Timeout::Error => err
          verbose err,Log4r::ERROR
          raise err unless options.force # if recursing with force, errors dont stop us
        end
      end
    elsif respond_to?(options.command)&&@@commands.include?(options.command.to_sym)
      method(options.command).call
    else
      raise "AMEEM cannot handle command "+options.command
    end
  end

  # return the target folder for this ameem operation.
  # That is, the AMEE API data category to be operated on, such as
  # /transport/car/generic. 
  # The target is defined as a path on the AMEE API.
  # 
  # Returns a single-member array of the targets
  # Should not get called when there are still multiple targets, because
  # AMEEM#exec should have farmed them out, so raises an exception in that circumstance
  def target
    if options.target.length>1
      raise 'Multiple targets not supported'
      # plan is to marshall out each target an re-instantiate an AMEEM for each one, and execute those.
    else
      options.target[0]
    end
  end

  # cleanup after an ameem execution
  def clean
  end

  # are we in testing (dryrun) mode?
  # TODO: Rename this to testing? following ruby best practice
  def testing
    return options.test
  end

  provides :dummy
  # dummy command, useful for checking ameem setup, e.g. "> ameem dummy"
  def dummy
    verbose "Doing nothing to #{target}"
  end

  # log to the ameem logfile and screen at given level
  def verbose(message,level=Log4r::INFO)
    @log.send(Log4r::LNAMES[level].downcase,message)
  end
  
  attr_accessor :amee # An AMEE#Connection instance to the AMEE API (see http://github.com/floppy/amee-ruby/lib/amee/conection.rb
  attr_accessor :amee_admin # An AMEE#Connection instance to the AMEE Admin API (e.g. admin-platform-science.amee.com)
  attr_accessor :log # A Log4r logging instance to log to.

  # With String argument, this is the AMEE commandline, i.e. args is an array of
  # command line entries and switches; With OpenStruct argument, behaves as a
  # copy constructor with those options
  def initialize(args)
    # Bootstrap logger
    @log=Log4r::Logger.new('AMEERuby')
    @log.outputters=[Log4r::StderrOutputter.new('AMEERubyStdout')]
    @log.level=Log4r::ERROR

    # Set up spellchecker
    @speller=Aspell.new("en_US")
    @speller.suggestion_mode = Aspell::NORMAL

    if args.class==OpenStruct
      @options=args
    else
      parse_options(args)
    end

    # Replace the bootstrap logger
    yc=Log4r::YamlConfigurator
    yc['HOME']=options.log_folder
    yc.load_yaml_file File.join(options.config_folder,'log.yml')
    @log=Log4r::Logger['Main']
    AMEE::Logger.to Log4r::Logger['Gem'] 
    Log4r::Outputter['stderr'].level=options.verbosity

    verbose "Selected amee servers #{options.server_url},#{options.admin_url}"
    if options.needs_connection
      @amee=AMEE::Connection.new(options.server_url,options.auser ? options.user : 'adminv2',options.password,:format=>:xml, :amee_source => 'ameem')
      @amee_admin=AMEE::Connection.new(options.admin_url,options.auser ? options.auser : 'admin',options.apassword,:format=>:xml, :amee_source => 'ameem')
      @amee.timeout=options.timeout
      @amee_admin.timeout=options.timeout
      @amee.authenticate
      @amee_admin.authenticate
    end
    verbose "AMEEM root: #{ameem_location}",Log4r::DEBUG
    verbose "CSV root: #{csv_root}",Log4r::INFO
    verbose "AMEEM config: #{options.config_folder}",Log4r::INFO
  end

  attr_reader :itemdef # An AMEEM ItemDefinition instance, constructed from the itemdef.csv file for the target category
  attr_reader :data # An AMEEM DataTable instance, constructed from the data.csv file for the target category
  attr_reader :return_values # An AMEEM ReturnValueDefinition instance, constructed from the return_values.csv file for the target category

  #(re)create the AMEEM#itemdef instance
  def parse_itemdef
    file=File.join(based_on_root(target),ItemDefinitionFile)
    @itemdef=ItemDefinition.from_file(file) if File.exist?(file)
  end

  #(re)create the AMEEM#return_values instance
  def parse_return_values
    file=File.join(based_on_root(target),ReturnValueDefinitionFile)
    @return_values=ReturnValueDefinition.from_file(file) if File.exist?(file)
  end

  #(re)create the AMEEM#data instance
  def parse_data
    file=File.join(based_on_root(target),DataFile)
    @data=DataTable.from_file(file,itemdef,options.force&&!options.data) if File.exist?(file)
  end

  #overwrite itemdef.csv file from current AMEEM#itemdef instance
  def save_itemdef
    file=File.join(based_on_root(target),ItemDefinitionFile)
    verbose "saving itemdef to #{file}"
    itemdef.save_csv(file)
  end

  #overwrite data.csv file from current AMEEM#data instance
  def save_data
    file=File.join(based_on_root(target),DataFile)
    verbose "saving data to #{file}"
    data.save_csv(file)
  end

  # Feature control- return true if the given feature is turned on in the
  # config file
  # TODO - follow ruby best practice by renaming to feature?
  def feature(feat)
    servs=options.features[feat.to_s]
    verbose "Feature check: #{servs.inspect} #{options.server_code}"
    servs&&servs.include?(options.server_code)
  end
end

# Root namespace (Object) method proxying for the AMEEM.provides class method
def provides *args
  AMEEM.provides *args
end

require 'ameem/ameem_generate'
require 'ameem/commit'
require 'ameem/get'
require 'ameem/delete'
require 'ameem/open'
require 'ameem/license'
require 'ameem/testsettings'
require 'ameem/history'
require 'ameem/meta'
require 'ameem/discover'
require 'ameem/namechange'
require 'ameem/manage_return_values'
require 'ameem/changelog'

class AMEEM
  include Generate
  include Commit
  include Delete
  include History
  include Get
  include Open
  include License
  include TestSettings
  include Meta
  include Discover
  include Namechange
  include ManagesReturnValues
  include Changelog
end

# Root namespace (Object) method constructing an ameem object, then executing it
def ameem(args)
  AMEEM.new(args).exec
end