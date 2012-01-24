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

import java.io.*;
import java.net.*;
import java.util.*;

import com.twicom.qdparser.TaggedElement;

//import javax.xml.parsers.*; using custom xml parsers
/**
 *
 * @author nalu
 */
public class Main {

    public static String localFileCharSet = "UTF-8";
    public static String apiWriteCharSet = "ISO-8859-1";
    public static String apiReadCharSet = "UTF-8";
    public static String authToken = null;
    public static HashMap cookiesMap = new HashMap();
    public static String host = "stage.co2.dgen.net"; //DON'T CHANGE THESE HERE, SET THEM IN THE main METHOD
    public static String ip = null;//"85.133.58.72";//normally null, useful if dns probs
    public static int port = 80;
    public static Proxy proxy = Proxy.NO_PROXY;
    public static String login;
    public static String password;
    public static boolean debug = false;

    public Main() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String response;

        //IMPORTANT: Remember to change these to your login details
        login = "***USERNAME***";
        password = "***PASSWORD***";

        /* EXAMPLE 1: Create a profile, create a profile item for a microwave and get the kgCO2 value*/
        //Create a profile in the API and get the profile uid for use subsequent examples
        System.err.println("\nEXAMPLE 1: create a profile with a microwave ********************************");

        String profile_uid = createProfile();
        System.err.println("profile_uid = " + profile_uid);

        //The microwave has no parameters, hence the "" at the end of the arg list.
        String profile_item_uid = createProfileItem(profile_uid, "/home/appliances/kitchen/generic", "device=microwave", "");
        System.err.println("microwave profile_item_uid = " + profile_item_uid);

        //The returned value is the kgCO2 emitted by typical usage of a microwave per month
        double kgCO2perMonth = getProfileItemValue(profile_uid, "/home/appliances/kitchen/generic", profile_item_uid);
        System.err.println("kgCO2 per month = " + kgCO2perMonth);

        /* EXAMPLE 2: Using same profile, create a profile item for 10 low energy bulbs and get the kgCO2 value*/
        //now there is one parameter - we specify the number of bulbs

        System.err.println("\nEXAMPLE 2: create a profile item with 10 low energy lightbulbs ********************************");
        profile_item_uid = createProfileItem(profile_uid, "/home/lighting", "type=lel", "noOfLowEnergyLightBulbs=10");
        System.err.println("low energy lightbulb profile_item_uid = " + profile_item_uid);

        //The returned value is the kgCO2 emitted by typical usage of 10 low energy light bulbs
        kgCO2perMonth = getProfileItemValue(profile_uid, "/home/lighting", profile_item_uid);
        System.err.println("10 low energy lightbulbs kgCO2 per month = " + kgCO2perMonth);

        /* EXAMPLE 3: Using same profile, update the previous lel profile to 1 bulb and get the kgCO2 value*/

        //now just call updateProfileItem with new parameter, and new value is returned
        System.err.println("\nEXAMPLE 3: update the last profile item to be just one low energy bulb ********************************");
        kgCO2perMonth = updateProfileItem(profile_uid, "/home/lighting", "noOfLowEnergyLightBulbs=1", profile_item_uid);
        System.err.println("1 low energy lightbulb kgCO2 per month = " + kgCO2perMonth);

        /* EXAMPLE 4: Delete a profile item */

        System.err.println("\nEXAMPLE 4: Delete a profile item ********************************");
        boolean success = deleteProfileItem(profile_uid, profile_item_uid, "/home/lighting");
        System.err.println("Deleted profile item " + profile_item_uid + " = " + success);

        /* EXAMPLE 5: Drill down - this can be used to populate options in a GUI */

