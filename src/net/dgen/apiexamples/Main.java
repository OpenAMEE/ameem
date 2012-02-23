/*
 * Main.java
 *
 * This class demonstrates how to access the API and process data in Java.
 *
 * IMPORTANT NOTE: This code is intended as an example of how to use the AMEE
 * API and is not meant to be a library of well-tested functions for general use.
 *
 * v0.1 Created on 08 June 2007, 14:41
 * v0.2 Created on 08 August 2007, 01:56
 * - adding new examples on profile item efficiency
 * - add notes on getDataUid() method vs drill-down efficiency
 * - other minor fixes/improvements
 *
 * Any questions? Email andrew@dgen.net
 * AJC 8aug07
 *
 * License: http://www.gnu.org/licenses/gpl.html
 *
 * This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package net.dgen.apiexamples;

import com.amee.client.AmeeException;
import java.io.*;
import java.util.*;

import com.twicom.qdparser.TaggedElement;

import com.amee.client.service.AmeeContext;
import net.dgen.apiexamples.AmeeInterface; // Use our own version for XML support
import com.amee.client.util.Choice;
import org.apache.commons.httpclient.HttpMethodBase;

//import javax.xml.parsers.*; using custom xml parsers
/**
 *
 * @author nalu
 */
public class Main {

    public static String localFileCharSet = "UTF-8";
    public static String apiWriteCharSet = "ISO-8859-1";
    public static String apiReadCharSet = "UTF-8";
    public static String host = "stage.amee.com"; //DON'T CHANGE THESE HERE, SET THEM IN THE main METHOD
    public static boolean debug = true;

    /**
     * @param aLogin the login to set
     */
    public static void setLogin(String aLogin) {
        AmeeContext.getInstance().setUsername(aLogin);
    }

    /**
     * @param aPassword the password to set
     */
    public static void setPassword(String aPassword) {
        AmeeContext.getInstance().setPassword(aPassword);
    }

    /**
     * @param aHost the host to set
     */
    public static void setHost(String aHost) {
        host = aHost;
        AmeeContext.getInstance().setBaseUrl("https://" + aHost);
    }

    /**
     * @return the host
     */
    public static String getHost() {
        return host;
    }

    public Main() {
    }

    public static String sendRequest(String path, String body) throws AmeeException {
        return sendRequest(path, body, true);
    }

    public static String sendRequest(String path, String body, boolean includeResponseHeader) throws AmeeException {
        return sendRequest(path, body, "Content-Type: application/x-www-form-urlencoded", includeResponseHeader);
    }

    public static String sendRequest(String method_and_path, String body, String contentType, boolean includeResponseHeader) throws AmeeException {
        
        // NOTE: This is NOT well designed.
        // This is a quick hackity hack replacement of the core request method here to support
        // SSL, and it does that by using the public Java AMEE SDK. This is patently ridiculous,
        // and should not be taken as good practise. However, all this code is being
        // derprecated, so it seemed the fastest way, generally, and bugger the consequences.
        
        String[] splitpath = method_and_path.split(" ", 2);
        String method = splitpath[0];
        String path = splitpath[1];
        String response = "";

        HttpMethodBase request = null;
        if ("GET".equals(method)) {
            response = AmeeInterface.getInstance().getAmeeResource(path);            
        }
        else if ("POST".equals(method) || "PUT".equals(method)) {
            response = AmeeInterface.getInstance().postOrPutAmeeResource(path, body, contentType, "PUT".equals(method));
        }
        else if ("DELETE".equals(method)) {
            AmeeInterface.getInstance().deleteAmeeResource(path);
        }

        
        
        if (includeResponseHeader) {
            // JESUS CHRIST THIS IS A HACK
            response = "200 OK\n" + response;
        }
        return response;
    }

    public static String getXMLfromResponse(String response) {
        boolean foundXml = false;
        String[] lines = response.split("\n");
        String xml = "";
        for (int i = 0; i < lines.length; i++) {
            if (foundXml || lines[i].indexOf("<?xml") == 0) {
                xml += lines[i] + "\n";
                foundXml = true;
            }
        }
        return xml;
    }

    //Creates a profile and returns the profile uid

    public static String createProfile() throws AmeeException {
        String response = sendRequest("POST /profiles", "profile=true", false);
        String pUid = AmeeXMLHelper.getElementContents(response, "Profile", "uid");

        return pUid;
    }

    public static String getProfile(String profileUid) throws AmeeException {
        String response = sendRequest("GET /profiles/" + profileUid + "?recurse=true", "", false);
        return response;
    }

