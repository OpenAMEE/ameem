require File.dirname(__FILE__) + '/spec_helper.rb'

describe ReturnValueDefinition do
  it 'should parse example data file' do
    in_cat(Preexistent) do
      rvd=ReturnValueDefinition.from_file(ReturnValueDefinitionFile)
      rvd.values.should have(4).items
      rvd.values.first.label.should eql 'CO2'
      rvd.values.first.unit.should eql 'kg'
      rvd.values.first.perunit.should eql 'month'
      rvd.values.first.type.should eql 'DECIMAL'
      rvd.values.first.default.should be_false
      rvd.values[1].default.should be_true
    end
  end
  it 'should parse example data file, then write to disk' do
    in_cat(Preexistent) do
      begin
        rvd=ReturnValueDefinition.from_file(ReturnValueDefinitionFile)
        rvd.to_file('rvd.temp')
        files_should_be_same(ReturnValueDefinitionFile,'rvd.temp')
      ensure
        system('rm rvd.temp')
      end
    end
  end
  it 'should upload return values to server,'+
    'deleting first as appropriate' do
    in_cat(Preexistent) do
      a=AMEEM.new("--marv #{TestVerbosity} add . ")
      a.exec
    end
  end
 
  it 'should download return values from server' do
    in_cat(Preexistent) do
      begin
        system("cp #{ReturnValueDefinitionFile} rvd.temp")
        a=AMEEM.new("--marv #{TestVerbosity} fetch . ")
        a.exec
        pending "PL-3692" do
          files_should_be_same(ReturnValueDefinitionFile,"#{ReturnValueDefinitionFile}.old.1")
        end
      ensure
        system("rm -f #{ReturnValueDefinitionFile}")
        system("rm -f #{ReturnValueDefinitionFile}.old.1")
        system("mv rvd.temp #{ReturnValueDefinitionFile}")
      end
    end
  end
end
