/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.test.stress;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

public class SshTest {
    
    public static final Logger s_logger = Logger.getLogger(SshTest.class.getName());
    public static String host = "";
    public static String password = "password";
    public static String url = "http://google.com";
    
    public static void main (String[] args) {
        
        // Parameters
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if (arg.equals("-h")) {
                host = iter.next();
            }   
            if (arg.equals("-p")) {
                password = iter.next();
            }  
            
            if (arg.equals("-u")) {
                url = iter.next();
            }  
        }
    
        if (host == null || host.equals("")) {
            s_logger.info("Did not receive a host back from test, ignoring ssh test");
            System.exit(2);
        }
        
        if (password == null){
            s_logger.info("Did not receive a password back from test, ignoring ssh test");
            System.exit(2);
        }

        try {
            s_logger.info("Attempting to SSH into host " + host);
            Connection conn = new Connection(host);
            conn.connect(null, 60000, 60000);

            s_logger.info("User + ssHed successfully into host " + host);

            boolean isAuthenticated = conn.authenticateWithPassword("root", password);

            if (isAuthenticated == false) {
                s_logger.info("Authentication failed for root with password" + password);
                System.exit(2);
            }
            
            String linuxCommand = "wget " + url;
            Session sess = conn.openSession();
            sess.execCommand(linuxCommand);
            sess.close();
            conn.close();
          
        } catch (Exception e) {
            s_logger.error("SSH test fail with error", e);
            System.exit(2);
        }
    }

}
