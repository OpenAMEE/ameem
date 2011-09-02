# Module providing generator functionality to AMEEM, creating empty configuration files
# 
# Usage:
# ameem generate <target> <command> <args>
# * generate itemdef header <name> -- create top lines of itemdef.csv file, given itemdef name
# * generate itemdef line <datatype> <name> [true|false] [true|false] -- append a line to itemdef.csv file
# * generate itemdef drill <datatype> <name> -- append a drill definition to itemdef.csv
# * generate itemdef data <datatype> <name> -- append a data item value definition to itemdef.csv
# * generate itemdef profile <datatype> <name> -- append a profile item value definition to itemdef.csv
# * generate spec header -- create a skeleton item_spec.rb file
#
# Other modules can provide more generaters by defining methods such as
# generate_foo_bar and then mixing them into AMEEM
module Generate
  provides :generate
  # dispatch generate subcommand
  def generate
    arguments=options.generate_args
    generate_target=arguments.shift
    command=arguments.shift
    dispatch="generate_"+generate_target+"_"+command
    if respond_to?(dispatch)
      method(dispatch).call(*arguments)
    else
      raise "AMEEM cannot generate "+command+" for "+generate_target
    end
  end

  # generates skeleton itemdef.csv file
  def generate_itemdef_header(name)
    itemdef=File.new('itemdef.csv','w')
    itemdef << "Name," << name << "\n"
    itemdef << "algFile,algorithm.js" << "\n"
    itemdef << "name,path,type,isDataItemValue,isDrillDown" << "\n"
    itemdef << "Source,source,TEXT,true,false" << "\n"
    itemdef.close;
  end

  # adds skeleton line to itemdef.csv file
  def generate_itemdef_line(type,name,isData,isDrill)
    itemdef=File.new('itemdef.csv','a')
    itemdef << name.underscore.humanize << "," << name << "," << type << "," << isData << "," << isDrill << "\n"
    itemdef.close;
  end

  # call generate_itemdef_line to generate a drill definition
  def generate_itemdef_drill(type,name)
    generate_itemdef_line(type,name,true,true)
  end

  # call generate_itemdef_line to generate a profile item value definition
  def generate_itemdef_profile(type,name)
    generate_itemdef_line(type,name,false,false)
  end

  # call generate_itemdef_line to generate a data item value definition
  def generate_itemdef_data(type,name)
    generate_itemdef_line(type,name,true,false)
  end

  # generate skeleton item_spec.rb Rspec file
  def generate_spec_header()
    aspec=File.new('item_spec.rb','w')
    specHeader=<<EOF
TEST_ROOT=ENV['AMEE_TEST_ROOT'] if !defined? AMEE_TEST_ROOT
raise 'Please set AMEE_TEST_ROOT  in your environment to point to internal/tests/api' if TEST_ROOT==nil  
require TEST_ROOT+'/spec_helper.rb'

folder=folder_category(__FILE__)

describe folder do
  it_should_behave_like "V2 XML profile"
  it "should give the right amount of carbon" do
    check_calculation(folder,
                      "DRILL=CHOICE",
                      CARBON_RESULT,
                      :PROFILE_ITEM => VALUE)
  end
end
EOF
    aspec << specHeader
    aspec.close
  end

end

