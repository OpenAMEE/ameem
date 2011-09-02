module Commit
  provides :add,:put,:validate
 

  # update amee from local file, e.g. data.csv or other file as configured by switches,
  # assuming that operand doesn't already exist
  def add
    call(:post)
  end

  # update amee from local file, e.g. data.csv or other file as configured by switches
  # assuming that the operand already exists
  def put
    call(:put)
  end

  def validate
    parse_itemdef
    parse_return_values
    parse_data
    @data.validate_all
  end

  private
  
  # # update amee from local file, e.g. data.csv or other file as configured by switches
  # With mode=:post, assume operand doesn't already exist
  # with mode=:put, assume it does
  def call(mode)
    parse_itemdef
    parse_data
    parse_return_values
    amode=mode
    amode="add" if mode==:post # apitools expects add
    if options.itemdef or options.algorithm and mode==:post
      begin
        uid=find_definition_uid_by_name(itemdef.name)
        raise "#{itemdef.name} already exists"
      rescue AMEE::NotFound
      end
    end
    apitools(amode,"itemdef",target) if options.itemdef or options.algorithm
    commit_category(mode) if options.category && !testing
    apitools(amode,"data",target) if options.data
    commit_history(mode) if options.history && !testing
    commit_meta(mode) if options.meta
    commit_return_values if options.return_values
    commit_changelog if options.changelog
  end

  # add an empty category to AMEE, ready for data, after first checking that
  # it doesn't already exist, parent exists, etc...
  def commit_category(mode)
    categ=category(target)
    if itemdef
      uid=find_definition_uid_by_name(itemdef.name)
    else
      uid=''
    end
    if mode==:put
      raise "Category put not implemented."
    end
    # does the category already exist?

    begin
      self_get=api_xml(categ)
      raise 'Trying to add a category which already exists on server.'
    rescue AMEE::NotFound
    rescue AMEE::UnknownError# Workaround for T247
    end

    verbose "Confirmed does not already exist",Log4r::DEBUG

    begin
      parent_get=api_xml(File.dirname(categ))
    rescue AMEE::NotFound
      raise 'Trying to add category where parent not on server.'
    end
    
    verbose "Confirmed parent does exist",Log4r::DEBUG

    load_meta_docs
    human_name=meta['name'] || File.basename(categ).humanize
    api_xml(File.dirname(categ),
           mode,
           'newObjectType'=>'DC',
           'name'=>human_name,
           'path'=>File.basename(categ),
           'itemDefinitionUid'=>uid)
  end
end
