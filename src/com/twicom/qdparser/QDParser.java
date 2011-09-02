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
 * This is based on QDParser.java from a Java World article by Steven R. Brandt at
 * http://www.javaworld.com/javaworld/javatips/jw-javatip128.html
 * 
 * The original code is in
 * http://www.javaworld.com/javaworld/javatips/javatip128/xmlparsertip.zip 
 *
 * $Log: QDParser.java,v $
 * Revision 1.4  2006/05/26 14:31:53  luckyjim
 * Updated comments to reflect origin of code from Java World article. Used with permission.
 *
 * Revision 1.3  2005/12/11 12:02:17  luckyjim
 * Removed duplicate new Stack()
 *
 * Revision 1.2  2005/10/22 09:53:50  luckyjim
 * Jar file creation. More comments.
 *
 * Revision 1.1  2005/10/21 10:19:11  luckyjim
 * First submission of the QDParser suite
 *
 */

package com.twicom.qdparser;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

/**
 * Quick and Dirty xml parser. This parser is, like the SAX parser, an event
 * based parser, but with much less functionality.
 */
public class QDParser {

  private static int popMode(Stack st) {
    if (!st.empty())
      return ((Integer)st.pop()).intValue();
    else
      return PRE;
  }

  private final static int TEXT = 1, ENTITY = 2, OPEN_TAG = 3, CLOSE_TAG = 4, START_TAG = 5,
      ATTRIBUTE_LVALUE = 6, ATTRIBUTE_EQUAL = 9, ATTRIBUTE_RVALUE = 10, QUOTE = 7, IN_TAG = 8,
      SINGLE_TAG = 12, COMMENT = 13, DOCTYPE = 14, PRE = 15, CDATA = 16;

