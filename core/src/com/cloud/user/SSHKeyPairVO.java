package com.cloud.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="ssh_keypairs")
public class SSHKeyPairVO implements SSHKeyPair {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private Long id = null;
	
	@Column(name="account_id")
	private long accountId;
	
    @Column(name="domain_id")
    private long domainId;
    
    @Column(name="keypair_name")
    private String name;
    
    @Column(name="fingerprint")
    private String fingerprint;
    
    @Column(name="public_key")
    private String publicKey;
    
    @Transient
    private String privateKey;

	@Override
	public long getId() {
		return id;
	}
	
	@Override
	public long getAccountId() {
		return accountId;
	}

	@Override
	public long getDomainId() {
		return domainId;
	}
	
	@Override
	public String getFingerprint() {
		return fingerprint;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPublicKey() {
		return publicKey;
	}
	
	@Override
	public String getPrivateKey() {
		return privateKey;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
	
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

}
