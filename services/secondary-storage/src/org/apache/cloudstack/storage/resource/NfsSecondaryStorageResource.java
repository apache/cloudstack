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
package org.apache.cloudstack.storage.resource;

import static com.cloud.utils.S3Utils.putFile;
import static com.cloud.utils.StringUtils.join;
import static com.cloud.utils.db.GlobalLock.executeWithNoWaitLock;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.substringAfterLast;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.storage.*;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.template.*;
import com.cloud.utils.SwiftUtil;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.template.DownloadManager;
import org.apache.cloudstack.storage.template.DownloadManagerImpl;
import org.apache.cloudstack.storage.template.DownloadManagerImpl.ZfsPathParser;
import org.apache.cloudstack.storage.template.UploadManager;
import org.apache.cloudstack.storage.template.UploadManagerImpl;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.agent.api.DownloadSnapshotFromS3Command;
import com.cloud.agent.api.DownloadSnapshotFromSwiftCommand;
import com.cloud.agent.api.DownloadTemplateFromSwiftToSecondaryStorageCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingStorageCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand.PortConfig;
import com.cloud.agent.api.SecStorageSetupAnswer;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.SecStorageSetupCommand.Certificates;
import com.cloud.agent.api.SecStorageVMSetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.api.UploadTemplateToSwiftFromSecondaryStorageCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ServerResourceBase;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.S3Utils;
import com.cloud.utils.S3Utils.FileNamingStrategy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.vm.SecondaryStorageVm;

public class NfsSecondaryStorageResource extends ServerResourceBase implements SecondaryStorageResource {

    private static final Logger s_logger = Logger.getLogger(NfsSecondaryStorageResource.class);

    private static final String TEMPLATE_ROOT_DIR = "template/tmpl";
    private static final String SNAPSHOT_ROOT_DIR = "snapshots";
    private static final String VOLUME_ROOT_DIR = "volumes";

    int _timeout;

    public int getTimeout() {
        return _timeout;
    }

    public void setTimeout(int _timeout) {
        this._timeout = _timeout;
    }

    String _instance;
    String _dc;
    String _pod;
    String _guid;
    String _role;
    Map<String, Object> _params;
    protected StorageLayer _storage;
    protected boolean _inSystemVM = false;
    boolean _sslCopy = false;

    protected DownloadManager _dlMgr;
    protected UploadManager _upldMgr;
    private String _configSslScr;
    private String _configAuthScr;
    private String _configIpFirewallScr;
    private String _publicIp;
    private String _hostname;
    private String _localgw;
    private String _eth1mask;
    private String _eth1ip;
    private String _storageIp;
    private String _storageNetmask;
    private String _storageGateway;
    private final List<String> nfsIps = new ArrayList<String>();
    protected String _parent = "/mnt/SecStorage";
    final private String _tmpltDir = "/var/cloudstack/template";
    final private String _tmpltpp = "template.properties";
    protected String createTemplateFromSnapshotXenScript;

