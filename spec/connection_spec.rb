require File.dirname(__FILE__) + '/spec_helper.rb'

describe AMEE::Connection do
  it 'should be able to connect to amee as admin' do
    a=AMEEM.new("#{TestVerbosity} dummy .")
    a.amee.authenticated?.should be_true
  end
  it 'should be able to connect to amee admin as admin' do
    a=AMEEM.new("#{TestVerbosity} dummy .")
    a.amee_admin.authenticated?.should be_true
  end
  it 'should be able to retrieve information from amee' do
    a=AMEEM.new("#{TestVerbosity} dummy .")
    a.amee.get('/data/home/energy/electricity')
    a.amee_admin.get('/admin/itemDefinitions')
  end
end

describe AMEEMXML do
  it 'should find the itemdef uid for some itemdefs' do
    check_name_round_trip('Electrical fridge_freezer')
    in_cat Preexistent do
      check_name_round_trip('AMEEM Prexistent Test Itemdef')
    end
  end
  def check_name_round_trip(name)
    a=AMEEM.new("#{TestVerbosity}  dummy .")
    uid=a.find_definition_uid_by_name(name)
    response=a.admin_xml("/itemDefinitions/#{uid}")
    REXML::XPath.first(response,"//Name/text()").value.should eql name
  end
end

describe Discover do
  
end
