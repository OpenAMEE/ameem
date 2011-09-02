require File.dirname(__FILE__) + '/spec_helper.rb'

describe History do
  it 'should read and understand the history data files' do
    in_cat(Historical) do
      a=AMEEM.new("-y #{TestVerbosity} put . ")
      folder=File.join(File.expand_path(a.target),History::HistoryFolder)
      files=History::HistoryDataFile.enumerate(folder)
      files.length.should eql 2
      files[0].should be_a History::HistoryDataFile
      albanias=files.select{|x| x.drills=="country=Albania"}
      albanias.length.should eql 1
      albania=albanias[0]
      albania.item_path.should eql "massCO2PerEnergy"
      albania.series(0).length.should eql 15
      albania.series(0)[0][0].should eql DateTime.new(1992)
      albania.series(0)[0][1].should eql '51.42'
    end
  end
  it 'should set and reset history for a category' do
    in_cat(Historical) do
      begin
        a=AMEEM.new("-y #{TestVerbosity} put . ")
        testcategorypath="/data#{a.category(a.target)}/"
        testitempath="massCO2PerEnergy"
        uid = AMEE::Data::DrillDown.get(a.amee, "#{testcategorypath}drill?country=Albania").data_item_uid
        hist = AMEE::Data::ItemValueHistory.get(a.amee, "#{testcategorypath}#{uid}/#{testitempath}")
        hist.series.should eql [[AMEE::Epoch,0.0324402]]
        a.exec
        newhist = AMEE::Data::ItemValueHistory.get(a.amee, "#{testcategorypath}#{uid}/#{testitempath}")
        newhist.series.should_not eql [[AMEE::Epoch,0.0324402]]
        newhist.series.length.should eql 15
        [['Albania','2.0324402'],['Algeria','2.6881182']].each { |country|
          uid = AMEE::Data::DrillDown.get(a.amee, "#{testcategorypath}drill?country=#{country[0]}").data_item_uid
          hist = AMEE::Data::ItemValueHistory.get(a.amee, "#{testcategorypath}#{uid}/#{testitempath}")
          hist.series.length.should_not eql 1
          hist.series=[[AMEE::Epoch,country[1]]]
          hist.save! # later we could implement an explicit ameem -y delete, but not now
          newhist = AMEE::Data::ItemValueHistory.get(a.amee, "#{testcategorypath}#{uid}/#{testitempath}")
          newhist.series.should eql [[AMEE::Epoch,country[1].to_f]]
        }
      ensure
        # Regardless of earlier fails, try as hard as you can to leave the history unchanged
        [['Albania','0.0324402'],['Algeria','0.6881182']].each { |country|
          uid = AMEE::Data::DrillDown.get(a.amee, "#{testcategorypath}drill?country=#{country[0]}").data_item_uid
          reset = AMEE::Data::ItemValueHistory.get(a.amee, "#{testcategorypath}#{uid}/#{testitempath}")
          reset.series=[[AMEE::Epoch,country[1]]]
          reset.save! # later we could implement an explicit ameem -y delete, but not now
        }
      end
    end
  end
  it 'should not allow history upload if algo doesnt contain integrate' do
    in_cat(Historical) do
      begin
        system("cp default.js default.js.bak")
        system("cp default.js.nohist default.js")
        a=AMEEM.new("-y #{TestVerbosity} put . ")  
        lambda{a.exec}.should raise_error /integrate/
      ensure
        system("cp default.js.bak default.js")
      end
    end
  end
end
