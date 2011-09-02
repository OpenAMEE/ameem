/**
 * Copyright (C) 2005 Kevin Twidle
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: Kevin Twidle <qdp@twicom.com>
 * 
 * Created on Oct 19, 2005
 *
 * $Log: XMLReader.java,v $
 * Revision 1.5  2006/07/21 11:12:45  luckyjim
 * Return null if null string given to parse
 *
 * Revision 1.4  2006/01/14 22:35:05  luckyjim
 * Improved namespace handling, made quote routine static
 *
 * Revision 1.3  2005/10/21 17:15:55  luckyjim
 * Renamed XML Element types
 *
 * Revision 1.2  2005/10/21 13:57:33  luckyjim
 * Disabled logging
 *
 * Revision 1.1  2005/10/21 10:19:11  luckyjim
 * First submission of the QDParser suite
 *
 */

package com.twicom.qdparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

/**
 * Parses an XML structure producing the equivalent nested class structure
 * 
 * @author Kevin Twidle
 * @version $Id: XMLReader.java,v 1.5 2006/07/21 11:12:45 luckyjim Exp $
 */
public class XMLReader implements DocHandler {

  /**
   * A comment to be embedded in each TaggedElement
   */
  private String comment;
  /**
   * The source of the XML
   */
  private Reader input;
  /**
   * The root of the class structure
   */
  private TaggedElement top;
  /**
   * The current element being worked on
   */
  private TaggedElement element;
  /**
   * Holds all the parent elements of the current element
   */
  Stack stack;

  public static TaggedElement parse(String xml) {
    return parse("XML string", xml);
  }

  public static TaggedElement parse(String comment, String xml) {
    TaggedElement result = null;
    try {
      result = new XMLReader(xml).parse();
    }
    catch (Exception e) {
    }
    return result;
  }

  /**
   * Class constructor that reads the XML from a string
   * 
   * @param xml
   *          the XML string to be parsed
   */
  public XMLReader(String xml) {
    initialise("XML String", new StringReader(xml));
  }

  /**
   * Class constructor that reads the XML from a string and specifies a comment
   * 
   * @param comment
   *          the comment to be stored with parsed elements
   * @param xml
   *          the XML string to be parsed
   */
  public XMLReader(String comment, String xml) {
    initialise(comment, new StringReader(xml));
  }

  /**
   * Class constructor that reads the XML from a Reader and specifies a comment
   * 
   * @param comment
   *          the comment to be stored with parsed elements
   * @param input
   *          the XML source to be parsed
   */
  public XMLReader(String comment, Reader input) {
    initialise(comment, input);
  }

  /**
   * Class constructor that reads the XML from a URI specified source and
   * specifies a comment
   * 
   * @param comment
   *          the comment to be stored with parsed elements
   * @param uri
   *          the location that contains the XML to be parsed
   */
  public XMLReader(String comment, URI uri) {
    String scheme = uri.getScheme();
    if (scheme != null && scheme.equals("resource")) {
      Reader input = openResource(uri.getSchemeSpecificPart());
      initialise(comment, input);
    }
  }

  /**
   * performs common initialisation for the constructors
   * 
   * @param comment
   * @param input
   */
  private void initialise(String comment, Reader input) {
    this.comment = comment;
    this.input = input;
  }

  /**
   * parses the XML and returns one top level XML class element
   * 
   * @return the XML element just parsed
   * @throws XMLParseException
   *           if an error occurs when parsing the XML
   * @throws IOException
   *           if an error occurs when reading from the input source
   */
  public TaggedElement parse() throws XMLParseException, IOException {
    top = null;
    element = null;
    stack = new Stack();
    QDParser.parse(this, input);
    return top;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.twicom.qdparser.DocHandler#startDocument()
   */
  public void startDocument() throws XMLParseException {
    top = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.twicom.qdparser.DocHandler#endDocument()
   */
  public void endDocument() throws XMLParseException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.twicom.qdparser.DocHandler#startElement(java.lang.String,
   *      java.lang.String, java.util.Map, int, int)
   */
  public void startElement(String nameSpace, String tag, Map attributes, int line, int col)
      throws XMLParseException {
    String fullns = "xmlns";
    if (nameSpace != null)
      fullns += ":" + nameSpace;
    if (attributes.containsKey(fullns)) {
      nameSpace = (String)attributes.get(fullns);
    }
    TaggedElement newElement = new TaggedElement(nameSpace, tag);
    log("startElement:" + newElement.getName());
    newElement.setLocation(line, col);
    newElement.setComment(comment);
    Iterator it;
    for (it = attributes.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry)it.next();
      newElement.setAttribute((String)entry.getKey(), (String)entry.getValue());
    }
    if (top == null)
      top = newElement;
    else
      element.add(newElement);
    if (element != null)
      stack.push(element);
    element = newElement;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.twicom.qdparser.DocHandler#endElement(java.lang.String,
   *      java.lang.String)
   */
  public void endElement(String nameSpace, String tag) throws XMLParseException {
    log("endElement " + element.getName());
    if (!stack.isEmpty())
      element = (TaggedElement)stack.pop();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.twicom.qdparser.DocHandler#text(java.lang.String, int, int)
   */
  public void text(String str, int line, int col) throws XMLParseException {
    str = str.trim();
    if (str.length() != 0 && element != null)
      element.add(str);
  }

  /**
   * gets a local resource and returns it as a Reader input stream
   * 
   * @param name
   *          the name of the resource to open
   * @return the resource opened as a Reader
   */
  public static Reader openResource(String name) {
    InputStream istream = name.getClass().getResourceAsStream(name);
    if (istream == null && !name.startsWith("/")) {
      istream = name.getClass().getResourceAsStream("/" + name);
    }
    if (istream == null)
      return null;
    return new InputStreamReader(istream);
  }

  /**
   * Writes a log message out. Used for debugging.
   * 
   * @param msg
   *          the message to write to the log
   */
  private void log(String msg) {
    // System.out.println("XMLReader: " + msg);
  }

}
