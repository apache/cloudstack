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
package com.cloud.hypervisor.vmware.mo;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.utils.Pair;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.FileInfo;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;

public class DatastoreMO extends BaseMO {
	private static final Logger s_logger = Logger.getLogger(DatastoreMO.class);

	private String _name;
	private Pair<DatacenterMO, String> _ownerDc;

	public DatastoreMO(VmwareContext context, ManagedObjectReference morDatastore) {
		super(context, morDatastore);
	}

	public DatastoreMO(VmwareContext context, String morType, String morValue) {
		super(context, morType, morValue);
	}

	public String getName() throws Exception {
		if(_name == null)
			_name = (String)_context.getVimClient().getDynamicProperty(_mor, "name");

		return _name;
	}

	public DatastoreSummary getSummary() throws Exception {
		return (DatastoreSummary)_context.getVimClient().getDynamicProperty(_mor, "summary");
	}

	public HostDatastoreBrowserMO getHostDatastoreBrowserMO() throws Exception {
		return new HostDatastoreBrowserMO(_context,
				(ManagedObjectReference)_context.getVimClient().getDynamicProperty(_mor, "browser"));
	}

	public String getInventoryPath() throws Exception {
		Pair<DatacenterMO, String> dcInfo = getOwnerDatacenter();
		return dcInfo.second() + "/" + getName();
	}

	public Pair<DatacenterMO, String> getOwnerDatacenter() throws Exception {
		if(_ownerDc != null)
			return _ownerDc;

		PropertySpec pSpec = new PropertySpec();
		pSpec.setType("Datacenter");
		pSpec.getPathSet().add("name");

	    TraversalSpec folderParentTraversal = new TraversalSpec();
	    folderParentTraversal.setType("Folder");
	    folderParentTraversal.setPath("parent");
	    folderParentTraversal.setName("folderParentTraversal");
	    SelectionSpec sSpec = new SelectionSpec();
	    sSpec.setName("folderParentTraversal");
	    folderParentTraversal.getSelectSet().add(sSpec);

	    TraversalSpec dsParentTraversal = new TraversalSpec();
	    dsParentTraversal.setType("Datastore");
	    dsParentTraversal.setPath("parent");
	    dsParentTraversal.setName("dsParentTraversal");
	    dsParentTraversal.getSelectSet().add(folderParentTraversal);

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(getMor());
	    oSpec.setSkip(Boolean.TRUE);
	    oSpec.getSelectSet().add(dsParentTraversal);

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.getPropSet().add(pSpec);
	    pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

	    List<ObjectContent> ocs = _context.getService().retrieveProperties(
	    	_context.getPropertyCollector(), pfSpecArr);

	    assert(ocs != null && ocs.size() > 0);
	    assert(ocs.get(0).getObj() != null);
	    assert(ocs.get(0).getPropSet() != null);
	    String dcName = ocs.get(0).getPropSet().get(0).getVal().toString();
	    _ownerDc = new Pair<DatacenterMO, String>(new DatacenterMO(_context, ocs.get(0).getObj()), dcName);
	    return _ownerDc;
	}

	public void makeDirectory(String path, ManagedObjectReference morDc) throws Exception {
		String datastoreName = getName();
		ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();

		String fullPath = path;
		if(!DatastoreFile.isFullDatastorePath(fullPath))
			fullPath = String.format("[%s] %s", datastoreName, path);

		_context.getService().makeDirectory(morFileManager, fullPath, morDc, true);
	}

