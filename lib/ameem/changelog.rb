# Module providing changelog functionality to AMEEM, uploading changelog information to AMEE
#
# Usage:
# ameem --changelog put : upload from changelog.yml to API
# ameem --changelog fetch : download info in API to changelog.yml
module Changelog

  # Upload changelog.yml to API
  def commit_changelog
    # Load category
    categ=category(target)
    cat = AMEE::Data::Category.get(amee, "/data#{categ}")
    # Clear history
    cat.meta.history.clear
    # Load history from file
    begin
      history = YAML.load_file(File.join('.', target, 'changelog.yml'))
      history.each_pair do |date, message|
        cat.meta.history <<
          AMEE::Data::Category::History::Changeset.new(Date.parse(date), message)
      end
    rescue
      puts 'No changelog file found. History will be empty.'
    end
    # Store in platform
    cat.putmeta
  end

  # Download info from API to changelog.yml
  # creating an appropriate skeleton changelog if history is empty on server
  def fetch_changelog
    categ=category(target)
    cat = AMEE::Data::Category.get(amee, "/data#{categ}")
    if cat.meta.history.empty?
      cat.meta.history <<
        AMEE::Data::Category::History::Changeset.new(cat.created.to_date, 'Created')
      if cat.modified != cat.created
        cat.meta.history <<
          AMEE::Data::Category::History::Changeset.new(cat.modified.to_date, 'Modified')
      end
    end
    history = {}
    cat.meta.history.each do |changeset|
      history[changeset.date.to_s] = changeset.message.to_s
    end
    File.open(File.join('.', target, 'changelog.yml'), 'w') do |f|
      f.write history.to_yaml
    end
  end

end
