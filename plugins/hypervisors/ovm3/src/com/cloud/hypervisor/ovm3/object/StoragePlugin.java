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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;

/*
 * should become an interface implementation
 */
public class StoragePlugin extends OvmObject {
    /* nfs or iscsi should be an "enabled" flag */
    /*
     * storage_plugin_mount('oracle.generic.NFSPlugin.GenericNFSPlugin', {
     * 'status': '', 'admin_user': '', 'admin_host': '', 'uuid':
     * '0004fb00000900000c2461c2f62ba43e', 'total_sz': 0, 'admin_passwd':
     * '******', 'storage_desc': '', 'free_sz': 0, 'access_host': 'cs-mgmt',
     * 'storage_type': 'FileSys', 'alloc_sz': 0, 'access_grps': [], 'used_sz':
     * 0, 'name': '0004fb00000900000c2461c2f62ba43e'}, { 'status': '', 'uuid':
     * 'b8ca41cb-3469-4f74-a086-dddffe37dc2d', 'ss_uuid':
     * '0004fb00000900000c2461c2f62ba43e', 'size': '263166853120', 'free_sz':
     * '259377299456', 'state': 1, 'access_grp_names': [], 'access_path':
     * 'cs-mgmt:/volumes/cs-data/secondary', 'name':
     * 'nfs:/volumes/cs-data/secondary'},
     * '/nfsmnt/b8ca41cb-3469-4f74-a086-dddffe37dc2d', '', True, [])
     */
    private String pluginType = "oracle.generic.NFSPlugin.GenericNFSPlugin";