    /** Does a batch GET to AMEE and then results a map containing TaggedElements
     *  for all profile categories. The map keys are paths, e.g. "/home/heating"
     * @param profileUid
     * @return
     */
    public static Map<String, TaggedElement> getProfileMap(String profileUid) throws AmeeException {
        String response = getProfile(profileUid);
        //System.err.println("batch get xml =\n" + response);

        ArrayList al = AmeeXMLHelper.getElement(response,
                "Resources/ProfileCategoryResource/Children/ProfileCategories",
                "ProfileCategory");
        Map<String, TaggedElement> map = new HashMap();
        for (int i = 0; i < al.size(); i++) {
            TaggedElement te = (TaggedElement) al.get(i);
            //TaggedElement te = AmeeXMLHelper.getElement(response);
            TaggedElement pathTe = te.find("Path");
            String s = pathTe.getChild(0).toString();
            //System.err.println(i + " = " + s);
            map.put(s, te);
        }
        return map;
    }

    public static String createProfileItem(String profileUid, String path, String dataUid, String values) throws AmeeException {
        return createProfileItem(profileUid, path, dataUid, values, false);
    }

    /** Creates a profile item using key, sets values using values and return the kgCO2 value.*/
    public static String createProfileItem(String profileUid, String path, String key, String values, boolean returnResponse) throws AmeeException {
        //System.err.println("Creating profile item: "+profileUid+path+" : "+key+" : "+values);
        //Now search for relevant data uid
        String[] keyvals = key.split("&");
        Map map = new LinkedHashMap();
        for (int i = 0; i < keyvals.length; i++) {
            String[] ss = keyvals[i].split("=");
            if (ss.length > 1) {
                map.put(ss[0], ss[1]);
            }
        }

        String uid = AmeeXMLHelper.getDataUid(path, map);
        if (uid == null) {
            System.err.println("Failed to find uid");
            return null;
        }

        return createProfieItemFromDataUid(profileUid, path, uid, values, returnResponse);
    }

    public static String createProfieItemFromDataUid(String profileUid, String path, String dataUid, String values) throws AmeeException {
        return createProfieItemFromDataUid(profileUid, path, dataUid, values, false);
    }

    public static String createProfieItemFromDataUid(String profileUid, String path, String dataUid, String values, boolean returnResponse) throws AmeeException {
        String request = "POST /profiles/" + profileUid + path;
        String body = "dataItemUid=" + dataUid;
        if (values.length() > 0) {
            body += "&" + values;
        }
        String response = Main.sendRequest(request, body);

        String piUid = AmeeXMLHelper.getElementContents(response, "ProfileItem", "uid");
        if (returnResponse) {
            return response;
        } else {
            return piUid;
        }
    }

    public static double extractAmountPerMonth(String response) {
        double result = Double.NaN;
        String rString;
        try {
            rString = AmeeXMLHelper.getElementContents(response, "AmountPerMonth", null);
        } catch (Exception e) {
            System.err.println("Couldn't find AmountPerMonth tag");
            return result;
        }
        try {
            result = Double.parseDouble(rString);
        } catch (Exception e) {
            System.err.println("Couldn't parse result: " + e);
        }
        return result;
    }

    public static double updateProfileItem(String profileUid, String path, String values, String piUid) throws AmeeException {
        String request, response;

        request = "PUT /profiles/" + profileUid + path + "/" + piUid;
        response = Main.sendRequest(request, values, false);

        return extractAmountPerMonth(response);
    }

    public static double getProfileItemValue(String profileUid, String path, String piUid) throws AmeeException {
        double result = Double.NaN;
        String request, response;

        request = "GET /profiles/" + profileUid + path + "/" + piUid;
        response = Main.sendRequest(request, "", false);

        return extractAmountPerMonth(response);
    }

    static boolean deleteProfile(String profileUid) throws AmeeException {
        String request = "DELETE /profiles/" + profileUid;
        String response = Main.sendRequest(request, "");
        boolean success = false;
        if (response.indexOf("200 OK") >= 0) {
            success = true;
        }
        return success;
    }
    //Deletes just one item - path should be like "/home/appliances"

    static boolean deleteProfileItem(String profileUid, String piUid, String path) throws AmeeException {
        String request = "DELETE /profiles/" + profileUid + path + "/" + piUid;
        String response = Main.sendRequest(request, "");
        boolean success = false;
        if (response.indexOf("200 OK") >= 0) {
            success = true;
        }
        return success;
    }

    public static String urlEncode(String s) {
        String encode = null;
        try {
            encode = java.net.URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        return encode;
    }
}
