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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.exception.CloudException;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.utils.Pair;
import com.vmware.pbm.PbmProfile;
import com.vmware.vim25.DatastoreHostMount;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.FileInfo;
import com.vmware.vim25.FileQueryFlags;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.HostDatastoreBrowserSearchSpec;
import com.vmware.vim25.HostMountInfo;
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

    @Override
    public String getName() throws Exception {
        if (_name == null)
            _name = _context.getVimClient().getDynamicProperty(_mor, "name");

        return _name;
    }

    public DatastoreSummary getDatastoreSummary() throws Exception {
        return (DatastoreSummary)_context.getVimClient().getDynamicProperty(_mor, "summary");
    }

    public ManagedObjectReference getDataCenterMor() throws Exception {
        return getOwnerDatacenter().first().getMor();
    }

    public HostDatastoreBrowserMO getHostDatastoreBrowserMO() throws Exception {
        return new HostDatastoreBrowserMO(_context, (ManagedObjectReference)_context.getVimClient().getDynamicProperty(_mor, "browser"));
    }

    public List<DatastoreHostMount> getHostMounts() throws Exception {
        return _context.getVimClient().getDynamicProperty(_mor, "host");
    }

    public String getInventoryPath() throws Exception {
        Pair<DatacenterMO, String> dcInfo = getOwnerDatacenter();
        return dcInfo.second() + "/" + getName();
    }

    public Pair<DatacenterMO, String> getOwnerDatacenter() throws Exception {
        if (_ownerDc != null)
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
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> ocs = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        assert (ocs != null && ocs.size() > 0);
        assert (ocs.get(0).getObj() != null);
        assert (ocs.get(0).getPropSet() != null);
        String dcName = ocs.get(0).getPropSet().get(0).getVal().toString();
        _ownerDc = new Pair<>(new DatacenterMO(_context, ocs.get(0).getObj()), dcName);
        return _ownerDc;
    }

    public void renameDatastore(String newDatastoreName) throws Exception {
        _context.getService().renameDatastore(_mor, newDatastoreName);
    }

    public void makeDirectory(String path, ManagedObjectReference morDc) throws Exception {
        String datastoreName = getName();
        ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();

        String fullPath = path;
        if (!DatastoreFile.isFullDatastorePath(fullPath))
            fullPath = String.format("[%s] %s", datastoreName, path);

        _context.getService().makeDirectory(morFileManager, fullPath, morDc, true);

        int retry = 2;
        for (int i = 0; i < retry; i++) {
            DatastoreFile datastoreFile = new DatastoreFile(fullPath);
            if (!folderExists(String.format("[%s]", datastoreName), datastoreFile.getFileName())) {
                _context.getService().makeDirectory(morFileManager, fullPath, morDc, true);
            } else {
                return;
            }
        }
    }

    String getDatastoreRootPath() throws Exception {
        return String.format("[%s]", getName());
    }

    public String getDatastorePath(String relativePathWithoutDatastoreName) throws Exception {
        return getDatastorePath(relativePathWithoutDatastoreName, false);
    }

    public String getDatastorePath(String relativePathWithoutDatastoreName, boolean endWithPathDelimiter) throws Exception {
        String path = String.format("[%s] %s", getName(), relativePathWithoutDatastoreName);
        if (endWithPathDelimiter) {
            if (!path.endsWith("/"))
                return path + "/";
        }
        return path;
    }

    public boolean deleteFolder(String folder, ManagedObjectReference morDc) throws Exception {
        ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();
        ManagedObjectReference morTask = _context.getService().deleteDatastoreFileTask(morFileManager, folder, morDc);

        boolean result = _context.getVimClient().waitForTask(morTask);

        if (result) {
            _context.waitForTaskProgressDone(morTask);

            return true;
        } else {
            s_logger.error("VMware deleteDatastoreFile_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    public boolean deleteFile(String path, ManagedObjectReference morDc, boolean testExistence) throws Exception {
        return deleteFile(path, morDc, testExistence, null);
    }

    public boolean deleteFile(String path, ManagedObjectReference morDc, boolean testExistence, String excludeFolders) throws Exception {
        String datastoreName = getName();
        ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();

        String fullPath = path;
        if (!DatastoreFile.isFullDatastorePath(fullPath))
            fullPath = String.format("[%s] %s", datastoreName, path);
        DatastoreFile file = new DatastoreFile(fullPath);
        // Test if file specified is null or empty. We don't need to attempt to delete and return success.
        if (file.getFileName() == null || file.getFileName().isEmpty()) {
            return true;
        }

        try {
            if (testExistence && !fileExists(fullPath)) {
                String searchResult = searchFileInSubFolders(file.getFileName(), true, excludeFolders);
                if (searchResult == null) {
                    return true;
                } else {
                    fullPath = searchResult;
                }
            }
        } catch (Exception e) {
            s_logger.info("Unable to test file existence due to exception " + e.getClass().getName() + ", skip deleting of it");
            return true;
        }

        ManagedObjectReference morTask = _context.getService().deleteDatastoreFileTask(morFileManager, fullPath, morDc);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware deleteDatastoreFile_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public boolean copyDatastoreFile(String srcFilePath, ManagedObjectReference morSrcDc, ManagedObjectReference morDestDs, String destFilePath,
            ManagedObjectReference morDestDc, boolean forceOverwrite) throws Exception {

        String srcDsName = getName();
        DatastoreMO destDsMo = new DatastoreMO(_context, morDestDs);
        String destDsName = destDsMo.getName();

        ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();
        String srcFullPath = srcFilePath;
        if (!DatastoreFile.isFullDatastorePath(srcFullPath))
            srcFullPath = String.format("[%s] %s", srcDsName, srcFilePath);

        String destFullPath = destFilePath;
        if (!DatastoreFile.isFullDatastorePath(destFullPath))
            destFullPath = String.format("[%s] %s", destDsName, destFilePath);

        ManagedObjectReference morTask = _context.getService().copyDatastoreFileTask(morFileManager, srcFullPath, morSrcDc, destFullPath, morDestDc, forceOverwrite);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware copyDatastoreFile_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public boolean moveDatastoreFile(String srcFilePath, ManagedObjectReference morSrcDc, ManagedObjectReference morDestDs, String destFilePath,
            ManagedObjectReference morDestDc, boolean forceOverwrite) throws Exception {

        String srcDsName = getName();
        DatastoreMO destDsMo = new DatastoreMO(_context, morDestDs);
        String destDsName = destDsMo.getName();

        ManagedObjectReference morFileManager = _context.getServiceContent().getFileManager();
        String srcFullPath = srcFilePath;
        if (!DatastoreFile.isFullDatastorePath(srcFullPath))
            srcFullPath = String.format("[%s] %s", srcDsName, srcFilePath);

        String destFullPath = destFilePath;
        if (!DatastoreFile.isFullDatastorePath(destFullPath))
            destFullPath = String.format("[%s] %s", destDsName, destFilePath);

        DatastoreMO srcDsMo = new DatastoreMO(_context, morDestDs);
        try {
            if (!srcDsMo.fileExists(srcFullPath)) {
                s_logger.error(String.format("Cannot move file to destination datastore due to file %s does not exists", srcFullPath));
                return false;
            }
        } catch (Exception e) {
            s_logger.error(String.format("Cannot move file to destination datastore due to file %s due to exeception %s", srcFullPath, e.getMessage()));
            return false;
        }

        ManagedObjectReference morTask = _context.getService().moveDatastoreFileTask(morFileManager, srcFullPath, morSrcDc, destFullPath, morDestDc, forceOverwrite);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware moveDatastoreFile_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    @Deprecated
    public String[] listDirContent(String path) throws Exception {
        String fullPath = path;
        if (!DatastoreFile.isFullDatastorePath(fullPath))
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
        if (results != null) {
            List<FileInfo> info = results.getFile();
            if (info != null && info.size() > 0) {
                s_logger.info("File " + fileFullPath + " exists on datastore");
                return true;
            }
        }

        s_logger.info("File " + fileFullPath + " does not exist on datastore");
        return false;
    }

    public long fileDiskSize(String fileFullPath) throws Exception {
        long size = 0;
        DatastoreFile file = new DatastoreFile(fileFullPath);
        DatastoreFile dirFile = new DatastoreFile(file.getDatastoreName(), file.getDir());

        HostDatastoreBrowserMO browserMo = getHostDatastoreBrowserMO();

        HostDatastoreBrowserSearchSpec searchSpec = new HostDatastoreBrowserSearchSpec();
        FileQueryFlags fqf = new FileQueryFlags();
        fqf.setFileSize(true);
        fqf.setFileOwner(true);
        fqf.setFileType(true);
        fqf.setModification(true);
        searchSpec.setDetails(fqf);
        searchSpec.setSearchCaseInsensitive(false);
        searchSpec.getMatchPattern().add(file.getFileName());
        s_logger.debug("Search file " + file.getFileName() + " on " + dirFile.getPath()); //ROOT-2.vmdk, [3ecf7a579d3b3793b86d9d019a97ae27] s-2-VM
        HostDatastoreBrowserSearchResults result = browserMo.searchDatastore(dirFile.getPath(), searchSpec);
        if (result != null) {
            List<FileInfo> info = result.getFile();
            for (FileInfo fi : info) {
                if (file.getFileName().equals(fi.getPath())) {
                    s_logger.debug("File found = " + fi.getPath() + ", size=" + toHumanReadableSize(fi.getFileSize()));
                    return fi.getFileSize();
                }
            }
        }
        s_logger.debug("File " + fileFullPath + " does not exist on datastore");
        return size;
    }

    public boolean folderExists(String folderParentDatastorePath, String folderName) throws Exception {
        HostDatastoreBrowserMO browserMo = getHostDatastoreBrowserMO();

        HostDatastoreBrowserSearchResults results = browserMo.searchDatastore(folderParentDatastorePath, folderName, true);
        if (results != null) {
            List<FileInfo> info = results.getFile();
            if (info != null && info.size() > 0) {
                s_logger.info("Folder " + folderName + " exists on datastore");
                return true;
            }
        }

        s_logger.info("Folder " + folderName + " does not exist on datastore");
        return false;
    }

    public String searchFileInSubFolders(String fileName, boolean caseInsensitive) throws Exception {
        return searchFileInSubFolders(fileName,caseInsensitive,null);
    }

    public String searchFileInSubFolders(String fileName, boolean caseInsensitive, String excludeFolders) throws Exception {
        String datastorePath = "[" + getName() + "]";
        String rootDirectoryFilePath = String.format("%s %s", datastorePath, fileName);
        String[] searchExcludedFolders = getSearchExcludedFolders(excludeFolders);
        if (fileExists(rootDirectoryFilePath)) {
            return rootDirectoryFilePath;
        }

        String parentFolderPath;
        String absoluteFileName = null;
        s_logger.info("Searching file " + fileName + " in " + datastorePath);

        HostDatastoreBrowserMO browserMo = getHostDatastoreBrowserMO();
        ArrayList<HostDatastoreBrowserSearchResults> results = browserMo.searchDatastoreSubFolders("[" + getName() + "]", fileName, caseInsensitive);
        if (results != null && results.size() > 1) {
            s_logger.warn("Multiple files with name " + fileName + " exists in datastore " + datastorePath + ". Trying to choose first file found in search attempt.");
        } else if (results == null) {
            String msg = "No file found with name " + fileName + " found in datastore " + datastorePath;
            s_logger.error(msg);
            throw new CloudException(msg);
        }
        for (HostDatastoreBrowserSearchResults result : results) {
            List<FileInfo> info = result.getFile();
            if (info != null && info.size() > 0) {
                for (FileInfo fi : info) {
                    absoluteFileName = parentFolderPath = result.getFolderPath();
                    s_logger.info("Found file " + fileName + " in datastore at " + absoluteFileName);
                    if (parentFolderPath.endsWith("]"))
                        absoluteFileName += " ";
                    else if (!parentFolderPath.endsWith("/"))
                        absoluteFileName +="/";
                    absoluteFileName += fi.getPath();
                    if(isValidCloudStackFolderPath(parentFolderPath, searchExcludedFolders)) {
                        return absoluteFileName;
                    }
                    break;
                }
            }
        }
        return absoluteFileName;
    }

    private String[] getSearchExcludedFolders(String excludeFolders) {
        return excludeFolders != null ?  excludeFolders.replaceAll("\\s","").split(",") : new String[] {};
    }

    private boolean isValidCloudStackFolderPath(String dataStoreFolderPath, String[] searchExcludedFolders) throws Exception {
        String dsFolder = dataStoreFolderPath.replaceFirst("\\[" + getName() + "\\]", "").trim();
        for( String excludedFolder : searchExcludedFolders) {
            if (dsFolder.startsWith(excludedFolder)) {
                return  false;
            }
        }
        return true;
    }

    public boolean isAccessibleToHost(String hostValue) throws Exception {
        boolean isAccessible = true;
        List<DatastoreHostMount> hostMounts = getHostMounts();
        for (DatastoreHostMount hostMount : hostMounts) {
            String hostMountValue = hostMount.getKey().getValue();
            if (hostMountValue.equalsIgnoreCase(hostValue)) {
                HostMountInfo mountInfo = hostMount.getMountInfo();
                isAccessible = mountInfo.isAccessible();
                break;
            }
        }
        return isAccessible;
    }

    public boolean isDatastoreStoragePolicyComplaint(String storagePolicyId) throws Exception {
        PbmProfileManagerMO profMgrMo = new PbmProfileManagerMO(_context);
        PbmProfile profile = profMgrMo.getStorageProfile(storagePolicyId);

        PbmPlacementSolverMO placementSolverMo = new PbmPlacementSolverMO(_context);
        boolean isDatastoreCompatible = placementSolverMo.isDatastoreCompatibleWithStorageProfile(_mor, profile);

        return isDatastoreCompatible;
    }

    public String getDatastoreType() throws Exception {
        DatastoreSummary summary = _context.getVimClient().getDynamicProperty(getMor(), "summary");
        return summary.getType() == null ? "" : summary.getType();
    }
}
