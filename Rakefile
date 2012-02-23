require 'rubygems'
require 'rake'
require 'spec'
require 'fileutils'
require 'spec/rake/spectask'
require 'rake/gempackagetask'

task :default=> [:test,:gem]

file "jars/AMEEM.jar"=>:ant

task :ant do
  FileUtils.cd(File.dirname __FILE__) do
    raise 'Ant task failed' if !system('ant') 
    cp 'dist/AMEEM.jar', 'jars/AMEEM.jar'
  end
end

load 'ameem.gemspec'
Rake::GemPackageTask.new($gemspec) do |s|
  s.package_dir='gems'
end

Spec::Rake::SpecTask.new(:test) do |t|
  t.pattern='spec/*_spec.rb'
end

task :test => "jars/AMEEM.jar"
