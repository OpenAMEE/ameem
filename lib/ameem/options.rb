# -*- coding: undecided -*-
require 'optparse'
require 'optparse/time'
require 'ostruct'
require 'shellwords'
require 'ameem/config_file'
require 'fileutils'


# Module providing to AMEEM the ability to handle the options for how to behave
module AMEEMOptions
  # Get the options object, an OpenStruct instance.
  attr_accessor :options

  # Default discover instance to use, depending on each server shortcode
  Discovers=
    { 'sci' => 'http://discover-test.amee.com',
    'live' => 'http://discover.amee.com',
    'stage' => 'http://discover.amee.com',
    'dev' => 'http://discover-test.amee.com'
  }

  # URLs, git branches, and Java APITools shortcodes for AMEE servers, depending on each server shortcode
  Servers=
    { # it is not sufficient to change these here - they need changing in the
    # java code as well
    'dev' => { :server=>'DEV',
      :url=>'platform-dev.amee.com',
      :admin=> 'admin-platform-dev.amee.com',
      :branch=>'science'
    },
    'sci' => {
      :server => 'SCIENCE', # constant name in Java code
      :url => 'platform-science.amee.com',
      :admin=> 'admin-platform-science.amee.com',
      :branch=>'science'
    },
    "live" => {
      :server => 'LIVE', # constant name in Java code
      :url => 'live.amee.com',
      :admin=> 'admin-live.amee.com',
      :branch=>'live'
    },
    "stage" => {
      :server=>"STAGE",
      :url=>'stage.amee.com',
      :admin=>'admin-stage.amee.com',
      :branch=>'live'
    },
    "defra" => {
      :server=>"DEFRA",
      :url=>'defra.co2.dgen.net',
      :admin => 'admin.defra.co2.dgen.net'
    },
    "jb" => {
      :server=>"JB",
      :url=>"jb.live.amee.com",
      :admin_url=>"admin-jb.live.amee.com"
    },
    "doc" => {
      :server=>"DOC",
      :url=>"doc-check.amee.com",
      :admin_url=>"NOT REAL"
    }
  }

  # Get which git branch is currently checked out
  def current_git_branch
    begin
      res=`git branch 2>&1`.match(/\* (.*)/)[1]
    rescue => err
      verbose "Problem determining git branch #{res}", Log4r::WARN
      # At this stage, the logger might not be up and running, so also print warning
      print "Problem determining git branch. #{res}\n"
      return nil
    end
    return res
  end

  # Given a server shortcode, set the options ostruct fields for URL and admin url.
  def parse_server(server)
    if Servers[server]==nil
      verbose 'No such server as #{server}'
      options.server_code='free'
      options.server=server
      options.server_url=server
      options.admin_url="admin-#{server}"
      return
    end
    options.server_code=server
    options.server=Servers[server][:server]
    options.server_url=Servers[server][:url]
    options.admin_url=Servers[server][:admin]
    if server=='live'&&!options.force
      print "Are you really sure you want to work with LIVE ?[No]\n"
      raise "Confirmation failed" if !($stdin.gets=~/[yY]/)
    end
  end

  # Verify curret git branch is as expected for server, and complain if wrong
  def check_server_branch(server)
    gb=current_git_branch
    return if server=='free' || Servers[server]==nil
    unless (Servers[server][:branch]==gb)||gb.nil?||Servers[server][:branch].nil?
      unless options.force||ENV['AMEEM_NOPROMPT']
        print "Really commit to #{server} from branch #{gb}?[No]\n"
        raise "Confirmation failed" if !($stdin.gets=~/[yY]/)
      end
      options.wrong_branch=true
    end
  end

  # Parse the command line options, setting defaults.
  # Resulting OpenStruct AMEEM#options is defined as follows:
  #  * options.target -- usage varies, typically the folder on which to act
  #  * options.discover -- URL of the AMEE Discover instance with which to work
  #  * options.recurse_down -- boolean, true if should act on folders below target
  #  * options.recurse_up -- boolean, true if should act on folders above target
  #  * options.root -- Target paths are specified relative to this, either current folder or root of apicsvs
  #  * options.default_server -- server to use if none specifically specified.
  #  * options.force -- true if should not prompt gor risky actions
  #  * options.test -- true if should not carry out actions, just run framework code
  #  * options.verbosity -- a Log4r level indicating logging/warning level
  #  * options.needs_connection - TODO doc this
  #  * options.password -- password to use for access to AMEE instance
  #  * options.server_code -- shortcode for amee instance
  #  * options.server -- shortcode or url for amee instance
  #  * options.server_url -- url for amee instance
  #  * options.admin_url -- url for amee-admin instance
  #  * options.itemdef -- true if should act on (e.g. update/delete/fetch) itemdef.csv
  #  * options.algorithm -- true if should act on algorithm.js
  #  * options.category -- true if should act on AMEE dc
  #  * options.data -- true if should act on data.csv
  #  * options.metadata -- true if should act on meta.yml and documentation.creole
  #  * options.history -- true if should act on DIVH history data
  #  * options.changelog -- true if should act on changelog
  #  * options.return_values -- true if should act on MARV RVD
  #  * options.timeout -- timeout for web interactions in seconds
  #  * options.command -- AMEEM function to which to dispatch
  #  * options.action -- subcommand info used by some functions
  #  * options.license -- with license command, the license to apply
  #  * options.newname -- with namechange command, new name to use
  def parse_options(args)
    args=Shellwords.shellwords(args) if args.class==String
    @options = OpenStruct.new
    parse_config_file
    options.discover||=Discovers
    options.recurse_down=false
    options.recurse_up=false
    options.root=FileUtils.pwd
    server=options.default_server
    #server="dev" # choose the default here
    #server="stage"
    options.force=false
    parse_server(server)
    options.test=false
    options.verbosity=Log4r::WARN
    options.needs_connection=true
    opts = OptionParser.new do |opts|
      opts.banner = "Usage: ameem [options] command target"

      opts.separator ""
      opts.separator "Sub commands:"
      opts.separator "delete, add, put, fetch, status, generate itemdef,
      generate spec, open, license <licensename>, testsettings <enable|disable>,
      pathchange <newpath>,namechange,spellcheck,validate"
      opts.separator ""
      opts.separator "Switches:"
      opts.on("-v [verbosity]",Integer) do |verbosity|
        if verbosity
          options.verbosity = verbosity # integer description
        else
          options.verbosity=Log4r::INFO
        end
      end
      opts.on("--verbose verbosity",String) do |verbosity|
        if verbosity
          options.verbosity = "Log4r::#{verbosity.upcase}".constantize
        else
          options.verbosity=Log4r::INFO
        end
        verbose("Verbosity: #{options.verbosity}")
      end
      opts.on("-r", "--recurse",
        "Recurse downward") do
        options.recurse_down=true
        verbose "Recurse downward"
      end
      opts.on("-u","--upward",
        "Recurse upward") do
        options.recurse_up=true;
        verbose "Recurse upward"
      end
      opts.on("-p","--password [password]",
        "Set password") do |password|
        #verbose "password #{password}"
        options.password=password
      end
      opts.on("-t","--test",
        "Don't change anything, do dry run") do
        verbose "Test"
        options.test=true
      end
      opts.on("-f","--force", "force without confirmation") do
        options.force=true
        verbose "Forcing"
      end
      opts.on("-s","--server [server]",
        "Set AMEE Instance: {dev,sci (DEFAULT),live,stage,defra, whatever.amee.com}") do |serv|
        parse_server(serv)
        verbose "Server: #{options.serv}"
      end
      opts.on("--admin [server]","Set admin instance") do |serv|
        options.admin_url=serv
      end
      # options.data is the default if nothing else is given
      options.itemdef=false
      options.algorithm=false
      options.category=false
      options.history=false
      options.metadata=false
      opts.on("-d","--data",
        "operate on data items") do
        options.data=true
        verbose("Data")
      end
      opts.on("-i","--itemdef",
        "operate on item definition") do
        options.itemdef=true
        options.data||=false
        verbose("Itemdef")
      end
      opts.on("-a","--algorithm",
        "operate on algorithm") do
        options.algorithm=true
        options.data||=false
        verbose("Algorithm")
      end
      opts.on("-c","--category",
        "operate on data category") do
        options.category=true
        options.data||=false
        verbose("Category")
      end
      opts.on("-y","--history",
        "operate on data history") do
        options.history=true
        options.data||=false
        verbose("History")
      end
      opts.on("-m","--metadata",
        "operate on metadata") do
        options.meta=true
        options.data||=false
        verbose("Metadata")
      end
      opts.on("--marv",
        "operate on return value definitions") do
        options.return_values=true
        options.data||=false
        verbose("Metadata")
      end
      opts.on("--changelog",
        "operate on changelog") do
        options.changelog=true
        options.data||=false
        verbose("Changelog")
      end
      opts.on("-e","--everything",
        "same as -d -i -a -c -y -m --marv --changelog") do
        options.category=true
        options.data=true
        options.itemdef=true
        options.algorithm=true
        options.history=true
        options.meta=true
        options.return_values=true
        options.changelog=true
        verbose("Everything")
      end
      opts.on("-h","--home", "specify target relative to /api_csvs") do
        options.root=csv_root
        verbose "Root is #{options.root}"
      end
      opts.on("--discover [url]",
        "choose discover instance, overriding config file") do |url|
        options.discover=url
      end
      opts.on("--timeout [time]",
        "choose discover instance, overriding config file") do |time|
        options.timeout=time.to_i
      end
    end
    opts.parse!(args)
    check_server_branch(options.server_code)
    if options.discover.class==Hash
      discover=options.discover[options.server_code]
      discover||=options.discover['default']
      discover||=options.discover['dev']
      raise "Invalid discover option #{options.discover.inspect}
         with #{options.server_code}" unless discover
      options.discover=discover
    end
    options.data = true if !defined?(options.data)
    # without other instruction, operate only on data
    options.command=args.shift
    # push the second qualifier of the command appropriately
    # i.e. for ameem testsettings enable
    options.license=args.shift if options.command=='license'
    options.newname=args.shift if options.command=='pathchange'
    options.needs_connection=false if options.command=='testsettings'
    options.action=args.shift if 
    (options.command=='testsettings' || options.command=='discover')
    # remainder of the command line is the list of folders to target
    if options.command=='generate'
      options.generate_args=args
      options.target=[nil]
    else
      options.target=args
    end
    verbose "Target: '#{options.target}'"
    if options.target[0]==nil
      options.target=['.'] # default to current folder
    end
    folder=File.expand_path(File.join(options.root,options.target[0]))
    if options.recurse_down
      Find.find(folder) do |path|
        next if path=~/\.svn/
        next if path=~/divh/
        next if path=~/sap/
        next if path=~/sources/
        next if path=~/edit_files/
        if FileTest.directory?(path)&&path!=folder
          # get path relative to current options.root
          options.target.push relative_to_root path
        end
      end
    end

    if options.recurse_up
      if options.root!=csv_root
        raise "Upward recursion not supported in relative mode
              (use -h and specify relative to api_csvs)"
      end
      while folder!=csv_root
        folder=File.dirname(folder)
        # get folder relative to current options.root
        relpath=relative_to_root folder
        options.target.push relpath if relpath!=""
      end
    end
  end
end
