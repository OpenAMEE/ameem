# Module providing delete functionality to AMEEM
# Can delete category, data, and itemdef
# Usage:
# ameem -i delete : delete itemdef
# ameem -c delete : delete data category and data items
# ameem -d delete : delete data category and data items
# Warning: deleting itemdef while leaving a category using that itemdef is error prone
# Should perhaps trigger cascade deletion, but this doesn't work robustly in platform.
module Delete
  provides :delete

  # delete category, data, or itemdef from amee
  def delete
    api_xml(category(target),:delete) if options.data or options.category
    if options.itemdef
      parse_itemdef
      uid=find_definition_uid_by_name(itemdef.name)
      response=admin_xml("/itemDefinitions/#{uid}")
      verbose "About to delete: #{REXML::XPath.first(response,'//Name/text()').value} item definition.\n"
      admin_xml("/itemDefinitions/#{uid}",
               :delete) if itemdef.name
    end
  end
end
