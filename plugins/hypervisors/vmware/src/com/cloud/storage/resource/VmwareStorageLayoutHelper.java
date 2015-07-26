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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreFile;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.utils.Pair;

/**
 *
 * To provide helper methods to handle storage layout in one place
 *
 */
public class VmwareStorageLayoutHelper {
    private static final Logger s_logger = Logger.getLogger(VmwareStorageLayoutHelper.class);

    public static String[] getVmdkFilePairDatastorePath(DatastoreMO dsMo, String vmName, String vmdkName, VmwareStorageLayoutType layoutType, boolean linkedVmdk)
        throws Exception {

        String[] filePair = new String[2];
        switch (layoutType) {
            case VMWARE:
                assert (vmName != null && !vmName.isEmpty());
                filePair[0] = getVmwareDatastorePathFromVmdkFileName(dsMo, vmName, vmdkName + ".vmdk");

                if (linkedVmdk)
                    filePair[1] = getVmwareDatastorePathFromVmdkFileName(dsMo, vmName, vmdkName + "-delta.vmdk");
                else
                    filePair[1] = getVmwareDatastorePathFromVmdkFileName(dsMo, vmName, vmdkName + "-flat.vmdk");
                return filePair;

            case CLOUDSTACK_LEGACY:
                filePair[0] = getLegacyDatastorePathFromVmdkFileName(dsMo, vmdkName + ".vmdk");

                if (linkedVmdk)
                    filePair[1] = getLegacyDatastorePathFromVmdkFileName(dsMo, vmdkName + "-delta.vmdk");
                else
                    filePair[1] = getLegacyDatastorePathFromVmdkFileName(dsMo, vmdkName + "-flat.vmdk");
                return filePair;

            default:
                assert (false);
                break;
        }

        assert (false);
        return null;
    }

    public static String findVolumeDatastoreFullPath(DatastoreMO dsMo, String vmName, String vmdkFileName) throws Exception {
        if (vmName != null) {
            String path = getVmwareDatastorePathFromVmdkFileName(dsMo, vmName, vmdkFileName);
            if (!dsMo.fileExists(path)) {
                path = getLegacyDatastorePathFromVmdkFileName(dsMo, vmdkFileName);

                // to save one call to vCenter, we won't check file existence for this round, so the caller
                // may still fail with exception, but if that's case, we will let it fail anyway
            }
            return path;
        } else {
            String path = getLegacyDatastorePathFromVmdkFileName(dsMo, vmdkFileName);
            if (!dsMo.fileExists(path)) {
                // Datastore file movement is not atomic operations, we need to sync and repair
                path = dsMo.searchFileInSubFolders(vmdkFileName, false);

                // to save one call to vCenter, we won't check file existence for this round, so the caller
                // may still fail with exception, but if that's case, we will let it fail anyway
            }
            return path;
        }
    }

    public static String syncVolumeToVmDefaultFolder(DatacenterMO dcMo, String vmName, DatastoreMO ds, String vmdkName) throws Exception {

        assert (ds != null);
        if (!ds.folderExists(String.format("[%s]", ds.getName()), vmName)) {
            s_logger.info("VM folder does not exist on target datastore, we will create one. vm: " + vmName + ", datastore: " + ds.getName());

            ds.makeDirectory(String.format("[%s] %s", ds.getName(), vmName), dcMo.getMor());
        }

        String[] vmdkLinkedCloneModeLegacyPair = getVmdkFilePairDatastorePath(ds, vmName, vmdkName, VmwareStorageLayoutType.CLOUDSTACK_LEGACY, true);
        String[] vmdkFullCloneModeLegacyPair = getVmdkFilePairDatastorePath(ds, vmName, vmdkName, VmwareStorageLayoutType.CLOUDSTACK_LEGACY, false);

        String[] vmdkLinkedCloneModePair = getVmdkFilePairDatastorePath(ds, vmName, vmdkName, VmwareStorageLayoutType.VMWARE, true);
        String[] vmdkFullCloneModePair = getVmdkFilePairDatastorePath(ds, vmName, vmdkName, VmwareStorageLayoutType.VMWARE, false);

        if (!ds.fileExists(vmdkLinkedCloneModeLegacyPair[0]) && !ds.fileExists(vmdkLinkedCloneModePair[0])) {
            // To protect against inconsistency caused by non-atomic datastore file management, detached disk may
            // be left over in its previous owner VM. We will do a fixup synchronization here by moving it to root
            // again.
            //
            syncVolumeToRootFolder(dcMo, ds, vmdkName, vmName);
        }

        if (ds.fileExists(vmdkFullCloneModeLegacyPair[1])) {
            s_logger.info("sync " + vmdkFullCloneModeLegacyPair[1] + "->" + vmdkFullCloneModePair[1]);

            ds.moveDatastoreFile(vmdkFullCloneModeLegacyPair[1], dcMo.getMor(), ds.getMor(), vmdkFullCloneModePair[1], dcMo.getMor(), true);
        }

        if (ds.fileExists(vmdkLinkedCloneModeLegacyPair[1])) {
            s_logger.info("sync " + vmdkLinkedCloneModeLegacyPair[1] + "->" + vmdkLinkedCloneModePair[1]);

            ds.moveDatastoreFile(vmdkLinkedCloneModeLegacyPair[1], dcMo.getMor(), ds.getMor(), vmdkLinkedCloneModePair[1], dcMo.getMor(), true);
        }

        if (ds.fileExists(vmdkLinkedCloneModeLegacyPair[0])) {
            s_logger.info("sync " + vmdkLinkedCloneModeLegacyPair[0] + "->" + vmdkLinkedCloneModePair[0]);
            ds.moveDatastoreFile(vmdkLinkedCloneModeLegacyPair[0], dcMo.getMor(), ds.getMor(), vmdkLinkedCloneModePair[0], dcMo.getMor(), true);
        }

        // Note: we will always return a path
        return vmdkLinkedCloneModePair[0];
    }

