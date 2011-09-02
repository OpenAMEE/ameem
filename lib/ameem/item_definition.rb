require 'csv'

ValidTypes=['DECIMAL','TEXT','BOOLEAN','INTEGER']
ItemdefHeaders=['name','path','type','isDataItemValue','isDrillDown',
  'unit','perUnit','default','choices']

# model for api_csvs itemdefinition.csv file
class ItemDefinition

  # read itemdefinition.csv file
  def ItemDefinition.from_file(file_name)
    instance=ItemDefinition.new
    instance.parse_csv(file_name)
    instance
  end

  # read from xml api return
  # 
  # Stub - DOES NOT FUNCTION
  def ItemDefinition.from_xml(doc)
    instance=ItemDefinition.new
    instance.parse_xml(doc)
    instance
  end

  def initialize
    @log=Log4r::Logger['Main']
  end

  attr_accessor :name,:algorithm,:algorithm_content
  attr_accessor :items,:data,:drill,:profile,:xml,:paths

 
  # check data item validity against this itemdef
  def validate_item(data)
    items.each_value do |value|
      next if value.profile
      begin
        value.validate(data.get value.path)
      rescue => err
        raise "Parse error while parsing data #{data.string_data.join(',')}: #{err}"
      end
    end
  end

  def verbose(message,level=Log4r::INFO)
     @log.send(Log4r::LNAMES[level].downcase,message)
     @log.debug message
  end

  def validate_columns(data)
    verbose "Validating data columns"
    items.each_value do |value|
      next if value.profile
      verbose "Validating data column #{value.path}"
      column=data.map{|x| x.get value.path}
      value.validate_column(column)
    end
  end

 
  # write to api_csv data.csv file format
  def save_csv(file_name)
    CSV.open(file_name,'w') do |w|
      w<<['name',@name]
      w<<['algFile',@algorithm]
      w<<ItemdefHeaders
      paths.each do |p|
        items[p].save_csv(w)
      end
    end
  end

  # add an IVD row
  def add_item_value_definition(item)
      paths.push item.path
      items[item.path]=item
      data[item.path]=item if item.data && !item.drill
      drill[item.path]=item if item.drill
      profile[item.path]=item if item.profile
  end

  # return the headers only for data or drill, not profile
  def data_headers
    @data_headers = @paths.delete_if { |p| self.drill.keys.include?("#{p}") == false && self.data.keys.include?("#{p}") == false }
    return @data_headers
  end

  # Stub function for parsing XML from API
  # Recommend to junk this functionality and replace this with the Rubygem's model
   def parse_xml(doc)
    @xml=doc
  end

   # Read contents of csv file into this model
    def parse_csv(file_name)
    @file=CSV.read(file_name)
    @name=@file.shift[1]
    @algorithm=@file.shift[1].strip
    @algorithm_content=IO.read(File.join(File.dirname(file_name),@algorithm)) rescue nil
    @items={}
    @data={}
    @drill={}
    @profile={}
    @paths=[] # keep the order of the items by storing the paths as a sequence
    @file.shift # the headers
    @file.each do |line|
      next if line.length==0 || (line.length==1 && line[0]==nil)
      item=ItemValueDefinition.new(line)
      add_item_value_definition(item)
    end
  end

end

# model for one row of an itemdef.csv file
class ItemValueDefinition
  attr_accessor :name,:path,:type,:data,:drill,:profile
  def initialize(data)
    @name=data.shift.strip
    @path=data.shift.strip
    @type=data.shift.strip
    ValidTypes.include?(@type) or raise "Invalid type #{@type} for itemdef line #{data}, valid types are #{ValidTypes.join(',')}"
    ivd_type = data.shift.strip.downcase
    @data=['true','both'].include? ivd_type
    @profile=['false','both'].include? ivd_type
    @drill=data.shift.strip.downcase=='true'
    @unit=data.shift
    @perunit=data.shift
    @default=data.shift
    @choices=data.shift
  end

  # write this row to a writer object
  def save_csv(w)
    data_val = @data ? (@profile ? 'both' : 'true') : 'false'
    w << [@name,@path,@type,data_val,@drill,@unit,@perunit,@default,@choices]
  end

  # prettyprint as string
  def to_s
    "#{mytype} item definition: #{@path} with type #{@type} "
  end

  # return 'drill', 'profile', 'data', or 'both'
  def mytype
    return 'drill' if @drill
    if @data
      return 'both' if @profile
      return 'data'
    end
    return 'profile'
  end

  # check this is a valid api_csvs item definition
  def validate(value)
    return if !value
    return if value==''
    begin
    case @type
    when 'DECIMAL'
      Float value
    when 'INTEGER'
      Integer value
    when 'BOOLEAN'
      Boolean value
    end
    rescue
      raise "Invalid data #{value} for #{self}"
    end
  end

  def verbose(message,level=Log4r::INFO)
     @log||=Log4r::Logger['Main'] or raise 'Undefined log'
     @log.send(Log4r::LNAMES[level].downcase,message)
     @log.debug message
  end

  # check that the data doesn't fit a more restrictive item definition
  def validate_column(column)
    return if column.all?{|x| x.nil?}
    verbose "Checking for overly-relaxed item definitions",Log4r::INFO
    case @type
    when 'TEXT'
      begin
        column.each do |val|
          Float val if val
        end
        raise "All nonnull entries for #{self} validate as Floats #{column[1..5].inspect}..."
      rescue ArgumentError=>err
        verbose "As expected, error #{err} when checking for overly-relaxed itemdef",Log4r::DEBUG
      end
      begin
        column.each do |val|
          Boolean val if val
        end
        raise "All nonnull entries for #{self} validate as Booleans #{column[1..5].inspect}..."
      rescue ArgumentError =>err
        verbose "As expected, error #{err} when checking for overly-relaxed itemdef",Log4r::DEBUG
      end
    when 'FLOAT'
      begin
        column.each do |val|
          Integer val if val
        end
        raise "All nonnull entries for #{self} validate as Integers #{column[1..5].inspect}..."
      rescue ArgumentError =>err
        verbose "As expected, error #{err} when checking for overly-relaxed itemdef",Log4r::DEBUG
      end
   
    end
  end

end
