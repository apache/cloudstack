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

package com.cloud.hypervisor.ovm3.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

/*
 * should become an interface implementation
 */
public class StoragePlugin extends OvmObject {
    private static final String EMPTY_STRING = "";
    private static final String PLUGINPATH = "//Discover_Storage_Plugins_Result/storage_plugin_info_list/storage_plugin_info";
    private static final String NFSPLUGIN = "oracle.generic.NFSPlugin.GenericNFSPlugin";
    private static final String FILESYS = "FileSys";
    private static final String STATUS = "status";
    private static final String UUID = "uuid";
    private static final String SSUUID = "ss_uuid";
    private static final String SIZE = "size";
    private static final String FREESIZE = "free_sz";
    private static final String STATE = "state";
    private static final String ACCESSGROUPNAMES = "access_grp_names";
    private static final String ACCESSPATH = "access_path";
    private static final String NAME = "name";
    private static final String MOUNTOPTIONS = "mount_options";
    private static final String ADMINUSER = "admin_user";
    private static final String ADMINHOST = "admin_host";
    private static final String TOTALSIZE = "total_sz";
    private static final String ADMINPASSWORD = "admin_passwd";
    private static final String STORAGEDESC = "storage_desc";
    private static final String ACCESSHOST = "access_host";
    private static final String STORAGETYPE = "storage_type";
    private static final String ALLOCSIZE = "alloc_sz";
    private static final String ACCESSGROUPS = "access_grps";
    private static final String USEDSIZE = "used_sz";
    private static final String FRTYPE = "fr_type";
    private static final String ONDISKSIZE = "ondisk_sz";
    private static final String FSUUID = "fs_uuid";
    private static final String FILEPATH = "file_path";
    private static final String FILESIZE = "file_sz";
    private static final Boolean ACTIVE = true;

    private String getPluginType = NFSPLUGIN;
    private List<String> supportedPlugins = new ArrayList<String>();
    private final List<String> someList = new ArrayList<String>(); /* empty */
    private FileProperties fileProperties = new FileProperties();
    private StorageDetails storageDetails = new StorageDetails();
    private StorageServer storageServer = new StorageServer();

    public StoragePlugin(Connection c) {
        setClient(c);
    }

    /* uuid has dashes here!, and ss_uuid is the relation to the storage source uuid */
    public class StorageDetails {
        private Map<String, Object> storageDetails = new HashMap<String, Object>() {
            {
                put(STATUS, EMPTY_STRING);
                put(UUID, EMPTY_STRING);
                put(SSUUID, EMPTY_STRING);
                put(SIZE, EMPTY_STRING);
                put(FREESIZE, 0);
                put(STATE, 0);
                put(ACCESSGROUPNAMES, new ArrayList<String>());
                put(ACCESSPATH, EMPTY_STRING);
                put(NAME, EMPTY_STRING);
                put(MOUNTOPTIONS, new ArrayList<String>());
            }
            private static final long serialVersionUID = 3L;
        };
        public Map<String, Object> getDetails() {
            return storageDetails;
        }
        public void setDetails(Map<String, Object> details) {
            storageDetails = details;
        }
        public void setSize(String val) {
            storageDetails.put(SIZE, val);
        }
        public String getSize() {
            return (String) storageDetails.get(SIZE);
        }
        public void setFreeSize(String val) {
            storageDetails.put(FREESIZE, val);
        }
        public String getFreeSize() {
            return (String) storageDetails.get(FREESIZE);
        }
        public void setState(Integer val) {
            storageDetails.put(STATE, val);
        }
        public Integer getState() {
            return (Integer) storageDetails.get(STATE);
        }
        public void setStatus(String val) {
            storageDetails.put(STATUS, val);
        }
        public String getStatus() {
            return (String) storageDetails.get(STATUS);
        }
        /* format depends on storagesource type ? */
        public void setAccessPath(String val) {
            storageDetails.put(ACCESSPATH, val);
        }
        public String getAccessPath() {
            return (String) storageDetails.get(ACCESSPATH);
        }
        public void setName(String val) {
            storageDetails.put(NAME, val);
        }
        public String getName() {
            return (String) storageDetails.get(NAME);
        }
        public void setUuid(String val) throws Ovm3ResourceException {
            if (!val.contains("-")) {
                throw new Ovm3ResourceException("Storage Details UUID should contain dashes: " + val);
            }
            storageDetails.put(UUID, val);
        }
        public String getUuid() {
            return (String) storageDetails.get(UUID);
        }
        public void setDetailsRelationalUuid(String val) throws Ovm3ResourceException {
            if (val.contains("-")) {
                throw new Ovm3ResourceException("Storage Details UUID that relates to Storage Source should notcontain dashes: " + val);
            }
            storageDetails.put(SSUUID, val);
        }
        public String getDetailsRelationalUuid() {
            return (String) storageDetails.get(SSUUID);
        }
        public void setAccessGroupNames(List<String> l) {
            storageDetails.put(ACCESSGROUPNAMES, l);
        }
        public List<String> getAccessGroupNames() {
            return (List<String>) storageDetails.get(ACCESSGROUPNAMES);
        }
        public void setMountOptions(List<String> l) {
            storageDetails.put(MOUNTOPTIONS, l);
        }
        public List<String> getMountOptions() {
            return (List<String>) storageDetails.get(MOUNTOPTIONS);
        }
    }

