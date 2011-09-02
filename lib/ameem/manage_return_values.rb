# Module providing Return Value Definition functionality to AMEEM
#
# Usage:
#
# ameem --marv put
# 
# ameem --marv fetch
module ManagesReturnValues

  # Commit return values, as specified in return_values.csv
  def commit_return_values
    load_return_values
    @rvdlist.each do |value|
      AMEE::Admin::ReturnValueDefinition.delete(amee,@rvduid,value)
    end
    return_values.values.each do |value|
      AMEE::Admin::ReturnValueDefinition.create(
        amee,@rvduid,:type=>value.label,:valuetype=>value.type.downcase,
        :unit=>value.unit,:perUnit=>value.perunit,:default=>value.default)
    end
  end
  # Save return value definition to the return_valeus.csv file, renaming if necessary
  def fetch_return_values
    load_return_values
    csv=ReturnValueDefinition.from_amee_model(@rvdlist)
    FolderTools.rename_existing_file(ReturnValueDefinitionFile)
    csv.to_file(ReturnValueDefinitionFile)
  end
  # Load return values from API to the model
  def load_return_values
    @itemdef ||= ItemDefinition.from_file(ItemDefinitionFile)
    @rvduid=find_definition_uid_by_name(itemdef.name)
    @rvdlist=AMEE::Admin::ReturnValueDefinitionList.new(amee,@rvduid)
  end

end
