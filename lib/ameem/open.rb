# Module providing interaction between ameem and browser (e.g. firefox)
#
# Usage:
# * ameem -c open : Open the data category in B&O
# * ameem -i open : Open the itemdef in B&O
# * ameem -m preview : Open a preview page of the discover documentation
module Open
  provides :open,:preview

  # open the data or itemdef in the black and orange interface
  def open
    user_open_url(category(target)) if options.data or options.category
    if options.itemdef or options.algorithm
      parse_itemdef
      uid=find_definition_uid_by_name(itemdef.name)
    end
    admin_open_itemdef(uid) if options.itemdef or options.algorithm
  end

  # Open the given path in the browser
  def open_url(path)
    print "\nOpening #{path}\n"
    call_browser(path)
  end

  # Given an itemdef UID, get the URL in the black and orange interface
  def admin_open_itemdef(uid)
    open_url("http://#{amee_admin.server}/admin/itemDefinitions/#{uid}")
  end

  # Given a data category path, get the URL in the black and orange interface
  def user_open_url(path)
    open_url("http://#{amee.server}/data#{path}")
  end

  def call_browser(path)
    `firefox "#{path}" &`
  end

  # preview the documentation in discover format
  def preview
    preview_html=''
    preview_html = preview_documentation if options.meta # defined in meta.rb
    call_browser("data:text/html,#{CGI::escape(preview_html).gsub(/\+/,'%20')}")
  end

end
