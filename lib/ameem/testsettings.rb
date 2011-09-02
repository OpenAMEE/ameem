TSFileName='testsettings.yml'

#Module providing to AMEEM functionality for turning on/off data test for a categor
#on a per-server basis
#usage:
#
#ameem -s <server> testsettings [enable|disable] <target>
module TestSettings
  provides :testsettings

  # modify the testsettings.yml file to enable or disable tests on a given server
  def testsettings
    file=File.join(File.expand_path(File.join(options.root,target)),TSFileName)
    @server=options.server_url.split('.').first.downcase
    @tsdata = File.exists?(file) ? YAML.load_file(file) : {@server => { 'enabled' => true } }
    @tsdata[@server]||={}
    if respond_to?(options.action)
      method(options.action).call
    else
      raise "AMEEM cannot handle testsettings command "+options.action
    end
    File.open( file, 'w' ) do |out|
      YAML.dump( @tsdata, out )
    end
  end

  # Enable tests for given server
  def enable
    @tsdata[@server]['enabled']=true
  end

  # disable tests for given server
  def disable
    @tsdata[@server]['enabled']=false
  end
end