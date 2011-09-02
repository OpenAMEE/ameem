
Licenses='licenses/licenses.yml'
LicenseItemdef=ItemValueDefinition.new([
  'License','license','TEXT','true','false',nil,nil,nil,nil
  ])

# Module providing functionality for adding a "license" to a data category
# This is currently not used, but the usage is as follows:
#
# ameem license <code>
#
# Sets the 'license' data item value for all rows in the data.csv
# to the URL specified for the code in the 'licenses' file (location as configured in ameem.yml, usually ~/.ameem/licenses)
# adding the column if necessary
module License
  provides :license

  # set license for this category in itemdef and data files
  def license
    license_link=find_license_text(options.license)
    parse_itemdef
    parse_data
    ensure_itemdef_has_license_field
    ensure_data_has_license_column(license_link)
    save_itemdef
    save_data
    data.validate_all
  end

  # lookup the license text for the given abbreviated license name
  def find_license_text(terms)
    #load yml file
    licenses = YAML.load_file(File.join(options.config_folder,Licenses))
    # hash out from the terms supplied
    begin
      CGI.escapeHTML licenses[terms]['license_link']
    rescue
      raise "No such license as #{terms}. Available licenses: #{licenses.keys.inject(){|x,y| x+', '+y}}"
    end
  end

  def ensure_itemdef_has_license_field
    unless itemdef.items['license']
      verbose 'Adding license line to item definition',Log4r::DEBUG
      itemdef.add_item_value_definition(LicenseItemdef)
    end
  end

  # see if there's a license column
  # if there isn't,
  # add the header
  # and the license text to each column
  def ensure_data_has_license_column(license_link)

    if data.headers.include?('license')
      verbose "Replacing licence information #{license_link}",Log4r::DEBUG
        data.replace_column('license',license_link)
    else
      verbose "Adding license to data file #{license_link}",Log4r::DEBUG
      data.add_column('license',license_link)
    end
  end
end