    public static void syncVolumeToRootFolder(DatacenterMO dcMo, DatastoreMO ds, String vmdkName, String vmName) throws Exception {
        String fileDsFullPath = ds.searchFileInSubFolders(vmdkName + ".vmdk", false);
        if (fileDsFullPath == null)
            return;

        String folderName = null;
        if (ds.folderExists(String.format("[%s]", ds.getName()), vmName)) {
            folderName = String.format("[%s] %s", ds.getName(), vmName);
        }

        DatastoreFile srcDsFile = new DatastoreFile(fileDsFullPath);
        String companionFilePath = srcDsFile.getCompanionPath(vmdkName + "-flat.vmdk");
        if (ds.fileExists(companionFilePath)) {
            String targetPath = getLegacyDatastorePathFromVmdkFileName(ds, vmdkName + "-flat.vmdk");

            s_logger.info("Fixup folder-synchronization. move " + companionFilePath + " -> " + targetPath);
            ds.moveDatastoreFile(companionFilePath, dcMo.getMor(), ds.getMor(), targetPath, dcMo.getMor(), true);
        }

        companionFilePath = srcDsFile.getCompanionPath(vmdkName + "-delta.vmdk");
        if (ds.fileExists(companionFilePath)) {
            String targetPath = getLegacyDatastorePathFromVmdkFileName(ds, vmdkName + "-delta.vmdk");

            s_logger.info("Fixup folder-synchronization. move " + companionFilePath + " -> " + targetPath);
            ds.moveDatastoreFile(companionFilePath, dcMo.getMor(), ds.getMor(), targetPath, dcMo.getMor(), true);
        }

        // move the identity VMDK file the last
        String targetPath = getLegacyDatastorePathFromVmdkFileName(ds, vmdkName + ".vmdk");
        s_logger.info("Fixup folder-synchronization. move " + fileDsFullPath + " -> " + targetPath);
        ds.moveDatastoreFile(fileDsFullPath, dcMo.getMor(), ds.getMor(), targetPath, dcMo.getMor(), true);

        if (folderName != null) {
            String[] files = ds.listDirContent(folderName);
            if (files == null || files.length == 0) {
                ds.deleteFolder(folderName, dcMo.getMor());
            }
        }
    }

