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
 * This is based on DocHandler.java from
 * http://www.javaworld.com/javaworld/javatips/javatip128/xmlparsertip.zip 
 *
 * $Log: DocHandler.java,v $
 * Revision 1.1  2005/10/21 10:19:11  luckyjim
 * First submission of the QDParser suite
 *
 */

package com.twicom.qdparser;

import java.util.Map;

/**
 * Interface for callbacks for the XML parser.
 * 
 * @author Kevin Twidle
 * @version $Id: DocHandler.java,v 1.1 2005/10/21 10:19:11 luckyjim Exp $
 */
public interface DocHandler {

  /**
   * Called to indicate the start of a tagged element
   * 
   * @param nameSpace
   *          the namespace part of the tag
   * @param tag
   *          the name part of the tag
   * @param attributes
   *          the table of attributes for this element
   * @param line
   *          the line number where this element was started
   * @param col
   *          the column number where this element was started
   * @throws XMLParseException
   *           if a semantic error is observed
   */
  public void startElement(String nameSpace, String tag, Map attributes, int line, int col)
      throws XMLParseException;

  /**
   * Called to indicate the end of a tagged element
   * 
   * @param nameSpace
   *          the namespace part of the tag
   * @param tag
   *          the name part of the tag
   * @throws XMLParseException
   *           if a semantic error is observed
   */
  public void endElement(String nameSpace, String tag) throws XMLParseException;

  /**
   * Called to indicate the start of the XML document being read
   * 
   * @throws XMLParseException
   *           if a semantic error is observed
   */
  public void startDocument() throws XMLParseException;

  /**
   * Called to indicate the end of the XML document being read
   * 
   * @throws XMLParseException
   *           if a semantic error is observed
   */
  public void endDocument() throws XMLParseException;

  /**
   * Called to indicate that an untagged element has been read
   * 
   * @param str
   *          the value of the untagged element
   * @param line
   *          the line number where this element was started
   * @param col
   *          the column number where this element was started
   * @throws XMLParseException
   *           if a semantic error is observed
   */
  public void text(String str, int line, int col) throws XMLParseException;
}
