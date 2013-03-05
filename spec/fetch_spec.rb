require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe Get, "when fetching" do
  it "should fetch itemdef" do
    in_cat Preexistent do
      a=AMEEM.new("-i fetch .")
      system("mv itemdef.csv itemdef.csv.temp")
      begin
        a.exec
        itemdef=ItemDefinition.from_file('itemdef.csv')
        test=DataTable.from_file('data.csv',itemdef)
        test.drill("Type"=>"wormhole").drills["Type"].should eql "wormhole"
        test.drill("Type"=>"wormhole").data["source"].should eql "Hamilton"
      ensure
        system("mv itemdef.csv.temp itemdef.csv")
      end
    end
  end
  it "should fetch data" do
    in_cat(Preexistent) do
      a=AMEEM.new("-d fetch .")
      begin
      system("mv data.csv data.csv.temp")
      a.exec
      itemdef=ItemDefinition.from_file('itemdef.csv')
      test=DataTable.from_file('data.csv',itemdef)
      test.drill("Type"=>"wormhole").drills["Type"].should eql "wormhole"
      test.drill("Type"=>"wormhole").data["source"].should eql "Hamilton"
      ensure
        system("mv data.csv.temp data.csv")
      end
    end
  end

end

