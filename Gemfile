source "http://rubygems.org"
source 'http://amee:aeC5ahx4@gems.amee.com'

gem "amee", ">= 4.3.0"
gem "amee-internal", ">= 5.1.0"
gem "WikiCreole"
gem "log4r"
gem "raspell"

group :development, :test do
  gem 'flexmock'
  gem 'rspec', '< 2.0.0'  
  gem "rspec_spinner", "= 1.1.3"
  gem 'ci_reporter'
  gem 'rb-fsevent', :require => false if RUBY_PLATFORM =~ /darwin/i  
  gem 'guard-rspec'
  gem 'guard-rake'
end