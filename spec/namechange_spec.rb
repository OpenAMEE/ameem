require File.dirname(__FILE__) + '/spec_helper.rb'

describe Namechange do
  it "should change category human-name" do
    in_cat(Preexistent) do
      begin
        a=AMEEM.new("--verbose error  -c namechange . ")
        $tlog.debug "/data#{Preexistent}"
        cat=AMEE::Data::Category.get(a.amee,"/data#{Preexistent}")
        $tlog.debug cat.uid
        uid=cat.uid
        # the fixture meta.yml file is set up with a :name different from that in
        # the db, so that this execution will actually change the name
        a.exec
        cat=AMEE::Data::Category.get(a.amee,"/data#{Preexistent}")
        cat.path.should eql '/test/jh/ameem/preexistent'
        cat.name.should eql 'New name'
        cat.uid.should eql uid
        AMEE::Data::Category.update(a.amee,"/data#{Preexistent}",:name=>'Preexistent',:path=>'preexistent',:get_item=>false)
        cat=AMEE::Data::Category.get(a.amee,"/data#{Preexistent}")
        cat.path.should eql Preexistent
        cat.name.should eql 'Preexistent'
        cat.uid.should eql uid
      ensure
        # Duplicate the cleanup part of the test as an ensure block for safety
        AMEE::Data::Category.update(a.amee,"/data#{Preexistent}",:name=>'Preexistent',:path=>'preexistent',:get_item=>false)
      end
    end
  end
  it "should change category path" do
    in_cat(Preexistent) do
      begin
        a=AMEEM.new("--verbose error  -c pathchange new_name . ")
        $tlog.debug "/data#{Preexistent}"
        cat=AMEE::Data::Category.get(a.amee,"/data#{Preexistent}")
        $tlog.debug cat.uid
        uid=cat.uid
        a.exec
        cat=AMEE::Data::Category.get(a.amee,"/data/test/jh/ameem/new_name")
        cat.path.should eql '/test/jh/ameem/new_name'
        cat.name.should eql 'New name'
        cat.uid.should eql uid
        AMEE::Data::Category.update(a.amee,"/data/test/jh/ameem/new_name",:name=>'Preexistent',:path=>'preexistent',:get_item=>false)
        cat=AMEE::Data::Category.get(a.amee,"/data#{Preexistent}")
        cat.path.should eql Preexistent
        cat.name.should eql 'Preexistent'
        cat.uid.should eql uid
      ensure
        # Duplicate the cleanup part of the test as an ensure block for safety
        # If the test succeeded, then it won't be at the new name
        begin
          AMEE::Data::Category.update(a.amee,"/data/test/jh/ameem/new_name",:name=>'Preexistent',:path=>'preexistent',:get_item=>false)
        rescue AMEE::NotFound,AMEE::UnknownError
        end
      end
    end
  end
  it "should change itemdef name" do
    pending "PL-3297 for itemdef cluster invalidation"
    in_cat(Preexistent) do
      begin
        a=AMEEM.new("--verbose error  -i pathchange new_name . ")
        uid=a.find_definition_uid_by_name('AMEEM Prexistent Test Itemdef')
        AMEE::Admin::ItemDefinition.load(a.amee,uid).name.should eql 'AMEEM Prexistent Test Itemdef'
        a.exec
        AMEE::Admin::ItemDefinition.load(a.amee,uid).name.should eql 'new_name'
        AMEE::Admin::ItemDefinition.update(a.amee,"/definitions/itemDefinitions/#{uid}",:name=>'AMEEM Prexistent Test Itemdef')
        AMEE::Admin::ItemDefinition.load(a.amee,uid).name.should eql 'AMEEM Prexistent Test Itemdef'
      ensure
        # Duplicate the cleanup part of the test as an ensure block for safety
        AMEE::Admin::ItemDefinition.update(a.amee,"/definitions/itemDefinitions/#{uid}",:name=>'AMEEM Prexistent Test Itemdef')
      end
    end
  end
end
