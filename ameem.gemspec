require 'rake'
$gemspec=Gem::Specification.new do |s|
  s.name = "ameem"
  s.version = "1.5.1"
  s.date = "2012-02-23"
  s.summary = "Command line tool for the amee admin API"
  s.email = "james@amee.com"
  s.homepage = "http://www.amee.com"
  s.has_rdoc = true
  s.authors = ["James Smith", "James Hetherington", "Andrew Berkeley", "Andrew Conway"]
  s.files = FileList["lib/**/*.rb", "bin/*", "[A-Z]*",
    "dist/AMEEM.jar","dist/lib/*.jar","config/*.yml"].to_a-["config/ameem.yml"]
  s.bindir="#{$root}bin"
  s.executables = ['ameem','ameem_configure']
  s.add_dependency("activesupport", "~> 2.3.5")
  s.add_dependency("amee","~>4.3.1")
  s.add_dependency("amee-internal","~>5.1.0")
  s.add_dependency("rspec_spinner")
  s.add_dependency("WikiCreole")
  s.add_dependency("log4r")
  s.add_dependency("raspell")
  s.requirements << "Via raspell, requires aspell, spellchecking tool, with headers:
        get with sudo apt-get install aspell libaspell-dev aspell-en
      "
  s.post_install_message= <<-END
    Execute sudo ameem_configure to set up your ameem install.
    Then, edit the new ~/.ameem/ameem.yml file to your preferences, or cp your old one
    Then execute ameem -h dummy . to quickly check the install is sane.
  END
end
