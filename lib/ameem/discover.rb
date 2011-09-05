# Command line functionality to manipulate AMEE Discover from AMEEM
module Discover
  provides :discover

  #dispatch discover subcommand
  #These must be defined by other modules, by mixing into AMEEM methods
  #named discover_somecommand
  def discover    
    dispatch="discover_"+options.action
    if respond_to?(dispatch)
      method(dispatch).call
    else
      raise "AMEEM cannot resolve command "+dispatch
    end
  end

  # write or read category specified by path to/from discover
  # with specified Net::HTTP request (klass)
  #
  # Yields skeleton http request to a block to fill with body, params etc.
  #
  # e.g.
  #
  # discovertouch(Net::HTTP::Put,"/path/on/discover") { |req|
  # req.body="mybody"
  # }
  def discovertouch(klass,path)
    url=URI.parse(options.discover)
    ex=Net::HTTP.new(url.host,url.port)
    req=klass.new(path)
    req.basic_auth(options.discover_user,options.discover_password)
    yield req if block_given?
    verbose "Communicating with discover #{req.class}, #{req.path}, #{req.body}"
    resp=ex.request req
    raise AMEEM::Exceptions::Discover.new("Problem from Discover: #{resp.code},#{resp}") unless resp.code=='200' if resp
  end
end
