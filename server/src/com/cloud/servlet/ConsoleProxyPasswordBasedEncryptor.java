package com.cloud.servlet;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// To maintain independency of console proxy project, we duplicate this class from console proxy project
public class ConsoleProxyPasswordBasedEncryptor {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyPasswordBasedEncryptor.class);
	
	private String password;
	private Gson gson;
	
	public ConsoleProxyPasswordBasedEncryptor(String password) {
		this.password = password;
		gson = new GsonBuilder().create();
	}
	
	public String encryptText(String text) {
		if(text == null || text.isEmpty())
			return text;
		
		assert(password != null);
		assert(!password.isEmpty());
		
		try {
			Cipher cipher = Cipher.getInstance("DES");
			int maxKeySize = 8;
			SecretKeySpec keySpec = new SecretKeySpec(normalizeKey(password.getBytes(), maxKeySize), "DES");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
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
		}
	}

	public String decryptText(String encryptedText) {
		if(encryptedText == null || encryptedText.isEmpty())
			return encryptedText;

		assert(password != null);
		assert(!password.isEmpty());

		try {
			Cipher cipher = Cipher.getInstance("DES");
			int maxKeySize = 8;
			SecretKeySpec keySpec = new SecretKeySpec(normalizeKey(password.getBytes(), maxKeySize), "DES");
			cipher.init(Cipher.DECRYPT_MODE, keySpec);
			
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
	
	private static byte[] normalizeKey(byte[] keyBytes, int keySize) {
		assert(keySize > 0);
		byte[] key = new byte[keySize];
		
		for(int i = 0; i < keyBytes.length; i++)
			key[i%keySize] ^= keyBytes[i];
		
		return key;
	}
}
