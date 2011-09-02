#!/usr/bin/env ruby

require 'net/http'

SERVER = ARGV[0]
USERNAME = ARGV[1]
PASSWORD = ARGV[2]
REPEAT = ARGV[3].to_i

tests = [
  "/data",
  "/data/metadata/actonco2/actions",
  "/data/transport/car/specific/drill",
  "/profiles"
]

url = URI.parse("http://#{SERVER}")

tests.each do |path|

  timings = []
  
  puts "--- testing GET #{path}"
  req = Net::HTTP::Get.new(path)
  req.basic_auth USERNAME, PASSWORD
  req['Accept'] = "application/xml"
  response = nil
  Net::HTTP.new(url.host, url.port).start do |http|
    REPEAT.times do
      start_time = Time.now
      response = http.request(req)
      end_time = Time.now
      time = end_time - start_time
      timings << time
    end
  end
  
  # Get a profile UID for further tests if this is the result of GET /profiles
  if path == "/profiles"
    uid = response.body.match(/<Profile .*? uid="(.*?)">/)[1]
    tests << "/profiles/#{uid}"
    tests << "/profiles/#{uid}?recurse=true"
    tests << "/profiles/#{uid}/metadata"
    tests << "/profiles/#{uid}/home/energy/quantity"
  end
  
  timings.sort!
  total = timings.inject {|sum, n| sum + n }

  puts "#{REPEAT} requests performed"
  puts "min: #{timings.first}s"
  puts "avg: #{total / REPEAT}s"
  puts "med: #{timings[REPEAT/2]}s"
  puts "max: #{timings.last}s"
  puts "# over 0.2s: #{timings.select{|x|x>0.2}.length}"
end
