module APITools
  # Call the java apitools jar
  # Assembles the arguments to the Jar's entry method
  def apitools(command,mode,target)
    folder=category(target)
    test=options.test
    server=options.server
    admin=options.admin_url
    classpath = "-classpath '#{ameem_location}/jars/*'"
    ccommand="java #{classpath} net.dgen.apitools.DataCategory admin #{options.password} #{folder} #{command} #{mode} #{test} #{server} #{admin} #{csv_root} 2>&1"
    safecommand="java #{classpath} net.dgen.apitools.DataCategory admin ####### #{folder} #{command} #{mode} #{test} #{server} #{admin} #{csv_root}"
    verbose safecommand
    output = `#{ccommand}`
    if $?==0
      verbose "Java output '#{output}'",Log4r::DEBUG
    else
      verbose "Java error '#{output}'",Log4r::ERROR
      raise "AMEEM Java returned error #{output}"
    end
  end
end
