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

public class StartupCommand extends Command {
    String dataCenter;
    String pod;
    String cluster;
    String guid;
    String name;
    Long id;
    String version;
    String iqn;
    String publicIpAddress;
    String publicNetmask;
    String publicMacAddress;
    String privateIpAddress;
    String privateMacAddress;
    String privateNetmask;
    String storageIpAddress;
    String storageNetmask;
    String storageMacAddress;
    String storageIpAddressDeux;
    String storageMacAddressDeux;
    String storageNetmaskDeux;
    String agentTag;
    String resourceName;
    
    public StartupCommand() {
    }
    
    public StartupCommand(Long id, String name, String dataCenter, String pod, String guid, String version) {
        super();
        this.id = id;
        this.dataCenter = dataCenter;
        this.pod = pod;
        this.guid = guid;
        this.name = name;
        this.version = version;
    }
    
    public String getIqn() {
        return iqn;
    }
    
    public void setCluster(String cluster) {
        this.cluster = cluster;
    }
    
    public String getCluster() {
        return cluster;
    }
    
    public void setIqn(String iqn) {
        this.iqn = iqn;
    }
    
    public String getDataCenter() {
        return dataCenter;
    }
    
    public String getPod() {
        return pod;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getStorageIpAddressDeux() {
		return storageIpAddressDeux;
	}

	public void setStorageIpAddressDeux(String storageIpAddressDeux) {
		this.storageIpAddressDeux = storageIpAddressDeux;
	}

	public String getStorageMacAddressDeux() {
		return storageMacAddressDeux;
	}

	public void setStorageMacAddressDeux(String storageMacAddressDeux) {
		this.storageMacAddressDeux = storageMacAddressDeux;
	}

	public String getStorageNetmaskDeux() {
		return storageNetmaskDeux;
	}

	public void setStorageNetmaskDeux(String storageNetmaskDeux) {
		this.storageNetmaskDeux = storageNetmaskDeux;
	}

	public String getGuid() {
        return guid;
    }

    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setDataCenter(String dataCenter) {
        this.dataCenter = dataCenter;
    }

    public void setPod(String pod) {
        this.pod = pod;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }
    
    public void setGuid(String guid, String resourceName) {
    	this.resourceName = resourceName;
    	this.guid = guid + "-" + resourceName;
    }

    public String getPublicNetmask() {
        return publicNetmask;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    public String getPublicMacAddress() {
        return publicMacAddress;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    public void setPrivateMacAddress(String privateMacAddress) {
        this.privateMacAddress = privateMacAddress;
    }

    public String getPrivateNetmask() {
        return privateNetmask;
    }

    public void setPrivateNetmask(String privateNetmask) {
        this.privateNetmask = privateNetmask;
    }

    public String getStorageIpAddress() {
        return storageIpAddress;
    }

    public void setStorageIpAddress(String storageIpAddress) {
        this.storageIpAddress = storageIpAddress;
    }

    public String getStorageNetmask() {
        return storageNetmask;
    }

    public void setStorageNetmask(String storageNetmask) {
        this.storageNetmask = storageNetmask;
    }

    public String getStorageMacAddress() {
        return storageMacAddress;
    }

    public void setStorageMacAddress(String storageMacAddress) {
        this.storageMacAddress = storageMacAddress;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }
    
    public String getAgentTag() {
    	return agentTag;
    }
    
    public void setAgentTag(String tag) {
    	agentTag = tag;
    }

    public void setResourceName(String resourceName) {
    	this.resourceName = resourceName;
    }
    
    public String getGuidWithoutResource() {
    	if (resourceName == null)
    	  return guid;
    	else {
    	  int hyph = guid.lastIndexOf('-');
    	  if (hyph == -1) {
    		  return guid;
    	  }
    	  String tmpResource = guid.substring(hyph+1, guid.length());
    	  if (resourceName.equals(tmpResource)){
    		  return guid.substring(0, hyph);
    	  } else {
    		  return guid;
    	  }
    	}
    }
    
    public String getResourceName() {
    	return resourceName;
    }
    
  
    
    @Override
    public boolean executeInSequence() {
        return false;
    }
}
