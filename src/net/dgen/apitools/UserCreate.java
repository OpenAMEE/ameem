/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dgen.apitools;

import com.amee.client.AmeeException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import net.dgen.apiexamples.Main;

/**
 *
 * @author nalu
 */
public class UserCreate {

    private static boolean testMode = false;
    String fullname, username, password, email;
    String cloneUserUid = "2932650C435D";//regular user, no data item editting, agreenerplace

    public UserCreate(String csvLine) {
        String[] ss = csvLine.split(",");
        fullname = ss[0];
        username = ss[1];
        password = ss[2];
        email = ss[3];
    }

    public boolean create() throws AmeeException {
        boolean success = false;
        String request, response;

        request = "POST /admin/users";
        String body = "cloneUserUid=" + cloneUserUid;
        body += "&name=" + fullname;
        body += "&username=" + username;
        body += "&password=" + password;
        body += "&email=" + email;
        if (testMode) {
            System.err.println(request + "\n" + body);
            response = "TEST MODE 200 ok";
        } else {
            response = Main.sendRequest(request, body);
        }

        if (response.toLowerCase().indexOf("200 ok") >= 0) {
            System.err.println("success: "+username);
            success = true;
        } else {
            System.err.println("response = " + response);
        }
        return success;
    }

    private static boolean createFromCSV(File file) throws AmeeException {
        boolean success = false;
        BufferedReader br = ApiTools.getBufferedReader(file);
        try {
            String line = br.readLine();//skip first header line
            while ((line = br.readLine()) != null) {
                UserCreate uc = new UserCreate(line);
                success = uc.create();
                if (!success) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws AmeeException {
        int site = DataCategory.STAGE;
        ApiTools.isAdmin = true;
        ApiTools.init(site);
        //Main.debug = true;
        /*String s = "Aardvark Inc,aardvark,moo,ping@pong.com";
        UserCreate uc = new UserCreate(s);
        System.err.println("success = " + uc.create());*/
        File file = new File("/home/nalu/docs/amee/clients/nesta/apikeys.csv");
        //testMode = true;
        System.err.println("success = " + createFromCSV(file));
    }
}