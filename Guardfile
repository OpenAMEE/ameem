# A sample Guardfile
# More info at https://github.com/guard/guard#readme

guard 'rake', :task => 'ant' do
  watch(%r{^src/.+\.java$})
end

guard 'rspec', :version => 1 do
  watch(%r{^spec/.+_spec\.rb$})
  watch(%r{^lib/(.+)\.rb$})     { |m| "spec/lib/#{m[1]}_spec.rb" }
  watch('spec/spec_helper.rb')  { "spec" }
end

