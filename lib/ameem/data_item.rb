require 'csv'
require 'ostruct'

# AMEEM model of an data.csv file
class DataTable

  # read from data.csv file
  def self.from_file(file_name,item_definition,force=false)
    instance=DataTable.new
    instance.parse_csv(file_name,item_definition,force)
    instance
  end

  # read from api
  def self.from_api(connection,path,definition)
    instance = DataTable.new
    instance.fetch_data(connection,path,definition)
    return instance
  end

  def initialize
  end

  attr_accessor :definition,:items,:items,:raw_headers

  # array of data headers
  def headers
    # units and source have to go last, so if we push in a new header, these go
    # at end
    @headers+['units','source']
  end
  
 
  # write a csv file
  def save_csv(file_name)
    CSV.open(file_name,'w') do |w|
      w << headers
      items.each do |item|
        item.write_csv(w)
      end
    end
  end

  # add a column to this data table, and initialise content
  def add_column(header,init=nil)
    @headers.push header
    items.each do |item|
      item.add_column(header,init)
    end
  end

  # replace column with value
  def replace_column(header,val)
    headers.include?(header) or raise "No such column as #{path}"
    items.each do |i|
      i.replace_column(header,val)
    end
  end

  # return all data items (or single data item) matching specified drill header
  def drill_once(header,value)
    result = items.select do |item|
      item.get(header)==value
    end
    result=result[0] if result.length==1
  end

  # return all data items (or single data item) matching specified drills
  # drills specified as a map, i.e {drill1 => value1, drill2 => value2}
  def drill(selection_map)
    result=items.dup
    selection_map.each do |key,value|
      result=result.select do |item|
        item.get(key)==value
      end
    end
    raise "invalid drill #{selection_map.inspect}, result: '#{result.inspect}'" if result.length!=1
    result[0]
  end

  # validate the given data table against its itemdef
  def validate
    # valid if list of headers identical to list of drills and data items combined
    # except that there may be dummy units, algorithm columns
    headers.each do |header|
       if !(definition.drill.keys+definition.data.keys+['units','algorithm','default']).include?(header)
         raise "Header missing from itemdef: #{header}, itemdef has: #{(definition.drill.keys+definition.data.keys+['units','algorithm','default']).join(',')}"
       end
    end
    (definition.drill.keys+definition.data.keys+['units']).each do |key|
      raise "Header missing from data table: #{key}" if !headers.include?(key)
    end
  end

  # validate the data table and all its data items against its itemdef
  def validate_all
    validate
    items.each do |item|
      definition.validate_item item
    end
    definition.validate_columns items
  end
  

  # Fetch data from AMEE API
  def fetch_data(connection,path,definition)
    @definition=definition
    @headers = definition.data_headers
    @headers.delete_if {|x| x=='source'}
    @raw_headers=headers # headers are the full set here
    items = []
    page = 1
    while page <= AMEE::Data::Category.get(connection, "/data#{path}").pager.last_page do
      category = AMEE::Data::Category.get(connection, "/data#{path}", { 'page' => "#{page}" })
      category.items.each do |item|
        items << item
      end
      page += 1
    end
    parse_api_rows items
  end

  # Read CSV file
  def parse_csv(file_name,item_definition,force=false)
    @file=CSV.read(file_name)
    @raw_headers=@file.shift.map {|cell| cell.to_s.strip}
    @headers=@raw_headers.select {|x| !['units','source'].include?(x)}
    @definition=item_definition
    validate if @definition unless force
    @items=[]
    @file.each do |line|
      next if line.length==0 || (line.length==1 && line[0]==nil)
      item=DataItem.new(line,definition,self)# pass the raw headers
      @definition.validate_item(item) if @definition unless force
      items.push item
    end
  end

  private

  def parse_api_rows(api_items)
    @items=api_items.map do |item|
      item_values = []
      item[:units] = nil
      headers.each { |h| item_values << item[h.to_sym] }
      DataItem.new(item_values,@definition,self) # pass the raw headers
    end
  end
end

# Model for one row in a data.csv file
class DataItem
  attr_reader :string_data,:definition,:table
  def initialize(data,definition,table)
    @string_data = data.map {|cell| cell == nil ? nil : cell.to_s.strip }
    @data = Hash[*table.raw_headers.zip(string_data).flatten]
    @definition=definition
    @table=table
  end
  def headers
    table.headers
  end
  def write_csv(writer)
    writer<<headers.map {|h| @data[h]}
  end

  #get value corresponding to given column header
  def get(value)
    @data[value] 
  end

  #get all drill values
  def drills
    array=
         @data.select do |key,value|
           definition.drill.include?(key)
         end
    Hash[*array.flatten]
  end

  # get all data vlaues
  def data
    array=
         @data.select do |key,value|
           definition.data.include?(key)
         end
    Hash[*array.flatten]
  end

  # add a data value to this item
  def add_column(path,value)
    @data[path]=value
  end

  # replace a data value for this item
  def replace_column(path,value)
    @data.keys.include?(path) or raise "No such column as #{path}"
    @data[path]=value
  end
end
