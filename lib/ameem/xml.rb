require 'rexml/document'

AMEE::Connection.send :define_method, :server do
  @server
end

# Module providing interactions with AMEE API to delete/fetch XML information from API
module AMEEMXML

  # communicate directly with the specified AMEE API URL, ignoring rubygem models
  # returns the XML document
  def xml_message(connection,path,method=:get,opts={}) 
    verbose "Attempting #{method} on #{connection.server}#{path}",Log4r::DEBUG
    case method
    when :get
      rawxml=connection.get(path,opts) 
    when :post
      rawxml=connection.post(path,opts) if not testing
    when :put
      rawxml=connection.put(path,opts) if not testing
    when :delete
      # extra safety against bugs which could delete large subtrees
      # refuse to delete path without at least two steps
      path.scan(/\//).length>3 || connection. server=~/admin/ or raise "Drastic deletion #{path}: AMEEM not suitable."
      if !options.force
        print "Are you really sure you want to delete #{path} on #{connection.server}?[No]\n"
        raise "Confirmation failed" if !($stdin.gets=~/[yY]/)
      end      
      verbose "Sending delete request"
      rawxml=connection.delete(path) if not testing
    else
      raise 'Unknown method'
    end
    verbose "XML return:\n#{rawxml.body}",Log4r::DEBUG
    return REXML::Document.new(rawxml.body)
  end

  # communicate with main AMEE instance, returning XML document
  def api_xml(path,method=:get,options={})
    xml_message(amee,"/data"+path,method,options)
  end

  # communicate with Admin AMEE instance, returning XML document
  def admin_xml(path,method=:get,options={})
    xml_message(amee_admin,"/admin#{path}",method,options)
  end

  # page through the results from the admin api
  # yields each page as an XML document to supplied block
  def admin_xml_each_page(path)
    first_page=admin_xml(path)
    pages=REXML::XPath.first(first_page,'//LastPage/text()').value.to_i
    (1..pages).each do |page|
      result=admin_xml("#{path}?page=#{page}")
      yield result
    end
  end

  #Return the UID of AMEE Item definition of configured target, only if
  #the supplied name matches the found IVD name, otherwise return nil
  def find_definition_uid_via_category(name)
    begin
      cat=AMEE::Data::Category.get(amee,"/data#{category(target)}")
    rescue AMEE::NotFound, AMEEM::Exceptions::Location => err
      verbose "While looking for category to get itemdef #{err}",Log4r::DEBUG
    end
    if cat&&cat.itemdef&&cat.item_definition.name==name
      verbose "Found item definition via category",Log4r::INFO
      return cat.itemdef
    else
      return nil
    end
  end

  # Return the UID of the AMEE Item definition given the IVD name
  def find_definition_uid_by_name(name)
    # first, try a guess based on the category
    uid=find_definition_uid_via_category(name)
    return uid if uid
    verbose "Looking up definition for #{name}",Log4r::DEBUG
    admin_xml_each_page("/itemDefinitions") do |doc|
      uid=REXML::XPath.first(doc,"//ItemDefinition[Name='#{name.strip}']/@uid")
      if uid
        return uid.value
      end
    end
    raise AMEE::NotFound.new("No such itemdef as #{name}")
  end  
end