        System.err.println("\nEXAMPLE 5: Drill down ********************************");
        //Note: the following drill down urls can be directly entered into API's web interface:
        //try puttting this url into your browser: http://stage.co2.dgen.net/data/transport/car/generic/drill
        //See the heatingExample() method (which isn't called in this file) for a more complete
        //example on use of drill down to get from UI-supplied details to profile item creation.
        response = sendRequest("GET /data/transport/car/generic/drill", "");
        //the xml response will give the first parameter "fuel" and its possible values "petrol","diesel","petrol hybrid"
        response = sendRequest("GET /data/transport/car/generic/drill?fuel=petrol", "");
        //the xml response will give the next parameter "size" and its possible values "small","medium","large"
        response = sendRequest("GET /data/transport/car/generic/drill?fuel=petrol&size=large", "");
        //the xml response will now contain no further options but will give us the data item uid.
        //The data item uid can be then be used to create a profile item - see the createProfieItemFromDataUid method
        System.err.println("final drill down response = " + response);

        /* EXAMPLE 6: Get kgCO2 result without actually creating a profile item */

        System.err.println("\nEXAMPLE 6: kgCO2 result without creating a profile item ********************************");
        //First we need to look up the data item uid
        Map searchMap = new HashMap();
        searchMap.put("fuel", "diesel"); //case doesn't matter
        searchMap.put("size", "large");
        String duid = AmeeXMLHelper.getDataUid("/transport/car/generic", searchMap);
        response = sendRequest("GET /data/transport/car/generic/" + duid + "?distanceKmPerMonth=100", "");
        //The value will be in <AmountPerMonth> xml element
        System.err.println("large diesel car 100km response = " + response);
        System.err.println("large diesel car 100km amount per month = " + AmeeXMLHelper.getElementContents(response, "AmountPerMonth", null));

        /* EXAMPLE 7: Create profile item, set a value and get the result all in ONE API call */
        //Note: Doing this is efficient as it can - depending on the nature your
        //implementation - replace three API calls with just one.

        System.err.println("\nEXAMPLE 7: Three API calls in one ********************************");
        response = sendRequest("POST /profiles/" + profile_uid + "/transport/car/generic", "name=mainCar&dataItemUid=" + duid + "&distanceKmPerMonth=100");
        //The value will be in <AmountPerMonth> xml element
        System.err.println("large diesel car 100km response = " + response);
        System.err.println("large diesel car 100km amount per month = " + AmeeXMLHelper.getElementContents(response, "AmountPerMonth", null));

        /* EXAMPLE 8: Profile history - create a profile item for June 2007 */
        //NOTE: Normally you won't have to set validFrom explicitly as it will default
        //to the current date and you will be revisiting it at some later date.
        System.err.println("\nEXAMPLE 8: Profile history ********************************");
        response = sendRequest("POST /profiles/" + profile_uid + "/transport/car/generic", "validFrom=20070601&name=mainCar&dataItemUid=" + duid + "&distanceKmPerMonth=1000");
        //The value will be in <AmountPerMonth> xml element
        System.err.println("large diesel car June 2007, 1000km response =\n" + response);
        System.err.println("large diesel car June 2007, 1000km amount per month = " + AmeeXMLHelper.getElementContents(response, "AmountPerMonth", null));
        System.err.println("--");
        //Get the info about the profile item for the current date
        response = sendRequest("GET /profiles/" + profile_uid + "/transport/car/generic", "");
        //The value will be in <AmountPerMonth> xml element
        System.err.println("Get value for large diesel car, current date response =\n" + response);
        System.err.println("Get value for large diesel car, current date response = " + AmeeXMLHelper.getElementContents(response, "TotalAmountPerMonth", null));
        System.err.println("--");
        //Now get the profile item info for July - this BEFORE the current date, but AFTER the profile item 20070601 validFrom set above
        response = sendRequest("GET /profiles/" + profile_uid + "/transport/car/generic?profileDate=200707", "");
        //The value will be in <AmountPerMonth> xml element
        System.err.println("Get value for large diesel car, for July 2007 response =\n" + response);
        System.err.println("Get value for large diesel car, for July 2007 amount per month = " + AmeeXMLHelper.getElementContents(response, "TotalAmountPerMonth", null));
        System.err.println("--");
        /* EXAMPLE 9: Delete a profile */

