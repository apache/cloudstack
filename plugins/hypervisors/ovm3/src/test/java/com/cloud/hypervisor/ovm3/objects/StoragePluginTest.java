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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.StoragePlugin.FileProperties;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin.StorageDetails;

public class StoragePluginTest {
    ConnectionTest con = new ConnectionTest();
    StoragePlugin sPt = new StoragePlugin(con);
    XmlTestResultTest results = new XmlTestResultTest();
    String NFSHOST = "nfs-store-1";
    String NFSPATH = "/volumes/cs-data/primary";
    String NFSMNT = "/nfsmnt";
    String FSPROPUUID = sPt.deDash(sPt.newUuid());
    String FSMNTUUID = sPt.newUuid();
    String POOLUUID = sPt.deDash(FSMNTUUID);
    String FILE = "a.file";
    Long SIZE = 2096898048L;
    String STORAGEPLUGINXML = "<string>"
            + "&lt;?xml version=\"1.0\" ?&gt;"
            + "&lt;Discover_Storage_Plugins_Result&gt;"
            + "&lt;storage_plugin_info_list&gt;"
            + "&lt;storage_plugin_info plugin_impl_name=\"oracle.ocfs2.OCFS2.OCFS2Plugin\"&gt;"
            + "&lt;fs_api_version&gt;1,2,7&lt;/fs_api_version&gt;"
            + "&lt;generic_plugin&gt;False&lt;/generic_plugin&gt;"
            + "&lt;plugin_version&gt;0.1.0-38&lt;/plugin_version&gt;"
            + "&lt;filesys_type&gt;LocalFS&lt;/filesys_type&gt;"
            + "&lt;extended_api_version&gt;None&lt;/extended_api_version&gt;"
            + "&lt;plugin_desc&gt;Oracle OCFS2 File system Storage Connect Plugin&lt;/plugin_desc&gt;"
            + "&lt;cluster_required&gt;True&lt;/cluster_required&gt;"
            + "&lt;plugin_name&gt;Oracle OCFS2 File system&lt;/plugin_name&gt;"
            + "&lt;fs_extra_info_help&gt;None&lt;/fs_extra_info_help&gt;"
            + "&lt;required_api_vers&gt;1,2,7&lt;/required_api_vers&gt;"
            + "&lt;file_extra_info_help&gt;None&lt;/file_extra_info_help&gt;"
            + "&lt;ss_extra_info_help&gt;None&lt;/ss_extra_info_help&gt;"
            + "&lt;filesys_name&gt;ocfs2&lt;/filesys_name&gt;"
            + "&lt;vendor_name&gt;Oracle&lt;/vendor_name&gt;"
            + "&lt;plugin_type&gt;ifs&lt;/plugin_type&gt;"
            + "&lt;abilities&gt;"
            + "&lt;resize_is_sync&gt;YES&lt;/resize_is_sync&gt;"
            + "&lt;clone_is_sync&gt;YES&lt;/clone_is_sync&gt;"
            + "&lt;access_control&gt;NO&lt;/access_control&gt;"
            + "&lt;custom_clone_name&gt;YES&lt;/custom_clone_name&gt;"
            + "&lt;require_storage_name&gt;NO&lt;/require_storage_name&gt;"
            + "&lt;backing_device_type&gt;DEVICE_SINGLE&lt;/backing_device_type&gt;"
            + "&lt;splitclone_while_open&gt;NO&lt;/splitclone_while_open&gt;"
            + "&lt;custom_snap_name&gt;YES&lt;/custom_snap_name&gt;"
            + "&lt;snapshot&gt;ONLINE&lt;/snapshot&gt;"
            + "&lt;splitclone&gt;UNSUPPORTED&lt;/splitclone&gt;"
            + "&lt;snap_is_sync&gt;YES&lt;/snap_is_sync&gt;"
            + "&lt;snapclone&gt;ONLINE&lt;/snapclone&gt;"
            + "&lt;splitclone_is_sync&gt;NO&lt;/splitclone_is_sync&gt;"
            + "&lt;clone&gt;ONLINE&lt;/clone&gt;"
            + "&lt;resize&gt;ONLINE&lt;/resize&gt;"
            + "&lt;snapclone_is_sync&gt;YES&lt;/snapclone_is_sync&gt;"
            + "&lt;/abilities&gt;"
            + "&lt;/storage_plugin_info&gt;"
            + "&lt;storage_plugin_info plugin_impl_name=\"oracle.generic.SCSIPlugin.GenericPlugin\"&gt;"
            + "&lt;storage_types&gt;SAN,iSCSI&lt;/storage_types&gt;"
            + "&lt;generic_plugin&gt;True&lt;/generic_plugin&gt;"
            + "&lt;plugin_version&gt;1.1.0&lt;/plugin_version&gt;"
            + "&lt;extended_api_version&gt;1,2,9&lt;/extended_api_version&gt;"
            + "&lt;plugin_desc&gt;Oracle Storage Connect Plugin for Generic FC and iSCSI&lt;/plugin_desc&gt;"
            + "&lt;plugin_name&gt;Oracle Generic SCSI Plugin&lt;/plugin_name&gt;"
            + "&lt;sa_api_version&gt;1,2,7&lt;/sa_api_version&gt;"
            + "&lt;required_api_vers&gt;1,2,7&lt;/required_api_vers&gt;"
            + "&lt;ss_extra_info_help&gt;None&lt;/ss_extra_info_help&gt;"
            + "&lt;se_extra_info_help&gt;None&lt;/se_extra_info_help&gt;"
            + "&lt;vendor_name&gt;Oracle&lt;/vendor_name&gt;"
            + "&lt;plugin_type&gt;isa&lt;/plugin_type&gt;"
            + "&lt;abilities&gt;"
            + "&lt;resize_is_sync&gt;UNSUPPORTED&lt;/resize_is_sync&gt;"
            + "&lt;clone_is_sync&gt;UNSUPPORTED&lt;/clone_is_sync&gt;"
            + "&lt;access_control&gt;NO&lt;/access_control&gt;"
            + "&lt;custom_clone_name&gt;UNSUPPORTED&lt;/custom_clone_name&gt;"
            + "&lt;require_storage_name&gt;UNSUPPORTED&lt;/require_storage_name&gt;"
            + "&lt;custom_snap_name&gt;UNSUPPORTED&lt;/custom_snap_name&gt;"
            + "&lt;snapshot&gt;UNSUPPORTED&lt;/snapshot&gt;"
            + "&lt;splitclone&gt;UNSUPPORTED&lt;/splitclone&gt;"
            + "&lt;snap_is_sync&gt;UNSUPPORTED&lt;/snap_is_sync&gt;"
            + "&lt;snapclone&gt;UNSUPPORTED&lt;/snapclone&gt;"
            + "&lt;splitclone_is_sync&gt;UNSUPPORTED&lt;/splitclone_is_sync&gt;"
            + "&lt;clone&gt;UNSUPPORTED&lt;/clone&gt;"
            + "&lt;resize&gt;UNSUPPORTED&lt;/resize&gt;"
            + "&lt;snapclone_is_sync&gt;UNSUPPORTED&lt;/snapclone_is_sync&gt;"
            + "&lt;/abilities&gt;"
            + "&lt;/storage_plugin_info&gt;"
            + "&lt;storage_plugin_info plugin_impl_name=\"oracle.generic.NFSPlugin.GenericNFSPlugin\"&gt;"
            + "&lt;fs_api_version&gt;1,2,7&lt;/fs_api_version&gt;"
            + "&lt;generic_plugin&gt;True&lt;/generic_plugin&gt;"
            + "&lt;plugin_version&gt;1.1.0&lt;/plugin_version&gt;"
            + "&lt;filesys_type&gt;NetworkFS&lt;/filesys_type&gt;"
            + "&lt;extended_api_version&gt;None&lt;/extended_api_version&gt;"
            + "&lt;plugin_desc&gt;Oracle Generic Network File System Storage Connect Plugin&lt;/plugin_desc&gt;"
            + "&lt;cluster_required&gt;False&lt;/cluster_required&gt;"
            + "&lt;plugin_name&gt;Oracle Generic Network File System&lt;/plugin_name&gt;"
            + "&lt;fs_extra_info_help&gt;None&lt;/fs_extra_info_help&gt;"
            + "&lt;required_api_vers&gt;1,2,7&lt;/required_api_vers&gt;"
            + "&lt;file_extra_info_help&gt;None&lt;/file_extra_info_help&gt;"
            + "&lt;ss_extra_info_help&gt;None&lt;/ss_extra_info_help&gt;"
            + "&lt;filesys_name&gt;NFS&lt;/filesys_name&gt;"
            + "&lt;vendor_name&gt;Oracle&lt;/vendor_name&gt;"
            + "&lt;plugin_type&gt;ifs&lt;/plugin_type&gt;"
            + "&lt;abilities&gt;"
            + "&lt;resize_is_sync&gt;UNSUPPORTED&lt;/resize_is_sync&gt;"
            + "&lt;clone_is_sync&gt;UNSUPPORTED&lt;/clone_is_sync&gt;"
            + "&lt;access_control&gt;NO&lt;/access_control&gt;"
            + "&lt;custom_clone_name&gt;UNSUPPORTED&lt;/custom_clone_name&gt;"
            + "&lt;require_storage_name&gt;NO&lt;/require_storage_name&gt;"
            + "&lt;backing_device_type&gt;UNSUPPORTED&lt;/backing_device_type&gt;"
            + "&lt;splitclone_while_open&gt;UNSUPPORTED&lt;/splitclone_while_open&gt;"
            + "&lt;custom_snap_name&gt;UNSUPPORTED&lt;/custom_snap_name&gt;"
            + "&lt;snapshot&gt;UNSUPPORTED&lt;/snapshot&gt;"
            + "&lt;splitclone&gt;UNSUPPORTED&lt;/splitclone&gt;"
            + "&lt;snap_is_sync&gt;UNSUPPORTED&lt;/snap_is_sync&gt;"
            + "&lt;snapclone&gt;UNSUPPORTED&lt;/snapclone&gt;"
            + "&lt;splitclone_is_sync&gt;UNSUPPORTED&lt;/splitclone_is_sync&gt;"
            + "&lt;clone&gt;UNSUPPORTED&lt;/clone&gt;"
            + "&lt;resize&gt;UNSUPPORTED&lt;/resize&gt;"
            + "&lt;snapclone_is_sync&gt;UNSUPPORTED&lt;/snapclone_is_sync&gt;"
            + "&lt;/abilities&gt;" + "&lt;/storage_plugin_info&gt;"
            + "&lt;/storage_plugin_info_list&gt;"
            + "&lt;/Discover_Storage_Plugins_Result&gt;" + "</string>";
    String NFSMOUNTRESPONSEXML = "<struct>" + "<member>"
            + "<name>status</name>" + "<value><string></string></value>"
            + "</member>" + "<member>" + "<name>uuid</name>"
            + "<value><string>"
            + FSPROPUUID
            + "</string></value>"
            + "</member>"
            + "<member>"
            + "<name>ss_uuid</name>"
            + "<value><string>"
            + FSMNTUUID
            + "</string></value>"
            + "</member>"
            + "<member>"
            + "<name>size</name>"
            + "<value><string>263166853120</string></value>"
            + "</member>"
            + "<member>"
            + "<name>free_sz</name>"
            + "<value><string>259377299456</string></value>"
            + "</member>"
            + "<member>"
            + "<name>state</name>"
            + "<value><string>1</string></value>"
            + "</member>"
            + "<member>"
            + "<name>mount_options</name>"
            + "<value><array><data>"
            + "</data></array></value>"
            + "</member>"
            + "<member>"
            + "<name>access_grp_names</name>"
            + "<value><array><data>"
            + "</data></array></value>"
            + "</member>"
            + "<member>"
            + "<name>access_path</name>"
            + "<value><string>"
            + NFSHOST
            + ":"
            + NFSPATH
            + "</string></value>"
            + "</member>"
            + "<member>"
            + "<name>name</name>"
            + "<value><string>nfs:"
            + NFSPATH
            + "</string></value>" + "</member>" + "</struct>";
    public String getNfsMountResponseXml() {
        return NFSMOUNTRESPONSEXML;
    }