    /* mind you uuid has NO dashes here */
    public class StorageServer {
        private Map<String, Object> storageSource = new HashMap<String, Object>() {
            {
                put(STATUS, EMPTY_STRING);
                put(ADMINUSER, EMPTY_STRING);
                put(ADMINHOST, EMPTY_STRING);
                put(UUID, EMPTY_STRING);
                put(TOTALSIZE, 0);
                put(ADMINPASSWORD, EMPTY_STRING);
                put(STORAGEDESC, EMPTY_STRING);
                put(FREESIZE, 0);
                put(ACCESSHOST, EMPTY_STRING);
                put(STORAGETYPE, EMPTY_STRING);
                put(ALLOCSIZE, 0);
                put(ACCESSGROUPS,  new ArrayList<String>());
                put(USEDSIZE, 0);
                put(NAME, EMPTY_STRING);
            }
            private static final long serialVersionUID = 4L;
        };
        public Map<String, Object> getDetails() {
            return storageSource;
        }
        public void setDetails(Map<String, Object> details) {
            storageSource = details;
        }
        public void setAccessGroups(List<String> l) {
            storageSource.put(ACCESSGROUPS, l);
        }
        public List<String> getAccessGroups() {
            return (List<String>) storageSource.get(ACCESSGROUPS);
        }
        public void setStatus(String val) {
            storageSource.put(STATUS, val);
        }
        public String getStatus() {
            return (String) storageSource.get(STATUS);
        }
        public void setAdminUser(String val) {
            storageSource.put(ADMINUSER, val);
        }
        public String getAdminUser() {
            return (String) storageSource.get(ADMINUSER);
        }
        public void setAdminHost(String val) {
            storageSource.put(ADMINHOST, val);
        }
        public String getAdminHost() {
            return (String) storageSource.get(ADMINHOST);
        }
        public void setUuid(String val) throws Ovm3ResourceException {
            if (val.contains("-")) {
                throw new Ovm3ResourceException("Storage Source UUID should not contain dashes: " + val);
            }
            storageSource.put(UUID, val);
        }
        public String getUuid() {
            return (String) storageSource.get(UUID);
        }
        public String getTotalSize() {
            return (String) storageSource.get(TOTALSIZE);
        }
        public void setTotalSize(Integer val) {
            storageSource.put(TOTALSIZE, val);
        }
        public void setAdminPassword(String val) {
            storageSource.put("admin_password", val);
        }
        public String getAdminPassword() {
            return (String) storageSource.get("admin_password");
        }
        public void setDescription(String val) {
            storageSource.put(STORAGEDESC, val);
        }
        public String getDescription() {
            return (String) storageSource.get(STORAGEDESC);
        }
        public String getFreeSize() {
            return (String) storageSource.get(FREESIZE);
        }
        public void setFreeSize(Integer val) {
            storageSource.put(FREESIZE, val);
        }
        public void setAccessHost(String val) {
            storageSource.put(ACCESSHOST, val);
        }
        public String getAccessHost() {
            return (String) storageSource.get(ACCESSHOST);
        }
        public void setStorageType(String val) {
            storageSource.put(STORAGETYPE, val);
        }
        public String getStorageType() {
            return (String) storageSource.get(STORAGETYPE);
        }
        public void setAllocationSize(Integer val) {
            storageSource.put(ALLOCSIZE, val);
        }
        public Integer getAllocationSize() {
            return (Integer) storageSource.get(ALLOCSIZE);
        }
        public void setUsedSize(Integer val) {
            storageSource.put(USEDSIZE, val);
        }
        public Integer getUsedSize() {
            return (Integer) storageSource.get(USEDSIZE);
        }
        public void setName(String val) {
            storageSource.put(NAME, val);
        }
        public String getName() {
            return (String) storageSource.get(NAME);
        }
    }