        System.err.println("\nEXAMPLE 9: Delete the profile ********************************");
        success = deleteProfile(profile_uid);
        System.err.println("Deleted profile " + profile_uid + " = " + success);
    }

    public static String sendRequest(String path, String body) {
        return sendRequest(path, body, true);
    }

    public static String sendRequest(String path, String body, boolean includeResponseHeader) {
        return sendRequest(path, body, "Content-Type: application/x-www-form-urlencoded", includeResponseHeader);
    }

    private static Socket getSocket() throws IOException {
        Socket socket = new Socket();//proxy);
        InetSocketAddress insa;
        if (ip != null) {
            InetAddress ina = InetAddress.getByName(ip);
            //System.err.println("ina=" + ina);
            insa = new InetSocketAddress(ina, port);
        } else {
            //System.err.println("Connecting to "+host);
            insa = new InetSocketAddress(host, port);
        }

        socket.connect(insa);

        return socket;
    }
    private static boolean isSecondAttempt = false;

    public static String sendRequest(String path, String body, String contentType, boolean includeResponseHeader) {
        if (authToken == null && path.compareToIgnoreCase("POST /auth") != 0) {
            authToken = getAuthToken();
            //System.err.println("authToken=" + authToken);
        }

        String response = null;
        Socket socket = null;
        boolean error = false;
        try {
            //socket = new Socket(host, port); //orig method
            socket = getSocket();
            InputStreamReader isr = new InputStreamReader(socket.getInputStream(), Main.apiReadCharSet);
            OutputStreamWriter osr = new OutputStreamWriter(socket.getOutputStream(), Main.apiWriteCharSet);
            BufferedReader r = new BufferedReader(isr);
            BufferedWriter w = new BufferedWriter(osr);
            String s = path + " HTTP/1.0\n" + getCookieLines() //insert cookies
                    + "Accept: application/xml\n"
                    + "X-AMEE-Source: ameem\n";
            if (authToken != null) {
                s += "authToken: " + authToken + "\n";
            }
            s += "Host: " + host + "\n";
            s += contentType + "\n";
            s += "Content-Length: " + body.length() + "\n" + "\n" + body;
            System.err.println(s);
            writeLine(w, s);
            response = readLines(r);
            if (debug) {
                System.err.println("request=" + s);
                System.err.println("response=" + response);
            }
            socket.close();
            storeCookies(response); //remember cookies
        } catch (IOException ioe) {
            System.out.println("I/O Error: " + ioe);
            error = true;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }
        String firstLine = response.substring(0, response.indexOf('\n')).trim();
        if (error) {
            response = "ERROR";
        } else if (firstLine.indexOf("401") >= 0) {
            //Try again - just once
            if (isSecondAttempt) {
                System.err.println("FATAL ERROR - AUTH FAILED AFTER SECOND ATTEMPT - ABORTING");
                System.exit(666);
            }
            if (debug) {
                System.err.println("WARNING - AUTH FAILED - TRYING ONCE MORE...");
            }
            authToken = null;
            isSecondAttempt = true;
            response = sendRequest(path, body, true);
            isSecondAttempt = false; //if we got here, auth must have succeeded, so reset and carry on as normal
        }

        if (!includeResponseHeader) {
            response = getXMLfromResponse(response);
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

    public static void storeCookies(String response) {
        //find cookies and store them in the cookiesmap
        int pos = -1;
        while ((pos = response.indexOf("Set-Cookie:", pos)) >= 0) {
            int p1 = response.indexOf(":", pos);
            int p2 = response.indexOf(";", p1);
            String s = response.substring(p1 + 1, p2).trim();
            String[] ss = s.split("=");
            if (ss[0].compareToIgnoreCase("authtoken") != 0) {
                System.err.println("found cookie: " + ss[0] + "=" + ss[1]);
                cookiesMap.put(ss[0], ss[1]);
            }
            pos = p2;
        }
    }

    public static String getCookieLines() {
        String lines = "";
        //goes through the cookiesMap and outputs appropriate header lines
        Iterator iter = cookiesMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next().toString();
            String value = (String) cookiesMap.get(key);
            String cookie = "Cookie: " + key + "=" + value;
            lines += cookie + "\n";
        }
        //System.err.println("Sending these cookies:\n"+lines);
        return lines;
    }

    /** Read and discard count lines from BufferedReader r */
    public static String readLines(BufferedReader r) throws IOException {
        String all = "", t = null;
        do {
            t = r.readLine();
            if (t != null) {
                //System.err.println(">"+t);
                all += t + "\n";
            }
        } while (t != null);
        return all;
    }

    /** Write out to BufferedWriter w and flush */
    public static void writeLine(BufferedWriter w, String out) throws IOException {
        w.write(out + "\n", 0, (out + "\n").length());
        w.flush();
    }

    public static String getAuthToken() {
        if (debug) {
            System.err.println("Getting authToken...");
            //String request = "username=" + login + "&password=" + password;
            //System.err.println("request = " + request);
        }
        String response = sendRequest("POST /auth", "username=" + login + "&password=" + password);
        if (debug) {
            System.err.println("response = " + response);
        }
        String authToken = null;
        String[] lines = response.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].matches("authToken.*")) {
                String[] atv = lines[i].split(":");
                authToken = atv[1].trim();
                if (debug) {
                    System.err.println("token = " + authToken);
                }
            }
        }
        return authToken;
    }
    //Creates a profile and returns the profile uid

    public static String createProfile() {
        String response = sendRequest("POST /profiles", "profile=true", false);
        String pUid = AmeeXMLHelper.getElementContents(response, "Profile", "uid");

        return pUid;
    }

    public static String getProfile(String profileUid) {
        String response = sendRequest("GET /profiles/" + profileUid + "?recurse=true", "", false);
        return response;
    }

    /** Does a batch GET to AMEE and then results a map containing TaggedElements
     *  for all profile categories. The map keys are paths, e.g. "/home/heating"
     * @param profileUid
     * @return
     */
    public static Map<String, TaggedElement> getProfileMap(String profileUid) {
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

    public static String createProfileItem(String profileUid, String path, String dataUid, String values) {
        return createProfileItem(profileUid, path, dataUid, values, false);
    }

    /** Creates a profile item using key, sets values using values and return the kgCO2 value.*/
    public static String createProfileItem(String profileUid, String path, String key, String values, boolean returnResponse) {
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

    public static String createProfieItemFromDataUid(String profileUid, String path, String dataUid, String values) {
        return createProfieItemFromDataUid(profileUid, path, dataUid, values, false);
    }

    public static String createProfieItemFromDataUid(String profileUid, String path, String dataUid, String values, boolean returnResponse) {
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

    public static double updateProfileItem(String profileUid, String path, String values, String piUid) {
        String request, response;

        request = "PUT /profiles/" + profileUid + path + "/" + piUid;
        response = Main.sendRequest(request, values, false);

        return extractAmountPerMonth(response);
    }

    public static double getProfileItemValue(String profileUid, String path, String piUid) {
        double result = Double.NaN;
        String request, response;

        request = "GET /profiles/" + profileUid + path + "/" + piUid;
        response = Main.sendRequest(request, "", false);

        return extractAmountPerMonth(response);
    }

    static boolean deleteProfile(String profileUid) {
        String request = "DELETE /profiles/" + profileUid;
        String response = Main.sendRequest(request, "");
        boolean success = false;
        if (response.indexOf("200 OK") >= 0) {
            success = true;
        }
        return success;
    }
    //Deletes just one item - path should be like "/home/appliances"

    static boolean deleteProfileItem(String profileUid, String piUid, String path) {
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
