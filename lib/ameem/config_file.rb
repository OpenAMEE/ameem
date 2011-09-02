# Module provides to AMEEM the capability to manage a configuration file
# This can be stored at ~/.ameem, /etc/ameem/,
# or in /ameem/config in the ameem install folder
# (each checked in turn)
module AMEEMConfigFile

  # find and read the contents of the config file, and find the log file
  # see options.rb for details of all the options which can be set
  def parse_config_file
    options.config_folder=find_config_folder
    options.log_folder=find_log_folder
    options.config_file=File.join(options.config_folder,"ameem.yml")
    options.spelling_file=File.join(options.config_folder,"exceptions.yml")
    @config_file=YAML.load_file(options.config_file)
    options.apassword||=@config_file["adminpassword"]
    options.password||=@config_file["password"]
    options.user||=@config_file["username"]
    options.auser||=@config_file["adminuser"]
    options.discover_user||=@config_file["discoveruser"] or
      raise "Discover user configuration missing from #{options.config_file}"
    options.discover_password||=@config_file["discoverpassword"] or
      raise "Discover password configuration missing from #{options.config_file}"
    options.discover||=@config_file["discover"] 
    options.csv_root=@config_file["api_csvs"] || ENV['API_CSVS']
    options.features=@config_file["features"]
    options.timeout=@config_file["timeout"] || $ameem_timeout_default || 120
    options.default_server=@config_file["default_server"] || 'sci' # default server to use
    #@speller.set_option("ignore-case", "true")
    YAML.load_file(options.spelling_file).each do |word|
      @speller.add_to_session word
    end
  end

  # find the config file
  def find_config_folder
    folder_possibilities=[
      File.expand_path("~/.ameem/"),
      "/etc/ameem/",
      File.join(ameem_location,"config")
    ]
    
    folder_possibilities.detect{|x| File.exist?(File.join(x,"ameem.yml"))} or 
      raise "No ameem.yml file found in #{folder_possibilities.join(",")}"

  end

  private

  #find the log file
  def find_log_folder
    folder_possibilities=[
      File.expand_path("~/.ameem/logs"),
      "/var/log/ameem/",
      File.join(ameem_location,"logs")
    ]
    folder_possibilities.detect{|x| File.exist?(x)} or
      raise "No location found to log to, tried: #{folder_possibilities.join(",")}"
  end
end
