/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.utils.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.JSch;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

public class SSHKeysHelper {
	
	private KeyPair keyPair;
	private static final char[] hexChars = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };

    private static String toHexString(byte[] b) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < b.length; i++) {
                    sb.append(hexChars[ (int)(((int)b[i] >> 4) & 0x0f)]);
                    sb.append(hexChars[ (int)(((int)b[i]) & 0x0f)]);
            }
        return sb.toString();
    }

	public SSHKeysHelper() {
		try {
			 keyPair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA);
		} catch (JSchException e) {
			e.printStackTrace();
		}
	}
	
	public String getPublicKeyFingerPrint() {
		return getPublicKeyFingerprint(getPublicKey());
	}
	
	public static String getPublicKeyFingerprint(String publicKey) {
		String key[] = publicKey.split(" ");
		byte[] keyBytes = Base64.decodeBase64(key[1]);
		
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		String sumString = toHexString(md5.digest(keyBytes));
		String rString = "";
		
		for (int i = 2; i <= sumString.length(); i += 2) {
			rString += sumString.substring(i-2, i);
			if (i != sumString.length())
				rString += ":";
		}
		
		return rString;
	}
	
	public static String getPublicKeyFromKeyMaterial(String keyMaterial) {
		if (!keyMaterial.contains(" ")) 
			keyMaterial = new String(Base64.decodeBase64(keyMaterial.getBytes()));
		
		if (!keyMaterial.startsWith("ssh-rsa") || !keyMaterial.contains(" "))
			return null;
		
		String[] key = keyMaterial.split(" ");
		if (key.length < 2)
			return null;
				
		return key[0].concat(" ").concat(key[1]); 
	}
	
	public String getPublicKey() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		keyPair.writePublicKey(baos, "");
		
		return baos.toString();
	}
	
	public String getPrivateKey() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		keyPair.writePrivateKey(baos);
		
		return baos.toString();
	}
	
}
