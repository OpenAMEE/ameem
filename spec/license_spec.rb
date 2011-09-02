require File.dirname(__FILE__) + '/spec_helper.rb'

describe License do
  it "should add a license where there isn't one" do
    copying_test_category(Unlicensed) do
      file_should_be_same_as_in_another_category 'itemdef.csv',Unlicensed
      file_should_be_same_as_in_another_category 'data.csv',Unlicensed
      AMEEM.new("#{TestVerbosity} license Banana .").exec
      file_should_be_same_as_in_another_category 'itemdef.csv',Licensed
      file_should_be_same_as_in_another_category 'data.csv',Licensed
    end
  end
  it "should change license where there is one" do
    copying_test_category(Licensed) do
      AMEEM.new("#{TestVerbosity} license Grapefruit .").exec
      file_should_be_same_as_in_another_category 'itemdef.csv',Licensed
      itemdef=ItemDefinition.from_file('itemdef.csv')
      test=DataTable.from_file('data.csv',itemdef)
      test.drill("Type"=>"wormhole").data["license"].should eql "&lt;a href='DummyLicenseLink'&gt;The grapefruit license&lt;/a&gt;"
    end
  end
  it "should raise error when try to reference a nonexistent license" do
    copying_test_category(Unlicensed) do
      lambda{AMEEM.new("#{TestVerbosity} license Nonexistent .").exec}.should raise_error(
        /No such license as Nonexistent/)
    end
  end
end