    @Override
    public void disconnected() {
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadProgressCommand) {
            return _dlMgr.handleDownloadCommand(this, (DownloadProgressCommand) cmd);
        } else if (cmd instanceof DownloadCommand) {
            return execute((DownloadCommand) cmd);
        } else if (cmd instanceof UploadCommand) {
            return _upldMgr.handleUploadCommand(this, (UploadCommand) cmd);
        } else if (cmd instanceof CreateEntityDownloadURLCommand) {
            return _upldMgr.handleCreateEntityURLCommand((CreateEntityDownloadURLCommand) cmd);
        } else if (cmd instanceof DeleteEntityDownloadURLCommand) {
            return _upldMgr.handleDeleteEntityDownloadURLCommand((DeleteEntityDownloadURLCommand) cmd);
        } else if (cmd instanceof GetStorageStatsCommand) {
            return execute((GetStorageStatsCommand) cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return new CheckHealthAnswer((CheckHealthCommand) cmd, true);
        } else if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand) cmd);
        } else if (cmd instanceof SecStorageFirewallCfgCommand) {
            return execute((SecStorageFirewallCfgCommand) cmd);
        } else if (cmd instanceof SecStorageVMSetupCommand) {
            return execute((SecStorageVMSetupCommand) cmd);
        } else if (cmd instanceof SecStorageSetupCommand) {
            return execute((SecStorageSetupCommand) cmd);
        } else if (cmd instanceof ComputeChecksumCommand) {
            return execute((ComputeChecksumCommand) cmd);
        } else if (cmd instanceof ListTemplateCommand) {
            return execute((ListTemplateCommand) cmd);
        } else if (cmd instanceof ListVolumeCommand) {
            return execute((ListVolumeCommand) cmd);
        } else if (cmd instanceof DownloadSnapshotFromSwiftCommand) {
            return execute((DownloadSnapshotFromSwiftCommand) cmd);
        } else if (cmd instanceof DownloadSnapshotFromS3Command) {
            return execute((DownloadSnapshotFromS3Command) cmd);
        } else if (cmd instanceof DeleteSnapshotsDirCommand) {
            return execute((DeleteSnapshotsDirCommand) cmd);
        } else if (cmd instanceof DownloadTemplateFromSwiftToSecondaryStorageCommand) {
            return execute((DownloadTemplateFromSwiftToSecondaryStorageCommand) cmd);
        } else if (cmd instanceof UploadTemplateToSwiftFromSecondaryStorageCommand) {
            return execute((UploadTemplateToSwiftFromSecondaryStorageCommand) cmd);
        } else if (cmd instanceof CopyCommand) {
            return execute((CopyCommand) cmd);
        } else if (cmd instanceof DeleteCommand) {
            return execute((DeleteCommand) cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }


    protected CopyCmdAnswer postProcessing(File destFile, String downloadPath, String destPath, DataTO srcData, DataTO destData) throws ConfigurationException {
        // do post processing to unzip the file if it is compressed
        String scriptsDir = "scripts/storage/secondary";
        String createTmpltScr = Script.findScript(scriptsDir, "createtmplt.sh");
        if (createTmpltScr == null) {
            throw new ConfigurationException("Unable to find createtmplt.sh");
        }
        s_logger.info("createtmplt.sh found in " + createTmpltScr);
        String createVolScr = Script.findScript(scriptsDir, "createvolume.sh");
        if (createVolScr == null) {
            throw new ConfigurationException("Unable to find createvolume.sh");
        }
        s_logger.info("createvolume.sh found in " + createVolScr);
        String script = srcData.getObjectType() == DataObjectType.TEMPLATE ? createTmpltScr : createVolScr;

        int installTimeoutPerGig = 180 * 60 * 1000;
        int imgSizeGigs = (int) Math.ceil(destFile.length() * 1.0d / (1024 * 1024 * 1024));
        imgSizeGigs++; // add one just in case
        long timeout = imgSizeGigs * installTimeoutPerGig;

        String origPath = destFile.getAbsolutePath();
        String extension = null;
        if (srcData.getObjectType() == DataObjectType.TEMPLATE) {
            extension = ((TemplateObjectTO) srcData).getFormat().getFileExtension();
        } else {
            extension = ((VolumeObjectTO) srcData).getFormat().getFileExtension();
        }

        String templateName = UUID.randomUUID().toString();
        String templateFilename = templateName + "." + extension;
        Script scr = new Script(script, timeout, s_logger);
        scr.add("-s", Integer.toString(imgSizeGigs)); // not used for now
        scr.add("-n", templateFilename);

        scr.add("-t", downloadPath);
        scr.add("-f", origPath); // this is the temporary
        // template file downloaded
        String result;
        result = scr.execute();

        if (result != null) {
            // script execution failure
            throw new CloudRuntimeException("Failed to run script " + script);
        }

        String finalFileName = templateFilename;
        String finalDownloadPath = destPath + File.separator + templateFilename;
        // compute the size of
        long size = this._storage.getSize(downloadPath + File.separator + templateFilename);

        DataTO newDestTO = null;

        if (destData.getObjectType() == DataObjectType.TEMPLATE) {
            TemplateObjectTO newTemplTO = new TemplateObjectTO();
            newTemplTO.setPath(finalDownloadPath);
            newTemplTO.setName(finalFileName);
            newTemplTO.setSize(size);
            newDestTO = newTemplTO;
        } else {
            return new CopyCmdAnswer("not implemented yet");
        }

        return new CopyCmdAnswer(newDestTO);
    }
    protected Answer copyFromSwiftToNfs(CopyCommand cmd, DataTO srcData, SwiftTO swiftTO, DataTO destData, NfsTO destImageStore) {
        final String storagePath = destImageStore.getUrl();
        final String destPath = destData.getPath();
        try {
            String downloadPath = determineStorageTemplatePath(storagePath, destPath);
            final File downloadDirectory = _storage.getFile(downloadPath);
            downloadDirectory.mkdirs();
            File destFile = SwiftUtil.getObject(swiftTO, downloadDirectory, srcData.getPath());
            return postProcessing(destFile,downloadPath,destPath,srcData,destData);
        } catch (Exception e) {
            s_logger.debug("Failed to copy swift to nfs", e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    protected Answer copyFromS3ToNfs(CopyCommand cmd, DataTO srcData, S3TO s3, DataTO destData, NfsTO destImageStore) {
        final String storagePath = destImageStore.getUrl();
        final String destPath = destData.getPath();

        try {

            String downloadPath = determineStorageTemplatePath(storagePath, destPath);
            final File downloadDirectory = _storage.getFile(downloadPath);
            downloadDirectory.mkdirs();

            if (!downloadDirectory.exists()) {
                final String errMsg = "Unable to create directory " + downloadPath + " to copy from S3 to cache.";
                s_logger.error(errMsg);
                return new CopyCmdAnswer(errMsg);
            } else {
                s_logger.debug("Directory " + downloadPath + " already exists");
            }

            File destFile = S3Utils.getFile(s3, s3.getBucketName(), srcData.getPath(), downloadDirectory,
                    new FileNamingStrategy() {
                        @Override
                        public String determineFileName(final String key) {
                            return substringAfterLast(key, S3Utils.SEPARATOR);
                        }
                    });

            if (destFile == null) {
                return new CopyCmdAnswer("Can't find template");
            }

            return postProcessing(destFile,downloadPath,destPath,srcData,destData);
        } catch (Exception e) {

            final String errMsg = format("Failed to download" + "due to $2%s", e.getMessage());
            s_logger.error(errMsg, e);
            return new CopyCmdAnswer(errMsg);
        }
    }

    protected Answer copySnapshotToTemplateFromNfsToNfsXenserver(CopyCommand cmd, SnapshotObjectTO srcData,
            NfsTO srcDataStore, TemplateObjectTO destData, NfsTO destDataStore) {
        String srcMountPoint = this.getRootDir(srcDataStore.getUrl());
        String snapshotPath = srcData.getPath();
        int index = snapshotPath.lastIndexOf("/");
        String snapshotName = snapshotPath.substring(index + 1);
        if (!snapshotName.startsWith("VHD-") && !snapshotName.endsWith(".vhd")) {
            snapshotName = snapshotName + ".vhd";
        }
        snapshotPath = snapshotPath.substring(0, index);
        snapshotPath = srcMountPoint + File.separator + snapshotPath;
        String destMountPoint = this.getRootDir(destDataStore.getUrl());
        String destPath = destMountPoint + File.separator + destData.getPath();

        String errMsg = null;
        try {
            this._storage.mkdir(destPath);

            String templateUuid = UUID.randomUUID().toString();
            String templateName = templateUuid + ".vhd";
            Script command = new Script(this.createTemplateFromSnapshotXenScript, cmd.getWait(), s_logger);
            command.add("-p", snapshotPath);
            command.add("-s", snapshotName);
            command.add("-n", templateName);
            command.add("-t", destPath);
            String result = command.execute();

            if (result != null && !result.equalsIgnoreCase("")) {
                return new CopyCmdAnswer(result);
            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            Processor processor = new VhdProcessor();

            processor.configure("Vhd Processor", params);
            FormatInfo info = processor.process(destPath, null, templateUuid);

            TemplateLocation loc = new TemplateLocation(_storage, destPath);
            loc.create(1, true, templateName);
            loc.addFormat(info);
            loc.save();
            TemplateProp prop = loc.getTemplateInfo();
            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(destData.getPath() + File.separator + templateName);
            newTemplate.setFormat(ImageFormat.VHD);
            newTemplate.setSize(prop.getSize());
            return new CopyCmdAnswer(newTemplate);
        } catch (ConfigurationException e) {
            s_logger.debug("Failed to create template from snapshot: " + e.toString());
            errMsg = e.toString();
        } catch (InternalErrorException e) {
            s_logger.debug("Failed to create template from snapshot: " + e.toString());
            errMsg = e.toString();
        } catch (IOException e) {
            s_logger.debug("Failed to create template from snapshot: " + e.toString());
            errMsg = e.toString();
        }

        return new CopyCmdAnswer(errMsg);
    }

    protected Answer copySnapshotToTemplateFromNfsToNfs(CopyCommand cmd, SnapshotObjectTO srcData, NfsTO srcDataStore,
            TemplateObjectTO destData, NfsTO destDataStore) {

        if (srcData.getHypervisorType() == HypervisorType.XenServer) {
            return copySnapshotToTemplateFromNfsToNfsXenserver(cmd, srcData, srcDataStore, destData, destDataStore);
        }

        return new CopyCmdAnswer("");
    }

    protected Answer createTemplateFromSnapshot(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        DataStoreTO srcDataStore = srcData.getDataStore();
        DataStoreTO destDataStore = destData.getDataStore();
        if (srcDataStore.getRole() == DataStoreRole.Image || srcDataStore.getRole() == DataStoreRole.ImageCache) {
            if (!(srcDataStore instanceof NfsTO)) {
                s_logger.debug("only support nfs storage as src, when create template from snapshot");
                return Answer.createUnsupportedCommandAnswer(cmd);
            }

            if (destDataStore instanceof NfsTO) {
                return copySnapshotToTemplateFromNfsToNfs(cmd, (SnapshotObjectTO) srcData, (NfsTO) srcDataStore,
                        (TemplateObjectTO) destData, (NfsTO) destDataStore);
            }

        }
        return new CopyCmdAnswer("");
    }

    protected Answer copyFromNfsToImage(CopyCommand cmd) {
        DataTO destData = cmd.getDestTO();
        DataStoreTO destDataStore = destData.getDataStore();

        if (destDataStore instanceof S3TO) {
            return copyFromNfsToS3(cmd);
        } else {
            return new CopyCmdAnswer("unsupported ");
        }
    }

    protected Answer execute(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        DataStoreTO srcDataStore = srcData.getDataStore();
        DataStoreTO destDataStore = destData.getDataStore();

        if (srcData.getObjectType() == DataObjectType.SNAPSHOT && destData.getObjectType() == DataObjectType.TEMPLATE) {
            return createTemplateFromSnapshot(cmd);
        }

        if (destDataStore instanceof NfsTO
                && destDataStore.getRole() == DataStoreRole.ImageCache) {
            NfsTO destImageStore = (NfsTO) destDataStore;
            if (srcDataStore instanceof S3TO) {
                S3TO s3 = (S3TO) srcDataStore;
                return this.copyFromS3ToNfs(cmd, srcData, s3, destData, destImageStore);
            } else if (srcDataStore instanceof SwiftTO) {
                return copyFromSwiftToNfs(cmd, srcData, (SwiftTO)srcDataStore, destData, destImageStore);
            }
        }

        if (srcDataStore.getRole() == DataStoreRole.ImageCache && destDataStore.getRole() == DataStoreRole.Image) {
            return copyFromNfsToImage(cmd);
        }

        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    @SuppressWarnings("unchecked")
    protected String determineS3TemplateDirectory(final Long accountId, final Long templateId,
            final String templateUniqueName) {
        return join(asList(TEMPLATE_ROOT_DIR, accountId, templateId, templateUniqueName), S3Utils.SEPARATOR);
    }

    @SuppressWarnings("unchecked")
    private String determineS3TemplateNameFromKey(String key) {
        return StringUtils.substringAfterLast(StringUtils.substringBeforeLast(key, S3Utils.SEPARATOR),
                S3Utils.SEPARATOR);
    }

    @SuppressWarnings("unchecked")
    protected String determineS3VolumeDirectory(final Long accountId, final Long volId) {
        return join(asList(VOLUME_ROOT_DIR, accountId, volId), S3Utils.SEPARATOR);
    }

    @SuppressWarnings("unchecked")
    protected Long determineS3VolumeIdFromKey(String key) {
        return Long.parseLong(StringUtils.substringAfterLast(StringUtils.substringBeforeLast(key, S3Utils.SEPARATOR),
                S3Utils.SEPARATOR));
    }

    @SuppressWarnings("unchecked")
    private String determineStorageTemplatePath(final String storagePath, String dataPath) {
        return join(asList(getRootDir(storagePath), dataPath), File.separator);
    }

    private Answer execute(DownloadTemplateFromSwiftToSecondaryStorageCommand cmd) {
        SwiftTO swift = cmd.getSwift();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        Long accountId = cmd.getAccountId();
        Long templateId = cmd.getTemplateId();
        String path = cmd.getPath();
        String errMsg;
        String lDir = null;
        try {
            String parent = getRootDir(secondaryStorageUrl);
            lDir = parent + "/template/tmpl/" + accountId.toString() + "/" + templateId.toString();
            String result = createLocalDir(lDir);
            if (result != null) {
                errMsg = "downloadTemplateFromSwiftToSecondaryStorageCommand failed due to Create local directory failed";
                s_logger.warn(errMsg);
                throw new InternalErrorException(errMsg);
            }
            String lPath = lDir + "/" + path;
            result = swiftDownload(swift, "T-" + templateId.toString(), path, lPath);
            if (result != null) {
                errMsg = "failed to download template " + path + " from Swift to secondary storage " + lPath
                        + " , err=" + result;
                s_logger.warn(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
            path = "template.properties";
            lPath = lDir + "/" + path;
            result = swiftDownload(swift, "T-" + templateId.toString(), path, lPath);
            if (result != null) {
                errMsg = "failed to download template " + path + " from Swift to secondary storage " + lPath
                        + " , err=" + result;
                s_logger.warn(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
            return new Answer(cmd, true, "success");
        } catch (Exception e) {
            if (lDir != null) {
                deleteLocalDir(lDir);
            }
            errMsg = cmd + " Command failed due to " + e.toString();
            s_logger.warn(errMsg, e);
            return new Answer(cmd, false, errMsg);
        }
    }

    protected File downloadFromUrlToNfs(String url, NfsTO nfs, String path, String name) {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response =  client.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                s_logger.debug("Faled to get entity");
                throw new CloudRuntimeException("Failed to get url: " + url);
            }

            String nfsMountPath = getRootDir(nfs.getUrl());

            String filePath = nfsMountPath + File.separator + path;
            File directory = new File(filePath);
            if (!directory.exists()) {
                _storage.mkdirs(filePath);
            }
            File destFile = new File(filePath + File.separator + name);
            if (!destFile.exists()) {
                destFile.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(destFile);
            entity.writeTo(outputStream);
            return new File(destFile.getAbsolutePath());
        } catch (IOException e) {
            s_logger.debug("Faild to get url:"+ url + ", due to " + e.toString());
            throw new CloudRuntimeException(e);
        }
    }
    protected Answer registerTemplateOnSwift(DownloadCommand cmd) {
        SwiftTO swiftTO = (SwiftTO)cmd.getDataStore();
        String path = cmd.getInstallPath();
        DataStoreTO cacheStore = cmd.getCacheStore();
        if (cacheStore == null || !(cacheStore instanceof NfsTO)) {
            return new DownloadAnswer("cache store can't be null", VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
        }

        File file = null;
        try {
            NfsTO nfsCacheStore = (NfsTO)cacheStore;
            String fileName = UUID.randomUUID().toString() + "." + cmd.getFormat().getFileExtension();
            file = downloadFromUrlToNfs(cmd.getUrl(), nfsCacheStore, path, fileName);
            String swiftPath = SwiftUtil.putObject(swiftTO, file, "T-" + cmd.getId());
            String md5sum = null;
            try {
                md5sum = DigestUtils.md5Hex(new FileInputStream(file));
            } catch (IOException e) {
                s_logger.debug("Failed to get md5sum: " + file.getAbsoluteFile());
            }

            DownloadAnswer answer = new DownloadAnswer(null, 100, null, VMTemplateStorageResourceAssoc.Status.DOWNLOADED,
                    swiftPath, swiftPath, file.length(), file.length(), md5sum
            );
            return answer;
        } catch (Exception e) {
             s_logger.debug("Failed to register template into swift", e);
            return new DownloadAnswer(e.toString(), VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    private Answer execute(DownloadCommand cmd) {
        DataStoreTO dstore = cmd.getDataStore();
        if (dstore instanceof NfsTO || dstore instanceof S3TO) {
            return _dlMgr.handleDownloadCommand(this, cmd);
        } else if (dstore instanceof SwiftTO) {
            return registerTemplateOnSwift(cmd);
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + dstore);
        }

    }

    private Answer execute(UploadTemplateToSwiftFromSecondaryStorageCommand cmd) {
        SwiftTO swift = cmd.getSwift();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        Long accountId = cmd.getAccountId();
        Long templateId = cmd.getTemplateId();
        try {
            String parent = getRootDir(secondaryStorageUrl);
            String lPath = parent + "/template/tmpl/" + accountId.toString() + "/" + templateId.toString();
            if (!_storage.isFile(lPath + "/template.properties")) {
                String errMsg = cmd + " Command failed due to template doesn't exist ";
                s_logger.debug(errMsg);
                return new Answer(cmd, false, errMsg);
            }
            String result = swiftUpload(swift, "T-" + templateId.toString(), lPath, "*");
            if (result != null) {
                String errMsg = "failed to upload template from secondary storage " + lPath + " to swift  , err="
                        + result;
                s_logger.debug(errMsg);
                return new Answer(cmd, false, errMsg);
            }
            return new Answer(cmd, true, "success");
        } catch (Exception e) {
            String errMsg = cmd + " Command failed due to " + e.toString();
            s_logger.warn(errMsg, e);
            return new Answer(cmd, false, errMsg);
        }
    }

    private ImageFormat getTemplateFormat(String filePath) {
        String ext = null;
        int extensionPos = filePath.lastIndexOf('.');
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        int i = lastSeparator > extensionPos ? -1 : extensionPos;
        if (i > 0) {
            ext = filePath.substring(i + 1);
        }
        if (ext != null) {
            if (ext.equalsIgnoreCase("vhd"))
                return ImageFormat.VHD;
            else if (ext.equalsIgnoreCase("qcow2"))
                return ImageFormat.QCOW2;
            else if (ext.equalsIgnoreCase("ova"))
                return ImageFormat.OVA;
            else if (ext.equalsIgnoreCase("tar"))
                return ImageFormat.TAR;
            else if (ext.equalsIgnoreCase("img"))
                return ImageFormat.RAW;
        }

        return null;

    }

    protected Answer copyFromNfsToS3(CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        DataStoreTO srcDataStore = srcData.getDataStore();
        NfsTO srcStore = (NfsTO) srcDataStore;
        DataStoreTO destDataStore = destData.getDataStore();

        final S3TO s3 = (S3TO) destDataStore;

        try {
            final String templatePath = determineStorageTemplatePath(srcStore.getUrl(), srcData.getPath());

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + srcData.getObjectType() + " from directory " + templatePath
                        + " to upload to S3.");
            }

            final String bucket = s3.getBucketName();
            final File srcFile = _storage.getFile(templatePath);
            ImageFormat format = this.getTemplateFormat(templatePath);
            String key = destData.getPath() + S3Utils.SEPARATOR + srcFile.getName();
            putFile(s3, srcFile, bucket, key);

            DataTO retObj = null;
            if (destData.getObjectType() == DataObjectType.TEMPLATE) {
                TemplateObjectTO newTemplate = new TemplateObjectTO();
                newTemplate.setPath(key);
                newTemplate.setSize(srcFile.length());
                newTemplate.setFormat(format);
                retObj = newTemplate;
            } else if (destData.getObjectType() == DataObjectType.VOLUME) {
                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(key);
                newVol.setSize(srcFile.length());
                retObj = newVol;
            }

            return new CopyCmdAnswer(retObj);
        } catch (Exception e) {
            s_logger.error("failed to upload" + srcData.getPath(), e);
            return new CopyCmdAnswer("failed to upload" + srcData.getPath() + e.toString());
        }
    }

    String swiftDownload(SwiftTO swift, String container, String rfilename, String lFullPath) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl()
                + " -U " + swift.getAccount() + ":" + swift.getUserName() + " -K " + swift.getKey() + " download "
                + container + " " + rfilename + " -o " + lFullPath);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result != null) {
            String errMsg = "swiftDownload failed  err=" + result;
            s_logger.warn(errMsg);
            return errMsg;
        }
        if (parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                if (line.contains("Errno") || line.contains("failed")) {
                    String errMsg = "swiftDownload failed , err=" + lines.toString();
                    s_logger.warn(errMsg);
                    return errMsg;
                }
            }
        }
        return null;

    }

    String swiftDownloadContainer(SwiftTO swift, String container, String ldir) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("cd " + ldir + ";/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A "
                + swift.getUrl() + " -U " + swift.getAccount() + ":" + swift.getUserName() + " -K " + swift.getKey()
                + " download " + container);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result != null) {
            String errMsg = "swiftDownloadContainer failed  err=" + result;
            s_logger.warn(errMsg);
            return errMsg;
        }
        if (parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                if (line.contains("Errno") || line.contains("failed")) {
                    String errMsg = "swiftDownloadContainer failed , err=" + lines.toString();
                    s_logger.warn(errMsg);
                    return errMsg;
                }
            }
        }
        return null;

    }

    String swiftUpload(SwiftTO swift, String container, String lDir, String lFilename) {
        long SWIFT_MAX_SIZE = 5L * 1024L * 1024L * 1024L;
        List<String> files = new ArrayList<String>();
        if (lFilename.equals("*")) {
            File dir = new File(lDir);
            for (String file : dir.list()) {
                if (file.startsWith(".")) {
                    continue;
                }
                files.add(file);
            }
        } else {
            files.add(lFilename);
        }

        for (String file : files) {
            File f = new File(lDir + "/" + file);
            long size = f.length();
            Script command = new Script("/bin/bash", s_logger);
            command.add("-c");
            if (size <= SWIFT_MAX_SIZE) {
                command.add("cd " + lDir
                        + ";/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A "
                        + swift.getUrl() + " -U " + swift.getAccount() + ":" + swift.getUserName() + " -K "
                        + swift.getKey() + " upload " + container + " " + file);
            } else {
                command.add("cd " + lDir
                        + ";/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A "
                        + swift.getUrl() + " -U " + swift.getAccount() + ":" + swift.getUserName() + " -K "
                        + swift.getKey() + " upload -S " + SWIFT_MAX_SIZE + " " + container + " " + file);
            }
            OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            String result = command.execute(parser);
            if (result != null) {
                String errMsg = "swiftUpload failed , err=" + result;
                s_logger.warn(errMsg);
                return errMsg;
            }
            if (parser.getLines() != null) {
                String[] lines = parser.getLines().split("\\n");
                for (String line : lines) {
                    if (line.contains("Errno") || line.contains("failed")) {
                        String errMsg = "swiftUpload failed , err=" + lines.toString();
                        s_logger.warn(errMsg);
                        return errMsg;
                    }
                }
            }
        }

        return null;
    }

    String[] swiftList(SwiftTO swift, String container, String rFilename) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl()
                + " -U " + swift.getAccount() + ":" + swift.getUserName() + " -K " + swift.getKey() + " list "
                + container + " " + rFilename);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result == null && parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            return lines;
        } else {
            if (result != null) {
                String errMsg = "swiftList failed , err=" + result;
                s_logger.warn(errMsg);
            } else {
                String errMsg = "swiftList failed, no lines returns";
                s_logger.warn(errMsg);
            }
        }
        return null;
    }

    String swiftDelete(SwiftTO swift, String container, String object) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl()
                + " -U " + swift.getAccount() + ":" + swift.getUserName() + " -K " + swift.getKey() + " delete "
                + container + " " + object);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result != null) {
            String errMsg = "swiftDelete failed , err=" + result;
            s_logger.warn(errMsg);
            return errMsg;
        }
        if (parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                if (line.contains("Errno") || line.contains("failed")) {
                    String errMsg = "swiftDelete failed , err=" + lines.toString();
                    s_logger.warn(errMsg);
                    return errMsg;
                }
            }
        }
        return null;
    }

    public Answer execute(DeleteSnapshotsDirCommand cmd) {
        DataStoreTO dstore = cmd.getDataStore();
        if (dstore instanceof NfsTO) {
            NfsTO nfs = (NfsTO) dstore;
            String relativeSnapshotPath = cmd.getDirectory();
            String parent = getRootDir(nfs.getUrl());

            if (relativeSnapshotPath.startsWith(File.separator)) {
                relativeSnapshotPath = relativeSnapshotPath.substring(1);
            }

            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String absoluteSnapshotPath = parent + relativeSnapshotPath;
            File snapshotDir = new File(absoluteSnapshotPath);
            String details = null;
            if (!snapshotDir.exists()) {
                details = "snapshot directory " + snapshotDir.getName() + " doesn't exist";
                s_logger.debug(details);
                return new Answer(cmd, true, details);
            }
            // delete all files in the directory
            String lPath = absoluteSnapshotPath + "/*";
            String result = deleteLocalFile(lPath);
            if (result != null) {
                String errMsg = "failed to delete all snapshots " + lPath + " , err=" + result;
                s_logger.warn(errMsg);
                return new Answer(cmd, false, errMsg);
            }
            // delete the directory
            if (!snapshotDir.delete()) {
                details = "Unable to delete directory " + snapshotDir.getName() + " under snapshot path "
                        + relativeSnapshotPath;
                s_logger.debug(details);
                return new Answer(cmd, false, details);
            }
            return new Answer(cmd, true, null);
        } else if (dstore instanceof S3TO) {
            final S3TO s3 = (S3TO) dstore;
            final String path = cmd.getDirectory();
            final String bucket = s3.getBucketName();
            try {
                S3Utils.deleteDirectory(s3, bucket, path);
                return new Answer(cmd, true, String.format("Deleted snapshot %1%s from bucket %2$s.", path, bucket));
            } catch (Exception e) {
                final String errorMessage = String.format(
                        "Failed to delete snapshot %1$s from bucket %2$s due to the following error: %3$s", path,
                        bucket, e.getMessage());
                s_logger.error(errorMessage, e);
                return new Answer(cmd, false, errorMessage);
            }
        } else if (dstore instanceof SwiftTO) {
            String path = cmd.getDirectory();
            String volumeId = StringUtils.substringAfterLast(path, "/"); // assuming
                                                                         // that
                                                                         // the
                                                                         // filename
                                                                         // is
                                                                         // the
                                                                         // last
                                                                         // section
                                                                         // in
                                                                         // the
                                                                         // path
            String result = swiftDelete((SwiftTO) dstore, "V-" + volumeId.toString(), "");
            if (result != null) {
                String errMsg = "failed to delete snapshot for volume " + volumeId + " , err=" + result;
                s_logger.warn(errMsg);
                return new Answer(cmd, false, errMsg);
            }
            return new Answer(cmd, true, "Deleted snapshot " + path + " from swift");
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + dstore);
        }
    }

    public Answer execute(final DownloadSnapshotFromS3Command cmd) {

        final S3TO s3 = cmd.getS3();
        final String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        final Long accountId = cmd.getAccountId();
        final Long volumeId = cmd.getVolumeId();

        try {

            executeWithNoWaitLock(determineSnapshotLockId(accountId, volumeId), new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    final String directoryName = determineSnapshotLocalDirectory(secondaryStorageUrl, accountId,
                            volumeId);

                    String result = createLocalDir(directoryName);
                    if (result != null) {
                        throw new InternalErrorException(format(
                                "Failed to create directory %1$s during S3 snapshot download.", directoryName));
                    }

                    final String snapshotFileName = determineSnapshotBackupFilename(cmd.getSnapshotUuid());
                    final String key = determineSnapshotS3Key(accountId, volumeId, snapshotFileName);
                    final File targetFile = S3Utils.getFile(s3, s3.getBucketName(), key,
                            _storage.getFile(directoryName), new FileNamingStrategy() {

                                @Override
                                public String determineFileName(String key) {
                                    return snapshotFileName;
                                }

                            });

                    if (cmd.getParent() != null) {

                        final String parentPath = join(File.pathSeparator, directoryName,
                                determineSnapshotBackupFilename(cmd.getParent()));
                        result = setVhdParent(targetFile.getAbsolutePath(), parentPath);
                        if (result != null) {
                            throw new InternalErrorException(format(
                                    "Failed to set the parent for backup %1$s to %2$s due to %3$s.",
                                    targetFile.getAbsolutePath(), parentPath, result));
                        }

                    }

                    return null;

                }

            });

            return new Answer(cmd, true, format(
                    "Succesfully retrieved volume id %1$s for account id %2$s to %3$s from S3.", volumeId, accountId,
                    secondaryStorageUrl));

        } catch (Exception e) {
            final String errMsg = format(
                    "Failed to retrieve volume id %1$s for account id %2$s to %3$s from S3 due to exception %4$s",
                    volumeId, accountId, secondaryStorageUrl, e.getMessage());
            s_logger.error(errMsg);
            return new Answer(cmd, false, errMsg);
        }

    }

    private String determineSnapshotS3Directory(final Long accountId, final Long volumeId) {
        return join(S3Utils.SEPARATOR, SNAPSHOT_ROOT_DIR, accountId, volumeId);
    }

    private String determineSnapshotS3Key(final Long accountId, final Long volumeId, final String snapshotFileName) {

        final String directoryName = determineSnapshotS3Directory(accountId, volumeId);
        return join(S3Utils.SEPARATOR, directoryName, snapshotFileName);

    }

    private String determineSnapshotLocalDirectory(final String secondaryStorageUrl, final Long accountId,
            final Long volumeId) {
        return join(File.pathSeparator, getRootDir(secondaryStorageUrl), SNAPSHOT_ROOT_DIR, accountId, volumeId);
    }

    public Answer execute(DownloadSnapshotFromSwiftCommand cmd) {
        SwiftTO swift = cmd.getSwift();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String rFilename = cmd.getSnapshotUuid();
        String sParent = cmd.getParent();
        String errMsg = "";
        try {
            String parent = getRootDir(secondaryStorageUrl);
            String lPath = parent + "/snapshots/" + String.valueOf(accountId) + "/" + String.valueOf(volumeId);

            String result = createLocalDir(lPath);
            if (result != null) {
                errMsg = "downloadSnapshotFromSwiftCommand failed due to Create local path failed";
                s_logger.warn(errMsg);
                throw new InternalErrorException(errMsg);
            }
            String lFilename = rFilename;
            if (rFilename.startsWith("VHD-")) {
                lFilename = rFilename.replace("VHD-", "") + ".vhd";
            }
            String lFullPath = lPath + "/" + lFilename;
            result = swiftDownload(swift, "S-" + volumeId.toString(), rFilename, lFullPath);
            if (result != null) {
                return new Answer(cmd, false, result);
            }
            if (sParent != null) {
                if (sParent.startsWith("VHD-") || sParent.endsWith(".vhd")) {
                    String pFilename = sParent;
                    if (sParent.startsWith("VHD-")) {
                        pFilename = pFilename.replace("VHD-", "") + ".vhd";
                    }
                    String pFullPath = lPath + "/" + pFilename;
                    result = setVhdParent(lFullPath, pFullPath);
                    if (result != null) {
                        return new Answer(cmd, false, result);
                    }
                }
            }

            return new Answer(cmd, true, "success");
        } catch (Exception e) {
            String msg = cmd + " Command failed due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    private Answer execute(ComputeChecksumCommand cmd) {

        String relativeTemplatePath = cmd.getTemplatePath();
        DataStoreTO store = cmd.getStore();
        if (!(store instanceof NfsTO)) {
            return new Answer(cmd, false, "can't handle non nfs data store");
        }
        NfsTO nfsStore = (NfsTO) store;
        String parent = getRootDir(nfsStore.getUrl());

        if (relativeTemplatePath.startsWith(File.separator)) {
            relativeTemplatePath = relativeTemplatePath.substring(1);
        }

        if (!parent.endsWith(File.separator)) {
            parent += File.separator;
        }
        String absoluteTemplatePath = parent + relativeTemplatePath;
        MessageDigest digest;
        String checksum = null;
        File f = new File(absoluteTemplatePath);
        InputStream is = null;
        byte[] buffer = new byte[8192];
        int read = 0;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("parent path " + parent + " relative template path " + relativeTemplatePath);
        }

        try {
            digest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(f);
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            checksum = bigInt.toString(16);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Successfully calculated checksum for file " + absoluteTemplatePath + " - " + checksum);
            }

        } catch (IOException e) {
            String logMsg = "Unable to process file for MD5 - " + absoluteTemplatePath;
            s_logger.error(logMsg);
            return new Answer(cmd, false, checksum);
        } catch (NoSuchAlgorithmException e) {
            return new Answer(cmd, false, checksum);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Could not close the file " + absoluteTemplatePath);
                }
                return new Answer(cmd, false, checksum);
            }
        }

        return new Answer(cmd, true, checksum);
    }

    private void configCerts(Certificates certs) {
        if (certs == null) {
            configureSSL();
        } else {
            String prvKey = certs.getPrivKey();
            String pubCert = certs.getPrivCert();
            String certChain = certs.getCertChain();

            try {
                File prvKeyFile = File.createTempFile("prvkey", null);
                String prvkeyPath = prvKeyFile.getAbsolutePath();
                BufferedWriter out = new BufferedWriter(new FileWriter(prvKeyFile));
                out.write(prvKey);
                out.close();

                File pubCertFile = File.createTempFile("pubcert", null);
                String pubCertFilePath = pubCertFile.getAbsolutePath();

                out = new BufferedWriter(new FileWriter(pubCertFile));
                out.write(pubCert);
                out.close();

                configureSSL(prvkeyPath, pubCertFilePath, null);

                prvKeyFile.delete();
                pubCertFile.delete();

            } catch (IOException e) {
                s_logger.debug("Failed to config ssl: " + e.toString());
            }
        }
    }

    private Answer execute(SecStorageSetupCommand cmd) {
        if (!_inSystemVM) {
            return new Answer(cmd, true, null);
        }
        DataStoreTO dStore = cmd.getDataStore();
        if (dStore instanceof NfsTO) {
            String secUrl = cmd.getSecUrl();
            try {
                URI uri = new URI(secUrl);
                String nfsHost = uri.getHost();

                InetAddress nfsHostAddr = InetAddress.getByName(nfsHost);
                String nfsHostIp = nfsHostAddr.getHostAddress();

                addRouteToInternalIpOrCidr(_storageGateway, _storageIp, _storageNetmask, nfsHostIp);
                String nfsPath = nfsHostIp + ":" + uri.getPath();
                String dir = UUID.nameUUIDFromBytes(nfsPath.getBytes()).toString();
                String root = _parent + "/" + dir;
                mount(root, nfsPath);

                configCerts(cmd.getCerts());

                nfsIps.add(nfsHostIp);
                return new SecStorageSetupAnswer(dir);
            } catch (Exception e) {
                String msg = "GetRootDir for " + secUrl + " failed due to " + e.toString();
                s_logger.error(msg);
                return new Answer(cmd, false, msg);

            }
        } else {
            // TODO: what do we need to setup for S3/Swift, maybe need to mount
            // to some cache storage
            return new Answer(cmd, true, null);
        }
    }

    private String deleteSnapshotBackupFromLocalFileSystem(final String secondaryStorageUrl, final Long accountId,
            final Long volumeId, final String name, final Boolean deleteAllFlag) {
        String lPath = null;
        int index = name.lastIndexOf(File.separator);
        String snapshotPath = name.substring(0, index);
        if (deleteAllFlag) {
            lPath = this.getRootDir(secondaryStorageUrl) + File.separator + snapshotPath + File.separator + "*";
        } else {
            lPath = this.getRootDir(secondaryStorageUrl) + File.separator + name + "*";
        }

        final String result = deleteLocalFile(lPath);

        if (result != null) {
            return "failed to delete snapshot " + lPath + " , err=" + result;
        }

        return null;

    }

    private String deleteSnapshotBackupfromS3(final S3TO s3, final Long accountId, final Long volumeId,
            final String name, final Boolean deleteAllFlag) {

        try {

            final String bucket = s3.getBucketName();

            final String result = executeWithNoWaitLock(determineSnapshotLockId(accountId, volumeId),
                    new Callable<String>() {

                        @Override
                        public String call() throws Exception {

                            if (deleteAllFlag) {
                                S3Utils.deleteDirectory(s3, bucket, determineSnapshotS3Directory(accountId, volumeId));
                            } else {
                                S3Utils.deleteObject(
                                        s3,
                                        bucket,
                                        determineSnapshotS3Key(accountId, volumeId,
                                                determineSnapshotBackupFilename(name)));
                            }

                            return null;

                        }

                    });

            return result;

        } catch (Exception e) {

            s_logger.error(String.format("Failed to delete snapshot backup for account id %1$s volume id %2$sfrom S3.",
                    accountId, volumeId), e);
            return e.getMessage();

        }

    }

    private String determineSnapshotBackupFilename(final String snapshotUuid) {
        return snapshotUuid + ".vhd";
    }

    private String determineSnapshotLockId(final Long accountId, final Long volumeId) {
        return join("_", "SNAPSHOT", accountId, volumeId);
    }

    protected Answer deleteSnapshot(final DeleteCommand cmd) {
        DataTO obj = cmd.getData();
        DataStoreTO dstore = obj.getDataStore();
        if (dstore instanceof NfsTO) {
            NfsTO nfs = (NfsTO) dstore;
            String snapshotPath = obj.getPath();
            if (snapshotPath.startsWith(File.separator)) {
                snapshotPath = snapshotPath.substring(1);
            }
            int index = snapshotPath.lastIndexOf("/");
            String snapshotName = snapshotPath.substring(index + 1);
            snapshotPath = snapshotPath.substring(0, index);

            String parent = getRootDir(nfs.getUrl());
            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String absoluteSnapshotPath = parent + snapshotPath;
            // check if directory exists
            File snapshotDir = new File(absoluteSnapshotPath);
            String details = null;
            if (!snapshotDir.exists()) {
                details = "snapshot directory " + snapshotDir.getName() + " doesn't exist";
                s_logger.debug(details);
                return new Answer(cmd, false, details);
            }
            // delete snapshot in the directory if exists
            String lPath = absoluteSnapshotPath + "/*" + snapshotName + "*";
            String result = deleteLocalFile(lPath);
            if (result != null) {
                details = "failed to delete snapshot " + lPath + " , err=" + result;
                s_logger.warn(details);
                return new Answer(cmd, false, details);
            }
            return new Answer(cmd, true, null);
        } else if (dstore instanceof S3TO) {
            final S3TO s3 = (S3TO) dstore;
            final String path = obj.getPath();
            final String bucket = s3.getBucketName();
            try {
                S3Utils.deleteObject(s3, bucket, path);
                return new Answer(cmd, true, String.format("Deleted snapshot %1%s from bucket %2$s.", path, bucket));
            } catch (Exception e) {
                final String errorMessage = String.format(
                        "Failed to delete snapshot %1$s from bucket %2$s due to the following error: %3$s", path,
                        bucket, e.getMessage());
                s_logger.error(errorMessage, e);
                return new Answer(cmd, false, errorMessage);
            }
        } else if (dstore instanceof SwiftTO) {
            String path = obj.getPath();
            String filename = StringUtils.substringAfterLast(path, "/"); // assuming
                                                                         // that
                                                                         // the
                                                                         // filename
                                                                         // is
                                                                         // the
                                                                         // last
                                                                         // section
                                                                         // in
                                                                         // the
                                                                         // path
            String volumeId = StringUtils.substringAfterLast(StringUtils.substringBeforeLast(path, "/"), "/");
            String result = swiftDelete((SwiftTO) dstore, "V-" + volumeId, filename);
            if (result != null) {
                String errMsg = "failed to delete snapshot " + filename + " , err=" + result;
                s_logger.warn(errMsg);
                return new Answer(cmd, false, errMsg);
            }
            return new Answer(cmd, true, "Deleted snapshot " + path + " from swift");
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + dstore);
        }

    }

    Map<String, TemplateProp> swiftListTemplate(SwiftTO swift) {
        String[] containers = swiftList(swift, "", "");
        if (containers == null) {
            return null;
        }
        Map<String, TemplateProp> tmpltInfos = new HashMap<String, TemplateProp>();
        for (String container : containers) {
            if (container.startsWith("T-")) {
                String ldir = _tmpltDir + "/" + UUID.randomUUID().toString();
                createLocalDir(ldir);
                String lFullPath = ldir + "/" + _tmpltpp;
                swiftDownload(swift, container, _tmpltpp, lFullPath);
                TemplateLocation loc = new TemplateLocation(_storage, ldir);
                try {
                    if (!loc.load()) {
                        s_logger.warn("Can not parse template.properties file for template " + container);
                        continue;
                    }
                } catch (IOException e) {
                    s_logger.warn("Unable to load template location " + ldir + " due to " + e.toString(), e);
                    continue;
                }
                TemplateProp tInfo = loc.getTemplateInfo();
                tInfo.setInstallPath(container);
                tmpltInfos.put(tInfo.getTemplateName(), tInfo);
                loc.purge();
                deleteLocalDir(ldir);
            }
        }
        return tmpltInfos;

    }

    Map<String, TemplateProp> s3ListTemplate(S3TO s3) {
        String bucket = s3.getBucketName();
        // List the objects in the source directory on S3
        final List<S3ObjectSummary> objectSummaries = S3Utils.getDirectory(s3, bucket, this.TEMPLATE_ROOT_DIR);
        if (objectSummaries == null)
            return null;
        Map<String, TemplateProp> tmpltInfos = new HashMap<String, TemplateProp>();
        for (S3ObjectSummary objectSummary : objectSummaries) {
            String key = objectSummary.getKey();
            // String installPath = StringUtils.substringBeforeLast(key,
            // S3Utils.SEPARATOR);
            String uniqueName = this.determineS3TemplateNameFromKey(key);
            // TODO: isPublic value, where to get?
            TemplateProp tInfo = new TemplateProp(uniqueName, key, objectSummary.getSize(), objectSummary.getSize(),
                    true, false);
            tmpltInfos.put(uniqueName, tInfo);
        }
        return tmpltInfos;

    }

    Map<Long, TemplateProp> s3ListVolume(S3TO s3) {
        String bucket = s3.getBucketName();
        // List the objects in the source directory on S3
        final List<S3ObjectSummary> objectSummaries = S3Utils.getDirectory(s3, bucket, this.VOLUME_ROOT_DIR);
        if (objectSummaries == null)
            return null;
        Map<Long, TemplateProp> tmpltInfos = new HashMap<Long, TemplateProp>();
        for (S3ObjectSummary objectSummary : objectSummaries) {
            String key = objectSummary.getKey();
            // String installPath = StringUtils.substringBeforeLast(key,
            // S3Utils.SEPARATOR);
            Long id = this.determineS3VolumeIdFromKey(key);
            // TODO: how to get volume template name
            TemplateProp tInfo = new TemplateProp(id.toString(), key, objectSummary.getSize(), objectSummary.getSize(),
                    true, false);
            tmpltInfos.put(id, tInfo);
        }
        return tmpltInfos;

    }

    private Answer execute(ListTemplateCommand cmd) {
        if (!_inSystemVM) {
            return new Answer(cmd, true, null);
        }

        DataStoreTO store = cmd.getDataStore();
        if (store instanceof NfsTO) {
            NfsTO nfs = (NfsTO) store;
            String secUrl = nfs.getUrl();
            String root = getRootDir(secUrl);
            Map<String, TemplateProp> templateInfos = _dlMgr.gatherTemplateInfo(root);
            return new ListTemplateAnswer(secUrl, templateInfos);
        } else if (store instanceof SwiftTO) {
            SwiftTO swift = (SwiftTO) store;
            Map<String, TemplateProp> templateInfos = swiftListTemplate(swift);
            return new ListTemplateAnswer(swift.toString(), templateInfos);
        } else if (store instanceof S3TO) {
            S3TO s3 = (S3TO) store;
            Map<String, TemplateProp> templateInfos = s3ListTemplate(s3);
            return new ListTemplateAnswer(s3.getBucketName(), templateInfos);
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + store);
        }
    }

    private Answer execute(ListVolumeCommand cmd) {
        if (!_inSystemVM) {
            return new Answer(cmd, true, null);
        }
        DataStoreTO store = cmd.getDataStore();
        if (store instanceof NfsTO) {
            NfsTO nfs = (NfsTO) store;
            String root = getRootDir(cmd.getSecUrl());
            Map<Long, TemplateProp> templateInfos = _dlMgr.gatherVolumeInfo(root);
            return new ListVolumeAnswer(cmd.getSecUrl(), templateInfos);
        } else if (store instanceof S3TO) {
            S3TO s3 = (S3TO) store;
            Map<Long, TemplateProp> templateInfos = s3ListVolume(s3);
            return new ListVolumeAnswer(s3.getBucketName(), templateInfos);
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + store);
        }

    }

    private Answer execute(SecStorageVMSetupCommand cmd) {
        if (!_inSystemVM) {
            return new Answer(cmd, true, null);
        }
        boolean success = true;
        StringBuilder result = new StringBuilder();
        for (String cidr : cmd.getAllowedInternalSites()) {
            if (nfsIps.contains(cidr)) {
                /*
                 * if the internal download ip is the same with secondary
                 * storage ip, adding internal sites will flush ip route to nfs
                 * through storage ip.
                 */
                continue;
            }
            String tmpresult = allowOutgoingOnPrivate(cidr);
            if (tmpresult != null) {
                result.append(", ").append(tmpresult);
                success = false;
            }
        }
        if (success) {
            if (cmd.getCopyPassword() != null && cmd.getCopyUserName() != null) {
                String tmpresult = configureAuth(cmd.getCopyUserName(), cmd.getCopyPassword());
                if (tmpresult != null) {
                    result.append("Failed to configure auth for copy ").append(tmpresult);
                    success = false;
                }
            }
        }
        return new Answer(cmd, success, result.toString());

    }

    private String setVhdParent(String lFullPath, String pFullPath) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("/bin/vhd-util modify -n " + lFullPath + " -p " + pFullPath);
        String result = command.execute();
        if (result != null) {
            String errMsg = "failed to set vhd parent, child " + lFullPath + " parent " + pFullPath + ", err=" + result;
            s_logger.warn(errMsg);
            return errMsg;
        }
        return null;
    }

    private String createLocalDir(String folder) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("mkdir -p " + folder);
        String result = command.execute();
        if (result != null) {
            String errMsg = "Create local path " + folder + " failed , err=" + result;
            s_logger.warn(errMsg);
            return errMsg;
        }
        return null;
    }

    private String deleteLocalDir(String folder) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("rmdir " + folder);
        String result = command.execute();
        if (result != null) {
            String errMsg = "Delete local path " + folder + " failed , err=" + result;
            s_logger.warn(errMsg);
            return errMsg;
        }
        return null;
    }

    private String deleteLocalFile(String fullPath) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("rm -f " + fullPath);
        String result = command.execute();
        if (result != null) {
            String errMsg = "Failed to delete file " + fullPath + ", err=" + result;
            s_logger.warn(errMsg);
            return errMsg;
        }
        return null;
    }

    public String allowOutgoingOnPrivate(String destCidr) {
        if (!_inSystemVM) {
            return null;
        }
        Script command = new Script("/bin/bash", s_logger);
        String intf = "eth1";
        command.add("-c");
        command.add("iptables -I OUTPUT -o " + intf + " -d " + destCidr
                + " -p tcp -m state --state NEW -m tcp  -j ACCEPT");

        String result = command.execute();
        if (result != null) {
            s_logger.warn("Error in allowing outgoing to " + destCidr + ", err=" + result);
            return "Error in allowing outgoing to " + destCidr + ", err=" + result;
        }

        addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, destCidr);

        return null;
    }

    private Answer execute(SecStorageFirewallCfgCommand cmd) {
        if (!_inSystemVM) {
            return new Answer(cmd, true, null);
        }

        List<String> ipList = new ArrayList<String>();

        for (PortConfig pCfg : cmd.getPortConfigs()) {
            if (pCfg.isAdd()) {
                ipList.add(pCfg.getSourceIp());
            }
        }
        boolean success = true;
        String result;
        result = configureIpFirewall(ipList, cmd.getIsAppendAIp());
        if (result != null)
            success = false;

        return new Answer(cmd, success, result);
    }

    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        String rootDir = getRootDir(cmd.getSecUrl());
        final long usedSize = getUsedSize(rootDir);
        final long totalSize = getTotalSize(rootDir);
        if (usedSize == -1 || totalSize == -1) {
            return new GetStorageStatsAnswer(cmd, "Unable to get storage stats");
        } else {
            return new GetStorageStatsAnswer(cmd, totalSize, usedSize);
        }
    }

    protected Answer execute(final DeleteCommand cmd) {
        DataTO obj = cmd.getData();
        DataObjectType objType = obj.getObjectType();
        switch (objType) {
        case TEMPLATE:
            return deleteTemplate(cmd);
        case VOLUME:
            return deleteVolume(cmd);
        case SNAPSHOT:
            return deleteSnapshot(cmd);
        }
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    protected Answer deleteTemplate(DeleteCommand cmd) {
        DataTO obj = cmd.getData();
        DataStoreTO dstore = obj.getDataStore();
        if (dstore instanceof NfsTO) {
            NfsTO nfs = (NfsTO) dstore;
            String relativeTemplatePath = obj.getPath();
            String parent = getRootDir(nfs.getUrl());

            if (relativeTemplatePath.startsWith(File.separator)) {
                relativeTemplatePath = relativeTemplatePath.substring(1);
            }

            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String absoluteTemplatePath = parent + relativeTemplatePath;
            File tmpltParent = new File(absoluteTemplatePath).getParentFile();
            String details = null;
            if (!tmpltParent.exists()) {
                details = "template parent directory " + tmpltParent.getName() + " doesn't exist";
                s_logger.debug(details);
                return new Answer(cmd, true, details);
            }
            File[] tmpltFiles = tmpltParent.listFiles();
            if (tmpltFiles == null || tmpltFiles.length == 0) {
                details = "No files under template parent directory " + tmpltParent.getName();
                s_logger.debug(details);
            } else {
                boolean found = false;
                for (File f : tmpltFiles) {
                    if (!found && f.getName().equals("template.properties")) {
                        found = true;
                    }

                    // KVM HA monitor makes a mess in the templates with its
                    // heartbeat tests
                    // Don't let this stop us from cleaning up the template
                    if (f.isDirectory() && f.getName().equals("KVMHA")) {
                        s_logger.debug("Deleting KVMHA directory contents from template location");
                        File[] haFiles = f.listFiles();
                        for (File haFile : haFiles) {
                            haFile.delete();
                        }
                    }

                    if (!f.delete()) {
                        return new Answer(cmd, false, "Unable to delete file " + f.getName() + " under Template path "
                                + relativeTemplatePath);
                    }
                }

                if (!found) {
                    details = "Can not find template.properties under " + tmpltParent.getName();
                    s_logger.debug(details);
                }
            }
            if (!tmpltParent.delete()) {
                details = "Unable to delete directory " + tmpltParent.getName() + " under Template path "
                        + relativeTemplatePath;
                s_logger.debug(details);
                return new Answer(cmd, false, details);
            }
            return new Answer(cmd, true, null);
        } else if (dstore instanceof S3TO) {
            final S3TO s3 = (S3TO) dstore;
            final String path = obj.getPath();
            final String bucket = s3.getBucketName();
            try {
                S3Utils.deleteDirectory(s3, bucket, path);
                return new Answer(cmd, true, String.format("Deleted template %1$s from bucket %2$s.", path, bucket));
            } catch (Exception e) {
                final String errorMessage = String.format(
                        "Failed to delete template %1$s from bucket %2$s due to the following error: %3$s", path,
                        bucket, e.getMessage());
                s_logger.error(errorMessage, e);
                return new Answer(cmd, false, errorMessage);
            }
        } else if (dstore instanceof SwiftTO) {
            SwiftTO swift = (SwiftTO) dstore;
            String container = "T-" + obj.getId();
            String object = "";

            try {
                String result = swiftDelete(swift, container, object);
                if (result != null) {
                    String errMsg = "failed to delete object " + container + "/" + object + " , err=" + result;
                    s_logger.warn(errMsg);
                    return new Answer(cmd, false, errMsg);
                }
                return new Answer(cmd, true, "success");
            } catch (Exception e) {
                String errMsg = cmd + " Command failed due to " + e.toString();
                s_logger.warn(errMsg, e);
                return new Answer(cmd, false, errMsg);
            }
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + dstore);
        }
    }

    protected Answer deleteVolume(final DeleteCommand cmd) {
        DataTO obj = cmd.getData();
        DataStoreTO dstore = obj.getDataStore();
        if (dstore instanceof NfsTO) {
            NfsTO nfs = (NfsTO) dstore;
            String relativeVolumePath = obj.getPath();
            String parent = getRootDir(nfs.getUrl());

            if (relativeVolumePath.startsWith(File.separator)) {
                relativeVolumePath = relativeVolumePath.substring(1);
            }

            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String absoluteVolumePath = parent + relativeVolumePath;
            File tmpltParent = new File(absoluteVolumePath).getParentFile();
            String details = null;
            if (!tmpltParent.exists()) {
                details = "volume parent directory " + tmpltParent.getName() + " doesn't exist";
                s_logger.debug(details);
                return new Answer(cmd, true, details);
            }
            File[] tmpltFiles = tmpltParent.listFiles();
            if (tmpltFiles == null || tmpltFiles.length == 0) {
                details = "No files under volume parent directory " + tmpltParent.getName();
                s_logger.debug(details);
            } else {
                boolean found = false;
                for (File f : tmpltFiles) {
                    if (!found && f.getName().equals("volume.properties")) {
                        found = true;
                    }

                    // KVM HA monitor makes a mess in the templates with its
                    // heartbeat tests
                    // Don't let this stop us from cleaning up the template
                    if (f.isDirectory() && f.getName().equals("KVMHA")) {
                        s_logger.debug("Deleting KVMHA directory contents from template location");
                        File[] haFiles = f.listFiles();
                        for (File haFile : haFiles) {
                            haFile.delete();
                        }
                    }

                    if (!f.delete()) {
                        return new Answer(cmd, false, "Unable to delete file " + f.getName() + " under Volume path "
                                + relativeVolumePath);
                    }
                }
                if (!found) {
                    details = "Can not find volume.properties under " + tmpltParent.getName();
                    s_logger.debug(details);
                }
            }
            if (!tmpltParent.delete()) {
                details = "Unable to delete directory " + tmpltParent.getName() + " under Volume path "
                        + relativeVolumePath;
                s_logger.debug(details);
                return new Answer(cmd, false, details);
            }
            return new Answer(cmd, true, null);
        } else if (dstore instanceof S3TO) {
            final S3TO s3 = (S3TO) dstore;
            final String path = obj.getPath();
            final String bucket = s3.getBucketName();
            try {
                S3Utils.deleteDirectory(s3, bucket, path);
                return new Answer(cmd, true, String.format("Deleted volume %1%s from bucket %2$s.", path, bucket));
            } catch (Exception e) {
                final String errorMessage = String.format(
                        "Failed to delete volume %1$s from bucket %2$s due to the following error: %3$s", path, bucket,
                        e.getMessage());
                s_logger.error(errorMessage, e);
                return new Answer(cmd, false, errorMessage);
            }
        } else if (dstore instanceof SwiftTO) {
            Long volumeId = obj.getId();
            String path = obj.getPath();
            String filename = StringUtils.substringAfterLast(path, "/"); // assuming
                                                                         // that
                                                                         // the
                                                                         // filename
                                                                         // is
                                                                         // the
                                                                         // last
                                                                         // section
                                                                         // in
                                                                         // the
                                                                         // path
            String result = swiftDelete((SwiftTO) dstore, "V-" + volumeId.toString(), filename);
            if (result != null) {
                String errMsg = "failed to delete volume " + filename + " , err=" + result;
                s_logger.warn(errMsg);
                return new Answer(cmd, false, errMsg);
            }
            return new Answer(cmd, true, "Deleted volume " + path + " from swift");
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + dstore);
        }

    }

    @Override
    synchronized public String getRootDir(String secUrl) {
        if (!_inSystemVM) {
            return _parent;
        }
        try {
            URI uri = new URI(secUrl);
            String nfsHost = uri.getHost();

            InetAddress nfsHostAddr = InetAddress.getByName(nfsHost);
            String nfsHostIp = nfsHostAddr.getHostAddress();
            String nfsPath = nfsHostIp + ":" + uri.getPath();
            String dir = UUID.nameUUIDFromBytes(nfsPath.getBytes()).toString();
            String root = _parent + "/" + dir;
            mount(root, nfsPath);
            return root;
        } catch (Exception e) {
            String msg = "GetRootDir for " + secUrl + " failed due to " + e.toString();
            s_logger.error(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    protected long getUsedSize(String rootDir) {
        return _storage.getUsedSpace(rootDir);
    }

    protected long getTotalSize(String rootDir) {
        return _storage.getTotalSpace(rootDir);
    }

    protected long convertFilesystemSize(final String size) {
        if (size == null || size.isEmpty()) {
            return -1;
        }

        long multiplier = 1;
        if (size.endsWith("T")) {
            multiplier = 1024l * 1024l * 1024l * 1024l;
        } else if (size.endsWith("G")) {
            multiplier = 1024l * 1024l * 1024l;
        } else if (size.endsWith("M")) {
            multiplier = 1024l * 1024l;
        } else {
            assert (false) : "Well, I have no idea what this is: " + size;
        }

        return (long) (Double.parseDouble(size.substring(0, size.length() - 1)) * multiplier);
    }

    @Override
    public Type getType() {
        if (SecondaryStorageVm.Role.templateProcessor.toString().equals(_role))
            return Host.Type.SecondaryStorage;

        return Host.Type.SecondaryStorageCmdExecutor;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingStorageCommand(Host.Type.Storage, id, new HashMap<String, Boolean>());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _eth1ip = (String) params.get("eth1ip");
        _eth1mask = (String) params.get("eth1mask");
        if (_eth1ip != null) { // can only happen inside service vm
            params.put("private.network.device", "eth1");
        } else {
            s_logger.warn("eth1ip parameter has not been configured, assuming that we are not inside a system vm");
        }
        String eth2ip = (String) params.get("eth2ip");
        if (eth2ip != null) {
            params.put("public.network.device", "eth2");
        }
        _publicIp = (String) params.get("eth2ip");
        _hostname = (String) params.get("name");

        String inSystemVM = (String) params.get("secondary.storage.vm");
        if (inSystemVM == null || "true".equalsIgnoreCase(inSystemVM)) {
            _inSystemVM = true;
        }

        _storageIp = (String) params.get("storageip");
        if (_storageIp == null && _inSystemVM) {
            s_logger.warn("There is no storageip in /proc/cmdline, something wrong!");
        }
        _storageNetmask = (String) params.get("storagenetmask");
        _storageGateway = (String) params.get("storagegateway");
        super.configure(name, params);

        _params = params;
        String value = (String) params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 1440) * 1000;

        _storage = (StorageLayer) params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            value = (String) params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                value = "com.cloud.storage.JavaStorageLayer";
            }

            try {
                Class<?> clazz = Class.forName(value);
                _storage = (StorageLayer) clazz.newInstance();
                _storage.configure("StorageLayer", params);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to find class " + value);
            } catch (InstantiationException e) {
                throw new ConfigurationException("Unable to find class " + value);
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Unable to find class " + value);
            }
        }

        if (_inSystemVM)
            _storage.mkdirs(_parent);

        _configSslScr = Script.findScript(getDefaultScriptsDir(), "config_ssl.sh");
        if (_configSslScr != null) {
            s_logger.info("config_ssl.sh found in " + _configSslScr);
        }

        _configAuthScr = Script.findScript(getDefaultScriptsDir(), "config_auth.sh");
        if (_configSslScr != null) {
            s_logger.info("config_auth.sh found in " + _configAuthScr);
        }

        _configIpFirewallScr = Script.findScript(getDefaultScriptsDir(), "ipfirewall.sh");
        if (_configIpFirewallScr != null) {
            s_logger.info("_configIpFirewallScr found in " + _configIpFirewallScr);
        }

        createTemplateFromSnapshotXenScript = Script.findScript(getDefaultScriptsDir(),
                "create_privatetemplate_from_snapshot_xen.sh");
        if (createTemplateFromSnapshotXenScript == null) {
            throw new ConfigurationException("create_privatetemplate_from_snapshot_xen.sh not found in "
                    + getDefaultScriptsDir());
        }

        _role = (String) params.get("role");
        if (_role == null)
            _role = SecondaryStorageVm.Role.templateProcessor.toString();
        s_logger.info("Secondary storage runs in role " + _role);

        _guid = (String) params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        _dc = (String) params.get("zone");
        if (_dc == null) {
            throw new ConfigurationException("Unable to find the zone");
        }
        _pod = (String) params.get("pod");

        _instance = (String) params.get("instance");

        if (!_inSystemVM) {
            _parent = (String) params.get("mount.path");
        }

        if (_inSystemVM) {
            _localgw = (String) params.get("localgw");
            if (_localgw != null) { // can only happen inside service vm
                String mgmtHost = (String) params.get("host");
                addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, mgmtHost);

                String internalDns1 = (String) params.get("internaldns1");
                if (internalDns1 == null) {
                    s_logger.warn("No DNS entry found during configuration of NfsSecondaryStorage");
                } else {
                    addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, internalDns1);
                }

                String internalDns2 = (String) params.get("internaldns2");
                if (internalDns2 != null) {
                    addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, internalDns2);
                }

            }

            startAdditionalServices();
            _params.put("install.numthreads", "50");
            _params.put("secondary.storage.vm", "true");
        }

        try {
            _params.put(StorageLayer.InstanceConfigKey, _storage);
            _dlMgr = new DownloadManagerImpl();
            _dlMgr.configure("DownloadManager", _params);
            _upldMgr = new UploadManagerImpl();
            _upldMgr.configure("UploadManager", params);
        } catch (ConfigurationException e) {
            s_logger.warn("Caught problem while configuring DownloadManager", e);
            return false;
        }
        return true;
    }

    private void startAdditionalServices() {
        if (!_inSystemVM) {
            return;
        }
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("if [ -f /etc/init.d/ssh ]; then service ssh restart; else service sshd restart; fi ");
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Error in starting sshd service err=" + result);
        }
        command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("iptables -I INPUT -i eth1 -p tcp -m state --state NEW -m tcp --dport 3922 -j ACCEPT");
        result = command.execute();
        if (result != null) {
            s_logger.warn("Error in opening up ssh port err=" + result);
        }
    }

    private void addRouteToInternalIpOrCidr(String localgw, String eth1ip, String eth1mask, String destIpOrCidr) {
        if (!_inSystemVM) {
            return;
        }
        s_logger.debug("addRouteToInternalIp: localgw=" + localgw + ", eth1ip=" + eth1ip + ", eth1mask=" + eth1mask
                + ",destIp=" + destIpOrCidr);
        if (destIpOrCidr == null) {
            s_logger.debug("addRouteToInternalIp: destIp is null");
            return;
        }
        if (!NetUtils.isValidIp(destIpOrCidr) && !NetUtils.isValidCIDR(destIpOrCidr)) {
            s_logger.warn(" destIp is not a valid ip address or cidr destIp=" + destIpOrCidr);
            return;
        }
        boolean inSameSubnet = false;
        if (NetUtils.isValidIp(destIpOrCidr)) {
            if (eth1ip != null && eth1mask != null) {
                inSameSubnet = NetUtils.sameSubnet(eth1ip, destIpOrCidr, eth1mask);
            } else {
                s_logger.warn("addRouteToInternalIp: unable to determine same subnet: _eth1ip=" + eth1ip + ", dest ip="
                        + destIpOrCidr + ", _eth1mask=" + eth1mask);
            }
        } else {
            inSameSubnet = NetUtils.isNetworkAWithinNetworkB(destIpOrCidr,
                    NetUtils.ipAndNetMaskToCidr(eth1ip, eth1mask));
        }
        if (inSameSubnet) {
            s_logger.debug("addRouteToInternalIp: dest ip " + destIpOrCidr + " is in the same subnet as eth1 ip "
                    + eth1ip);
            return;
        }
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("ip route delete " + destIpOrCidr);
        command.execute();
        command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("ip route add " + destIpOrCidr + " via " + localgw);
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Error in configuring route to internal ip err=" + result);
        } else {
            s_logger.debug("addRouteToInternalIp: added route to internal ip=" + destIpOrCidr + " via " + localgw);
        }
    }

    private void configureSSL() {
        if (!_inSystemVM) {
            return;
        }
        Script command = new Script(_configSslScr);
        command.add("-i", _publicIp);
        command.add("-h", _hostname);
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to configure httpd to use ssl");
        }
    }

    private void configureSSL(String prvkeyPath, String prvCertPath, String certChainPath) {
        if (!_inSystemVM) {
            return;
        }
        Script command = new Script(_configSslScr);
        command.add("-i", _publicIp);
        command.add("-h", _hostname);
        command.add("-k", prvkeyPath);
        command.add("-p", prvCertPath);
        if (certChainPath != null) {
            command.add("-t", certChainPath);
        }
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to configure httpd to use ssl");
        }
    }

    private String configureAuth(String user, String passwd) {
        Script command = new Script(_configAuthScr);
        command.add(user);
        command.add(passwd);
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to configure httpd to use auth");
        }
        return result;
    }

    private String configureIpFirewall(List<String> ipList, boolean isAppend) {
        Script command = new Script(_configIpFirewallScr);
        command.add(String.valueOf(isAppend));
        for (String ip : ipList) {
            command.add(ip);
        }

        String result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to configure firewall for command : " + command);
        }
        return result;
    }

    protected String mount(String root, String nfsPath) {
        File file = new File(root);
        if (!file.exists()) {
            if (_storage.mkdir(root)) {
                s_logger.debug("create mount point: " + root);
            } else {
                s_logger.debug("Unable to create mount point: " + root);
                return null;
            }
        }

        Script script = null;
        String result = null;
        script = new Script(!_inSystemVM, "mount", _timeout, s_logger);
        List<String> res = new ArrayList<String>();
        ZfsPathParser parser = new ZfsPathParser(root);
        script.execute(parser);
        res.addAll(parser.getPaths());
        for (String s : res) {
            if (s.contains(root)) {
                return root;
            }
        }

        Script command = new Script(!_inSystemVM, "mount", _timeout, s_logger);
        command.add("-t", "nfs");
        if (_inSystemVM) {
            // Fedora Core 12 errors out with any -o option executed from java
            command.add("-o", "soft,timeo=133,retrans=2147483647,tcp,acdirmax=0,acdirmin=0");
        }
        command.add(nfsPath);
        command.add(root);
        result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to mount " + nfsPath + " due to " + result);
            file = new File(root);
            if (file.exists())
                file.delete();
            return null;
        }

        // XXX: Adding the check for creation of snapshots dir here. Might have
        // to move it somewhere more logical later.
        if (!checkForSnapshotsDir(root)) {
            return null;
        }

        // Create the volumes dir
        if (!checkForVolumesDir(root)) {
            return null;
        }

        return root;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public StartupCommand[] initialize() {

        final StartupSecondaryStorageCommand cmd = new StartupSecondaryStorageCommand();
        fillNetworkInformation(cmd);
        if (_publicIp != null)
            cmd.setPublicIpAddress(_publicIp);

        if (_inSystemVM) {
            Script command = new Script("/bin/bash", s_logger);
            command.add("-c");
            command.add("ln -sf " + _parent + " /var/www/html/copy");
            String result = command.execute();
            if (result != null) {
                s_logger.warn("Error in linking  err=" + result);
                return null;
            }
        }
        return new StartupCommand[] { cmd };
    }

    protected boolean checkForSnapshotsDir(String mountPoint) {
        String snapshotsDirLocation = mountPoint + File.separator + "snapshots";
        return createDir("snapshots", snapshotsDirLocation, mountPoint);
    }

    protected boolean checkForVolumesDir(String mountPoint) {
        String volumesDirLocation = mountPoint + "/" + "volumes";
        return createDir("volumes", volumesDirLocation, mountPoint);
    }

    protected boolean createDir(String dirName, String dirLocation, String mountPoint) {
        boolean dirExists = false;

        File dir = new File(dirLocation);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                s_logger.debug(dirName + " already exists on secondary storage, and is mounted at " + mountPoint);
                dirExists = true;
            } else {
                if (dir.delete() && _storage.mkdir(dirLocation)) {
                    dirExists = true;
                }
            }
        } else if (_storage.mkdir(dirLocation)) {
            dirExists = true;
        }

        if (dirExists) {
            s_logger.info(dirName + " directory created/exists on Secondary Storage.");
        } else {
            s_logger.info(dirName + " directory does not exist on Secondary Storage.");
        }

        return dirExists;
    }

    @Override
    protected String getDefaultScriptsDir() {
        return "./scripts/storage/secondary";
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub

    }

    @Override
    public void fillNetworkInformation(final StartupCommand cmd) {
        final String dummyMac = "00:06:0A:0B:0C:0D";
        final String dummyNetmask = "255.255.255.0";
        if (!_inSystemVM) {
            cmd.setPrivateIpAddress(_eth1ip);
            cmd.setPrivateMacAddress(dummyMac);
            cmd.setPrivateNetmask(dummyNetmask);
            cmd.setPublicIpAddress(_publicIp);
            cmd.setPublicMacAddress(dummyMac);
            cmd.setPublicNetmask(dummyNetmask);
            cmd.setName(_hostname);
        } else {
            super.fillNetworkInformation(cmd);
        }
    }
}