    public class FileProperties {
        private Map<String, Object> fileProperties = new HashMap<String, Object>() {
            {
                put(FRTYPE, EMPTY_STRING);
                put(ONDISKSIZE, EMPTY_STRING);
                put(FSUUID, EMPTY_STRING);
                put(FILEPATH, EMPTY_STRING);
                put(FILESIZE, EMPTY_STRING);
            }
            private static final long serialVersionUID = 1234L;
        };
        public Map<String, Object> getProperties() {
            return fileProperties;
        }
        public void setProperties(Map<String, Object> props) {
            fileProperties = props;
        }
        public String getName() {
            return (String) fileProperties.get(FILEPATH);
        }
        public String setName(String f) {
            return (String) fileProperties.put(FILEPATH, f);
        }
        public String setType(String t) {
            return (String) fileProperties.put(FRTYPE, t);
        }
        public String getType() {
            return (String) fileProperties.get(FRTYPE);
        }
        public void setSize(Long t) {
            fileProperties.put(FILESIZE, t);
        }
        public Long getSize() {
            return Long.parseLong((String) fileProperties.get(FILESIZE));
        }
        public String setOnDiskSize(String t) {
            return (String) fileProperties.put(ONDISKSIZE, t);
        }
        public String getOnDiskSize() {
            return (String) fileProperties.get(ONDISKSIZE);
        }
        public String setUuid(String t) {
            return (String) fileProperties.put(FSUUID, t);
        }
        public String getUuid() {
            return (String) fileProperties.get(FSUUID);
        }
    }

    public String getPluginType() {
        return getPluginType;
    }
    private Boolean setPluginType(String val) throws Ovm3ResourceException {
        for(String plugin : discoverStoragePlugins()) {
            if (plugin.matches("(?i:.*"+val+".*)")) {
                getPluginType = plugin;
                return true;
            }
        }
        return false;
    }
    public Boolean setISCSI() throws Ovm3ResourceException {
        return setPluginType("SCSI");
    }
    public Boolean setOCFS2() throws Ovm3ResourceException {
        return setPluginType("OCFS2");
    }
    public Boolean setNFS() throws Ovm3ResourceException {
        return setPluginType("NFS");
    }

    /* Actions for the storage plugin */
    /*
     * storage_plugin_resizeFileSystem, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getStatus, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None meh ?
     */

    /*
     * storage_plugin_validate, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_setQoS, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * now only for files
     * storage_plugin_create, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None - calls resize secretly.. after "create"
     */
    public FileProperties storagePluginCreate(String poolUuid, String host,
            String file, Long size, Boolean dir) throws Ovm3ResourceException{
        /* this is correct ordering stuff and correct naming!!! */
        String uuid = deDash(poolUuid);
        StorageServer ss = new StorageServer();
        StorageDetails sd = new StorageDetails();
        FileProperties fp = new FileProperties();
        ss.setUuid(uuid);
        ss.setStorageType(FILESYS);
        ss.setAccessHost(host);
        sd.setUuid(poolUuid);
        sd.setDetailsRelationalUuid(uuid);
        sd.setState(2);
        String type = "File";
        if (dir) {
            type = "Directory";
        }
        fp.setProperties((HashMap<String, Object>) callWrapper("storage_plugin_create",
                getPluginType, ss.getDetails(),
                sd.getDetails(), file, type, size));
        return fp;
    }

    /*
     * storage_plugin_createAccessGroups, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_deviceTeardown, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_startPresent, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_listFileSystems, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */
    public Boolean storagePluginListFs(String host) throws Ovm3ResourceException {
        StorageServer ss = new StorageServer();
        ss.setAccessHost(host);
        ss.setStorageType(FILESYS);
        ss.setDetails((Map<String, Object>) callWrapper("storage_plugin_listFileSystems",
                getPluginType, ss.getDetails()));
        return true;
    }

