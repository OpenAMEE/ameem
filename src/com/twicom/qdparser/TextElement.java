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
 * $Log: TextElement.java,v $
 * Revision 1.2  2005/11/23 14:47:42  luckyjim
 * Removed Iterable from QDParser.
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

/**
 * An XML element that simply contains text. This is normally a child element of
 * {@link TaggedElement}.
 * 
 * @author Kevin Twidle
 * @version $Id: TextElement.java,v 1.2 2005/11/23 14:47:42 luckyjim Exp $
 */
public class TextElement extends Element implements Cloneable {

  /**
   * The value of the TextElement.
   */
  String string;
  Integer integer;

  /**
   * Constructs a TextElement using a string
   * 
   * @param string
   */
  public TextElement(String string) {
    setValue(string);
  }

  /**
   * Constructs a TextElement using an integer.
   * 
   * @param value
   */
  public TextElement(int value) {
    setValue(value);
  }

  public Object clone() throws CloneNotSupportedException {
    TextElement newText = (TextElement)super.clone();
    if (integer != null)
      newText.integer = new Integer(integer.intValue());
    return newText;
  }

  /**
   * Sets the value of this TextElement using a String
   * 
   * @param value
   *          the value to store in this TextElement
   */
  public void setValue(String value) {
    this.string = value;
    integer = null;
  }

  /**
   * Sets the value of this TextElement using an integer
   * 
   * @param value
   *          the value to store in this TextElement
   */
  public void setValue(int value) {
    this.string = "" + value;
    integer = new Integer(value);
  }

  /**
   * checks to see if this TextElement contains a numeric value
   * 
   * @return true if this TextElement contains a numeric value
   */
  public boolean isNumber() {
    checkValue();
    return integer != null;
  }

  /**
   * Gets the value of this TextElement as an integer.
   * 
   * @return the value of this TextElement as an integer
   */
  public int getInteger() {
    if (isNumber())
      return integer.intValue();
    else
      return 0;
  }

  /**
   * Checks the content of the string and parses it if possible
   * 
   */
  private void checkValue() {
    if (integer == null)
      try {
        integer = new Integer(string);
      }
      catch (NumberFormatException e) {
      }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return string;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.twicom.qdparser.Element#toString(boolean, int)
   */
  protected String toString(boolean formatted, int level) {
    StringBuffer result = new StringBuffer();
    if (formatted) {
      result.append("\n");
      for (int i = level; i > 0; i--)
        result.append("  ");
    }
    result.append(string);
    return result.toString();
  }

}
