# Module mixing into AMEEM capabilities to investigate status of data in API,
# and/or fetch data files from API into file.
# Usage:
#
# ameem -i -d get -- log to screen and log file information regarding state of API
# ameem -i -d status -- print detailed status information regarding state of current folder on API
# ameem -i fetch -- fetch itemdef info from api to itemdef.csv
# ameem -d fetch -- fetch data info from api to data.csv
module Get
  provides :status,:fetch,:get

  # print status information for this category in amee
  def status
    get
    pp @data
    pp @itemdefuri
    pp @itemdef
  end

  # overwrite the local itemdef.csv or data.csv file from the api
  def fetch
    # invoke the download functionality of the java apitools
    apitools('fetch',"itemdef",target) if options.itemdef or options.algorithm
    if options.data
      apitools('fetch',"itemdef",target) unless File.exist?(ItemDefinitionFile)
      @itemdef = ItemDefinition.from_file(ItemDefinitionFile)
      data_from_api
      FolderTools.rename_existing_file(DataFile)    
      @data.save_csv(DataFile)
      verbose "New csv generated in #{category(target)}: \"#{DataFile}\""
    end
    fetch_return_values if options.return_values
    fetch_changelog if options.changelog
  end

    
  # load itemdefinition and/or data table from api
  def get
    # attempt to get information from AMEE into the AMEEM classes
    begin
      data=api_xml(category(target))
      itemdef=REXML::XPath.first(data,'//ItemDefinition/@uid/')
      if itemdef
        @itemdefuri=itemdef.value
      else
        @itemdefuri=nil
      end
    rescue AMEE::NotFound
      @data= "No category exists at '#{target}'. Using local itemdef to check for existence"
      parse_itemdef
      begin
        @itemdefuri=find_definition_uid_by_name(@itemdef.name) if
          options.algorithm or options.itemdef
      rescue AMEE::NotFound
        @itemdef="No itemdef for #{target}"
      end
    end
    if (@itemdefuri and (options.algorithm or options.itemdef))
      itemdefxml=admin_xml("/itemDefinitions/#{@itemdefuri}")
      @itemdef=ItemDefinition.from_xml(itemdefxml) if
        options.algorithm or options.itemdef      
    end
    parse_itemdef # because itemdef from xml is not implemented yet
    @itemdef.xml=itemdefxml
    data_from_api if options.data or options.category
  end

  private

  def data_from_api
    verbose "Fetching data for category: #{category(target)}"
    @data = DataTable.from_api(@amee, category(target), @itemdef)
    verbose "#{@data.headers.join(",")}\n",Log4r::DEBUG
    @data.items.each do |i|
        verbose "#{i.string_data.join(",")}\n",Log4r::DEBUG
      end
  end
end
