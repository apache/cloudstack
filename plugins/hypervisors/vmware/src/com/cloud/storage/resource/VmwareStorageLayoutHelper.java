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
package com.cloud.storage.resource;

import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.utils.Pair;

/**
 * 
 * To provide helper methods to handle storage layout in one place
 *
 */
public class VmwareStorageLayoutHelper {
	
    public static String[] getVmdkFilePairDatastorePath(DatastoreMO dsMo, String vmName, String vmdkName, 
    	VmwareStorageLayoutType layoutType, boolean linkedVmdk) throws Exception {
 
    	String[] filePair = new String[2];
    	switch(layoutType) {
    	case VMWARE :
    		assert(vmName != null && !vmName.isEmpty());
    		filePair[0] = String.format("[%s] %s/%s.vmdk", dsMo.getName(), vmName, vmdkName);
    		
    		if(linkedVmdk)
    			filePair[1] = String.format("[%s] %s/%s-delta.vmdk", dsMo.getName(), vmName, vmdkName);
    		else
    			filePair[1] = String.format("[%s] %s/%s-flat.vmdk", dsMo.getName(), vmName, vmdkName);
    		return filePair;
    	
    	case CLOUDSTACK_LEGACY :
    		filePair[0] = String.format("[%s] %s.vmdk", dsMo.getName(), vmdkName);
    		
    		if(linkedVmdk)
    			filePair[1] = String.format("[%s] %s-delta.vmdk", dsMo.getName(), vmdkName);
    		else
    			filePair[1] = String.format("[%s] %s-flat.vmdk", dsMo.getName(), vmdkName);
    		return filePair;
    		
    	default :
    		assert(false);
    		break;
    	}
    	
    	assert(false);
    	return null;
    }
    
    public static String getTemplateOnSecStorageFilePath(String secStorageMountPoint, String templateRelativeFolderPath,
    	String templateName, String fileExtension) {
    	
    	StringBuffer sb = new StringBuffer();
    	sb.append(secStorageMountPoint);
    	if(!secStorageMountPoint.endsWith("/"))
    		sb.append("/");
    	
    	sb.append(templateRelativeFolderPath);
    	if(!secStorageMountPoint.endsWith("/"))
    		sb.append("/");
    	
    	sb.append(templateName);
    	if(!fileExtension.startsWith("."))
    		sb.append(".");
    	sb.append(fileExtension);
    	
    	return sb.toString();
    }
    
    /*
     *  return Pair of <Template relative path, Template name>
     *  Template url may or may not end with .ova extension
     */
    public static Pair<String, String> decodeTemplateRelativePathAndNameFromUrl(String storeUrl, String templateUrl, 
    	String defaultName) {
    	
        String templateName = null;
        String mountPoint = null;
        if (templateUrl.endsWith(".ova")) {
            int index = templateUrl.lastIndexOf("/");
            mountPoint = templateUrl.substring(0, index);
            mountPoint = mountPoint.substring(storeUrl.length() + 1);
            if (!mountPoint.endsWith("/")) {
                mountPoint = mountPoint + "/";
            }

            templateName = templateUrl.substring(index + 1).replace(".ova", "");
            
            if (templateName == null || templateName.isEmpty()) {
                templateName = defaultName;
            }
        } else {
            mountPoint = templateUrl.substring(storeUrl.length() + 1);
            if (!mountPoint.endsWith("/")) {
                mountPoint = mountPoint + "/";
            }
            templateName = defaultName;
        }
        
        return new Pair<String, String>(mountPoint, templateName);
    }
    
	public static void deleteVolumeVmdkFiles(DatastoreMO dsMo, String volumeName, DatacenterMO dcMo) throws Exception {
        String volumeDatastorePath = String.format("[%s] %s.vmdk", dsMo.getName(), volumeName);
        dsMo.deleteFile(volumeDatastorePath, dcMo.getMor(), true);

        volumeDatastorePath = String.format("[%s] %s-flat.vmdk", dsMo.getName(), volumeName);
        dsMo.deleteFile(volumeDatastorePath, dcMo.getMor(), true);

        volumeDatastorePath = String.format("[%s] %s-delta.vmdk", dsMo.getName(), volumeName);
        dsMo.deleteFile(volumeDatastorePath, dcMo.getMor(), true);
	}
}
