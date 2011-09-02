require 'rubygems'
require 'amee'

# Connect to AMEE instance
amee = AMEE::Connection.new(ARGV[0], ARGV[1], ARGV[2], :format => :xml, :enable_debug => false)
amee.authenticate
# Load file
list = File.open(ARGV[3])
list.each_line do |line|
  profile = line.split(',')[0]
  profile.delete!('"')
  profile.strip!
  # Delete
  AMEE::Profile::Profile.delete(amee, profile)
  puts "deleted #{profile}"
end