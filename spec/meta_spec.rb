require File.dirname(__FILE__) + '/spec_helper.rb'

describe Meta do
  it "should put the metadata" do
    in_cat(Preexistent) do
      a=flexmock(AMEEM.new("-m #{TestVerbosity} -s sci  put . "))
      a.options.discover_user='me'
      a.options.discover_password='mine'
      a.should_receive(:find_definition_uid_via_category).and_return nil
      flexmock(AMEE::Data::Category).new_instances do |m|
        m.should_receive(:putmeta).
          with(
          :tags=>['scifi','test','fictional'],
          :provenance=>"Various novels and one scientific paper",
          :authority=>"tentative",
          :wikiName=> 'AMEEM_Teleporter_Example',
          :wikiDoc=> 'Example wikicreole documentation'
        ).once
        m.should_receive(:item_definition).and_return(
          flexmock do |itemdef|
            itemdef.should_receive(:usages=).with(['default']).once
            itemdef.should_receive(:save!).once
          end
        )
      end
      flexmock(AMEE::Admin::ItemValueDefinitionList).should_receive(:new).and_return(
        [flexmock { |mock|
            mock.should_receive('meta.wikidoc=').with('Mass of carbon per distance')
            mock.should_receive(:putmeta).once
            mock.should_receive(:path).and_return('kgCO2PerKm')
            mock.should_receive(:clear_usages!).once}, #Test asserting bug - no need for clear usages on DI
          # Not a PI, so no usage expected,
          flexmock { |mock| mock.should_receive(:putmeta).once
            mock.should_receive('meta.wikidoc=').with('Distance travelled per month')
            mock.should_receive(:path).and_return('parsecsPerMonth')
            mock.should_receive(:clear_usages!).once #Each new instance (of 3) should receive 1 time.
            mock.should_receive(:set_usage_type).with('default',:compulsory).once},
          flexmock { |mock| mock.should_receive(:putmeta).once
            mock.should_receive('meta.wikidoc=').with("Type of teleporter")
            mock.should_receive(:path).and_return('Type')
            mock.should_receive(:clear_usages!).once} #Test asserting bug - no need for clear usages on drill
          # Not a PI, so no usage expected,
        ]
      )
      flexmock(AMEE::Data::Item).new_instances.should_receive(:putmeta).
        with(:wikiDoc=> "A note").once
      m=flexmock(Net::HTTP).new_instances
      m.should_receive(:request).with(FlexMock.on{ |request|
          request.class==Net::HTTP::Post &&
            request.path=='/path/test/jh/ameem/preexistent/update' &&
            request['authorization']=="Basic bWU6bWluZQ==" &&
            request.body.include?("category%5bgallery%5d=Some%20gallery%20text")&&
            request.body.include?("&category%5bshow%5d=1")&&
            request.body.include?("&category%5bpath%5d=%2ftest%2fjh%2fameem%2fpreexistent")&&
            request.body.include?("&category%5bwikiname%5d=AMEEM_Teleporter_Example")&&
            request.body.include?("&category%5brelated%5d=---%20Aluminium")
        }).once
      a.options.meta.should eql true
      a.options.features={'usages'=>['sci'],'related'=>'sci'}
      a.exec
      a.meta['wikiname'].should eql "AMEEM_Teleporter_Example"
      a.creole.should eql "Example wikicreole documentation"
    end
  end
  it "should preview the metadata" do
    in_cat(Preexistent) do
      flexmock(AMEEM).new_instances.should_receive(:call_browser).
        with(/Example.*wikicreole.*documentation/).once
      a=AMEEM.new("-m #{TestVerbosity} preview . ").exec
    end
  end
  it "should check spelling" do
    copying_test_category(Preexistent) do
      original_creole=File.read('documentation.creole')
      File.open('documentation.creole','w') do |w|
        w << "My heairt is spelled wrong"
      end
      a=AMEEM.new("-m #{TestVerbosity} spellcheck . ")
      b=flexmock(a)
      b.should_receive(:verbose).with("executing spellcheck", 1)
      b.should_receive(:verbose).with(
        "Spellcheck: Possible correction for heairt: heart",Log4r::WARN)
      b.exec
      File.open('documentation.creole','w') do |w|
        w << "AMEE Loves Acronyms like IVD"
      end
      c=flexmock(a)
      c.should_receive(:verbose).with("executing spellcheck", 1)
      c.exec
      File.open('documentation.creole','w') do |w|
        w << original_creole
      end
      d=flexmock(a)
      d.should_receive(:verbose).with("executing spellcheck", 1)
      d.exec
      File.read('documentation.creole').should eql original_creole
      File.open('documentation.creole','w') do |w|
        w << "I like to 'quote strings' and to use grocer's apostrophes."
      end
      a=AMEEM.new("-m #{TestVerbosity} spellcheck . ")
      d=flexmock(a)
      d.should_receive(:verbose).with("executing spellcheck", 1)
      d.should_receive(:verbose).with(
        "Spellcheck: Possible correction for heairt: heart",Log4r::WARN)
      d.exec
    end
  end
end
