dir = File.dirname(__FILE__)
$LOAD_PATH << dir unless $LOAD_PATH.include?(dir)
$LOAD_PATH << File.join(File.dirname(dir),'lib') unless $LOAD_PATH.include?(File.join(File.dirname(dir),'lib'))
require 'digest/md5'
require 'rubygems'
require 'rspec_spinner'
require 'ameem'
$tlog=AMEEM.new("dummy -f .").log
$ameem_timeout_default=120
Spec::Runner.configure do |config|
  config.mock_with :flexmock
end

RootFolder='/test/jh/ameem'
Empty='/test/jh/ameem/empty'
Teleport='/test/jh/ameem/teleport'
Teleport2='/test/jh/ameem/teleport2'
Awkward='/test/jh/ameem/awkward'
ChildOfNonexistent='/test/jh/ameem/nonexistent/childOfNonexistent'
Preexistent='/test/jh/ameem/preexistent'
InvalidType='/test/jh/ameem/negative/invalidtype'
WrongType='/test/jh/ameem/negative/wrongtype'
Licensed='/test/jh/ameem/license/licensed'
Unlicensed='/test/jh/ameem/license/unlicensed'
Workspace='/test/jh/ameem/workspace'
Historical='/test/jh/ameem/history'
Negative='/test/jh/ameem/negative'
Nonexistent='/test/jh/ameem/nonexistent'
Abcase='/test/jh/ameem/abcase'
Licenser='/test/jh/ameem/license'
DataProfile='/test/jh/ameem/dataprofile'
Example='/documentation/Example'
Relaxed='/test/jh/ameem/relaxed'
Utfunits='/test/jh/ameem/utfunits'
Large='/test/jh/ameem/large'

RoundTripCategories = [
  [Teleport,'AMEEM Teleporter'],
  [Awkward,'AMEEM Awkward Test'],
  [DataProfile,'AMEEM Data Profile Duality Test']
]

TestVerbosity="" # e.g. -v DEBUG

def each_test_category(set=RoundTripCategories)
  RoundTripCategories.each do |cat|
    yield(cat[0],cat[1])
  end
end

def in_cat(category)
  FileUtils.cd(AMEEM.new("dummy .").csv_root+category) do
    yield
  end
end

def files_should_be_same(candidate,reference)
  candidate_md5=Digest::MD5.hexdigest(File.read(candidate))
  reference_md5=Digest::MD5.hexdigest(File.read(reference))
  $tlog.info `diff #{candidate} #{reference}`
  candidate_md5.should eql reference_md5
end

def file_should_be_same_as_in_another_category(file,category)
  candidate=file
  reference=File.join(AMEEM.new("dummy .").csv_root,category,file)
  files_should_be_same candidate,reference
end

def copying_test_category(category)
  workspace_path=File.join(AMEEM.new("dummy .").csv_root,Workspace)
  category_path=File.join(AMEEM.new("dummy .").csv_root,category)
  system("cp #{category_path}/* #{workspace_path}")
  in_cat(Workspace) {yield}
  system("rm #{workspace_path}/*")
end
