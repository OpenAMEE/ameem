$ameem_location= File.expand_path(File.dirname(File.dirname(File.dirname(__FILE__))))

#Provide to AMEEM the capability of determining folder in apicsvs based on the
# API path
module FolderTools

  # the root of the api_csvs tree for this install
  def csv_root
    options.csv_root
  end

  # the install location of ameem
  def ameem_location
    $ameem_location
  end

  # find the category name corresponding to the given file path
  # e.g. if input is /home/me/devel/amee/transport/car/generic, output is
  # tranport/car/generic
  def find_category(file)
    file.match(Regexp.new(Regexp.escape(csv_root))) or 
      raise AMEEM::Exceptions::Location.new("#{file} is not in CSV tree")
    file.sub(Regexp.new(Regexp.escape(csv_root)),"");
  end

  # supply a file path for the given file relative to the current root of ameem operations
  # e.g. if root is set to current folder (.), and are standing in
  # ~/devel/amee/apicsvs/transport/car, input: ~/devel/amee/apicsvs/transport/car/generic,
  # output transport/car/generic
  def relative_to_root(file)
    file.sub(Regexp.new(Regexp.escape(options.root)),"");
  end

  # create an absolute file path for a path given relative to the current root of ameem operations
  # e.g. if root is set to current folder (.) , and are standing in
  # ~/devel/amee/apicsvs/transport/car
  # input: generic, output: /home/me/devel/amee/apicsvs/transport/car/generic
  def based_on_root(rel_path)
    File.expand_path(File.join(options.root,rel_path))
  end

  # return api category path, based on path relative to current ameem root
  # e.g. when standing in ~/devel/amee/apicsvs/transport/car,
  # input: generic, output: transport/car/generic
  def category(rel_path)
    find_category based_on_root rel_path
  end

  # robustly rename an existing file for safe overwrite
  def self.rename_existing_file(file_name)
    if File.exist?(file_name)
      n = 1
      while File.exist?("#{file_name}.old.#{n}")
        n += 1
      end
      (1...n).reverse_each do |n|
        File.rename("#{file_name}.old.#{n}", "#{file_name}.old.#{n + 1}")
      end
      File.rename("#{file_name}", "#{file_name}.old.1")
    end
  end
end
