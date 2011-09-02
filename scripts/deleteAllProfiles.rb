require 'rubygems'
gem 'Floppy-amee', '>=2.0.16'
require 'amee'

users = [
  {:username=> "user", :password => "pass"}
]

users.each do |user|
  # Connect to AMEE instances with appropriate credentials
  amee = AMEE::Connection.new(ARGV[0], user[:username], user[:password], :format => :xml, :enable_debug => false)
  amee.authenticate
  # Get complete profile list
  list = AMEE::Profile::ProfileList.new(amee)
  profiles = []
  profiles += list
  while profiles.size != list.pager.items do
    list = AMEE::Profile::ProfileList.new(amee, :page => list.pager.next_page)
    profiles += list
  end
  # For each profile
  profiles.each do |profile|
    # Delete
    AMEE::Profile::Profile.delete(amee, profile.uid)
    puts "deleted #{profile.uid}"
  end
end