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
package com.cloud.consoleproxy;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * @author Kelven Yang
 * A simple password based encyrptor based on AES/CBC. It can serialize simple POJO object into URL safe string
 * and deserialize it back.
 * 
 */
public class ConsoleProxyPasswordBasedEncryptor {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyPasswordBasedEncryptor.class);
	
	private Gson gson;
	
	// key/IV will be set in 128 bit strength
	private KeyIVPair keyIvPair;
	
	public ConsoleProxyPasswordBasedEncryptor(String password) {
		gson = new GsonBuilder().create();
		keyIvPair = gson.fromJson(password, KeyIVPair.class);
	}
	
	public String encryptText(String text) {
		if(text == null || text.isEmpty())
			return text;
		
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec keySpec = new SecretKeySpec(keyIvPair.getKeyBytes(), "AES");

			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(keyIvPair.getIvBytes()));
		
			byte[] encryptedBytes = cipher.doFinal(text.getBytes());
			return Base64.encodeBase64URLSafeString(encryptedBytes);
		} catch (NoSuchAlgorithmException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (NoSuchPaddingException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (IllegalBlockSizeException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (BadPaddingException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (InvalidKeyException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (InvalidAlgorithmParameterException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		}
	}

	public String decryptText(String encryptedText) {
		if(encryptedText == null || encryptedText.isEmpty())
			return encryptedText;

		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec keySpec = new SecretKeySpec(keyIvPair.getKeyBytes(), "AES");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(keyIvPair.getIvBytes()));
			
			byte[] encryptedBytes = Base64.decodeBase64(encryptedText);
			return new String(cipher.doFinal(encryptedBytes));
		} catch (NoSuchAlgorithmException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (NoSuchPaddingException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (IllegalBlockSizeException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (BadPaddingException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (InvalidKeyException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		} catch (InvalidAlgorithmParameterException e) {
			s_logger.error("Unexpected exception ", e);
			return null;
		}
	}
	
	public <T> String encryptObject(Class<?> clz, T obj) {
		if(obj == null)
			return null;
		
		String json = gson.toJson(obj);
		return encryptText(json);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T decryptObject(Class<?> clz, String encrypted) {
		if(encrypted == null || encrypted.isEmpty())
			return null;
		
		String json = decryptText(encrypted);
		return (T)gson.fromJson(json, clz);
	}
	
	public static class KeyIVPair {
		String base64EncodedKeyBytes;
		String base64EncodedIvBytes;
		
		public KeyIVPair() {
		}
		
		public KeyIVPair(String base64EncodedKeyBytes, String base64EncodedIvBytes) {
			this.base64EncodedKeyBytes = base64EncodedKeyBytes;
			this.base64EncodedIvBytes = base64EncodedIvBytes;
		}

		public byte[] getKeyBytes() {
			return Base64.decodeBase64(base64EncodedKeyBytes);
		}
		
		public void setKeyBytes(byte[] keyBytes) {
			base64EncodedKeyBytes = Base64.encodeBase64URLSafeString(keyBytes);
		}

		public byte[] getIvBytes() {
			return Base64.decodeBase64(base64EncodedIvBytes);
		}
		
		public void setIvBytes(byte[] ivBytes) {
			base64EncodedIvBytes = Base64.encodeBase64URLSafeString(ivBytes);
		}
	}
	
	public static void main(String[] args) {
		SecureRandom random;
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
	        byte[] keyBytes = new byte[16];
	        random.nextBytes(keyBytes);
	        
	        byte[] ivBytes = new byte[16];
	        random.nextBytes(ivBytes);
			
			KeyIVPair keyIvPair = new KeyIVPair("8x/xUBgX0Up+3UEo39dSeG277JhVj31+ElHkN5+EC0Q=", "Y2SUiIN6JXTdKNK/ZMDyVtLB7gAM9MCCiyrP1xd3bSQ=");
			//keyIvPair.setKeyBytes(keyBytes);	
			//keyIvPair.setIvBytes(ivBytes);
			
			Gson gson = new GsonBuilder().create();
			ConsoleProxyPasswordBasedEncryptor encryptor = new ConsoleProxyPasswordBasedEncryptor(gson.toJson(keyIvPair));
			
			String encrypted = encryptor.encryptText("Hello, world");
			
			System.out.println("Encrypted result: " + encrypted);
			
			String decrypted = encryptor.decryptText(encrypted);
			
			System.out.println("Decrypted result: " + decrypted);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
 	}
}
