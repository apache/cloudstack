/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.objects;

import java.io.File;
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
    private final String pluginPath = "//Discover_Storage_Plugins_Result/storage_plugin_info_list/storage_plugin_info";
    private final String nfsPlugin = "oracle.generic.NFSPlugin.GenericNFSPlugin";
    private String getPluginType = nfsPlugin;
    private List<String> supportedPlugins = new ArrayList<String>();
    private final Map<String, Object> supportedPluginsProperties = new HashMap<String, Object>();
    private final String unknown = EMPTY_STRING; /* empty */
    private final Boolean active = true;
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
                put("status", EMPTY_STRING);
                put("uuid", EMPTY_STRING);
                put("ss_uuid", EMPTY_STRING);
                put("size", EMPTY_STRING);
                put("free_sz", 0);
                put("state", 0);
                put("access_grp_names", new ArrayList<String>());
                put("access_path", EMPTY_STRING);
                put("name", EMPTY_STRING);
                put("mount_options", new ArrayList<String>());
            }
        };
        public Map<String, Object> getDetails() {
            return storageDetails;
        }
        public void setDetails(Map<String, Object> details) {
            storageDetails = details;
        }
        public void setSize(String val) {
            storageDetails.put("size", val);
        }
        public String getSize() {
            return (String) storageDetails.get("size");
        }
        public void setFreeSize(String val) {
            storageDetails.put("free_sz", val);
        }
        public String getFreeSize() {
            return (String) storageDetails.get("free_sz");
        }
        public void setState(Integer val) {
            storageDetails.put("state", val);
        }
        public Integer getState() {
            return (Integer) storageDetails.get("state");
        }
        public void setStatus(String val) {
            storageDetails.put("status", val);
        }
        public String getStatus() {
            return (String) storageDetails.get("status");
        }
        /* format depends on storagesource type ? */
        public void setAccessPath(String val) {
            storageDetails.put("access_path", val);
        }
        public String getAccessPath() {
            return (String) storageDetails.get("access_path");
        }
        public void setName(String val) {
            storageDetails.put("name", val);
        }
        public String getName() {
            return (String) storageDetails.get("name");
        }
        public void setUuid(String val) throws Ovm3ResourceException {
            if (!val.contains("-")) {
                throw new Ovm3ResourceException("Storage Details UUID should contain dashes: " + val);
            }
            storageDetails.put("uuid", val);
        }
        public String getUuid() {
            return (String) storageDetails.get("uuid");
        }
        public void setDetailsRelationalUuid(String val) throws Ovm3ResourceException {
            if (val.contains("-")) {
                throw new Ovm3ResourceException("Storage Details UUID that relates to Storage Source should notcontain dashes: " + val);
            }
            storageDetails.put("ss_uuid", val);
        }
        public String getDetailsRelationalUuid() {
            return (String) storageDetails.get("ss_uuid");
        }
        public void setAccessGroupNames(List<String> l) {
            storageDetails.put("access_grp_names", l);
        }
        public List<String> getAccessGroupNames() {
            return (List<String>) storageDetails.get("access_grp_names");
        }
        public void setMountOptions(List<String> l) {
            storageDetails.put("mount_options", l);
        }
        public List<String> getMountOptions() {
            return (List<String>) storageDetails.get("mount_options");
        }
    }

    /* mind you uuid has NO dashes here */
    public class StorageServer {
        private Map<String, Object> storageSource = new HashMap<String, Object>() {
            {
                put("status", EMPTY_STRING);
                put("admin_user", EMPTY_STRING);
                put("admin_host", EMPTY_STRING);
                put("uuid", EMPTY_STRING);
                put("total_sz", 0);
                put("admin_passwd", EMPTY_STRING);
                put("storage_desc", EMPTY_STRING);
                put("free_sz", 0);
                put("access_host", EMPTY_STRING);
                put("storage_type", EMPTY_STRING);
                put("alloc_sz", 0);
                put("access_grps",  new ArrayList<String>());
                put("used_sz", 0);
                put("name", EMPTY_STRING);
            }
        };
        public Map<String, Object> getDetails() {
            return storageSource;
        }
        public void setDetails(Map<String, Object> details) {
            storageSource = details;
        }
        public void setAccessGroups(List<String> l) {
            storageSource.put("access_grps", l);
        }
        public List<String> getAccessGroups() {
            return (List<String>) storageSource.get("access_grps");
        }
        public void setStatus(String val) {
            storageSource.put("status", val);
        }
        public String getStatus() {
            return (String) storageSource.get("status");
        }
        public void setAdminUser(String val) {
            storageSource.put("admin_user", val);
        }
        public String getAdminUser() {
            return (String) storageSource.get("admin_user");
        }
        public void setAdminHost(String val) {
            storageSource.put("admin_host", val);
        }
        public String getAdminHost() {
            return (String) storageSource.get("admin_host");
        }
        public void setUuid(String val) throws Ovm3ResourceException {
            if (val.contains("-")) {
                throw new Ovm3ResourceException("Storage Source UUID should not contain dashes: " + val);
            }
            storageSource.put("uuid", val);
        }
        public String getUuid() {
            return (String) storageSource.get("uuid");
        }
        public String getTotalSize() {
            return (String) storageSource.get("total_sz");
        }
        public void setTotalSize(Integer val) {
            storageSource.put("total_sz", val);
        }
        public void setAdminPassword(String val) {
            storageSource.put("admin_password", val);
        }
        public String getAdminPassword() {
            return (String) storageSource.get("admin_password");
        }
        public void setDescription(String val) {
            storageSource.put("storage_desc", val);
        }
        public String getDescription() {
            return (String) storageSource.get("storage_desc");
        }
        public String getFreeSize() {
            return (String) storageSource.get("free_sz");
        }
        public void setFreeSize(Integer val) {
            storageSource.put("free_sz", val);
        }
        public void setAccessHost(String val) {
            storageSource.put("access_host", val);
        }
        public String getAccessHost() {
            return (String) storageSource.get("access_host");
        }
        public void setStorageType(String val) {
            storageSource.put("storage_type", val);
        }
        public String getStorageType() {
            return (String) storageSource.get("storage_type");
        }
        public void setAllocationSize(Integer val) {
            storageSource.put("alloc_sz", val);
        }
        public Integer getAllocationSize() {
            return (Integer) storageSource.get("alloc_sz");
        }
        public void setUsedSize(Integer val) {
            storageSource.put("used_sz", val);
        }
        public Integer getUsedSize() {
            return (Integer) storageSource.get("used_sz");
        }
        public void setName(String val) {
            storageSource.put("name", val);
        }
        public String getName() {
            return (String) storageSource.get("name");
        }
    }

    public class FileProperties {
        private Map<String, Object> fileProperties = new HashMap<String, Object>() {
            {
                put("fr_type", EMPTY_STRING);
                put("ondisk_sz", EMPTY_STRING);
                put("fs_uuid", EMPTY_STRING);
                put("file_path", EMPTY_STRING);
                put("file_sz", EMPTY_STRING);
            }
        };
        public Map<String, Object> getProperties() {
            return fileProperties;
        }
        public void setProperties(Map<String, Object> props) {
            fileProperties = props;
        }
        public String getName() {
            return (String) fileProperties.get("file_path");
        }
        public String setName(String f) {
            return (String) fileProperties.put("file_path", f);
        }
        public String setType(String t) {
            return (String) fileProperties.put("fr_type", t);
        }
        public String getType() {
            return (String) fileProperties.get("fr_type");
        }
        public void setSize(Long t) {
            fileProperties.put("file_sz", t);
        }
        public Long getSize() {
            return Long.getLong((String) fileProperties.get("file_sz"));
        }
        public String setOnDiskSize(String t) {
            return (String) fileProperties.put("ondisk_sz", t);
        }
        public String getOnDiskSize() {
            return (String) fileProperties.get("ondisk_sz");
        }
        public String setUuid(String t) {
            return (String) fileProperties.put("fs_uuid", t);
        }
        public String getUuid() {
            return (String) fileProperties.get("fs_uuid");
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
            String file, Long size) throws Ovm3ResourceException{
        /* this is correct ordering stuff and correct naming!!! */
        String uuid = deDash(poolUuid);
        StorageServer ss = new StorageServer();
        StorageDetails sd = new StorageDetails();
        FileProperties fp = new FileProperties();
        ss.setUuid(uuid);
        ss.setStorageType("FileSys");
        ss.setAccessHost(host);
        sd.setUuid(poolUuid);
        sd.setDetailsRelationalUuid(uuid);
        sd.setState(2);
        fp.setProperties((HashMap<String, Object>) callWrapper("storage_plugin_create",
                getPluginType, ss.getDetails(),
                sd.getDetails(), file, "File", size));
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
        ss.setStorageType("FileSys");
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
            mountPoint += File.separator + mntUuid;
        }
        sd.setDetails((HashMap<String, Object>) callWrapper(
                "storage_plugin_mount", getPluginType, ss.getDetails(),
                sd.getDetails(), mountPoint, unknown, active,
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
        ss.setStorageType("FileSys");
        String mountPoint = localPath + File.separator + mntUuid;
        /* */
        callWrapper("storage_plugin_unmount", getPluginType,
                ss.getDetails(), sd.getDetails(), mountPoint, active);
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
        Object result = callWrapper("discover_storage_plugins");
        if (result == null) {
            return null;
        }
        Document xmlDocument = prepParse((String) result);
        supportedPlugins = new ArrayList<String>();
        supportedPlugins.addAll(xmlToList(pluginPath + "/@plugin_impl_name", xmlDocument));
        return supportedPlugins;
    }

    private Map<String,String> checkStoragePluginDetails(String plugin, Boolean ability) throws Ovm3ResourceException {
        Object result = callWrapper("discover_storage_plugins");
        Document xmlDocument = prepParse((String) result);
        if (discoverStoragePlugins().contains(plugin)) {
            String details = pluginPath + "[@plugin_impl_name='" + plugin + "']";
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
        // ss.setStorageType("FileSys");));
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
        ss.setStorageType("FileSys");
        sd.setDetails((HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileSystemInfo", getPluginType,
                ss.getDetails(), sd.getDetails()));
        // System.out.println(sd.getDetails());
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