    public String getNfsFileSystemInfo() {
        return NFSFILESYSTEMINFO;
    }
    public String getFileCreateXml() {
        return FILECREATEXML;
    }
    public Long getFileSize() {
        return SIZE;
    }
    public String getPoolUuid() {
        return POOLUUID;
    }
    public String getFileName() {
        return FILE;
    }
    String NFSFILESYSTEMINFO = NFSMOUNTRESPONSEXML;
    String FILECREATEXML = "<struct>" + "<member>" + "<name>fr_type</name>"
            + "<value><string>File</string></value>" + "</member>" + "<member>"
            + "<name>ondisk_sz</name>" + "<value><string>0</string></value>"
            + "</member>" + "<member>" + "<name>fs_uuid</name>"
            + "<value><string>" + FSMNTUUID + "</string></value>" + "</member>"
            + "<member>" + "<name>file_path</name>"
            + "<value><string>/OVS/Repositories/" + POOLUUID + "/VirtualDisks/"
            + FILE + "</string></value>" + "</member>" + "<member>"
            + "<name>file_sz</name>" + "<value><string>" + SIZE
            + "</string></value>" + "</member>" + "</struct>";

    @Test
    public void testNFSStorageMountCreation() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(NFSMOUNTRESPONSEXML));
        StorageDetails sd = sPt.storagePluginMountNFS(NFSHOST, NFSPATH,
                FSMNTUUID, NFSMNT);
        con.setResult(results.simpleResponseWrapWrapper(NFSMOUNTRESPONSEXML));
        NFSMNT = NFSMNT + "/" + FSMNTUUID;
        sd = sPt.storagePluginMountNFS(NFSHOST, NFSPATH, FSMNTUUID, NFSMNT);
        results.basicLongTest(Long.valueOf(sd.getSize()), 263166853120L);
        results.basicLongTest(Long.valueOf(sd.getFreeSize()), 259377299456L);
        results.basicStringTest(sd.getName(), "nfs:" + NFSPATH);
        results.basicStringTest(sd.getUuid(), FSPROPUUID);
        results.basicStringTest(sd.getDetailsRelationalUuid(),FSMNTUUID);
    }

    @Test
    public void testNFSStorageUnmount() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(sPt.storagePluginUnmountNFS(NFSHOST, NFSPATH, FSMNTUUID, NFSMNT));
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testStoragePluginIncorrectSsUuid() throws Ovm3ResourceException {
        sPt.getStorageDetails().setDetailsRelationalUuid(FSMNTUUID);
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testStoragePluginIncorrectMntUuid()
            throws Ovm3ResourceException {
        sPt.getStorageDetails().setUuid(FSPROPUUID);
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testStoragePluginIncorrectUuid() throws Ovm3ResourceException {
        sPt.getStorageServer().setUuid(FSMNTUUID);
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testStoragePluginNFSmountInvalidUuid()
            throws Ovm3ResourceException {
        NFSMOUNTRESPONSEXML = NFSMOUNTRESPONSEXML.replaceAll(FSPROPUUID,
                sPt.deDash(FSMNTUUID));
        con.setResult(results.simpleResponseWrapWrapper(NFSMOUNTRESPONSEXML));
        assertNotNull(sPt.storagePluginMountNFS(NFSHOST, NFSPATH,
                FSPROPUUID, NFSMNT));
    }

    @Test
    public void testStorageFileCreation() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(FILECREATEXML));
        FileProperties file = sPt.storagePluginCreate(FSMNTUUID, NFSHOST, FILE,
                SIZE, false);
        file.getOnDiskSize();
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testStorageFileCreationFileExistS()
            throws Ovm3ResourceException {
        con.setResult(results
                .errorResponseWrap("exceptions OSError:[Errno.17] File exists "
                        + FILE));
        FileProperties file = sPt.storagePluginCreate(FSMNTUUID, NFSHOST, FILE,
                SIZE, false);
        file.getSize();
    }

    @Test
    public void testStorageFileInfo() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(FILECREATEXML));
        FileProperties file = sPt.storagePluginGetFileInfo(FSMNTUUID, NFSHOST,
                FILE);
        file.getName();
        file.getUuid();
        file.getType();
        file.getSize();
    }

    @Test
    public void testDiscoverStoragePlugins() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(STORAGEPLUGINXML));
        for (String plugin : sPt.discoverStoragePlugins()) {
            sPt.checkStoragePluginProperties(plugin, "plugin_version");
            sPt.checkStoragePluginAbility(plugin, "snapshot");
        }
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testCheckStoragePluginBogusPlugin()
            throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(STORAGEPLUGINXML));
        sPt.checkStoragePluginProperties("bogus", "plugin_version");
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testCheckStoragePluginBogusProperty()
            throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(STORAGEPLUGINXML));
        sPt.checkStoragePluginAbility(sPt.getPluginType(), "blabla");
    }

    @Test
    public void testMounts() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(NFSMOUNTRESPONSEXML));
        sPt.storagePluginListFs(NFSHOST);
    }

    @Test
    public void testGetFileSystemInfo() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(NFSMOUNTRESPONSEXML));
        sPt.storagePluginGetFileSystemInfo(FSPROPUUID, FSMNTUUID, NFSHOST,
                NFSPATH);
    }

    @Test
    public void testStoragepluginDestroy() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        sPt.storagePluginDestroy(FSMNTUUID, FILE);
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testStoragepluginDestroyWrongUUID()
            throws Ovm3ResourceException {
        con.setResult(results.getNil());
        sPt.storagePluginDestroy(FSPROPUUID, FILE);
    }

    @Test
    public void testStoragePluginSwitch() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(STORAGEPLUGINXML));
        sPt.setISCSI();
        sPt.setNFS();
        sPt.setOCFS2();
    }
}