	public boolean deleteFile(String path, ManagedObjectReference morDc, boolean testExistence) throws Exception {
		String datastoreName = getName();
		ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();

		String fullPath = path;
		if(!DatastoreFile.isFullDatastorePath(fullPath))
			fullPath = String.format("[%s] %s", datastoreName, path);

		try {
			if(testExistence && !fileExists(fullPath))
				return true;
		} catch(Exception e) {
			s_logger.info("Unable to test file existence due to exception " + e.getClass().getName() + ", skip deleting of it");
			return true;
		}

		ManagedObjectReference morTask = _context.getService().deleteDatastoreFileTask(morFileManager, fullPath, morDc);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
			_context.waitForTaskProgressDone(morTask);
			return true;
		} else {
        	s_logger.error("VMware deleteDatastoreFile_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}
		return false;
	}

	public boolean copyDatastoreFile(String srcFilePath, ManagedObjectReference morSrcDc,
		ManagedObjectReference morDestDs, String destFilePath, ManagedObjectReference morDestDc,
		boolean forceOverwrite) throws Exception {

		String srcDsName = getName();
		DatastoreMO destDsMo = new DatastoreMO(_context, morDestDs);
		String destDsName = destDsMo.getName();

		ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();
		String srcFullPath = srcFilePath;
		if(!DatastoreFile.isFullDatastorePath(srcFullPath))
			srcFullPath = String.format("[%s] %s", srcDsName, srcFilePath);

		String destFullPath = destFilePath;
		if(!DatastoreFile.isFullDatastorePath(destFullPath))
			destFullPath = String.format("[%s] %s", destDsName, destFilePath);

		ManagedObjectReference morTask = _context.getService().copyDatastoreFileTask(morFileManager,
			srcFullPath, morSrcDc, destFullPath, morDestDc, forceOverwrite);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
			_context.waitForTaskProgressDone(morTask);
			return true;
		} else {
        	s_logger.error("VMware copyDatastoreFile_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}
		return false;
	}

	public boolean moveDatastoreFile(String srcFilePath, ManagedObjectReference morSrcDc,
		ManagedObjectReference morDestDs, String destFilePath, ManagedObjectReference morDestDc,
		boolean forceOverwrite) throws Exception {

		String srcDsName = getName();
		DatastoreMO destDsMo = new DatastoreMO(_context, morDestDs);
		String destDsName = destDsMo.getName();

		ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();
		String srcFullPath = srcFilePath;
		if(!DatastoreFile.isFullDatastorePath(srcFullPath))
			srcFullPath = String.format("[%s] %s", srcDsName, srcFilePath);

		String destFullPath = destFilePath;
		if(!DatastoreFile.isFullDatastorePath(destFullPath))
			destFullPath = String.format("[%s] %s", destDsName, destFilePath);

		ManagedObjectReference morTask = _context.getService().moveDatastoreFileTask(morFileManager,
			srcFullPath, morSrcDc, destFullPath, morDestDc, forceOverwrite);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
			_context.waitForTaskProgressDone(morTask);
			return true;
		} else {
        	s_logger.error("VMware moveDatgastoreFile_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}
		return false;
	}

	public String[] getVmdkFileChain(String rootVmdkDatastoreFullPath) throws Exception {
		Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();

		List<String> files = new ArrayList<String>();
		files.add(rootVmdkDatastoreFullPath);

		String currentVmdkFullPath = rootVmdkDatastoreFullPath;
		while(true) {
			String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), currentVmdkFullPath);
			byte[] content = getContext().getResourceContent(url);
			if(content == null || content.length == 0)
				break;

			VmdkFileDescriptor descriptor = new VmdkFileDescriptor();
			descriptor.parse(content);

			String parentFileName = descriptor.getParentFileName();
			if(parentFileName == null)
				break;

			if(parentFileName.startsWith("/")) {
				// when parent file is not at the same directory as it is, assume it is at parent directory
				// this is only valid in cloud.com primary storage deployment
				DatastoreFile dsFile = new DatastoreFile(currentVmdkFullPath);
				String dir = dsFile.getDir();
				if(dir != null && dir.lastIndexOf('/') > 0)
					dir = dir.substring(0, dir.lastIndexOf('/'));
				else
					dir = "";

				currentVmdkFullPath = new DatastoreFile(dsFile.getDatastoreName(), dir,
					parentFileName.substring(parentFileName.lastIndexOf('/') + 1)).getPath();
				files.add(currentVmdkFullPath);
			} else {
				currentVmdkFullPath = DatastoreFile.getCompanionDatastorePath(currentVmdkFullPath, parentFileName);
				files.add(currentVmdkFullPath);
			}
		}

		return files.toArray(new String[0]);
	}

	@Deprecated
	public String[] listDirContent(String path) throws Exception {
		String fullPath = path;
		if(!DatastoreFile.isFullDatastorePath(fullPath))
			fullPath = String.format("[%s] %s", getName(), fullPath);

		Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();
		String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), fullPath);

		// TODO, VMware currently does not have a formal API to list Datastore directory content,
		// folloing hacking may have performance hit if datastore has a large number of files
		return _context.listDatastoreDirContent(url);
	}

	public boolean fileExists(String fileFullPath) throws Exception {
		DatastoreFile file = new DatastoreFile(fileFullPath);
		DatastoreFile dirFile = new DatastoreFile(file.getDatastoreName(), file.getDir());

		HostDatastoreBrowserMO browserMo = getHostDatastoreBrowserMO();

		s_logger.info("Search file " + file.getFileName() + " on " + dirFile.getPath());
		HostDatastoreBrowserSearchResults results = browserMo.searchDatastore(dirFile.getPath(), file.getFileName(), true);
		if(results != null) {
			List<FileInfo> info = results.getFile();
			if(info != null && info.size() > 0) {
				s_logger.info("File " + fileFullPath + " exists on datastore");
				return true;
			}
		}

		s_logger.info("File " + fileFullPath + " does not exist on datastore");
		return false;

/*
		String[] fileNames = listDirContent(dirFile.getPath());

		String fileName = file.getFileName();
		for(String name : fileNames) {
			if(name.equalsIgnoreCase(fileName))
				return true;
		}

		return false;
*/
	}

	public boolean folderExists(String folderParentDatastorePath, String folderName) throws Exception {
		HostDatastoreBrowserMO browserMo = getHostDatastoreBrowserMO();

		HostDatastoreBrowserSearchResults results = browserMo.searchDatastore(folderParentDatastorePath, folderName, true);
		if(results != null) {
			List<FileInfo> info = results.getFile();
			if(info != null && info.size() > 0) {
				s_logger.info("Folder " + folderName + " exists on datastore");
				return true;
			}
		}

		s_logger.info("Folder " + folderName + " does not exist on datastore");
		return false;
	}
}
