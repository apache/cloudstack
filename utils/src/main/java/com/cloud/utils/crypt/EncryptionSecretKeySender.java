//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils.crypt;


import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import com.cloud.utils.NumbersUtil;


public class EncryptionSecretKeySender {
    public static void main(String args[]) {
        try {
            // Create a socket to the host
            String hostname = "localhost";
            int port = 8097;

            if (args.length == 2) {
                hostname = args[0];
                port = NumbersUtil.parseInt(args[1], port);
            }
            InetAddress addr = InetAddress.getByName(hostname);
            try(Socket socket = new Socket(addr, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);)
            {
                java.io.BufferedReader stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
                String validationWord = "cloudnine";
                String validationInput = "";
                while (!validationWord.equals(validationInput)) {
                    System.out.print("Enter Validation Word:");
                    validationInput = stdin.readLine();
                    System.out.println();
                }
                System.out.print("Enter Secret Key:");
                String input = stdin.readLine();
                if (input != null) {
                    out.println(input);
                }
            }catch (Exception e)
            {
                System.out.println("Exception " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.print("Exception while sending secret key " + e);
        }
   }
}
