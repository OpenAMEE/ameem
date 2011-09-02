require File.dirname(__FILE__) + '/spec_helper.rb'

describe Commit do
  each_test_category do |cat,name|
    it "should run in #{cat} with test flag set" do
      in_cat(cat) do
        ameem "-c -d #{TestVerbosity} -t add . "
      end
    end
    it "should add #{cat} item defintition" do
      in_cat(cat) do
        begin
          a=AMEEM.new("-i #{TestVerbosity} add . ")
          lambda{a.find_definition_uid_by_name(name)}.
            should raise_error(AMEE::NotFound,"No such itemdef as #{name}")
          a.exec
          a.find_definition_uid_by_name(name).should be_a String
        ensure
          # Delete the IVD (not via ameem)
          AMEE::Admin::ItemDefinition.delete(a.amee,AMEE::Admin::ItemDefinition.load(a.amee,a.find_definition_uid_by_name(name)))
        end
      end
    end
    it "should add #{cat} category" do
      in_cat(cat) do
        begin
          a=AMEEM.new("-i -c #{TestVerbosity} add . ")
          lambda{a.api_xml(a.category(a.target))}.should raise_error
          a.exec
          a.options.data=true
          REXML::XPath.first(a.api_xml(a.category(a.target)),'//ItemDefinition/@uid/').
            value.should eql a.find_definition_uid_by_name(name)
        ensure
          # Delete the IVD and category (not via ameem)
          begin
            AMEE::Data::Category.delete(a.amee,"/data#{cat}")
          rescue AMEE::NotFound,AMEE::UnknownError
          end
          begin
            AMEE::Admin::ItemDefinition.delete(a.amee,AMEE::Admin::ItemDefinition.load(a.amee,a.find_definition_uid_by_name(name)))
          rescue AMEE::NotFound
          end
        end
      end
    end
    it "should add data to empty #{cat} category" do
      in_cat(cat) do
        begin
          a=AMEEM.new("-i -c -d #{TestVerbosity} add . ")
          a.exec
          a.options.data=true
          a.get
          val=((cat==Large) ? 'AAE' : 'quantum')
          REXML::XPath.first(a.api_xml(a.category(a.target)),'//DataItems/DataItem/Type/text()').
            value.should eql(val)
        ensure
          # Delete the IVD and category (not via ameem)
          begin
            AMEE::Data::Category.delete(a.amee,"/data#{cat}")
          rescue AMEE::NotFound,AMEE::UnknownError
          end
          begin
            AMEE::Admin::ItemDefinition.delete(a.amee,AMEE::Admin::ItemDefinition.load(a.amee,a.find_definition_uid_by_name(name)))
          rescue AMEE::NotFound
          end
        end
      end
    end
  end
end

describe Get, 'during roundtrip' do
  each_test_category do |cat,name|
    it "should get basic XML on request from #{cat}" do
      in_cat(cat) do
        begin      
          a=AMEEM.new "-d -i #{TestVerbosity} get ."
          AMEEM.new("-i -c -d #{TestVerbosity} add . ").exec
          a.exec
          nitems= cat==Large ? 2988 : 3
          a.data.items.should have(nitems).items
          REXML::XPath.first(a.api_xml(a.category(a.target)),'//ItemDefinition/@uid/').
            value.should eql a.find_definition_uid_by_name(name)
          $tlog.debug REXML::XPath.first(a.api_xml(a.category(a.target)),'//ItemDefinition/@uid/').value
          REXML::XPath.first(a.itemdef.xml,'//DrillDown/text()').
            value.should eql 'Type'
        ensure
          begin
            AMEE::Data::Category.delete(a.amee,"/data#{cat}")
          rescue AMEE::NotFound,AMEE::UnknownError
          end
          begin
            AMEE::Admin::ItemDefinition.delete(a.amee,AMEE::Admin::ItemDefinition.load(a.amee,a.find_definition_uid_by_name(name)))
          rescue AMEE::NotFound
          end
        end
      end
    end
  end
end

describe Delete do
  each_test_category do |cat,name|
    it "should delete data category #{cat}" do
      in_cat(cat) do
        begin         
          a=AMEEM.new "-d -f #{TestVerbosity} delete ."
          AMEEM.new("-i -c -d #{TestVerbosity} add . ").exec
          REXML::XPath.first(a.api_xml(a.category(a.target)),'//ItemDefinition/@uid/').
            value.should eql a.find_definition_uid_by_name(name)
          a.exec
          lambda{a.api_xml(a.category(a.target))}.should raise_error
        ensure
         begin
            AMEE::Data::Category.delete(a.amee,"/data#{cat}")
          rescue AMEE::NotFound,AMEE::UnknownError
          end
          begin
            AMEE::Admin::ItemDefinition.delete(a.amee,AMEE::Admin::ItemDefinition.load(a.amee,a.find_definition_uid_by_name(name)))
          rescue AMEE::NotFound
          end
        end
      end
    end
    it "should delete the item definition #{cat}" do
      in_cat(cat) do
        begin         
          a=AMEEM.new "-i -f #{TestVerbosity} delete ."
          AMEEM.new("-i #{TestVerbosity} add . ").exec
          a.find_definition_uid_by_name(name).should be_a String
          a.exec
          lambda{a.find_definition_uid_by_name(name)}.
            should raise_error(AMEE::NotFound,"No such itemdef as #{name}")
        ensure
          begin
            AMEE::Data::Category.delete(a.amee,"/data#{cat}")
          rescue AMEE::NotFound,AMEE::UnknownError
          end
          begin
            AMEE::Admin::ItemDefinition.delete(a.amee,AMEE::Admin::ItemDefinition.load(a.amee,a.find_definition_uid_by_name(name)))
          rescue AMEE::NotFound
          end
        end
      end
    end
  end
end