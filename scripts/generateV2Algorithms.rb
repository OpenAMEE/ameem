#!/usr/bin/ruby

# required modules
require 'csv'
require 'find'

# constants
SVN_ROOT="/home/jamespjh/devel/amee/svn.amee.com"
API_CSVS="/internal/api_csvs"
SCRIPTS_DIR="/internal/projects/apitools/scripts/"
MIGRATION_TABLE="migrationTable"
NEW_ALGORITHM="v2Algorithm.js"
ITEMDEF="itemdef.csv"

# to load the table of substitutions
class Subtable
  def initialize(path)
    @file=path
    # read in the lines
    @data=CSV.read(@file)
    @pathSubstitutions={}
    # fill the before,after values into pairs
    @data.each do |row| 
      @pathSubstitutions[row[0]]=row[1];
    end
  end
  def to_s
    result=""
    @pathSubstitutions.each do |oldPath, newPath|
      result+="#{oldPath}->#{newPath}\n"
    end
    result
  end
  def replace(input)
    # do nothing for now
    buffer=input
    @pathSubstitutions.each do |oldPath, newPath|
      oldRe=Regexp.new(Regexp.escape(oldPath))
      buffer.gsub!(oldRe,newPath)
    end    
    buffer
  end
end

# to load a table of algorithm files
# use ruby find to get the list
class FileTable
  attr_reader(:files)
  def initialize(startPath,nameFilter)
    @filter=nameFilter
    @start=startPath
    @files=[];
    Find.find(@start) do |file|
      @files.push(File.new(file)) if nameFilter.match(file)
    end
  end
  def to_s
    result=""
    @files.each do |file|
      result+=file.path+"\n"
    end
    result
  end
end

# to process a table of algorithm files and substitutions
class FileSubstituter
  def initialize(subsTable,fileList)
    @substitutions=subsTable
    @files=fileList
  end
  def process
    # for each file
    @files.each do |file|
      # generate the appropriate new name
      newName=File.join(File.dirname(file.path),NEW_ALGORITHM)
      outFile=File.new(newName,'w');
      # for each line
      file.each_line do |line|
        # make each substitution
        outFile.puts(@substitutions.replace(line))
      end
    end
  end
end
      

class LookupDatabaseName
  def initialize(fileList,output)
# look in the itemdef file in the same folder
    @files=fileList
    @outTable=File.new(output,'a')
    @outFullTable=File.new(output+"FullList",'a')
  end
  def process
    # to determine the database name for a given algorithm file
    # for each file
    @files.each do |file|
      dirname=File.dirname(file.path)
      itemdef=File.join(dirname,ITEMDEF)
      newf=File.join(dirname,NEW_ALGORITHM)
      # find the database name
      @data=CSV.read(itemdef)
      dataname=@data[0][1];
      # save them if there has been a change
      diffopt = `diff #{file.path} #{newf}`
      if diffopt!=""
        @outTable.puts(File.dirname(file.path)+","+dataname)
      end
      @outFullTable.puts(File.dirname(file.path)+","+dataname)
    end
  end
end

def processExtension(pathAddendum,filename)
# load the table of substitutions for home
subs= Subtable.new(SVN_ROOT+SCRIPTS_DIR+MIGRATION_TABLE)
# load the table of all v1 algorithm files
v1files=FileTable.new(SVN_ROOT+API_CSVS+"/"+pathAddendum,Regexp.new(Regexp.escape(filename)+"$"))
# process
substituter=FileSubstituter.new(subs,v1files.files);
substituter.process;
LookupDatabaseName.new(v1files.files,SVN_ROOT+SCRIPTS_DIR+"databaseNamesRaw").process
end

def processSubset(pathAddendum)
processExtension(pathAddendum,"perMonth.js")
processExtension(pathAddendum,"perMonth.js.migrate")
end

processSubset("home");
processSubset("transport");
