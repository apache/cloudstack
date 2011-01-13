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
