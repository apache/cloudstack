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
package com.cloud.hypervisor.ovm3.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

/*
 * should become an interface implementation
 */
public class StoragePlugin extends OvmObject {
    private String pluginType = "oracle.generic.NFSPlugin.GenericNFSPlugin";
    private String unknown = ""; /* empty */
    private Boolean active = true;
    private List<String> someList = new ArrayList<String>(); /* empty */

    public StoragePlugin(Connection c) {
        setClient(c);
    }
    /* TODO: subclass Storage Base Properties*/
    private Map<String, Object> baseProps = new HashMap<String, Object>() {
        {
            put("status", "");
            /* iscsi */
            put("admin_user", "");
            put("admin_host", "");
            /* no dash uuid */
            put("uuid", "");
            put("total_sz", "");
            /* iscsi or fc */
            put("admin_passwd", "");
            put("storage_desc", "");
            put("free_sz", 0);
            /* remote host for fs */
            put("access_host", "");
            /* type, guess lun ? */
            put("storage_type", "FileSys");
            put("alloc_size", 0);
            put("access_groups", new ArrayList<String>());
            put("sed_size", 0);
            /* uuid no dashes */
            put("name", "");
        }
    };

    public String setUuid(String val) {
        this.baseProps.put("uuid", val);
        return val;
    }

    public String getUuid() {
        return (String) this.baseProps.get("uuid");
    }

    public String setName(String val) {
        this.baseProps.put("name", val);
        return val;
    }

    public String getName() {
        return (String) this.baseProps.get("name");
    }

    public String setFsType(String val) {
        this.baseProps.put("storage_type", val);
        return val;
    }

    public String getFsType() {
        return (String) this.baseProps.get("storage_type");
    }

    public String setFsHost(String val) {
        this.baseProps.put("access_host", val);
        return val;
    }

    public String getFsHost() {
        return (String) this.baseProps.get("access_host");
    }

    public String setFsServer(String val) {
        return this.setFsHost(val);
    }

    public String getFsServer() {
        return this.getFsHost();
    }

    /* TODO: subclass Storage Specific Properties */
    private Map<String, Object> ssProps = new HashMap<String, Object>() {
        {
            /* empty */
            put("status", "");
            /* with dashes */
            put("uuid", "");
            /* no dashes */
            put("ss_uuid", "");
            put("size", "");
            put("free_sz", "");
            /* guess this is active ? */
            put("state", 1);
            put("access_grp_names", new ArrayList<String>());
            /* remote path */
            put("access_path", "");
            put("name", "");
            /* array of values that match normal mount options */
            put("mount_options", new ArrayList<String>());
        }
    };

    public String setFsSourcePath(String val) {
        this.ssProps.put("access_path", val);
        return val;
    }

    public String getFsSourcePath() {
        return (String) this.ssProps.get("access_path");
    }

    public String setMntUuid(String val) {
        this.ssProps.put("uuid", val);
        return val;
    }

    public String getMntUuid() {
        return (String) this.ssProps.get("uuid");
    }

    public String setSsUuid(String val) {
        this.ssProps.put("ss_uuid", val);
        return val;
    }

    public String getSsUuid() {
        return (String) this.ssProps.get("ss_uuid");
    }

    public String setSsName(String val) {
        this.ssProps.put("name", val);
        return val;
    }

    public String getSsName() {
        return (String) this.ssProps.get("ss_uuid");
    }

    public String getFreeSize() {
        return this.ssProps.get("free_sz").toString();
    }

    public String getTotalSize() {
        return this.ssProps.get("size").toString();
    }

    public String getSize() {
        return this.ssProps.get("size").toString();
    }

    /* TODO: subclass FileProperties */
    private Map<String, Object> fileProps = new HashMap<String, Object>() {
        {
            put("fr_type", "");
            put("ondisk_sz", "");
            put("fs_uuid", "");
            put("file_path", "");
            put("file_sz", "");
        }
    };

    public String getFileName() {
        return (String) this.fileProps.get("file_path");
    }

    public long getFileSize() {
        return Long.parseLong((String) this.fileProps.get("file_sz"));
    }