  /**
   * Parses XML from a reader and returns a data structure containg the parsed
   * XML.
   * 
   * @param doc
   *          the DocHandler that will be given the different elements of the
   *          XML
   * @param r
   *          the Reader to get the source XML from
   * @throws XMLParseException
   *           if an error in the XML is detected
   * @throws IOException
   *           if an error using the Reader is detected
   */
  public static void parse(DocHandler doc, Reader r) throws XMLParseException, IOException {

    Stack st = new Stack();
    int depth = 0;
    int mode = PRE;
    int c = 0;
    int quotec = '"';
    depth = 0;
    StringBuffer sb = new StringBuffer();
    StringBuffer etag = new StringBuffer();
    String nameSpace = null;
    String tagName = null;
    String lvalue = null;
    String rvalue = null;
    Map attrs = null;
    doc.startDocument();
    int line = 1, col = 0;
    boolean eol = false;

    while ((c = r.read()) != -1) {
      // We need to map \r, \r\n, and \n to \n
      // See XML spec section 2.11
      if (c == '\n' && eol) {
        eol = false;
        continue;
      }
      else if (eol) {
        eol = false;
      }
      else if (c == '\n') {
        line++;
        col = 0;
      }
      else if (c == '\r') {
        eol = true;
        c = '\n';
        line++;
        col = 0;
      }
      else {
        col++;
      }

      if (mode == TEXT) {
        // We are between tags collecting text.
        if (c == '<') {
          st.push(new Integer(mode));
          mode = START_TAG;
          if (sb.length() > 0) {
            doc.text(sb.toString(), line, col);
            sb.setLength(0);
          }
        }
        else if (c == '&') {
          st.push(new Integer(mode));
          mode = ENTITY;
          etag.setLength(0);
        }
        else
          sb.append((char)c);

      }
      else if (mode == CLOSE_TAG) {
        // we are processing a closing tag: e.g. </foo>
        if (c == '>') {
          mode = popMode(st);
          tagName = sb.toString();
          sb.setLength(0);
          depth--;
          doc.endElement(nameSpace, tagName);
          if (depth == 0) {
            doc.endDocument();
            return;
          }
        }
        else {
          sb.append((char)c);
        }

      }
      else if (mode == CDATA) {
        // we are processing CDATA
        if (c == '>' && sb.toString().endsWith("]]")) {
          sb.setLength(sb.length() - 2);
          doc.text(sb.toString(), line, col);
          sb.setLength(0);
          mode = popMode(st);
        }
        else
          sb.append((char)c);

      }
      else if (mode == COMMENT) {
        // we are processing a comment. We are inside
        // the <!-- .... --> looking for the -->.
        if (c == '>' && sb.toString().endsWith("--")) {
          sb.setLength(0);
          mode = popMode(st);
        }
        else
          sb.append((char)c);

      }
      else if (mode == PRE) {
        // We are outside the root tag element
        if (c == '<') {
          mode = TEXT;
          st.push(new Integer(mode));
          mode = START_TAG;
        }

      }
      else if (mode == DOCTYPE) {
        // We are inside one of these <? ... ?>
        // or one of these <!DOCTYPE ... >
        if (c == '>') {
          mode = popMode(st);
          if (mode == TEXT)
            mode = PRE;
        }

      }
      else if (mode == START_TAG) {
        // we have just seen a < and
        // are wondering what we are looking at
        // <foo>, </foo>, <!-- ... --->, etc.
        mode = popMode(st);
        if (c == '/') {
          st.push(new Integer(mode));
          mode = CLOSE_TAG;
        }
        else if (c == '?') {
          mode = DOCTYPE;
        }
        else {
          st.push(new Integer(mode));
          mode = OPEN_TAG;
          tagName = null;
          attrs = new Hashtable();
          sb.append((char)c);
        }

      }
      else if (mode == ENTITY) {
        // we are processing an entity, e.g. &lt;, &#187;, etc.
        if (c == ';') {
          mode = popMode(st);
          String cent = etag.toString();
          etag.setLength(0);
          if (cent.equals("lt"))
            sb.append('<');
          else if (cent.equals("gt"))
            sb.append('>');
          else if (cent.equals("amp"))
            sb.append('&');
          else if (cent.equals("quot"))
            sb.append('"');
          else if (cent.equals("apos"))
            sb.append('\'');
          else if (cent.startsWith("#x"))
            sb.append((char)Integer.parseInt(cent.substring(2), 16));
          else if (cent.startsWith("#"))
            sb.append((char)Integer.parseInt(cent.substring(1)));
          // Insert custom entity definitions here
          else
            exc("Unknown entity: &" + cent + ";", line, col);
        }
        else {
          etag.append((char)c);
        }

      }
      else if (mode == SINGLE_TAG) {
        // we have just seen something like this:
        // <foo a="b"/
        // and are looking for the final >.
        if (tagName == null)
          tagName = sb.toString();
        if (c != '>')
          exc("Expected > for tag: <" + tagName + "/>", line, col);
        doc.startElement(nameSpace, tagName, attrs, line, col);
        doc.endElement(nameSpace, tagName);
        if (depth == 0) {
          doc.endDocument();
          return;
        }
        sb.setLength(0);
        attrs = new HashMap();
        tagName = null;
        mode = popMode(st);

      }
      else if (mode == OPEN_TAG) {
        // we are processing something
        // like this <foo ... >. It could
        // still be a <!-- ... --> or something.
        if (c == '>') {
          if (tagName == null)
            tagName = sb.toString();
          sb.setLength(0);
          depth++;
          doc.startElement(nameSpace, tagName, attrs, line, col);
          nameSpace = null;
          tagName = null;
          attrs = new HashMap();
          mode = popMode(st);
        }
        else if (c == '/') {
          mode = SINGLE_TAG;
        }
        else if (c == '-' && sb.toString().equals("!-")) {
          mode = COMMENT;
        }
        else if (c == '[' && sb.toString().equals("![CDATA")) {
          mode = CDATA;
          sb.setLength(0);
        }
        else if (c == 'E' && sb.toString().equals("!DOCTYP")) {
          sb.setLength(0);
          mode = DOCTYPE;
        }
        else if (Character.isWhitespace((char)c)) {
          tagName = sb.toString();
          sb.setLength(0);
          mode = IN_TAG;
        }
        else if (c == ':') {
          nameSpace = sb.toString();
          sb.setLength(0);
        }
        else {
          // We have a character to add to the name
          // Check for validity
          // TODO Maybe replace by a valid boolean table for speed
          if (c == '_' || Character.isLetter(c))
            sb.append((char)c);
          else if (sb.length() > 0 && c == '-' || c == '.' || Character.isDigit(c))
            sb.append((char)c);
          else
            exc("Illegal character " + c + " in tag name", line, col);
        }

      }
      else if (mode == QUOTE) {
        // We are processing the quoted right-hand side
        // of an element's attribute.
        if (c == quotec) {
          rvalue = sb.toString();
          sb.setLength(0);
          attrs.put(lvalue, rvalue);
          mode = IN_TAG;
          // See section the XML spec, section 3.3.3
          // on normalization processing.
        }
        else if (" \r\n\u0009".indexOf(c) >= 0) {
          sb.append(' ');
        }
        else if (c == '&') {
          st.push(new Integer(mode));
          mode = ENTITY;
          etag.setLength(0);
        }
        else {
          sb.append((char)c);
        }

      }
      else if (mode == ATTRIBUTE_RVALUE) {
        if (c == '"' || c == '\'') {
          quotec = c;
          mode = QUOTE;
        }
        else if (Character.isWhitespace((char)c)) {
          ;
        }
        else {
          exc("Error in attribute processing", line, col);
        }

      }
      else if (mode == ATTRIBUTE_LVALUE) {
        if (Character.isWhitespace((char)c)) {
          lvalue = sb.toString();
          sb.setLength(0);
          mode = ATTRIBUTE_EQUAL;
        }
        else if (c == '=') {
          lvalue = sb.toString();
          sb.setLength(0);
          mode = ATTRIBUTE_RVALUE;
        }
        else {
          sb.append((char)c);
        }

      }
      else if (mode == ATTRIBUTE_EQUAL) {
        if (c == '=') {
          mode = ATTRIBUTE_RVALUE;
        }
        else if (Character.isWhitespace((char)c)) {
          ;
        }
        else {
          exc("Error in attribute processing.", line, col);
        }

      }
      else if (mode == IN_TAG) {
        if (c == '>') {
          mode = popMode(st);
          doc.startElement(nameSpace, tagName, attrs, line, col);
          depth++;
          nameSpace = null;
          tagName = null;
          attrs = new HashMap();
        }
        else if (c == '/') {
          mode = SINGLE_TAG;
        }
        else if (Character.isWhitespace((char)c)) {
          ;
        }
        else {
          mode = ATTRIBUTE_LVALUE;
          sb.append((char)c);
        }
      }
    }
    if (mode != PRE)
      exc("missing end tag", line, col);
  }

  /**
   * Throws an XMLparseException containing a message and the current line and
   * column numbers.
   * 
   * @param s
   *          the message to be included in the exception
   * @param line
   *          the input line where the error occured
   * @param col
   *          the input column where the error occured
   * @throws XMLParseException
   *           bacause this is the designed function of the method
   */
  private static void exc(String s, int line, int col) throws XMLParseException {
    throw new XMLParseException(s + " near line " + line + ", column " + col);
  }
}
