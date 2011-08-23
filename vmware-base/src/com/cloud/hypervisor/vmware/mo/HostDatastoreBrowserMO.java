/* *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 *
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
 
package com.cloud.hypervisor.vmware.mo;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.HostDatastoreBrowserSearchSpec;
import com.vmware.vim25.ManagedObjectReference;

public class HostDatastoreBrowserMO extends BaseMO {
	
    private static final Logger s_logger = Logger.getLogger(HostDatastoreBrowserMO.class);
	
	public HostDatastoreBrowserMO(VmwareContext context, ManagedObjectReference morHostDatastoreBrowser) {
		super(context, morHostDatastoreBrowser);
	}
	
	public HostDatastoreBrowserMO(VmwareContext context, String morType, String morValue) {
		super(context, morType, morValue);
	}
	
	public void DeleteFile(String datastoreFullPath) throws Exception {
		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - deleteFile(). target mor: " + _mor.get_value() + ", file datastore path: " + datastoreFullPath);
		
		_context.getService().deleteFile(_mor, datastoreFullPath);
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - deleteFile() done");
	}
	
	public HostDatastoreBrowserSearchResults searchDatastore(String datastorePath, HostDatastoreBrowserSearchSpec searchSpec) throws Exception {
		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - searchDatastore(). target mor: " + _mor.get_value() + ", file datastore path: " + datastorePath);

		try {
			ManagedObjectReference morTask = _context.getService().searchDatastore_Task(_mor, datastorePath, searchSpec);
			
			String result = _context.getServiceUtil().waitForTask(morTask);
			if(result.equals("sucess")) {
				_context.waitForTaskProgressDone(morTask);
				
				return (HostDatastoreBrowserSearchResults)_context.getServiceUtil().getDynamicProperty(morTask, "info.result");
			} else {
	        	s_logger.error("VMware searchDaastore_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
			}
		} finally {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - searchDatastore() done");
		}
		
		return null;
	}
	
	public HostDatastoreBrowserSearchResults searchDatastore(String datastorePath, String fileName, boolean caseInsensitive) throws Exception {
		HostDatastoreBrowserSearchSpec spec = new HostDatastoreBrowserSearchSpec();
		spec.setSearchCaseInsensitive(caseInsensitive);
		spec.setMatchPattern(new String[] { fileName });

		return searchDatastore(datastorePath, spec);
	}
	
	public HostDatastoreBrowserSearchResults searchDatastoreSubFolders(String datastorePath, HostDatastoreBrowserSearchSpec searchSpec) throws Exception {
		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - searchDatastoreSubFolders(). target mor: " + _mor.get_value() + ", file datastore path: " + datastorePath);

		try {
			ManagedObjectReference morTask = _context.getService().searchDatastoreSubFolders_Task(_mor, datastorePath, searchSpec);
			
			String result = _context.getServiceUtil().waitForTask(morTask);
			if(result.equals("sucess")) {
				_context.waitForTaskProgressDone(morTask);
				
				return (HostDatastoreBrowserSearchResults)_context.getServiceUtil().getDynamicProperty(morTask, "info.result");
			} else {
	        	s_logger.error("VMware searchDaastoreSubFolders_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
			}
		} finally {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - searchDatastore() done");
		}
		
		return null;
	}
	
	public HostDatastoreBrowserSearchResults searchDatastoreSubFolders(String datastorePath, String folderName, boolean caseInsensitive) throws Exception {
		HostDatastoreBrowserSearchSpec spec = new HostDatastoreBrowserSearchSpec();
		spec.setSearchCaseInsensitive(caseInsensitive);
		spec.setMatchPattern(new String[] { folderName });

		return searchDatastore(datastorePath, spec);
	}
}

