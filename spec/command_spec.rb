require File.dirname(__FILE__) + '/spec_helper.rb'

class AMEEM
  provides :callcat
  def callcat
    AMEEM.withcategory(category(target))
  end
end

describe FolderTools do
  it 'should not change location of csv root on cd' do  
    @csv_root=AMEEM.new("#{TestVerbosity} dummy  .").csv_root
    @ameem_location=AMEEM.new("#{TestVerbosity} dummy  .").ameem_location
    in_cat(Empty) do
      AMEEM.new("dummy .").csv_root.should eql @csv_root
      AMEEM.new("#{TestVerbosity} dummy  .").ameem_location.should eql @ameem_location
    end
  end
end

describe AMEEMOptions do 
  it 'should parse a basic dummy command correctly' do
    a=AMEEM.new("#{TestVerbosity} dummy .")
    a.options.command.should eql 'dummy'
    a.options.target.should eql ['.']
    a.exec
  end

  it 'should default to data if nothing given' do
    a=AMEEM.new("#{TestVerbosity} dummy .")
    a.options.command.should eql 'dummy'
    a.options.target.should eql ['.']
    a.options.data.should be_true
    a.options.itemdef.should be_false
  end

  it 'should not do data if something else given' do
    a=AMEEM.new("#{TestVerbosity} dummy -i .")
    a.options.command.should eql 'dummy'
    a.options.target.should eql ['.']
    a.options.data.should be_false
    a.options.itemdef.should be_true
  end

  it 'should default to current folder' do
    a=AMEEM.new("#{TestVerbosity} dummy")
    a.options.target.should eql ['.']
  end

  it 'should default to sci server' do
    a=AMEEM.new("#{TestVerbosity} dummy")
    unless a.options.default_server=='dev' # don't run this test when AMEEM-DEV test run
      a.options.server.should eql 'SCIENCE'
      a.options.server_url.should eql 'platform-science.amee.com'
      a.options.admin_url.should eql 'admin-platform-science.amee.com'
    end
  end

  it 'should have stage if specified' do
    a=AMEEM.new("-s stage #{TestVerbosity}  dummy")
    a.options.server.should eql 'STAGE'
    a.options.server_url.should eql 'stage.amee.com'
    a.options.admin_url.should eql 'admin-stage.amee.com'
  end

  it "should accept a free server" do
    a=AMEEM.new("-s platform-science.amee.com #{TestVerbosity}  dummy")
    a.options.server.should eql 'platform-science.amee.com'
    a.options.server_url.should eql 'platform-science.amee.com'
    a.options.admin_url.should eql 'admin-platform-science.amee.com'
  end

  it "should accept a free server with admin" do
    a=AMEEM.new("-s platform-science.amee.com #{TestVerbosity} --admin admin-platform-science.amee.com  dummy")
    a.options.server.should eql 'platform-science.amee.com'
    a.options.server_url.should eql 'platform-science.amee.com'
    a.options.admin_url.should eql 'admin-platform-science.amee.com'
  end

  it "should choose discover based on server" do
    a=AMEEM.new("-s sci")
    a.options.discover.should eql 'http://discover-test.amee.com'
    a=AMEEM.new(" -f -s live")
    a.options.discover.should eql 'http://discover.amee.com'
    # Dev amee server is offline, so this test is pending until then.
    #a=AMEEM.new("-s dev")
    #a.options.discover.should eql 'http://dev.discover.amee.com:3000'
  end

  it "should use internal default when ameem.yml file has no discover entry" do
    configfolder=AMEEM.new("").find_config_folder
    configfile=File.join(configfolder,'ameem.yml')
    config_file=YAML.load_file(configfile)
    configoriginal=config_file.clone
    config_file.delete_if{|k,v| k=='discover'}
    begin
      File.open(configfile, 'w' ) do |out|
        YAML.dump( config_file, out )
      end
      a=AMEEM.new("dummy -s sci")
      a.options.discover.should=='http://discover-test.amee.com'
   
    ensure
      File.open(configfile, 'w' ) do |out|
        YAML.dump( configoriginal, out )
      end
    end
  end
  it "should succeed in free server test mode request to apitools" do

    in_cat Teleport do
      a=AMEEM.new("-s platform-science.amee.com #{TestVerbosity} --admin admin-platform-science.amee.com -t -i add .")
      a.exec
    end
  end

  it 'should raise an error when supplied with an unknown command' do
    lambda{ameem "#{TestVerbosity} fibble ."}.
      should raise_error(RuntimeError,"AMEEM cannot handle command fibble")
  end

  it "should set timeout" do
    a=AMEEM.new("--timeout 40 dummy")
    a.options.timeout.should eql 40
  end
end



describe AMEEM do
  it "should recurse down through folders" do
    in_cat(RootFolder) do
      m=flexmock(AMEEM)
      [Empty,Teleport,Negative,Awkward,Nonexistent,
        ChildOfNonexistent,Preexistent,InvalidType,WrongType,
        Licensed,Unlicensed,Workspace,Teleport2,Utfunits,Abcase,
        Licenser,Historical,RootFolder,DataProfile,Relaxed,Large].each do |folder|
        m.should_receive(:withcategory).with(folder).once
      end
      a=AMEEM.new("-c -t -r #{TestVerbosity} callcat .")
      a.exec
    end
  end
  it "should recurse up through folders" do
    
    m=flexmock(AMEEM)
    m.should_receive(:withcategory).with(RootFolder).once
    m.should_receive(:withcategory).with(Teleport).once
    m.should_receive(:withcategory).with('/test/jh').once
    m.should_receive(:withcategory).with('/test').once
    a=AMEEM.new("-c -t -h -u #{TestVerbosity} callcat test/jh/ameem/teleport")
    a.exec
    
  end
  it "should support multiple targets" do
    in_cat(RootFolder) do
      m=flexmock(AMEEM)
      m.should_receive(:withcategory).with(RootFolder).once
      m.should_receive(:withcategory).with(Teleport).once
      a=AMEEM.new("-c -t #{TestVerbosity} callcat . teleport")
      a.exec
    end
  end
end
