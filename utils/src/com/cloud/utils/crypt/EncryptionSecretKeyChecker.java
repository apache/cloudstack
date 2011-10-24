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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = {SystemIntegrityChecker.class})
public class EncryptionSecretKeyChecker implements SystemIntegrityChecker {
	
	private static final Logger s_logger = Logger.getLogger(EncryptionSecretKeyChecker.class);
	
    private static final String s_keyFile = "/etc/cloud/management/key";
    private static final String s_envKey = "CLOUD_SECRET_KEY";
    private static StandardPBEStringEncryptor s_encryptor = new StandardPBEStringEncryptor();
    private static boolean s_useEncryption = false;
    
    @Override
    public void check() {
    	//Get encryption type from db.properties
    	final File dbPropsFile = PropertiesUtil.findConfigFile("db.properties");
        final Properties dbProps = new Properties();
        try {
        	dbProps.load(new FileInputStream(dbPropsFile));

        	final String encryptionType = dbProps.getProperty("db.cloud.encryption.type");
        	
        	s_logger.debug("Encryption Type: "+ encryptionType);

        	if(encryptionType == null || encryptionType.equals("none")){
        		return;
        	}
        	
        	s_encryptor.setAlgorithm("PBEWithMD5AndDES");
        	String secretKey = null;
        	
        	SimpleStringPBEConfig stringConfig = new SimpleStringPBEConfig(); 
        	
        	if(encryptionType.equals("file")){
        		try {
        			BufferedReader in = new BufferedReader(new FileReader(s_keyFile));
        			secretKey = in.readLine();
        			//Check for null or empty secret key
        		} catch (FileNotFoundException e) {
        			throw new CloudRuntimeException("File containing secret key not found: "+s_keyFile, e);
        		} catch (IOException e) {
        			throw new CloudRuntimeException("Error while reading secret key from: "+s_keyFile, e);
        		}
        		
        		if(secretKey == null || secretKey.isEmpty()){
        			throw new CloudRuntimeException("Secret key is null or empty in file "+s_keyFile);
        		}
        		
        	} else if(encryptionType.equals("env")){
        		secretKey = System.getenv(s_envKey);
        		if(secretKey == null || secretKey.isEmpty()){
        			throw new CloudRuntimeException("Environment variable "+s_envKey+" is not set or empty");
        		}
        	} else if(encryptionType.equals("web")){
        		ServerSocket serverSocket = null;
        		int port = 8097;
        		try {
                    serverSocket = new ServerSocket(port);
                } catch (IOException ioex) {
                	throw new CloudRuntimeException("Error initializing secret key reciever", ioex);
                }
        		s_logger.info("Waiting for admin to send secret key on port "+port);
        		Socket clientSocket = null;
        		try {
        		    clientSocket = serverSocket.accept();
        		} catch (IOException e) {
        			throw new CloudRuntimeException("Accept failed on "+port);
        		}
        		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        		BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        		String inputLine, outputLine;
        		if ((inputLine = in.readLine()) != null) {
        			s_logger.info("Input : "+inputLine);
        			secretKey = inputLine;
        		}
        		out.close();
        		in.close();
        		clientSocket.close();
        		serverSocket.close();
        	} else {
        		throw new CloudRuntimeException("Invalid encryption type: "+encryptionType);
        	}

        	stringConfig.setPassword(secretKey);
			s_encryptor.setConfig(stringConfig);
			s_useEncryption = true;
        } catch (FileNotFoundException e) {
        	throw new CloudRuntimeException("File db.properties not found", e);
        } catch (IOException e) {
        	throw new CloudRuntimeException("Error while reading db.properties", e);
        }
    }
    
    public static StandardPBEStringEncryptor getEncryptor() {
        return s_encryptor;
    }
    
    public static boolean useEncryption(){
    	return s_useEncryption;
    }
}
