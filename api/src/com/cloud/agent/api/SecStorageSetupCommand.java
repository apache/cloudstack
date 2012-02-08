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
package com.cloud.agent.api;

import com.cloud.agent.api.LogLevel.Log4jLevel;

public class SecStorageSetupCommand extends Command {
	private String secUrl;
	private Certificates certs;
	
	public static class Certificates {
	    @LogLevel(Log4jLevel.Off)
		private String privKey;
	    @LogLevel(Log4jLevel.Off)
		private String privCert;
	    @LogLevel(Log4jLevel.Off)
		private String certChain;
	    
	    public Certificates() {
	    	
	    }
	    
	    public Certificates(String prvKey, String privCert, String certChain) {
	    	this.privKey = prvKey;
	    	this.privCert = privCert;
	    	this.certChain = certChain;
	    }
	    
	    public String getPrivKey() {
	    	return this.privKey;
	    }
	    
	    public String getPrivCert() {
	    	return this.privCert;
	    }
	    
	    public String getCertChain() {
	    	return this.certChain;
	    }
	}
	
	public SecStorageSetupCommand() {
		super();
	}

	public SecStorageSetupCommand(String secUrl, Certificates certs) {
		super();
		this.secUrl = secUrl;
		this.certs = certs;
	}
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

    public String getSecUrl() {
        return secUrl;
    }
    
    public Certificates getCerts() {
    	return this.certs;
    }

    public void setSecUrl(String secUrl) {
        this.secUrl = secUrl;
       
    }
}
