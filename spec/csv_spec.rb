require File.dirname(__FILE__) + '/spec_helper.rb'

describe DataTable do
  it 'should parse example data file' do
    in_cat(Teleport) do
      itemdef=ItemDefinition.from_file('itemdef.csv')
      test=DataTable.from_file('data.csv',itemdef)
      test.drill("Type"=>"wormhole").drills["Type"].should eql "wormhole"
      test.drill("Type"=>"wormhole").data["source"].should eql "Hamilton"
    end
  end
  it 'should parse example data file on commit invoke' do
    in_cat(Teleport) do
      a=AMEEM.new("#{TestVerbosity} -t -i add .")
      a.exec
      a.data.drill("Type"=>"wormhole").get("kgCO2PerKm").should eql "1.0"
    end
  end
  it 'should get correct data from platform' do
    in_cat(Preexistent) do
      a=AMEEM.new("#{TestVerbosity} dummy .")
      a.exec
      @definition = ItemDefinition.from_file(ItemDefinitionFile)
      data = DataTable.from_api(a.amee, Preexistent,@definition)
      data.headers.sort.join(",").should eql ["kgCO2PerKm",'Type','units',"source"].sort.join(",")
      data.items[0].string_data.should include '4'
      data.items[0].string_data.should include 'quantum'
      data.items[0].string_data.should include 'Bennett'
      data.items[0].string_data.should include nil
      data.items[0].string_data.length.should eql 4
     
    end
  end
  it 'should rename files correctly' do
    in_cat(Preexistent) do
      dir = Dir.new(Dir.getwd)
      FolderTools.rename_existing_file(DataFile)
      files = dir.to_a
      File.rename("data.csv.old.1", "data.csv")
      files.should include 'data.csv.old.1'
      files.should_not include 'data.csv'
    end
  end
end

describe ItemDefinition do
  it 'should parse example itemdef file' do
    in_cat(Teleport) do
      test=ItemDefinition.from_file('itemdef.csv')
      test.name.should eql "AMEEM Teleporter"
      test.algorithm.should eql "default.js"
      test.drill['Type'].type.should eql 'TEXT'
      test.data['source'].type.should eql 'TEXT'
      test.profile['parsecsPerMonth'].type.should eql 'DECIMAL'
      test.algorithm_content.should match /sec=31/
      test.data['kmPerParsec'].should be_present
      test.profile['kmPerParsec'].should be_present
    end
  end
  it 'should generate correct type strings' do
    in_cat(Teleport) do
      test=ItemDefinition.from_file('itemdef.csv')
      test.items['kmPerParsec'].mytype.should eql 'both'
      test.items['parsecsPerMonth'].mytype.should eql 'profile'
      test.items['kgCO2PerKm'].mytype.should eql 'data'
    end
  end
  it 'should flag IVD types correctly' do
    in_cat(Teleport) do
      test=ItemDefinition.from_file('itemdef.csv')
      test.items['kmPerParsec'].data.should be_true
      test.items['kmPerParsec'].profile.should be_true
      test.items['parsecsPerMonth'].data.should be_false
      test.items['parsecsPerMonth'].profile.should be_true
      test.items['kgCO2PerKm'].data.should be_true
      test.items['kgCO2PerKm'].profile.should be_false
    end
  end
  it 'should parse example itemdef file on commit invoke' do
    in_cat(Teleport) do
      a=AMEEM.new("#{TestVerbosity} -t -i add .")
      a.exec
      a.itemdef.name.should eql "AMEEM Teleporter"
    end
  end
end
