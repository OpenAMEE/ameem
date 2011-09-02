require 'time'
require 'thread'
require 'monitor'

require 'rubygems'
require 'mysql'

$num_threads = 1

$from = { :server => "db1",
          :database => "carbon",
          :table => ARGV[1],
          :username => "",
          :password => "" }
$to = {   :server => "db1",
          :database => "amee",
          :table => ARGV[1],
          :username => "",
          :password => "" }

def transfer(old_connection, old_uid, new_connection, new_uid)
  starttime = Time.now
  # Get dates from old item
  q = old_connection.prepare("SELECT created, modified FROM #{$from[:table]} WHERE UID = ?")
  q.execute(old_uid)
  old_timestamps = q.fetch
  if old_timestamps
    # Set dates in new item
    q = new_connection.prepare("UPDATE #{$to[:table]} SET created=?, 
modified=? WHERE UID = ?")
    q.execute(old_timestamps[0], old_timestamps[1], new_uid)
    # Done
    puts "Transferred #{old_uid} to #{new_uid} in #{Time.now - starttime} seconds"
  end
end

def transfer_thread
  # Make MySQL connections
  from = Mysql.new($from[:server], $from[:username], $from[:password], $from[:database])
  to = Mysql.new($to[:server], $to[:username], $to[:password], $to[:database])
  # Process
  while (uids = $queue.deq)
    # Transfer
    transfer(from, uids[0], to, uids[1])
  end
  puts "Thread finishing"
end

puts "Parsing input file"
$queue = Queue.new
input_file = File.open(ARGV[0])
input_file.each_line do |uids|
  uids = uids.split(',').first(2)
  uids.each {|x| x.delete!('"'); x.strip!}
  if uids.all? { |x| x.match(/[A-F0-9]{12}/) }
    $queue.enq uids
  end
end
# Add termination signals to queue
$num_threads.times { $queue.enq nil }

puts "Starting transfer processes - #{$num_threads} concurrent connections"
threads = []
$num_threads.times do
  threads << Thread.new {
    transfer_thread
  }
end
threads.each {|t| t.join }

puts "Transfer complete"
