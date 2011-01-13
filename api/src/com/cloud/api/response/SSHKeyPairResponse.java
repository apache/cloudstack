package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SSHKeyPairResponse extends BaseResponse {
	
    @SerializedName(ApiConstants.NAME) @Param(description="Name of the keypair")
    private String name;

    @SerializedName("fingerprint") @Param(description="Fingerprint of the public key")
    private String fingerprint;
    
    @SerializedName("privatekey") @Param(description="Private key")
    private String privateKey;

    public SSHKeyPairResponse() {}
    
    public SSHKeyPairResponse(String name, String fingerprint) {
    	this(name, fingerprint, null);
    }
    
    public SSHKeyPairResponse(String name, String fingerprint, String privateKey) {
    	this.name = name;
    	this.fingerprint = fingerprint;
    	this.privateKey = privateKey;
    }
    
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}
    
}