    /*
     * storage_plugin_getFileSystemCloneLimits, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getQoSList, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_stopPresent, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_isCloneable, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /**
     * . storage_plugin_mount, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */
    public final StorageDetails storagePluginMountNFS(String nfsHost, String nfsRemotePath,
            String mntUuid, String mountPoint) throws Ovm3ResourceException {
        String propUuid = deDash(mntUuid);
        StorageServer ss = new StorageServer();
        StorageDetails sd = new StorageDetails();
        ss.setUuid(propUuid);
        ss.setName(propUuid);
        ss.setAccessHost(nfsHost);
        sd.setDetailsRelationalUuid(propUuid);
        sd.setUuid(mntUuid);
        sd.setAccessPath(nfsHost + ":" + nfsRemotePath);
        if (!mountPoint.contains(mntUuid)) {
            mountPoint += "/" + mntUuid;
        }
        sd.setDetails((HashMap<String, Object>) callWrapper(
                "storage_plugin_mount", getPluginType, ss.getDetails(),
                sd.getDetails(), mountPoint, EMPTY_STRING, ACTIVE,
                someList));
        /* this magically means it's already mounted....
         * double check */
        if (sd.getDetails() == null) {
            sd = storagePluginGetFileSystemInfo(propUuid,
                    mntUuid, nfsHost, nfsRemotePath);
        }
        if (EMPTY_STRING.contains(ss.getUuid())) {
            throw new Ovm3ResourceException("Unable to mount NFS FileSystem");
        }
        return sd;
    }

    /**
     * . storage_plugin_unmount, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     *
     * @return boolean
     *
     */
    public final Boolean storagePluginUnmountNFS(String nfsHost, String remotePath, String mntUuid, String localPath) throws Ovm3ResourceException {
        StorageServer ss = new StorageServer();
        StorageDetails sd = new StorageDetails();
        sd.setUuid(mntUuid);
        sd.setDetailsRelationalUuid(deDash(mntUuid));
        ss.setUuid(deDash(mntUuid));
        ss.setAccessHost(nfsHost);
        sd.setAccessPath(nfsHost + ":" + remotePath);
        sd.setState(1);
        ss.setStorageType(FILESYS);
        String mountPoint = localPath + "/" + mntUuid;
        callWrapper("storage_plugin_unmount", getPluginType,
                ss.getDetails(), sd.getDetails(), mountPoint, ACTIVE);
        return true;
    }

    /*
     * storage_plugin_resize, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_deviceSizeRefresh, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */
    /*
     * storage_plugin_getStorageNames, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_splitClone, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_destroyFileSystem, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_snapRestore, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_updateSERecords, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getSnapLimits, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * discover_storage_plugins, <class 'agent.api.storageplugin.StoragePlugin'>
     */
    public List<String> discoverStoragePlugins() throws Ovm3ResourceException{
        supportedPlugins = new ArrayList<String>();
        Object result = callWrapper("discover_storage_plugins");
        if (result == null) {
            return supportedPlugins;
        }
        Document xmlDocument = prepParse((String) result);
        supportedPlugins.addAll(xmlToList(PLUGINPATH + "/@plugin_impl_name", xmlDocument));
        return supportedPlugins;
    }

    private Map<String,String> checkStoragePluginDetails(String plugin, Boolean ability) throws Ovm3ResourceException {
        Object result = callWrapper("discover_storage_plugins");
        Document xmlDocument = prepParse((String) result);
        if (discoverStoragePlugins().contains(plugin)) {
            String details = PLUGINPATH + "[@plugin_impl_name='" + plugin + "']";
            if (ability) {
                return xmlToMap(details + "/abilities", xmlDocument);
            } else {
                return xmlToMap(details, xmlDocument);
            }
        } else {
            throw new Ovm3ResourceException("StoragePlugin should be one of: " + supportedPlugins);
        }
    }

    private String checkStoragePluginBoth(String type, String property, Boolean ab) throws Ovm3ResourceException{
        String val = checkStoragePluginDetails(type, ab).get(property);
        if (val == null) {
            throw new Ovm3ResourceException("StoragePlugin " + type + " has no " + property);
        }
        return val;
    }

    public String checkStoragePluginAbility(String type, String property) throws Ovm3ResourceException {
        return checkStoragePluginBoth(type, property, true);
    }
    public String checkStoragePluginProperties(String type, String property) throws Ovm3ResourceException {
        return checkStoragePluginBoth(type, property, false);
    }

