/*
 * AmeeXMLHelper.java
 *
 * Created on 08 November 2007, 16:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.dgen.apiexamples;

import com.amee.client.AmeeException;
import com.twicom.qdparser.*;
import java.util.*;

/**
 *
 * @author nalu
 */
public class AmeeXMLHelper {
    private static boolean findOne=false;
    private static boolean debug=false;
    public static String dataItemUidXPath="Resources/DrillDownResource/Choices/Choices/Choice";
    
    /** Creates a new instance of AmeeXMLHelper */
    public AmeeXMLHelper() {
    }
    
    /** Convenience method that just gets the contents of the first element in xml with the given name.
     * If attribute is non-null then that attribute is returned.
     * Note: if attribute is null, the element must be a leaf, i.e. contain only text and have no other children.
     */
    public static String getElementContents(String xml, String name, String attribute){
        return getElementContents(getElement(xml), name, attribute);
    }
    
    /** Convenience method that just gets the contents of the first element in TaggedElement with the given name.
     * If attribute is non-null then that attribute is returned.
     * Note: if attribute is null, the element must be a leaf, i.e. contain only text and have no other children.
     */
    public static String getElementContents(TaggedElement te, String name, String attribute){
        findOne=true; //Return as soon as the first one is found
        ArrayList al = getElement(te, name);
        findOne=false;
        if(al.isEmpty())
            return null;
        te = (TaggedElement) al.get(0);
        if(attribute!=null)
            return te.getAttribute(attribute);
        //Assume this element contains only text, not sub elements
        if(te.elements()==0)//Element was empty, e.g. a self-closing tag
            return "";
        TextElement textE = (TextElement) te.getChild(0);
        return textE.toString();
    }
    
    public static TaggedElement getElement(String xml){
        TaggedElement te = XMLReader.parse(xml);
        return te;
    }
    
    /** Searches through the xml looking a TaggedElement called elementName, returns
     *  all matches, or if findOne==true, only the first TaggedElement one it finds. */
    public static ArrayList getElement(String xml, String elementName){
        return getElement(getElement(xml),elementName);
    }
    
    /** Searches through the xml looking a TaggedElement called elementName, returns
     *  all matches, or if findOne==true, only the first TaggedElement one it finds. */
    public static ArrayList getElement(TaggedElement te, String elementName){
        ArrayList al = new ArrayList();
        return getElement(te,elementName,al);
    }
    
    /** Searches through the xml looking for elementName under the / delimited path, returns
     *  all matches, or if findOne==true, only the first TaggedElement one it finds. */
    public static ArrayList getElement(String xml, String path, String elementName){
        ArrayList al = new ArrayList();
        TaggedElement te = getElement(xml);
        String[] elements = path.split("/");
        //skip i=0, the root element
        for(int i=1;i<elements.length;i++){
            if(te==null || te.elements()==0)
                return null;
            //System.err.println(te.getName()+":"+elements[i]);
            //System.err.println("te="+te);
            te=te.find(elements[i]);
        }
        if(te==null) //the xpath wasn't valid
            return null;
        return getElement(te,elementName,al);
    }
    
    /** Searches through the TaggedElement looking for elementName, returns
     *  the contents of the first one it finds. NOTE: Case insensitive as of 14aug08*/
    public static ArrayList getElement(TaggedElement te, String elementName, ArrayList foundList){
        if(debug){
            System.err.println("======================================================");
            System.err.println("Entering getElement te = "+te+"\nfoundList="+foundList);
        }
        if(te.getName().equalsIgnoreCase(elementName)) {
            foundList.add(te);
            if(debug)
                System.err.println("************ FOUND = "+te.getName());
            if(findOne)
                return foundList;
        }
        Iterator iter = te.getElements().iterator();
        while(iter.hasNext()){
            Object elem = iter.next();
            if(elem instanceof TaggedElement)
                foundList=getElement((TaggedElement) elem, elementName,foundList);
            if(findOne && foundList.isEmpty()==false)
                break;
        }
        return foundList;
    }
    
    public static String getDrillString(Map keyMap){
        String s="";
        Iterator iter = keyMap.keySet().iterator();
        while(iter.hasNext()){
            String name = Main.urlEncode(iter.next().toString());
            String choice = Main.urlEncode(keyMap.get(name).toString());
            s+=name+"="+choice;
            if(iter.hasNext())
                s+="&";
        }
        return s;
    }
    
    /** @param path The path of a data category, e.g. /home/lighting
     *  @param searchMap A map where keys are the names of data item
     *  drill choices and the values are the choices.
     *  @return The data item uid is return, null if not found.
     */
    public static String getDataUid(String path, Map keyMap) throws AmeeException{
        path+="/drill?"+getDrillString(keyMap);
        String xml = Main.sendRequest("GET /data"+path,"",false);
        //System.err.println("path="+path);
        //System.err.println(xml);
        ArrayList al = getElement(xml,dataItemUidXPath,"Name");
        if(al==null)
            return null;
        Element e = ((TaggedElement) al.get(0)).getChild(0);
        return e.toString();
    }
    
    
    public static Map getDataItemValues(String xml){
        Map map = new LinkedHashMap();
        ArrayList al = getElement(xml,"Resources/DataItemResource/DataItem/ItemValues","ItemValue");
        //System.err.println("xml = "+xml);
        for(int i=0;i<al.size();i++){
            TaggedElement te = (TaggedElement) al.get(i);
            //System.err.print("te"+i+" = "+te);
            String name = getElementContents(te,"Path",null);
            String value = getElementContents(te,"Value",null);
            //System.err.println("name = "+name+", value = "+value);
            map.put(name,value);
        }
        return map;
    }
    
    
}
