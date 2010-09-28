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
package com.cloud.template;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.cloud.api.commands.AttachIsoCmd;
import com.cloud.api.commands.CopyIsoCmd;
import com.cloud.api.commands.CopyTemplateCmd;
import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.DetachIsoCmd;
import com.cloud.api.commands.ExtractIsoCmd;
import com.cloud.api.commands.ExtractTemplateCmd;
import com.cloud.api.commands.RegisterIsoCmd;
import com.cloud.api.commands.RegisterTemplateCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.storage.Storage.FileSystem;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.component.Manager;

/**
 * TemplateManager manages the templates stored
 * on secondary storage.  It is responsible for
 * creating private/public templates.  It is
 * also responsible for downloading.
 */
public interface TemplateManager extends Manager {
	
    /**
     * Creates a Template
     * 
     * @param userId the  Id of the user
     * @param zoneId (optional) the zone to download the template to
     * @param name - user specified name for the template
     * @param displayText user readable name.
     * @param isPublic is this a public template?
     * @param featured is this template featured?
     * @param format which image format is the template.
     * @param fs what is the file system on the template
     * @param url url to download the template from.
     * @param chksum chksum to compare it to.
     * @param requiresHvm does this template require hvm?
     * @param bits is the os contained on the template 32 bit?
     * @param enablePassword Does the template support password change.
     * @param guestOSId OS that is on the template
     * @param bootable true if this template will represent a bootable ISO
     * @return id of the template created.
     */
//    Long create(long userId, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, ImageFormat format, FileSystem fs, URI url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable);
    Long registerTemplate(RegisterTemplateCmd cmd) throws InvalidParameterValueException, URISyntaxException, ResourceAllocationException;
    Long registerIso(RegisterIsoCmd cmd) throws InvalidParameterValueException, IllegalArgumentException, ResourceAllocationException;   
    /**
     * Creates a Template
     * 
     * @param zoneId zone to create the template in
     * @param displayText user readable name.
     * @param isPublic is this a public template?
     * @param featured is this template featured?
     * @param format which image format is the template.
     * @param fs what is the file system on the template
     * @param url url to download the template from.
     * @param chksum chksum to compare it to.
     * @param requiresHvm does this template require hvm?
     * @param bits is the os contained on the template 32 bit?
     * @param enablePassword Does the template support password change.
     * @param guestOSId OS that is on the template
     * @param bootable true if this template will represent a bootable ISO
     * @return id of the template created.
     */
    Long createInZone(long zoneId, long userId, String displayText, boolean isPublic, boolean featured, ImageFormat format, FileSystem fs, URI url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable);
    
    /**
     * Prepares a template for vm creation for a certain storage pool.
     * 
     * @param template template to prepare
     * @param pool pool to make sure the template is ready in.
     * @return VMTemplateStoragePoolVO if preparation is complete; null if not.
     */
    VMTemplateStoragePoolVO prepareTemplateForCreate(VMTemplateVO template, StoragePoolVO pool);
    
    /**
     * Copies a template from its current secondary storage server to the secondary storage server in the specified zone.
     * @param templateId
     * @param sourceZoneId
     * @param destZoneId
     * @return true if success
     * @throws InternalErrorException        URI uri = new URI(url);
        if ( (uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("ftp") )) {
           throw new IllegalArgumentException("Unsupported scheme for url: " + url);
        }
        String host = uri.getHost();
        
        try {
        	InetAddress hostAddr = InetAddress.getByName(host);
        	if (hostAddr.isAnyLocalAddress() || hostAddr.isLinkLocalAddress() || hostAddr.isLoopbackAddress() || hostAddr.isMulticastAddress() ) {
        		throw new IllegalArgumentException("Illegal host specified in url");
        	}
        	if (hostAddr instanceof Inet6Address) {
        		throw new IllegalArgumentException("IPV6 addresses not supported (" + hostAddr.getHostAddress() + ")");
        	}
        } catch (UnknownHostException uhe) {
        	throw new IllegalArgumentException("Unable to resolve " + host);
        }
        
    	if (_dcDao.findById(zoneId) == null) {
    		throw new IllegalArgumentException("Please specify a valid zone.");
    	}
        
        VMTemplateVO template = findTemplateById(templateId);
        
        VMTemplateHostVO tmpltHostRef = findTemplateHostRef(templateId, zoneId);
        if (tmpltHostRef != null && tmpltHostRef.getDownloadState() != com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED){
        	throw new IllegalArgumentException("The template hasnt been downloaded ");
        }
     * @throws StorageUnavailableException 
     * @throws InvalidParameterValueException 
     */
    boolean copy(long userId, long templateId, long sourceZoneId, long destZoneId, long startEventId) throws InternalErrorException, StorageUnavailableException, InvalidParameterValueException;
    VMTemplateVO copyIso(CopyIsoCmd cmd) throws InvalidParameterValueException, StorageUnavailableException, PermissionDeniedException;
    VMTemplateVO copyTemplate(CopyTemplateCmd cmd) throws InvalidParameterValueException, StorageUnavailableException, PermissionDeniedException;
    
    /**
     * Deletes a template from secondary storage servers
     * @param userId
     * @param templateId
     * @param zoneId - optional. If specified, will only delete the template from the specified zone's secondary storage server.
     * @return true if success
     */
    boolean delete(long userId, long templateId, Long zoneId) throws InternalErrorException;
    
    boolean detachIso(DetachIsoCmd cmd) throws InternalErrorException, InvalidParameterValueException, PermissionDeniedException;
    
    boolean attachIso(AttachIsoCmd cmd) throws InternalErrorException, InvalidParameterValueException, PermissionDeniedException;
    
    /**
     * Lists templates in the specified storage pool that are not being used by any VM.
     * @param pool
     * @return list of VMTemplateStoragePoolVO
     */
    List<VMTemplateStoragePoolVO> getUnusedTemplatesInPool(StoragePoolVO pool);
    
    /**
     * Deletes a template in the specified storage pool.
     * @param templatePoolVO
     */
    void evictTemplateFromStoragePool(VMTemplateStoragePoolVO templatePoolVO);
    
    boolean templateIsDeleteable(VMTemplateHostVO templateHostRef);
 
    /**
     * Deletes a template
     * @param cmd - the command specifying templateId
     */
    boolean deleteTemplate(DeleteTemplateCmd cmd) throws InvalidParameterValueException, InternalErrorException, PermissionDeniedException;
    
    /**
     * Deletes a template
     * @param cmd - the command specifying isoId
     * @return true if deletion is successful, false otherwise
     * @throws InvalidParameterValueException, InternalErrorException, PermissionDeniedException
     */
    boolean deleteIso(DeleteIsoCmd cmd) throws InvalidParameterValueException, InternalErrorException, PermissionDeniedException;

	void extract(VMTemplateVO template, String url, VMTemplateHostVO tmpltHostRef, Long zoneId, long eventId, long asyncJobId, AsyncJobManager asyncMgr);
    void extract(ExtractIsoCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    void extract(ExtractTemplateCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
}
