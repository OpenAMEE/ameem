# To change this template, choose Tools | Templates
# and open the template in the editor.

require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Encoding" do
  it "should succesfully roundtrip a file with special char units" do   
    in_cat Utfunits do
      begin
        system("cp itemdef.csv itemdef.csv.temp")
        commit=AMEEM.new("-i -c add .")
        fetch=AMEEM.new("-i fetch .")
        delete=AMEEM.new("-i -c -f delete .")
        commit.exec       
        fetch.exec
        system("sort itemdef.csv > itemdef.csv.sort")
        system("sort itemdef.csv.temp > itemdef.csv.temp.sort")
        files_should_be_same 'itemdef.csv.sort','itemdef.csv.temp.sort'
      ensure
        system("cp itemdef.csv.temp itemdef.csv")
        delete.exec # also deletes category via cascade
      end
    end
  end
end

