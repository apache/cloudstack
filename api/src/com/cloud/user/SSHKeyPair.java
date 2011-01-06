package com.cloud.user;

import com.cloud.acl.ControlledEntity;

public interface SSHKeyPair extends ControlledEntity {

	/**
	 * @return The id of the key pair.
	 */
	public long getId();
	
	/**
	 * @return The given name of the key pair.
	 */
	public String getName(); 
	
	/**
	 * @return The finger print of the public key.
	 */
	public String getFingerprint();
	
	/**
	 * @return The public key of the key pair. 
	 */
	public String getPublicKey();
	
	/**
	 * @return The private key of the key pair.
	 */
	public String getPrivateKey();
	
}