    /* TODO: subclass */
    /*
     * Json, but then it's xml... {'status': '', 'admin_user': '', 'admin_host':
     * '', 'uuid': '0004fb00000900000c2461c2f62ba43e', 'total_sz': 0,
     * 'admin_passwd': '******', 'storage_desc': '', 'free_sz': 0,
     * 'access_host': 'cs-mgmt', 'storage_type': 'FileSys', 'alloc_sz': 0,
     * 'access_grps': [], 'used_sz': 0, 'name':
     * '0004fb00000900000c2461c2f62ba43e'}
     */
    private Map<String, Object> baseProps = new HashMap<String, Object>() {
        {
            put("status", ""); /* empty */
            put("admin_user", ""); /* auth */
            put("admin_host", ""); /* auth host */
            put("uuid", ""); /* no dash uuid */
            put("total_sz", "");
            put("admin_passwd", ""); /* iscsi or fc */
            put("storage_desc", ""); /* description */
            put("free_sz", 0);
            put("access_host", ""); /* remote host for fs */
            put("storage_type", "FileSys"); /* type, guess lun ? */
            put("alloc_size", 0);
            put("access_groups", new ArrayList<String>());
            put("sed_size", 0);
            put("name", ""); /* uuid no dashes */
        };
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

    /* TODO: subclass */
    /*
     * Meh {'status': '', 'uuid': 'b8ca41cb-3469-4f74-a086-dddffe37dc2d',
     * 'ss_uuid': '0004fb00000900000c2461c2f62ba43e', 'size': '263166853120',
     * 'free_sz': '259377299456', 'state': 1, 'access_grp_names': [],
     * 'access_path': 'cs-mgmt:/volumes/cs-data/secondary', 'name':
     * 'nfs:/volumes/cs-data/secondary'}
     */
    private Map<String, Object> ssProps = new HashMap<String, Object>() {
        {
            put("status", ""); /* empty */
            put("uuid", ""); /* with dashes */
            put("ss_uuid", ""); /* no dashes */
            put("size", "");
            put("free_sz", "");
            put("state", 1); /* guess this is active ? */
            put("access_grp_names", new ArrayList<String>());
            put("access_path", ""); /* remote path */
            put("name", ""); /* just a name */
            put("mount_options", new ArrayList<String>()); /*
                                                            * array of values
                                                            * which match normal
                                                            * mount options
                                                            */
        };
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

    /* TODO: subclass */
    /*
     * {'fr_type': 'File', 'ondisk_sz': '48193536', 'fs_uuid':
     * '7718562d-872f-47a7-b454-8f9cac4ffa3a', 'file_path':
     * '/nfsmnt/7718562d-872f-47a7-b454-8f9cac4ffa3a/0004fb0000060000d4a1d2ec05a5e799.img',
     * 'file_sz': '52380672'}
     */
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

    public String unknown = ""; /* empty */
    public Boolean active = true;
    public List<String> someList = new ArrayList<String>(); /* empty */

    public StoragePlugin(Connection c) {
        client = c;
    }

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
            String file, Long size) throws XmlRpcException {
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

        /*
         * fileProps.put("fr_type", "File"); fileProps.put("fs_uuid", ssuuid);
         * fileProps.put("file_path", file); fileProps.put("file_sz", "");
         * fileProps.put("ondisk_sz", "");
         */
        Object x = (HashMap<String, Object>) callWrapper(
                "storage_plugin_create", this.pluginType, this.ssProps,
                this.baseProps, file, "File", size);

        if (x == null) {
            return true;
        }
        return true;
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
    public Boolean storagePluginListFs(String type) throws XmlRpcException {
        this.pluginType = type;
        return storagePluginListFs();
    }

    public Boolean storagePluginListFs() throws XmlRpcException {
        Map<String, String> y = new HashMap<String, String>();
        Object x = callWrapper("storage_plugin_listFileSystems",
                this.pluginType, y);
        if (x == null) {
            return true;
        }
        return false;
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
            String mntUuid, String mountPoint) throws XmlRpcException {
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
        Map<String, Object> x = (Map<String, Object>) callWrapper(
                "storage_plugin_mount", this.pluginType, this.baseProps,
                this.ssProps, this.mountPoint, this.unknown, this.active,
                this.someList);
        // System.out.println(x);
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * {ss_uuid=eb1dbafadee9450d876239bb5e3b7f4a,
     * mount_options=[Ljava.lang.Object;@2d8dea20, status=,
     * name=nfs:/volumes/cs-data/secondary, state=1,
     * access_path=cs-mgmt:/volumes/cs-data/secondary,
     * uuid=6ab917b0-a070-4254-8fe2-e2163ee0e885,
     * access_grp_names=[Ljava.lang.Object;@4005f23d, free_sz=, size=}
     */
    public Boolean storagePluginMount() throws XmlRpcException {
        Map<String, Object> x = (Map<String, Object>) callWrapper(
                "storage_plugin_mount", this.pluginType, this.baseProps,
                this.ssProps, this.mountPoint, this.unknown, this.active,
                this.someList);
        // System.out.println(x);
        if (x == null) {
            return true;
        }
        // System.out.println(x);
        /*
         * {ss_uuid=1b8685bf625642cb92ddd4c0d7b18620,
         * mount_options=[Ljava.lang.Object;@6f479e5f, status=,
         * name=nfs:/volumes/cs-data/secondary, state=1,
         * access_path=cs-mgmt:/volumes/cs-data/secondary,
         * uuid=54d78233-508f-4632-92a6-97fc1311ca23,
         * access_grp_names=[Ljava.lang.Object;@46eea80c, free_sz=, size=}
         */

        /*
         * if (!x.get("ss_uuid").equals(this.ssProps.get("ss_uuid"))) { return
         * false; } this.ssProps.put("mount_options", x.get("mount_options"));
         * this.ssProps.put("access_grp_names", x.get("access_grp_names"));
         */
        return false;
    }

    /**
     * . storage_plugin_unmount, <class 'agent.api.storageplugin.StoragePlugin'>
     * argument: impl_name - default: None
     *
     * @return boolean
     * @throws XmlRpcException
     */
    public final Boolean storagePluginUnmount() throws XmlRpcException {
        Object x = callWrapper("storage_plugin_unmount", this.pluginType,
                this.baseProps, this.ssProps, this.mountPoint, this.active);
        if (x == null) {
            return true;
        }
        return false;
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
    /*
     * <Discover_Storage_Plugins_Result> <storage_plugin_info_list>
     * <storage_plugin_info plugin_impl_name="oracle.ocfs2.OCFS2.OCFS2Plugin">
     * <fs_api_version>1,2,7</fs_api_version>
     * <generic_plugin>False</generic_plugin>
     * <plugin_version>0.1.0-38</plugin_version>
     * <filesys_type>LocalFS</filesys_type>
     * <extended_api_version>None</extended_api_version> <plugin_desc>Oracle
     * OCFS2 File system Storage Connect Plugin</plugin_desc>
     * <cluster_required>True</cluster_required> <plugin_name>Oracle OCFS2 File
     * system</plugin_name> <fs_extra_info_help>None</fs_extra_info_help>
     * <required_api_vers>1,2,7</required_api_vers>
     * <file_extra_info_help>None</file_extra_info_help>
     * <ss_extra_info_help>None</ss_extra_info_help>
     * <filesys_name>ocfs2</filesys_name> <vendor_name>Oracle</vendor_name>
     * <plugin_type>ifs</plugin_type> <abilities>
     * <resize_is_sync>YES</resize_is_sync> <clone_is_sync>YES</clone_is_sync>
     * <access_control>NO</access_control>
     * <custom_clone_name>YES</custom_clone_name>
     * <require_storage_name>NO</require_storage_name>
     * <backing_device_type>DEVICE_SINGLE</backing_device_type>
     * <splitclone_while_open>NO</splitclone_while_open>
     * <custom_snap_name>YES</custom_snap_name> <snapshot>ONLINE</snapshot>
     * <splitclone>UNSUPPORTED</splitclone> <snap_is_sync>YES</snap_is_sync>
     * <snapclone>ONLINE</snapclone> <splitclone_is_sync>NO</splitclone_is_sync>
     * <clone>ONLINE</clone> <resize>ONLINE</resize>
     * <snapclone_is_sync>YES</snapclone_is_sync> </abilities>
     * </storage_plugin_info> <storage_plugin_info
     * plugin_impl_name="oracle.generic.SCSIPlugin.GenericPlugin"> ....
     */
    public Boolean discoverStoragePlugins()
            throws ParserConfigurationException, IOException, Exception {
        Object result = callWrapper("discover_storage_plugins");
        // System.out.println(result);
        Document xmlDocument = prepParse((String) result);
        /* could be more subtle */
        String path = "//Discover_Storage_Plugins_Result/storage_plugin_info_list";
        /*
         * Capabilities = xmlToMap(path+"/Capabilities", xmlDocument); VMM =
         * xmlToMap(path+"/VMM", xmlDocument); NTP = xmlToMap(path+"/NTP",
         * xmlDocument); Date_Time = xmlToMap(path+"/Date_Time", xmlDocument);
         * Generic = xmlToMap(path, xmlDocument);
         */
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
    public Boolean storagePluginDestroy(String poolUuid, String file)
            throws XmlRpcException {
        /* clean the props, the empty ones are checked, but not for content... */
        // String uuid = this.deDash(poolUuid);
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
        Object x = (HashMap<String, Object>) callWrapper(
                "storage_plugin_destroy", this.pluginType, this.baseProps,
                this.ssProps, this.fileProps);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean storagePluginDestroy() throws XmlRpcException {
        /* clean the props */
        Object x = (HashMap<String, Object>) callWrapper(
                "storage_plugin_destroy", this.pluginType, this.baseProps,
                this.ssProps, this.fileProps);
        if (x == null) {
            return true;
        }
        return false;
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
    public Boolean storagePluginGetFileInfo() throws XmlRpcException {
        fileProps = (HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileInfo", this.pluginType, this.baseProps,
                this.fileProps);
        if (fileProps == null) {
            return true;
        }
        return false;
    }

    public Boolean storagePluginGetFileInfo(String file) throws XmlRpcException {
        fileProps.put("file_path", file);
        fileProps = (HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileInfo", this.pluginType, this.ssProps,
                this.baseProps, this.fileProps);
        if (fileProps == null) {
            return true;
        }
        return false;
    }

    public Boolean storagePluginGetFileInfo(String poolUuid, String host,
            String file) throws XmlRpcException {
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
            return true;
        }
        return false;
    }

    /*
     * TODO: input checking of ss and base /* storage_plugin_getFileSystemInfo,
     * <class 'agent.api.storageplugin.StoragePlugin'> argument: impl_name -
     * default: None requires a minumum of uuid, access_host, storage_type
     * ss_uuid, access_path, uuid (the ss
     */
    public Boolean storagePluginGetFileSystemInfo(String propUuid,
            String mntUuid, String nfsHost, String nfsPath)
            throws XmlRpcException {
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
    public Boolean storagePluginGetFileSystemInfo() throws XmlRpcException {
        HashMap<String, Object> props = (HashMap<String, Object>) callWrapper(
                "storage_plugin_getFileSystemInfo", this.pluginType,
                this.baseProps, this.ssProps);
        this.ssProps = props;
        // System.out.println(props);
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
    public Boolean storagePluginListMounts() throws XmlRpcException {
        Object x = callWrapper("storage_plugin_listMountPoints",
                this.pluginType, this.baseProps);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean storagePluginListMounts(String uuid) throws XmlRpcException {
        /* should allow for putting in the uuid */
        Object x = callWrapper("storage_plugin_listMountPoints",
                this.pluginType, this.baseProps);
        if (x == null) {
            return true;
        }
        return false;
    }
}
