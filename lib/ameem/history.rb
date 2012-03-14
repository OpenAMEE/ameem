# Module providing functionality for Data Item Value Histories with AMEEM
#
# Usage:
#
# ameem -y put : transfer history information from all files in divh/* to API
module History
  HistoryFolder='divh'

  # update data item history on amee from local csv files
  def commit_history(mode)
    categ=category(target)
    ensure_algorithm_has_integrate
    # enumerate the data files
    folder=File.join(File.expand_path(File.join(options.root,target)),HistoryFolder)
    files=HistoryDataFile.enumerate(folder)
    # loop over them
    files.each do |file| 
      # for each data file, get the drill sequence and iv name
      drills=file.drills
      path=file.item_path
      # find the data uid for the given drill sequence
      uid = AMEE::Data::DrillDown.get(amee, "/data#{categ}/drill?#{drills}").data_item_uid
      # load a history for the iv name
      history=AMEE::Data::ItemValueHistory.get(amee,"/data#{categ}/#{uid}/#{path}")
      # parse the data file to get the new history, inserting the orignal value
      # for epoch if there isn't one already there
      file_series=file.series(history.value_at(AMEE::Epoch).value)
      # set the history in the history object from the data file,
      history.series=file_series
      # save it back to amee
      history.save!
    end
  end
  
  #Verify that the algorithm for the folder which is having data uploaded to it
  #is compatible with DIVH
  def ensure_algorithm_has_integrate
    itemdef.algorithm_content=~/integrate/ or
      raise "Algorithm is not history-compatible.
            Include at least one invocation of integrate() in algorithm and
            reupload item definition before committing history."
  end

  # model for history data file
  class HistoryDataFile

    # list of all history data files for this category
    def self.enumerate(folder)
      Dir.entries(folder).select{|x| !(x=~/^\./)}.map{|filename|
        HistoryDataFile.new(File.join(folder,filename))
      }
    end

    def initialize(filename)
      @filename=filename
      file=CSV.open(filename,'r')
      @drills=file.shift[0]
      @item_path=file.shift[0]
      file.shift
      @series=[]
      file.each do |line|
        @series.push([DateTime.parse(line[0]),line[1]])
      end
      file.close
    end
    attr_accessor :item_path,:drills,:filename

    # valid amee data series, with default value for epoch if one not given
    def series(epochvalue)
      @series=@series.select{|x| x[1]&&x[1]!='null'} # remove points with nil values from the series
      @series.push([AMEE::Epoch,epochvalue]) unless (@series.any? {|x| x[0]==AMEE::Epoch})
      @series
    end
  end
end
