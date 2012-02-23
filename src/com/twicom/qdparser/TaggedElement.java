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
 * $Log: TaggedElement.java,v $
 * Revision 1.10  2006/08/20 10:24:40  luckyjim
 * Added fast filling in of new TaggedElements using Object arrays
 *
 * Revision 1.9  2006/08/07 15:45:42  luckyjim
 * Added remove element at index, with tests
 *
 * Revision 1.8  2006/06/26 13:52:31  luckyjim
 * Tidied up namespace and string output
 *
 * Revision 1.7  2005/11/23 14:47:42  luckyjim
 * Removed Iterable from QDParser.
 *
 * Revision 1.6  2005/11/14 13:57:33  luckyjim
 * Added clearElements
 *
 * Revision 1.5  2005/11/12 18:01:55  luckyjim
 * Improved replace
 *
 * Revision 1.4  2005/10/23 06:14:38  luckyjim
 * Updated documentation for beta1 release
 *
 * Revision 1.3  2005/10/22 09:53:50  luckyjim
 * Jar file creation. More comments.
 *
 * Revision 1.2  2005/10/22 08:53:40  luckyjim
 * Changed TaggedElement getAtt to getAttribute
 *
 * Revision 1.1  2005/10/21 17:15:55  luckyjim
 * Renamed XML Element types
 *
 * Revision 1.2  2005/10/21 17:01:52  luckyjim
 * Fixed text values in toString
 *
 * Revision 1.1  2005/10/21 10:19:11  luckyjim
 * First submission of the QDParser suite
 *
 */

package com.twicom.qdparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a tagged XML element and contains all the information that the
 * original had. Attributes may be set, accessed and deleted. Child elements may
 * be set accessed or deleted. This can be converted back to XML, along with its
 * children, in a formatted or unformatted string.
 * 
 * @author Kevin Twidle
 * @version $Id: TaggedElement.java,v 1.10 2006/08/20 10:24:40 luckyjim Exp $
 */
public class TaggedElement extends Element implements Cloneable {

  String nameSpace;
  String tagName;
  String name;
  private int line;
  private int col;

  Map attributes;
  List elements;
  private String comment;

  /**
   * Class constructor specifying only the name part of the tag
   * 
   * @param tag
   *          the name of the tag
   */
  public TaggedElement(String tag) {
    this(null, tag);
  }

  /**
   * Class constructor specifying the name space and the tag name
   * 
   * @param nameSpace
   *          the name space name
   * @param tagName
   *          the name of the tag
   */
  public TaggedElement(String nameSpace, String tagName) {
    this.nameSpace = nameSpace;
    this.tagName = tagName;
    if (nameSpace == null)
      name = tagName;
    else
      name = nameSpace + ":" + tagName;
    attributes = null;
    elements = null;
    comment = null;
    line = col = 0;
  }

  /**
   * Class constructor specifying only the name part of the tag with an array of objects to be added as child elements
   * 
   * @param tag
   *          the name of the tag
   * @param children
   *          an array of objects to be added as children
   */
  public TaggedElement(String tag, Object[] children) {
    this(null, tag, children);
  }

  /**
   * Class constructor specifying only the name part of the tag with an array of objects to be added as child elements
   * 
   * @param nameSpace
   *          the name space name
   * @param tag
   *          the name of the tag
   * @param children
   *          an array of objects to be added as children
   */
  public TaggedElement(String nameSpace, String tag, Object[] children) {
    this(nameSpace, tag);
    for (int i = 0; i < children.length; i++) {
      Object obj = children[i];
      if (obj instanceof Element)
        add((Element)obj);
      else
        add(obj.toString());
    }
  }

 /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#clone()
   */
  public Object clone() throws CloneNotSupportedException {
    TaggedElement newElement = (TaggedElement)super.clone();
    if (attributes != null) {
      newElement.attributes = new HashMap(attributes);
    }
    if (elements != null) {
      newElement.elements = new ArrayList();
      Iterator it = elements.iterator();
      while (it.hasNext()) {
        newElement.elements.add(((Element)it.next()).clone());
      }
    }
    return newElement;
  }

