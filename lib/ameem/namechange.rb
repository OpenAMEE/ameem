# Module providing functionality to change name and path of amee data category or itemdef
#
# Usage
#
# * ameem -c pathchange <newpath>
# * ameem -c namechange
#
# In both these cases, the name is changed as read from the meta.yml,
# for pathchange, a new path can also be supplied on the command line
# if there's no name in meta.yml, the humanize of the path is used
#
# * ameem -i namechange <newname> -- change the itemdef name
module Namechange
  provides :namechange,:pathchange 
  #change category or itemdef name
  def namechange
    raise "Rename both category and itemdef at once not supported" if options.itemdef && options.category
    raise "Rename must be either -c (category) or -i (itemdef)" if options.itemdef && options.category
    if options.category
      cpath=category(target)
      options.newname ||= File.basename(cpath) # might just be trying to change human name
      load_meta_docs
      human_name=meta['name'] || options.newname.humanize
      verbose "New name for #{cpath}: '#{options.newname}', '#{human_name}'",Log4r::INFO
      AMEE::Data::Category.update(amee,"/data#{cpath}",:name=>human_name,:path=>options.newname,:get_item=>false)
      verbose "If you changed the path, YOU *MUST* now git move and commit and push the category foldername manually",Log4r::WARN
    end
    if options.itemdef
       parse_itemdef
       uid=find_definition_uid_by_name(itemdef.name)
       verbose "New name for #{itemdef.name}: '#{options.newname}'",Log4r::INFO
       AMEE::Admin::ItemDefinition.update(amee,"/definitions/itemDefinitions/#{uid}",:name=>options.newname)
       verbose "YOU *MUST* now edit the itemdef.csv file to match the new name manually and svn commit",Log4r::WARN
    end
  end
  alias :pathchange :namechange
end