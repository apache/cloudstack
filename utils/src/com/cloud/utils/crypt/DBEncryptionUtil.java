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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.properties.EncryptableProperties;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class DBEncryptionUtil {
	
	public static final Logger s_logger = Logger.getLogger(DBEncryptionUtil.class);
	private static StandardPBEStringEncryptor s_encryptor = null;
	
    public static String encrypt(String plain){
    	if(!EncryptionSecretKeyChecker.useEncryption() || (plain == null) || plain.isEmpty()){
    		return plain;
    	}
    	if(s_encryptor == null){
    		initialize();
    	}
    	String encryptedString = null;
		try {
			encryptedString = s_encryptor.encrypt(plain);
		} catch (EncryptionOperationNotPossibleException e) {
			s_logger.debug("Error while encrypting: "+plain);
			throw e;
		}
    	return encryptedString;
    }
    
    public static String decrypt(String encrypted){
    	if(!EncryptionSecretKeyChecker.useEncryption() || (encrypted == null) || encrypted.isEmpty()){
    		return encrypted;
    	}
    	if(s_encryptor == null){
    		initialize();
    	}
    	
    	String plain = null;
    	try {
			plain = s_encryptor.decrypt(encrypted);
		} catch (EncryptionOperationNotPossibleException e) {
			s_logger.debug("Error while decrypting: "+encrypted);
			throw e;
		}
    	return plain;
    }
    
    private static void initialize(){
    	final File dbPropsFile = PropertiesUtil.findConfigFile("db.properties");
        final Properties dbProps; 
        
        if(EncryptionSecretKeyChecker.useEncryption()){
        	StandardPBEStringEncryptor encryptor = EncryptionSecretKeyChecker.getEncryptor();
        	dbProps = new EncryptableProperties(encryptor);
        	try {
				dbProps.load(new FileInputStream(dbPropsFile));
			} catch (FileNotFoundException e) {
				throw new CloudRuntimeException("db.properties file not found while reading DB secret key", e);
			} catch (IOException e) {
				throw new CloudRuntimeException("Erroe while reading DB secret key from db.properties", e);
			}
        	
        	String dbSecretKey = dbProps.getProperty("db.cloud.encrypt.secret");
        	if(dbSecretKey == null || dbSecretKey.isEmpty()){
        		throw new CloudRuntimeException("Empty DB secret key in db.properties");
        	}
        	
        	s_encryptor = new StandardPBEStringEncryptor();
        	s_encryptor.setAlgorithm("PBEWithMD5AndDES");
        	s_encryptor.setPassword(dbSecretKey);
        } else {
        	throw new CloudRuntimeException("Trying to encrypt db values when encrytion is not enabled");
        }
    }
}
