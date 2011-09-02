require 'wiki_creole'
require 'erb'

# Module providing to ameem functionality to handle various pieces of metadata
# * ameem -m put : commit documentation, wikiname, usages, authority etc to API
# * ameem spellcheck : check spelling of documentation file
# * ameem -m preview : open in browser, a preview of how doc will look in discover
module Meta
  CreoleFile='documentation.creole'
  MetaFile='meta.yml'
  Compulsory='compulsory'
  Forbidden='forbidden'
  Ignored='ignored'
  Optional='optional'
  Usages=[Optional,Compulsory,Forbidden,Ignored]
  provides :spellcheck

  # set API v3 metadata based on ameem.yml file
  def commit_meta(mode)   
    begin
      load_meta_docs
      do_spellcheck
      categ=category(target)
      

      cat = AMEE::Data::Category.get(amee, "/data#{categ}")
      cat.putmeta(
        :wikiDoc=>creole,
        :wikiName=>canonize_with_warning(meta['wikiname'],'wikiname'),
        :authority=>meta['authority'],
        :provenance=>meta['provenance'],
        #:show=>meta['show'],
        #:gallery=>meta['gallery'],
        :tags=>meta['tags'] ? meta['tags'].map{|x| canonize_with_warning x,'tag'} : nil
        #:related
      )

      if itemdef
        meta['usages']=Meta.validate_and_complete_usages(meta['usages'],itemdef)
        amee_itemdef = cat.item_definition
        amee_itemdef.usages = meta['usages']['order']
        amee_itemdef.save!

        itemdefuid=find_definition_uid_by_name(itemdef.name)
        itemdefs=AMEE::Admin::ItemValueDefinitionList.new(amee,itemdefuid)
        meta['ivds'].each do |meta_ivd|
          ivd=itemdefs.detect{|amee_ivd| amee_ivd.path==meta_ivd[0]}
          ivd or raise AMEE::UnknownError.new("Path specified for itemdef note #{meta_ivd[0]} doesn't match one in ivd")
          ivd.meta.wikidoc = meta_ivd[1]
          Meta.store_ivd_usages(ivd, meta['usages'])
          ivd.putmeta
        end
        if meta['data_items']
          meta['data_items'].each do |item|
            drills=item['drills'].map{|x| "#{CGI::escape(x[0])}=#{CGI::escape(x[1])}"}.join('&')
            drilldown=AMEE::Data::DrillDown.get(amee, "/data#{categ}/drill?#{drills}")
            verbose "Meta drill: /data#{categ}/drill?#{drills}"
            drilldown.choice_name == "uid" or raise AMEE::UnknownError.new("Nonunique drill for data note #{item}")
            apiitem=AMEE::Data::Item.get(amee,"/data#{categ}/#{drilldown.data_item_uid}")
            apiitem.putmeta(:wikiDoc=>item['doc'])
          end
        end
      end
      begin
        data={"category[show]"=>meta['show'],
          "category[gallery]"=>meta['gallery'],
          "category[wikiname]"=>meta['wikiname'],
          "category[path]"=>categ == '' ? '/' : categ}
        data["category[related]"]=
          YAML.dump(meta['related']) if feature :related
        discovertouch(Net::HTTP::Post,"/path#{categ}/update"){|req|
          req.set_form_data(data)
        }
      rescue AMEEM::Exceptions::Discover => err
        verbose err,Log4r::ERROR
      end
    end
  end

  # Find the usages info from the meta.yml file
  # checks for validity, and fills in sensible defaults before uploading
  def self.validate_and_complete_usages(usages,itemdef)
    usages={'default'=>{}} unless usages
    default={}
    profiles=Set.new itemdef.profile.keys
    profiles.each do |profilepath|
      default[profilepath.to_s]=Compulsory
    end
    usages.each do |k,v|
      usages[k]={} unless v
    end
    order=usages.delete('order')
    raise "Order should be array of usage names, *not* a usage" if order.
      class==Hash
    if order
      o=Set.new order
      k=Set.new usages.keys
      (o-k).empty? or raise "Usage in order not given #{(o-k).inspect}"
      (k-o).empty? or raise "Order must order all usages if given #{(k-o).inspect}"
    end
    raise "Usage name cannot be 'unrestricted' " if usages.keys.include? 'unrestricted'
    usages.each do |name,usage|
      usage||={}
      usage.reverse_merge! default
      usagekeys=Set.new usage.keys
      usage.values.reject{|x|Usages.include? x}.empty? or
        raise "Invalid role in usage #{name}:#{usage.values}"
      usagekeys==profiles or
        raise "Key specified in usage not present in itemdef #{(usagekeys-profiles).inspect}"
    end
    order ||= usages.sort{|a,b|
      a[1].select{|x|x=='compulsory'}.
        size<=>b[1].
        select{|x|x=='compulsory'}.size
    }.map{|x|x[0]}
    usages['order']=order
    usages
  end

  # Set the usage info in the IVD model
  # The IVD model supplied is a Rubygem model, not an AMEEM csv-file model
  def self.store_ivd_usages(ivd, usages)
    ivd.clear_usages!
    usages.each_pair do |usage, options|
      if usage != 'order' && options[ivd.path]        
        ivd.set_usage_type(usage, options[ivd.path].to_sym)
      end
    end
  end

  # Check the spelling in the documenation.creole file
  def spellcheck
    load_meta_docs
    do_spellcheck
  end

  def do_spellcheck
    @creole.gsub(/[\w\']+/) do |word|
      if !@speller.check(word)&&!@speller.check(word.gsub(/\'/,''))
        # word is wrong
        verbose "Spellcheck: Possible correction for #{word}: #{@speller.suggest(word).first}",
          Log4r::WARN
      end
    end
  end

  # double check that the supplied text is canonically formed
  def canonize_with_warning(val,warning)
    return unless val
    canonize(val)==val or verbose "#{warning} #{val} has been canonized to #{canonize(val)}", Log4r::WARN
    return canonize(val)
  end

  # canonize the supplied string (replace space with underscore and remove nonalphanumeric)
  def canonize(val)
    val.gsub(' ','_').gsub(/[^a-zA-Z0-9_]/,'')
  end

  # load documentation.creole and meta.yml files
  def load_meta_docs
    folder=File.expand_path(File.join(options.root,target))
    @creole=File.read(File.join(folder,CreoleFile))
    @meta=YAML.load_file(File.join(folder,MetaFile)) || {}
  end

  # parse wikicreole to html
  def wiki doc
    doc ? WikiCreole.creole_parse(doc.strip) : ""
  end

  # build html page similar to discover rendering
  def preview_documentation
    # builds something that looks a bit like discover page, and uses discover
    # stylesheets so that it looks familiar to the discover eye.
    load_meta_docs
    template=ERB.new <<-HERE
      <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
      <html>
        <head>
          <title>Preview documentation for <%= category(target) %></title>
          <link href="http://test.my.amee.com/stylesheets/main.css"
            media="screen" rel="stylesheet" type="text/css" />
          <link href="http://test.my.amee.com/stylesheets/authority.css"
            media="screen" rel="stylesheet" type="text/css" />
          <link href="http://discover.amee.com/stylesheets/main.css"
            media="screen" rel="stylesheet" type="text/css" />

        </head>
        <body>
          <div id="header">
            <div id="sitenav">
              <img width="130" height="45" alt="AMEE logo" style="float: left; 
                  margin-left: 45px; margin-top: 8px;"
                  src="http://www2.amee.com/wp-content/themes/amee2/images/ameelogo.png"/>
            </div>
          </div>
          <div id="contentarea">
            <div class="intro">
            Preview documentation for AMEEdiscover
            </div>
            <div id="other_meta">
            <h2 class="firstheader">Meta</h2>
              <% ['authority', 'provenance', 'tags', 'show'].each do |metaname| %>
              <p class="info">
                <span class="meta-name">
                  <%= metaname.capitalize %>:
                </span>
                <%= meta[metaname].respond_to?(:join) ?
                            meta[metaname].join(',') : wiki(meta[metaname]).
                            gsub('<p>', '').gsub('</p>', '')  %>
              </p>
              <% end %>
            </div>
            <div id="preamble">
              <div id="category_wiki">
                <h1> <%= meta['wikiname'] %> </h1>
                <%= wiki creole %>
                <h2> Gallery code </h2>
                <pre> <%= meta['gallery'] %> </pre>
              </div>
            </div>
            <div id="item_definition_table">
              <h2>Item value definitions</h2>
              <table id="ivd_table">
              <tr>
                <th>Path</th>
                <th>Note</th>
              </tr>
              <% meta['ivds'].each do |ivd| %>
              <tr>
                <td><%= ivd[0] %></td>
                <td><%= wiki ivd[1] %></td>
              </tr>
              <% end if meta['ivds']%>
              </table>
            </div>
            <% if meta['data_items'] && meta['data_items'].length>0 %>
            <div id="data">
              <div id="data_content">
                <h2>Data</h2>
                <table id="data_table">
                <tr>
                  <% meta['data_items'].first['drills'].each do |drill| %>
                    <th> <%= drill[0] %> </th>
                  <%end if meta['data_items']&&
                  meta['data_items'].first&&meta['data_items'].first['drills']%>
                  <th> Note </th>
                </tr>
                <% meta['data_items'].each do |datum| %>
                <tr>
                  <% datum['drills'].each do |drill| %>
                    <td><%= drill[1] %></td>
                  <% end if datum['drills']%>
                  <td><span><%= wiki datum['doc'] %></span></td>
                </tr>
                <% end if meta['data_items']%>
                </table>
              </div>
            </div>
            <% end %>
          </div>
        </body>
      </html>
    HERE
    template.result(binding)
  end

  attr_reader :meta,:creole
end
