require File.dirname(__FILE__) + '/spec_helper.rb'

describe Generate do
  it 'should generate an itemdef when instructed' do
    in_cat(Empty) do
      begin
        File.exist?("itemdef.csv").should be_false
        a=AMEEM.new("generate itemdef header 'Test Exemplar'")
        a.options.command.should eql 'generate'
        a.options.generate_args.should eql ['itemdef','header','Test Exemplar']
        a.exec
        File.exist?("itemdef.csv").should be_true
      ensure
        File.delete("itemdef.csv")
      end
      File.exist?("itemdef.csv").should be_false
    end
  end
  it 'should add itemdef lines when instructed' do
    in_cat(Empty) do
      begin
        File.exist?("itemdef.csv").should be_false
        ameem "#{TestVerbosity} generate itemdef header 'Test'"
        ameem "#{TestVerbosity} generate itemdef drill TEXT oneTwo"
        ameem "#{TestVerbosity} generate itemdef profile DECIMAL threeFour"
        ameem "#{TestVerbosity} generate itemdef data DECIMAL fiveSixSeven"
        File.exist?("itemdef.csv").should be_true
      ensure
        File.delete("itemdef.csv")
      end
      File.exist?("itemdef.csv").should be_false
    end
  end
end