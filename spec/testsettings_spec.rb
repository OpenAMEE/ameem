require File.dirname(__FILE__) + '/spec_helper.rb'


describe TestSettings do
  def settings_yaml
    YAML.load_file('testsettings.yml')
  end
  it "should add a testsettings file if there isn't one" do
    copying_test_category(Unlicensed) do
      File.exist?('testsettings.yml').should be_false
      AMEEM.new("#{TestVerbosity} testsettings -s sci enable .").exec
      File.exist?('testsettings.yml').should be_true
      settings_yaml['platform-science']['enabled'].should be_true
    end
  end
  it "should add settings for the supplied platform" do
    copying_test_category(Unlicensed) do
      AMEEM.new("#{TestVerbosity} -f -s live testsettings enable .").exec
      settings_yaml['live']['enabled'].should be_true
    end
  end
  it "should add settings for a dummy platform" do
    copying_test_category(Unlicensed) do
      AMEEM.new("#{TestVerbosity} -s doc testsettings enable .").exec
      settings_yaml['doc-check']['enabled'].should be_true
    end
  end
  it "should modify existing settings for the supplied platform" do
    copying_test_category(Unlicensed) do
      AMEEM.new("#{TestVerbosity} testsettings -s sci enable .").exec
      settings_yaml['platform-science']['enabled'].should be_true
      AMEEM.new("#{TestVerbosity} testsettings -s sci disable .").exec
      settings_yaml['platform-science']['enabled'].should be_false
    end
  end
end