    public static void moveVolumeToRootFolder(DatacenterMO dcMo, List<String> detachedDisks) throws Exception {
        if (detachedDisks.size() > 0) {
            for (String fileFullDsPath : detachedDisks) {
                DatastoreFile file = new DatastoreFile(fileFullDsPath);

                s_logger.info("Check if we need to move " + fileFullDsPath + " to its root location");
                DatastoreMO dsMo = new DatastoreMO(dcMo.getContext(), dcMo.findDatastore(file.getDatastoreName()));
                if (dsMo.getMor() != null) {
                    DatastoreFile targetFile = new DatastoreFile(file.getDatastoreName(), file.getFileName());
                    if (!targetFile.getPath().equalsIgnoreCase(file.getPath())) {
                        s_logger.info("Move " + file.getPath() + " -> " + targetFile.getPath());
                        dsMo.moveDatastoreFile(file.getPath(), dcMo.getMor(), dsMo.getMor(), targetFile.getPath(), dcMo.getMor(), true);

                        String pairSrcFilePath = file.getCompanionPath(file.getFileBaseName() + "-flat.vmdk");
                        String pairTargetFilePath = targetFile.getCompanionPath(file.getFileBaseName() + "-flat.vmdk");
                        if (dsMo.fileExists(pairSrcFilePath)) {
                            s_logger.info("Move " + pairSrcFilePath + " -> " + pairTargetFilePath);
                            dsMo.moveDatastoreFile(pairSrcFilePath, dcMo.getMor(), dsMo.getMor(), pairTargetFilePath, dcMo.getMor(), true);
                        }

                        pairSrcFilePath = file.getCompanionPath(file.getFileBaseName() + "-delta.vmdk");
                        pairTargetFilePath = targetFile.getCompanionPath(file.getFileBaseName() + "-delta.vmdk");
                        if (dsMo.fileExists(pairSrcFilePath)) {
                            s_logger.info("Move " + pairSrcFilePath + " -> " + pairTargetFilePath);
                            dsMo.moveDatastoreFile(pairSrcFilePath, dcMo.getMor(), dsMo.getMor(), pairTargetFilePath, dcMo.getMor(), true);
                        }
                    }
                } else {
                    s_logger.warn("Datastore for " + fileFullDsPath + " no longer exists, we have to skip");
                }
            }
        }
    }

    public static String getTemplateOnSecStorageFilePath(String secStorageMountPoint, String templateRelativeFolderPath, String templateName, String fileExtension) {

        StringBuffer sb = new StringBuffer();
        sb.append(secStorageMountPoint);
        if (!secStorageMountPoint.endsWith("/"))
            sb.append("/");

        sb.append(templateRelativeFolderPath);
        if (!secStorageMountPoint.endsWith("/"))
            sb.append("/");

        sb.append(templateName);
        if (!fileExtension.startsWith("."))
            sb.append(".");
        sb.append(fileExtension);

        return sb.toString();
    }

    /*
     *  return Pair of <Template relative path, Template name>
     *  Template url may or may not end with .ova extension
     */
    public static Pair<String, String> decodeTemplateRelativePathAndNameFromUrl(String storeUrl, String templateUrl, String defaultName) {

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

        String fileName = volumeName + ".vmdk";
        String fileFullPath = getLegacyDatastorePathFromVmdkFileName(dsMo, fileName);
        if (!dsMo.fileExists(fileFullPath))
            fileFullPath = dsMo.searchFileInSubFolders(fileName, false);
        if (fileFullPath != null) {
            dsMo.deleteFile(fileFullPath, dcMo.getMor(), true);
        } else {
            s_logger.warn("Unable to locate VMDK file: " + fileName);
        }

        fileName = volumeName + "-flat.vmdk";
        fileFullPath = getLegacyDatastorePathFromVmdkFileName(dsMo, fileName);
        if (!dsMo.fileExists(fileFullPath))
            fileFullPath = dsMo.searchFileInSubFolders(fileName, false);
        if (fileFullPath != null) {
            dsMo.deleteFile(fileFullPath, dcMo.getMor(), true);
        } else {
            s_logger.warn("Unable to locate VMDK file: " + fileName);
        }

        fileName = volumeName + "-delta.vmdk";
        fileFullPath = getLegacyDatastorePathFromVmdkFileName(dsMo, fileName);
        if (!dsMo.fileExists(fileFullPath))
            fileFullPath = dsMo.searchFileInSubFolders(fileName, false);
        if (fileFullPath != null) {
            dsMo.deleteFile(fileFullPath, dcMo.getMor(), true);
        } else {
            s_logger.warn("Unable to locate VMDK file: " + fileName);
        }
    }

    public static String getLegacyDatastorePathFromVmdkFileName(DatastoreMO dsMo, String vmdkFileName) throws Exception {
        return String.format("[%s] %s", dsMo.getName(), vmdkFileName);
    }

    public static String getVmwareDatastorePathFromVmdkFileName(DatastoreMO dsMo, String vmName, String vmdkFileName) throws Exception {
        return String.format("[%s] %s/%s", dsMo.getName(), vmName, vmdkFileName);
    }
}
