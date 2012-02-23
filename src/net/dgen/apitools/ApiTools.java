/*
 * ApiTools.java
 *
 * Created on 29 August 2007, 18:53
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.dgen.apitools;

import com.amee.client.AmeeException;
import com.twicom.qdparser.TaggedElement;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import net.dgen.apiexamples.Main;
import net.dgen.apiexamples.AmeeXMLHelper;

/**
 *
 * T2D
 * 1 auto generate release notes" and "undo instructions"
 * @author nalu
 */
public class ApiTools {
    public static boolean isAdmin = false;
    public static File cachePath;
    public static String adminEnvironmentUid;
    public static String freeHost="";
    public static String freeAdminHost="";
    public static Map<String, String> valueDefMap;
    static int currentSite;
    static File csvDir;

    /** Creates a new instance of ApiTools */
    public ApiTools() {
    }

    /** Note: Main.login and Main.password must be set elsewhere. */
    public static void init(int site) {
        
        currentSite = site;
        if (site == DataCategory.STAGE) {
            Main.setHost("stage.amee.com");
            cachePath = new File("/home/nalu/dev/amee/uid_cache/stage");
            if (isAdmin) {
                adminEnvironmentUid = "5F5887BCF726";
                valueDefMap = ItemDefinitionSync.stageValueDefMap;
            }
        } else if (site == DataCategory.SANDBOX) {
            Main.setHost("sandbox.amee.com");
            cachePath = new File("/home/nalu/dev/amee/uid_cache/sandbox");
            if (isAdmin) {
                adminEnvironmentUid = "5F5887BCF726";
                valueDefMap = ItemDefinitionSync.stageValueDefMap;
            }
        } else if (site == DataCategory.SCIENCE) {
            Main.setHost("platform-science.amee.com");
            cachePath = new File("/home/nalu/dev/amee/uid_cache/sandbox");
            if (isAdmin) {
                adminEnvironmentUid = "5F5887BCF726";
                valueDefMap = ItemDefinitionSync.stageValueDefMap;
            }
        }
        else if (site == DataCategory.LIVE) {
            Main.setHost("live.amee.com");
            cachePath = new File("/home/nalu/dev/amee/uid_cache/live");
            if (isAdmin) {
                //fortunately, these are the same for live as stage! Might no always be so tho.
                adminEnvironmentUid = "5F5887BCF726";
                valueDefMap = ItemDefinitionSync.liveValueDefMap;
            }
        } else if (site == DataCategory.JB) {
            Main.setHost("jb.live.amee.com");
            cachePath = new File("/home/nalu/dev/amee/uid_cache/live");
            if (isAdmin) {
                //fortunately, these are the same for live as stage! Might no always be so tho.
                adminEnvironmentUid = "5F5887BCF726";
                valueDefMap = ItemDefinitionSync.liveValueDefMap;
            }
        } else if (site == DataCategory.DEFRA) {
            Main.setHost("defra.co2.dgen.net");
            cachePath = new File("/home/nalu/dev/amee/uid_cache/defra");
        } else if (site == DataCategory.DEV) {
            Main.setHost("platform-dev.amee.com");
            cachePath = new File("/home/nalu/dev/amee/uid_cache/dev");
            if (isAdmin) {
                //fortunately, these are the same for live as stage! Might no always be so tho.
                adminEnvironmentUid = "5F5887BCF726";
                valueDefMap = ItemDefinitionSync.stageValueDefMap;
            }
        } else if (site==DataCategory.FREE) {
            Main.setHost(freeHost);
            cachePath = new File("/home/nalu/dev/amee/uid_cache/stage");
            if (isAdmin) {
                adminEnvironmentUid = "5F5887BCF726";
                valueDefMap = ItemDefinitionSync.stageValueDefMap;
                Main.setHost(freeAdminHost);
            }
            return;
        } else {
            System.err.println("FATAL ERROR - ABORTING: Unknown site value: site = " + site);
            System.exit(0);
        }

        if (isAdmin) {
            if(site == DataCategory.LIVE){
                Main.setHost("admin-live.amee.com");
            } else if (site != DataCategory.DEV && site != DataCategory.JB && site != DataCategory.SCIENCE && site != DataCategory.STAGE) {
                Main.setHost("admin." + Main.getHost());
            } else if (site == DataCategory.DEV || site == DataCategory.JB || site == DataCategory.SCIENCE || site == DataCategory.STAGE) {
                Main.setHost("admin-" + Main.getHost());
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String request, response;

        //This block deletes all data items found in a drill down - use with care!
        //Doesn't work where drill data item values are blank
        /*String path = "/data/test/tv";
        ArrayList al = getDataItemsForDrill(path, "");
        System.err.println("data item uids for deletion=" + al);
        Iterator iter = al.iterator();
        while (iter.hasNext()) {
        String req = "DELETE " + path + "/" + iter.next();
        System.err.println("req=" + req);
        response = Main.sendRequest(req, "");
        if (response.toLowerCase().indexOf("200 ok") < 0) {
        System.err.println("ERROR:\n" + response);
        }
        }*/
    }

    public static void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList getDataItemsForDrill(String path, String drill) throws AmeeException {
        ArrayList uidAl = new ArrayList();
        //System.err.println("drill="+drill);
        String response = Main.sendRequest(
                "GET " + path + "/drill?" + drill, "");
        //System.err.println("response=" + response);
        ArrayList al = AmeeXMLHelper.getElement(response,
                "Resources/DrillDownResource/Choices/", "Name");
        //System.err.println("al = " + al);
        TaggedElement te = (TaggedElement) al.remove(0);
        String drillName = Main.urlEncode(te.getChild(0).toString());
        //System.err.println("drillName=" + drillName);
        Iterator iter = al.iterator();
        while (iter.hasNext()) {
            te = (TaggedElement) iter.next();
            String choice = te.getChild(0).toString();
            //System.err.println("te="+te);
            //System.err.println("choice="+choice);
            if (drillName.equals("uid")) {
                uidAl.add(choice);
                System.err.println("Adding " + drill);
                /*String req = "DELETE " + path + "/" + choice;
                System.err.println("req=" + req);
                response = Main.sendRequest(req, "");
                if (response.toLowerCase().indexOf("200 ok") < 0) {
                System.err.println("ERROR:\n" + response);
                }*/
            } else {
                ArrayList retAl = getDataItemsForDrill(path, drill + "&" + drillName + "=" + Main.urlEncode(choice));
                uidAl.addAll(retAl);
            }
        }
        return uidAl;
    }

    public static String getValueFromXML(
            String xml, String dataItemValueName) {
        String xmlName = dataItemValueName + "</name><value>";
        xmlName =
                xmlName.toLowerCase();
        //System.err.println("xmlName="+xmlName);
        xml =
                xml.toLowerCase();
        //System.err.println(xml);
        int start = xml.indexOf(xmlName) + xmlName.length();
        int end = xml.indexOf("</value>", start);
        String s = xml.substring(start, end);
        return s;
    }
    public static String lastDataItemUid;

    public static String getDataItemFromKeyMap(
            String path, Map keyMap) throws AmeeException {
        //System.err.println("Getting data item: "+path+" : "+key);
        double result = Double.NaN;
        //Now search for relevant data uid
        //String uid = Main.getDataUid(path,searchList);
        String uid = AmeeXMLHelper.getDataUid(path, keyMap);
        lastDataItemUid =
                uid;
        if (uid == null) {
            System.err.println("Failed to find uid: try clearing uid_cache?");
            return null;
        } else if (uid.indexOf("missing") >= 0) {
            System.err.println("Silly uid: " + uid);
            return null;
        }

        return getDataItemFromUid(path, uid);
    }

    public static String getDataItemFromUid(String path, String uid) throws AmeeException {
        String request = "GET /data" + path + "/" + uid;
        //System.err.println("request = "+request);
        String response = Main.sendRequest(request, "");
        //System.err.println("response = "+response);
        if (response.toLowerCase().indexOf("404 not") >= 0) {
            response = null;
        }
        return response;
    }

    public static boolean updateDataItem(String path, Map keyMap, String values, boolean update) throws AmeeException {
        //Now search for relevant data uid
        //String uid = Main.getDataUid(path,searchList);
        String uid = AmeeXMLHelper.getDataUid(path, keyMap);
        if (uid == null) {
            System.err.println("    " + "Failed to find uid");
            return false;
        } else if (uid.indexOf("missing") >= 0) {
            System.err.println("    " + "Silly uid: " + uid);
            return false;
        }

        String request = "PUT /data" + path + "/" + uid;
        System.err.println("    " + request + "\n" + "    " + values);
        if (update) {
            String response = Main.sendRequest(request, values);
            //System.err.println("    "+"response = "+response);
            if (response.indexOf("200 OK") < 0) {
                System.err.println("ERROR: failed to update data item:\n" + request + "\n" + response);
                return false;
                //System.exit(0);
            } else {
                System.err.println("    " + "Item successfully updated\n");
            }

        } else {
            System.err.println("    " + "CHECK ONLY: API NOT ALTERED");
        }

        return true;
    }

    public static boolean createDataItem(String apiPath, String postString, boolean update) throws AmeeException {
        String request = "POST /data" + apiPath;
        System.err.println("    " + request + "\n" + "    " + postString);
        if (update) {
            String response = Main.sendRequest(request, postString);
            if (response.indexOf("200 OK") < 0) {
                System.err.println("ERROR: failed to create data item:\n" + request + "\n" + response);
                return false;
                //System.exit(0);
            } else {
                System.err.println("Item successfully created.\n");
            }

        } else {
            System.err.println("    " + "CHECK ONLY: API NOT ALTERED");
        }

        return true;
    }

    public static boolean batchDataItems(String method, String apiPath, String xml, boolean update) throws AmeeException {
        String request = method + " /data" + apiPath;
        System.err.println("    " + request + "\n" + "    " + xml);
        if (update) {
            String response = Main.sendRequest(request, xml, "Content-Type:application/xml", true);
            if (response.indexOf("200 OK") < 0) {
                System.err.println("ERROR: failed to batch " + method + " data items:\n" + request + "\n" + response);
                return false;
                //System.exit(0);
            } else {
                System.err.println("Batched " + method + "data items successfully.\n");
            }

        } else {
            System.err.println("    " + "CHECK ONLY: API NOT ALTERED");
        }

        return true;
    }

    public static String cleanString(
            String s) {
        //return s.trim().toLowerCase().replaceAll("\"","");
        return s.trim();
    }

    public static BufferedReader getBufferedReader(
            File file) {
        BufferedReader br = null;

        if (file != null) {
            //fname=validateFileName(fname);

            //            fname=fname.replace('?', 'Q'); // Replace any question marks with Qs.
            //System.out.println("Trying to save this file: " + fname);
            try {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, Main.localFileCharSet);
                br =
                        new BufferedReader(isr);
                return br;
            } catch (IOException e) {
                System.err.println("IOException opening file " + file + " for reading: " + e);
            }

        }
        return null;
    }

    public static BufferedWriter getBufferedWriter(
            File file) {
        BufferedWriter bw = null;

        if (file != null) {
            //fname=validateFileName(fname);
            //            fname=fname.replace('?', 'Q'); // Replace any question marks with Qs.
            //System.out.println("Trying to save this file: " + fname);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter osr = new OutputStreamWriter(fos, Main.localFileCharSet);
                bw =
                        new BufferedWriter(osr);
                return bw;
            } catch (IOException e) {
                System.err.println("IOException opening file " + file + " for writing: " + e);
            }

        }
        return null;
    }
}
