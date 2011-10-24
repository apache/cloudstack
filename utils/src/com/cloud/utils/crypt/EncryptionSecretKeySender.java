/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.utils.crypt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import com.cloud.utils.NumbersUtil;


public class EncryptionSecretKeySender {
	public static void main(String args[]){
		try {

		    // Create a socket to the host
		    String hostname = "localhost";
		    int port = 8097;
		    
		    if(args.length == 2){
		    	hostname = args[0];
		    	port = NumbersUtil.parseInt(args[1], port);
		    }
		    		
		    		
		    InetAddress addr = InetAddress.getByName(hostname);
		    Socket socket = new Socket(addr, port);
		    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		    BufferedReader in = new BufferedReader(new InputStreamReader(
		                                socket.getInputStream()));
		    java.io.BufferedReader stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
		    String validationWord = "cloudnine";
		    String validationInput = "";
		    while(!validationWord.equals(validationInput)){
		    	System.out.print("Enter Validation Word:");
		    	validationInput = stdin.readLine();
		    	System.out.println();
		    }
		    System.out.print("Enter Secret Key:");
		    String input = stdin.readLine();
		    if (input != null) {
		        out.println(input);
		    }
		} catch (Exception e) {
			System.out.print("Exception while sending secret key "+e);
		}
	}
}