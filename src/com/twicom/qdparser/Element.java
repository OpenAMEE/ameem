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
 * Created on Oct 18, 2005
 *
 * $Log: Element.java,v $
 * Revision 1.7  2006/03/25 22:44:32  luckyjim
 * Made serializable
 *
 * Revision 1.6  2006/03/23 07:59:35  luckyjim
 * Added unquote as opposite of quote
 *
 * Revision 1.5  2006/02/13 12:56:47  luckyjim
 * Added factory method for Element
 *
 * Revision 1.4  2006/01/14 22:35:05  luckyjim
 * Improved namespace handling, made quote routine static
 *
 * Revision 1.3  2005/10/21 17:15:55  luckyjim
 * Renamed XML Element types
 *
 * Revision 1.1  2005/10/21 10:19:11  luckyjim
 * First submission of the QDParser suite
 *
 */

package com.twicom.qdparser;

import java.io.Serializable;

/**
 * This is the base class for the XML Elements.
 * 
 * @author Kevin Twidle
 * @version $Id: Element.java,v 1.7 2006/03/25 22:44:32 luckyjim Exp $
 */
public abstract class Element implements Serializable {

  /**
   * Creates a new TaggedElement or TextElement depending on the argument
   * 
   * @param value
   *          the string to be turned into an element
   * @return a new TaggedElement or TextElement
   */
  public static Element newElement(String value) {
    Element result;
    if (value.trim().startsWith("<"))
      result = XMLReader.parse(value);
    else
      result = new TextElement(value);
    return result;
  }

  /**
   * Class constructor.
   */
  public Element() {
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  /**
   * Returns formatted or unformatted XML source.
   * 
   * @param formatted
   *          whether to return formatted XML source. If true, the source is
   *          pretty-printed with new lines and indentation. If false, the XML
   *          string is returned as one lone, unformatted line.
   * @return a String containing the XML source
   */
  public String toString(boolean formatted) {
    return toString(formatted, 0);
  }

  /**
   * Internal method used recursively to format XML with appropriate
   * indentation.
   * 
   * @param formatted
   *          whether to return formatted XML source. If true, the source is
   *          pretty-printed with new lines and indentation. If false, the XML
   *          string is returned as one long, unformatted line.
   * @param level
   *          the indentation level used to write leading spaces
   * @return a String containing th XML source
   */
  protected abstract String toString(boolean formatted, int level);

  /**
   * quotes a string according to XML rules. When attributes and text elements
   * are written out special characters have to be quoted.
   * 
   * @param string
   *          the string to process
   * @return the string with quoted special characters
   */
  public static String quote(String string) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      switch (ch) {
        case '&':
          sb.append("&amp;");
          break;
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '"':
          sb.append("&quot;");
          break;
        case '\'':
          sb.append("&apos;");
          break;
        default:
          sb.append(ch);
      }
    }
    return sb.toString();
  }

  /**
   * quotes a string according to XML rules. When attributes and text elements
   * are written out special characters have to be quoted.
   * 
   * @param string
   *          the string to process
   * @return the string with quoted special characters
   */
  public static String unquote(String string) {
    StringBuffer sb = new StringBuffer();
    boolean inQuote = false;
    StringBuffer quoteBuf = new StringBuffer();
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      if (inQuote) {
        if (ch == ';') {
          String quote = quoteBuf.toString();
          if (quote.equals("lt"))
            sb.append('<');
          else if (quote.equals("gt"))
            sb.append('>');
          else if (quote.equals("amp"))
            sb.append('&');
          else if (quote.equals("quot"))
            sb.append('"');
          else if (quote.equals("apos"))
            sb.append('\'');
          else if (quote.startsWith("#x"))
            sb.append((char)Integer.parseInt(quote.substring(2), 16));
          else if (quote.startsWith("#"))
            sb.append((char)Integer.parseInt(quote.substring(1)));
          else
            sb.append(quoteBuf);
          inQuote = false;
          quoteBuf.setLength(0);
        }
        else {
          quoteBuf.append(ch);
        }
      }
      else {
        if (ch == '&')
          inQuote = true;
        else
          sb.append(ch);
      }
    }
    if (inQuote)
      sb.append(quoteBuf);
    return sb.toString();
  }
}