  /**
   * gets the original tag name
   * 
   * @return the complete namespace and tag name
   */
  public String getName() {
    return name;
  }

  /**
   * gets the name space name
   * 
   * @return the name space name
   */
  public String getNameSpace() {
    return nameSpace;
  }

  /**
   * gets the name part of the tag
   * 
   * @return the name part of the tag
   */
  public String getTag() {
    return tagName;
  }

  /**
   * sets an attribute of this element
   * 
   * @param name
   *          the name of the attribute to be set
   * @param value
   *          the value of the attribute to be set
   */
  public void setAttribute(String name, String value) {
    if (attributes == null)
      attributes = new HashMap();
    attributes.put(name, value);
  }

  /**
   * gets the specified attribute of this element
   * 
   * @param name
   *          the name of the attribute to get
   * @return the value of the attribute
   */
  public String getAttribute(String name) {
    return (String)(attributes == null ? null : attributes.get(name));
  }

  /**
   * gets the specified attribute of this element but returns given default
   * value if the attribute does not exist
   * 
   * @param name
   *          the name of the attribute to get
   * @param defaultValue
   *          the value to be returned if the attribute doesn't exist
   * @return the value of the attribute
   */
  public String getAttribute(String name, String defaultValue) {
    String result = getAttribute(name);
    return result == null ? defaultValue : result;
  }

  /**
   * removes the named attribute
   * 
   * @param string
   *          the name of the attribute to be removed
   */
  public void delAttribute(String string) {
    if (hasAttributes()) {
      attributes.remove(string);
    }
  }

  /**
   * finds out whether an attribute exists
   * 
   * @param name
   *          the name of the attribute to look for
   * @return whether the attribute exists in this element
   */
  public boolean hasAttribute(String name) {
    return attributes == null ? false : attributes.containsKey(name);
  }

  /**
   * finds out whether this element has any attributes
   * 
   * @return whether this element has any attributes
   */
  public boolean hasAttributes() {
    return attributes != null && !attributes.isEmpty();
  }

  /**
   * gets the attributes in the form of an indexed table
   * 
   * @return the attribute table for this element
   */
  public Map getAttributes() {
    if (attributes == null)
      attributes = new HashMap();
    return attributes;
  }

  /**
   * gets the number of child elements this element has
   * 
   * @return the number of child elements this element has
   */
  public int elements() {
    return elements == null ? 0 : elements.size();
  }

  /**
   * finds out whether this element has any child elements
   * 
   * @return whether this element has any child elements
   */
  public boolean hasElements() {
    return elements() != 0;
  }

  /**
   * removes the children from this element
   * 
   */
  public void clearElements() {
    if (elements != null)
      elements.clear();
  }

  /**
   * adds a child to this element
   * 
   * @param element
   *          the child element to be added
   */
  public void add(Element element) {
    if (elements == null)
      elements = new ArrayList();
    elements.add(element);
  }

  /**
   * adds a child to this element
   * 
   * @param string
   *          the child element to be added
   */
  public void add(String string) {
    add(new TextElement(string));
  }

  /**
   * gets a child from the specified place in this element
   * 
   * @param i
   *          the index where the child is to be added
   * @return the specified child
   */
  public Element getChild(int i) {
    if (i < 0 || i >= elements())
      return null;
    return (Element)elements.get(i);
  }

  /**
   * replaces a child element with the one given
   * 
   * @param index
   *          the child element number to replace
   * @param newElement
   *          the new element to replace the old one
   */
  public void replace(int index, Element newElement) {
    if (elements == null)
      elements = new ArrayList();
    if (index >= elements.size())
      elements.add(newElement);
    else
      elements.set(index, newElement);
  }

  /**
   * Removes a child from this element
   * 
   * @param index
   *          the index of the child to be removed
   * @return whether the child was found and removed or not
   */
  public Element remove(int index) {
    Element result = null;
    if (elements == null)
      elements = new ArrayList();
    if (index >= 0 && index < elements.size())
      result =  (Element)elements.remove(index);
    return result;
  }