    private String mountPoint = "";

    public String setFsMountPoint(String val) {
        this.mountPoint = val;
        return val;
    }

    public String getFsMountPoint() {
        return mountPoint;
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
     * TODO: make more generic now only for files storage_plugin_create, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None - calls resize secretly.. after "create"
     */
    public Boolean storagePluginCreate(String poolUuid, String host,
            String file, Long size) throws Ovm3ResourceException{
        /* this is correct ordering stuff */
        String uuid = this.deDash(poolUuid);
        ssProps.put("uuid", uuid);
        ssProps.put("access_host", host);
        ssProps.put("storage_type", "FileSys");
        ssProps.put("name", "");
        ssProps.put("status", "");
        ssProps.put("admin_user", "");
        ssProps.put("admin_passwd", "");
        ssProps.put("admin_host", "");
        ssProps.put("total_sz", "");
        ssProps.put("free_sz", "");
        ssProps.put("used_sz", "");
        ssProps.put("access_grps", "");
        ssProps.put("storage_desc", "");

        baseProps.put("ss_uuid", uuid);
        baseProps.put("state", 2);
        baseProps.put("uuid", poolUuid);
        /* some more bogus values */
        baseProps.put("status", "");
        baseProps.put("access_path", "");
        baseProps.put("access_grp_names", "");
        baseProps.put("name", "");
        baseProps.put("size", "");

        return nullIsTrueCallWrapper(
                "storage_plugin_create", this.pluginType, this.ssProps,
                this.baseProps, file, "File", size);
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
    public Boolean storagePluginListFs(String type) throws Ovm3ResourceException {
        this.pluginType = type;
        return storagePluginListFs();
    }

    public Boolean storagePluginListFs() throws Ovm3ResourceException {
        Map<String, String> y = new HashMap<String, String>();
        return nullIsTrueCallWrapper("storage_plugin_listFileSystems",
                this.pluginType, y);
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
    public final Boolean storagePluginMount(String nfsHost, String nfsPath,
            String mntUuid, String mountPoint) throws Ovm3ResourceException {
        String propUuid = this.deDash(mntUuid);
        this.setUuid(propUuid);
        this.setName(propUuid);
        this.setFsServer(nfsHost);
        this.setFsSourcePath(nfsHost + ":" + nfsPath);
        this.setMntUuid(mntUuid);
        this.setSsUuid(propUuid);
        this.setSsName("nfs:" + nfsPath);
        this.setFsMountPoint(mountPoint);
        this.storagePluginMount();
        return nullIsTrueCallWrapper(
                "storage_plugin_mount", this.pluginType, this.baseProps,
                this.ssProps, this.mountPoint, this.unknown, this.active,
                this.someList);
    }

    public Boolean storagePluginMount() throws Ovm3ResourceException {
        return nullIsTrueCallWrapper(
                "storage_plugin_mount", this.pluginType, this.baseProps,
                this.ssProps, this.mountPoint, this.unknown, this.active,
                this.someList);
    }

    /**
     * . storage_plugin_unmount, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     *
     * @return boolean
     *
     */
    public final Boolean storagePluginUnmount() throws Ovm3ResourceException{
        return nullIsTrueCallWrapper("storage_plugin_unmount", this.pluginType,
                this.baseProps, this.ssProps, this.mountPoint, this.active);
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
    public Boolean discoverStoragePlugins() throws Ovm3ResourceException{
        Object result = callWrapper("discover_storage_plugins");
        /* TODO: Actually parse this */
        Document xmlDocument = prepParse((String) result);
        String path = "//Discover_Storage_Plugins_Result/storage_plugin_info_list";
        if (result != null) {
            return true;
        }
        return false;
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
     * TODO: is used for files and dirs..., we only implement files for now...
     * storage_plugin_destroy, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     */
    public Boolean storagePluginDestroy(String poolUuid, String file) throws Ovm3ResourceException{
        /* TODO: clean the props, the empty ones are checked, but not for content... */
        baseProps.put("uuid", "");
        baseProps.put("access_host", "");
        baseProps.put("storage_type", "FileSys");
        ssProps.put("ss_uuid", "");
        ssProps.put("access_path", "");
        ssProps.put("uuid", poolUuid);
        fileProps.put("fr_type", "File");
        fileProps.put("fs_uuid", poolUuid);
        fileProps.put("file_path", file);
        fileProps.put("file_sz", "");
        fileProps.put("ondisk_sz", "");
        return nullIsTrueCallWrapper(
                "storage_plugin_destroy", this.pluginType, this.baseProps,
                this.ssProps, this.fileProps);
    }

    public Boolean storagePluginDestroy() throws Ovm3ResourceException{
        /* TODO: clean the props */
        return nullIsTrueCallWrapper(
                "storage_plugin_destroy", this.pluginType, this.baseProps,
                this.ssProps, this.fileProps);
    }

    /*
     * storage_plugin_isSnapable, <class
     * 'agent.api.storageplugin.StoragePlugin'> argument: impl_name - default:
     * None
     */

    /*
     * storage_plugin_getStorageServerInfo, <class
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
    public Boolean storagePluginGetFileInfo() throws Ovm3ResourceException {
        fileProps = (HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileInfo", this.pluginType, this.baseProps,
                this.fileProps);
        if (fileProps == null) {
            return false;
        }
        return true;
    }

    public Boolean storagePluginGetFileInfo(String file) throws Ovm3ResourceException{
        fileProps.put("file_path", file);
        fileProps = (HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileInfo", this.pluginType, this.ssProps,
                this.baseProps, this.fileProps);
        if (fileProps == null) {
            return false;
        }
        return true;
    }

    public Boolean storagePluginGetFileInfo(String poolUuid, String host,
            String file) throws Ovm3ResourceException {
        /* file path is the full path */
        String uuid = this.deDash(poolUuid);
        baseProps.put("uuid", poolUuid);
        baseProps.put("access_host", host);
        ssProps.put("access_path", "");
        ssProps.put("uuid", uuid);
        ssProps.put("state", 1);
        ssProps.put("ss_uuid", poolUuid);
        ssProps.put("name", "");
        fileProps.put("file_path", file);
        fileProps = (HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileInfo", this.pluginType, this.ssProps,
                this.baseProps, this.fileProps);
        if (fileProps == null) {
            return false;
        }
        return true;
    }

    /*
     * TODO: input checking of ss and base /* storage_plugin_getFileSystemInfo,
     * <class 'agent.api.storageplugin.StoragePlugin'> argument: impl_name -
     * default: None requires a minumum of uuid, access_host, storage_type
     * ss_uuid, access_path, uuid (the ss
     */
    public Boolean storagePluginGetFileSystemInfo(String propUuid,
            String mntUuid, String nfsHost, String nfsPath) throws Ovm3ResourceException{
        /* clean the props */
        this.setUuid(propUuid);
        this.setSsUuid(propUuid);
        this.setMntUuid(mntUuid);
        this.setFsHost(nfsHost);
        this.setFsSourcePath(nfsHost + ":" + nfsPath);
        this.setFsType("FileSys");
        Map<String, Object> props = (HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileSystemInfo", this.pluginType,
                this.baseProps, this.ssProps);
        this.ssProps = props;
        if (props == null) {
            return false;
        }
        return true;
    }

    /* TODO: double check base and ss ordering!!!! */
    public Boolean storagePluginGetFileSystemInfo() throws Ovm3ResourceException {
        Map<String, Object> props = (HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileSystemInfo", this.pluginType,
                this.baseProps, this.ssProps);
        this.ssProps = props;
        if (props == null) {
            return false;
        }
        return true;
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
    /* should really make that stuff a class as this is weird now... */
    public Boolean storagePluginListMounts() throws Ovm3ResourceException {
        Object x = callWrapper("storage_plugin_listMountPoints",
                this.pluginType, this.baseProps);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean storagePluginListMounts(String uuid) throws Ovm3ResourceException {
        /* should allow for putting in the uuid */
        Object x = callWrapper("storage_plugin_listMountPoints",
                this.pluginType, this.baseProps);
        if (x == null) {
            return true;
        }
        return false;
    }
}