    /*
     * storage_plugin_deviceResize, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getCloneLimits, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * INFO: is used for files and dirs..., we only implement files for now...
     * storage_plugin_destroy, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */
    public Boolean storagePluginDestroy(String poolUuid, String file) throws Ovm3ResourceException {
        String uuid = deDash(poolUuid);
        StorageServer ss = new StorageServer();
        StorageDetails sd = new StorageDetails();
        FileProperties fp = new FileProperties();
        ss.setUuid(uuid);
        sd.setDetailsRelationalUuid(uuid);
        sd.setUuid(poolUuid);
        fp.setType("file");
        fp.setUuid(poolUuid);
        fp.setName(file);
        return nullIsTrueCallWrapper(
                "storage_plugin_destroy", getPluginType, ss.getDetails(),
                sd.getDetails(), fp.getProperties());
    }

    /*
     * storage_plugin_isSnapable, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getDetailsInfo, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_removeFromAccessGroup, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_renameAccessGroup, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_stop, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_createMultiSnap, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getCurrentSnaps, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getFileInfo, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */
    public FileProperties storagePluginGetFileInfo(String poolUuid, String host,
            String file) throws Ovm3ResourceException {
        /* file path is the full path */
        String uuid = deDash(poolUuid);
        StorageServer ss = new StorageServer();
        StorageDetails sd = new StorageDetails();
        FileProperties fp = new FileProperties();
        ss.setUuid(uuid);
        ss.setAccessHost(host);
        sd.setUuid(poolUuid);
        sd.setDetailsRelationalUuid(uuid);
        sd.setState(1);
        fp.setName(file);
        fp.setProperties((HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileInfo",
                getPluginType,
                ss.getDetails(),
                sd.getDetails(),
                fp.getProperties()));
        if ("".equals(fp.getName())) {
            throw new Ovm3ResourceException("Unable to get file info for " + file);
        }
        return fp;
    }

    /*
     * Should do some input checking of ss and base
     * storage_plugin_getFileSystemInfo,
     * <class 'agent.api.storageplugin.StoragePlugin'> argument: impl_name -
     * default: None requires a minumum of uuid, access_host, storage_type
     * ss_uuid, access_path, uuid (the ss
     */
    public StorageDetails storagePluginGetFileSystemInfo(String propUuid,
            String mntUuid, String nfsHost, String nfsRemotePath) throws Ovm3ResourceException{
        /* clean the props */
        StorageServer ss = new StorageServer();
        StorageDetails sd = new StorageDetails();
        new FileProperties();
        ss.setUuid(propUuid);
        sd.setDetailsRelationalUuid(propUuid);
        sd.setUuid(mntUuid);
        ss.setAccessHost(nfsHost);
        if (nfsRemotePath.contains(nfsHost + ":")) {
            sd.setAccessPath(nfsRemotePath);
        } else {
            sd.setAccessPath(nfsHost + ":" + nfsRemotePath);
        }
        ss.setStorageType(FILESYS);
        sd.setDetails((HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileSystemInfo", getPluginType,
                ss.getDetails(), sd.getDetails()));
        return sd;
    }
    public StorageDetails getStorageDetails() {
        return storageDetails;
    }
    public void setStorageDetails(StorageDetails storageDetails) {
        this.storageDetails = storageDetails;
    }
    public StorageServer getStorageServer() {
        return storageServer;
    }
    public void setStorageServer(StorageServer storageServer) {
        this.storageServer = storageServer;
    }
    public FileProperties getFileProperties() {
        return fileProperties;
    }
    public void setFileProperties(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    /*
     * storage_plugin_clone, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_list, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_getInfo, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_snapRemove, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getCapabilities, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_createSnap, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getFileSystemSnapLimits, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_remove, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_getCurrentClones, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_online, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_isRestorable, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_iSCSI_logoutTarget, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: target - default: None
     * argument: portal - default: None
     */

    /*
     * storage_plugin_discover, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_start, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_removeAccessGroups, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_refresh, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_getAccessGroups, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_iSCSI_deletePortal, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: portal - default: None
     */

    /*
     * storage_plugin_createFileSystem, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_cloneFromSnap, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_addToAccessGroup, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_offline, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */

    /*
     * storage_plugin_listMountPoints, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */
}