  /**
   * Removes a child from this element
   * 
   * @param element
   *          the child to be removed
   * @return whether the child was found and removed or not
   */
  public boolean remove(Element element) {
    return elements.remove(element);
  }

  /**
   * Finds a named child tagged element. If there is no such child, null is
   * returned.
   * 
   * @param name
   *          the name of the child of this TaggedElement to find
   * @return the name of the found element or null if not found
   */
  public TaggedElement find(String name) {
    for (int i = 0; i < elements.size(); i++) {
      Element element = (Element)elements.get(i);
      if (element instanceof TaggedElement) {
        if (((TaggedElement)element).getName().equals(name))
          return (TaggedElement)element;
      }
    }
    return null;
  }

  /**
   * sets the list of children of this element. This method replaces the current
   * children.
   * 
   * @param elements
   *          the new list of children
   */
  public void setElements(List elements) {
    this.elements = elements;
  }

  /**
   * gets a list of the children of this element. This method always returns a
   * List even if it is empty.
   * 
   * @return the List containing the children of this element
   */
  public List getElements() {
    if (elements == null)
      elements = new ArrayList();
    return elements;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  public Iterator iterator() {
    return getElements().iterator();
  }

  /**
   * sets the XML source code location information for this element
   * 
   * @param line
   *          the line number of the start tag
   * @param col
   *          the column number of the start tag
   */
  public void setLocation(int line, int col) {
    this.line = line;
    this.col = col;
  }

  /**
   * gets the XML source code line number where this element was declared. This
   * number will be 0 if it was never set.
   * 
   * @return the XML source code line number of this element's declaration
   */
  public int getLine() {
    return line;
  }

  /**
   * gets the XML source code cloumn number where this element was declared.
   * This number will be 0 if it was never set.
   * 
   * @return the XML source code column number of this element's declaration
   */
  public int getCol() {
    return col;
  }

  /**
   * get the comment associated with this element
   * 
   * @return this element's comment
   */
  public String getComment() {
    return comment;
  }

  /**
   * set this element's comment
   * 
   * @param comment
   *          the comment to be stored
   */
  public void setComment(String comment) {
    this.comment = comment;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return toString(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.twicom.qdparser.Element#toString(boolean)
   */
  public String toString(boolean formatted) {
    return toString(formatted, 0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.twicom.qdparser.Element#toString(boolean, int)
   */
  protected String toString(boolean formatted, int level) {
    StringBuffer sb = new StringBuffer();
    // Add leading newline and spaces, if necessary
    if (formatted && level > 0) {
      sb.append("\r\n");
      for (int i = level; i > 0; i--)
        sb.append("  ");
    }

    String nsTagName;
    if (nameSpace == null) {
      nsTagName = tagName;
    }
    else {
      nsTagName = "nsqdp:" + tagName;
    }
    // Put the opening tag out
    sb.append("<" + nsTagName);

    // Write the attributes out
    if (nameSpace != null) {
      sb.append(" xmlns:nsqdp='" + nameSpace + "'");
    }
    if (hasAttributes()) {
      Iterator it = getAttributes().entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry entry = (Map.Entry)it.next();
        sb.append(' ' + (String)entry.getKey() + '=');
        sb.append("'" + quote((String)entry.getValue()) + "'");
      }
    }

    // write out the closing tag or other elements
    boolean formatThis = formatted;
    if (!hasElements()) {
      sb.append("/>");
    }
    else {
      sb.append(">");
      Iterator it = elements.iterator();
      while (it.hasNext()) {
        Element element = (Element)it.next();
        formatThis = element instanceof TextElement ? false : formatted;
        sb.append(element.toString(formatThis, level + 1));
        if (element instanceof TextElement && it.hasNext())
          sb.append("<!-- -->");
      }
      // Add leading newline and spaces, if necessary
      if (formatThis && level >= 0) {
        sb.append("\r\n");
        for (int i = level; i > 0; i--)
          sb.append("  ");
      }
      // Now put the closing tag out
      sb.append("</" + nsTagName + ">");
    }
    return sb.toString();
  }

}
