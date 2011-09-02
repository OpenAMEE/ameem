require File.expand_path(File.dirname(__FILE__) + '/spec_helper')
describe Meta, 'regarding usages' do
  Compulsory='compulsory'
  Forbidden='forbidden'
  Ignored='ignored'
  Optional='optional'
  def prepare_mock(ivds)
    @itemdef=flexmock do |m|
      m.should_receive('profile.keys').and_return(ivds)
    end
  end
  def t(ivds,usages)
    prepare_mock(ivds)
    Meta.validate_and_complete_usages(usages,@itemdef)
  end
  it "should parse empty usage to default" do
    t(['one'],nil).should eql 'default'=>{'one'=>Compulsory},'order'=>['default']
    t(['one','two'],nil).should eql 'default'=>
      {'one'=>Compulsory,'two'=>Compulsory},'order'=>['default']
  end
  it "should parse missing usages to compulsory" do
    t(['one','two'],'a'=>{'two'=>Optional}).should eql 'a'=>
      {'one'=>Compulsory,'two'=>Optional},'order'=>['a']
  end
  it "should raise error when usage references nonexistent itemdef" do
     lambda{t(['one'],'a'=>{'two'=>Optional})}.should raise_error /two/
  end
  it "should raise error when usage references invalid role" do
     lambda{t(['one'],'a'=>{'one'=>'fnord'})}.should raise_error /fnord/
  end
  it "should raise error when a usage is called order" do
     lambda{t(['one'],'order'=>{'one'=>Optional})}.should raise_error /Order/
  end
  it "should raise error when order doesn't match usage list" do
     lambda{t(['one'],'a'=>{'one'=>Optional},
         'order'=>['a','b'])}.should raise_error /b/
     lambda{t(['one'],'a'=>{'one'=>Optional},'b'=>{'one'=>Compulsory},
         'order'=>['a'])}.should raise_error /b/
  end
  it "should raise error when a usage is called unrestricted" do
     lambda{t(['one'],'unrestricted'=>Forbidden)}.should raise_error /unrestricted/
  end
  it "should order usages by number of compulsories" do
    t(['one','two','three'],
      'c'=>nil,
      'a'=>{'two'=>Optional,'three'=>Forbidden},
      'b'=>{'two'=>Compulsory,'three'=>Ignored}
    )['order'].should eql ['a','b','c']
  end
  it "should respect a supplied order" do
    t(['one','two','three'],
      'c'=>nil,
      'a'=>{'two'=>Optional,'three'=>Forbidden},
      'b'=>{'two'=>Compulsory,'three'=>Forbidden},
      'order'=>['b','a','c']
    )['order'].should eql ['b','a','c']
  end
end