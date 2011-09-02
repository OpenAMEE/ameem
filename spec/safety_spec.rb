require File.dirname(__FILE__) + '/spec_helper.rb'

describe "commit:negative_tests" do
  it 'should give meaningful error when trying to add a category which is a child of a thus-far unuploaded category T169' do
    in_cat(ChildOfNonexistent) do
      a=AMEEM.new("-c #{TestVerbosity} add . ")
      lambda{a.api_xml(a.category(a.target))}.should raise_error
      lambda{a.exec}.should raise_error("Trying to add category where parent not on server.")
    end
  end
  it 'should give meaningful error when trying to add a category which already exists T181' do
    in_cat(Preexistent) do
      a=AMEEM.new("-c #{TestVerbosity} add . ")
      doc=a.api_xml(a.category(a.target))
      REXML::XPath.first(doc,'//Name/text()').value.
        should eql 'Preexistent'
      lambda{a.exec}.should raise_error("Trying to add a category which already exists on server.")
    end
  end
  it 'should give meaningful error when trying to put a category T246' do
    in_cat(Preexistent) do
      a=AMEEM.new("-c #{TestVerbosity} put . ")
      doc=a.api_xml(a.category(a.target))
      REXML::XPath.first(doc,'//Name/text()').value.
        should eql 'Preexistent'
      lambda{a.exec}.should raise_error("Category put not implemented.")
    end
  end
  it 'should give meaningful error when trying to add an existing itemdef T250' do
    in_cat(Preexistent) do
      a=AMEEM.new("-i -t #{TestVerbosity} add . ")
      a.find_definition_uid_by_name('AMEEM Prexistent Test Itemdef').should be_a String
      lambda{a.exec}.should raise_error("AMEEM Prexistent Test Itemdef already exists")
    end
  end
  it 'should give meaningful error when itemdef type is not valid #244' do
    in_cat(InvalidType) do
      a=AMEEM.new("-i -t #{TestVerbosity} add . ")
      lambda{a.exec}.should raise_error(/DECIMEL/)
    end
  end
  it "should give meaningful error when data doesn't match itemdef type #244" do
    in_cat(WrongType) do
      a=AMEEM.new("-i -t #{TestVerbosity} add . ")
      lambda{a.exec}.should raise_error(/fish/)
    end
  end
  it "should give meaningful error when ameem.yml file has no discover password entry" do
    configfolder=AMEEM.new("").find_config_folder
    configfile=File.join(configfolder,'ameem.yml')
    config_file=YAML.load_file(configfile)
    configoriginal=config_file.clone
    config_file.delete_if{|k,v| k=~/discover/}
    begin
      lambda{AMEEM.new("dummy")}.should_not raise_error(/Discover/)
      File.open(configfile, 'w' ) do |out|
        YAML.dump( config_file, out )
      end
      lambda{AMEEM.new("dummy")}.should raise_error(/Discover/)
    ensure
      File.open(configfile, 'w' ) do |out|
        YAML.dump( configoriginal, out )
      end
    end
  end
  it "should fail to validate if itemdef can be more restrictive" do
    in_cat Relaxed do
      lambda{AMEEM.new("-t -d -i validate .").exec}.
        should raise_error(/validate as Floats/)
    end
  end
  it "should not find null data in home/appliances/entertainment/generic" do
    in_cat "/home/appliances/entertainment/generic" do
      a=AMEEM.new("-t -d -i validate .")
      lambda{a.exec}.
        should_not raise_error(/validate as Floats/)
    end
  end
end
