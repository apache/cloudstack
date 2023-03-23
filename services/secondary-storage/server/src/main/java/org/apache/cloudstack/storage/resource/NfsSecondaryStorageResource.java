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

import static com.cloud.network.NetworkModel.METATDATA_DIR;
import static com.cloud.network.NetworkModel.PASSWORD_DIR;
import static com.cloud.network.NetworkModel.PASSWORD_FILE;
import static com.cloud.network.NetworkModel.PUBLIC_KEYS_FILE;
import static com.cloud.network.NetworkModel.USERDATA_DIR;
import static com.cloud.network.NetworkModel.USERDATA_FILE;
import static com.cloud.utils.storage.S3.S3Utils.putFile;
import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.security.keystore.KeystoreManager;
import org.apache.cloudstack.storage.NfsMountManagerImpl.PathParser;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.command.MoveVolumeCommand;
import org.apache.cloudstack.storage.command.TemplateOrVolumePostUploadCommand;
import org.apache.cloudstack.storage.command.UploadStatusAnswer;
import org.apache.cloudstack.storage.command.UploadStatusAnswer.UploadStatus;
import org.apache.cloudstack.storage.command.UploadStatusCommand;
import org.apache.cloudstack.storage.configdrive.ConfigDrive;
import org.apache.cloudstack.storage.configdrive.ConfigDriveBuilder;
import org.apache.cloudstack.storage.template.DownloadManager;
import org.apache.cloudstack.storage.template.DownloadManagerImpl;
import org.apache.cloudstack.storage.template.UploadEntity;
import org.apache.cloudstack.storage.template.UploadManager;
import org.apache.cloudstack.storage.template.UploadManagerImpl;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.imagestore.ImageStoreUtil;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.HandleConfigDriveIsoAnswer;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingStorageCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand.PortConfig;
import com.cloud.agent.api.SecStorageSetupAnswer;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.SecStorageVMSetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.api.storage.CreateDatadiskTemplateAnswer;
import com.cloud.agent.api.storage.CreateDatadiskTemplateCommand;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.DeleteEntityDownloadURLCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.GetDatadisksAnswer;
import com.cloud.agent.api.storage.GetDatadisksCommand;
import com.cloud.agent.api.storage.ListTemplateAnswer;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.storage.ListVolumeAnswer;
import com.cloud.agent.api.storage.ListVolumeCommand;
import com.cloud.agent.api.storage.OVFHelper;
import com.cloud.agent.api.storage.UploadCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.configuration.Resource;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.element.NetworkElement;
import com.cloud.resource.ServerResourceBase;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.template.OVAProcessor;
import com.cloud.storage.template.Processor;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.storage.template.QCOW2Processor;
import com.cloud.storage.template.RawImageProcessor;
import com.cloud.storage.template.TARProcessor;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.storage.template.TemplateLocation;
import com.cloud.storage.template.TemplateProp;
import com.cloud.storage.template.VhdProcessor;
import com.cloud.storage.template.VmdkProcessor;
import com.cloud.utils.EncryptionUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.SwiftUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.utils.storage.S3.S3Utils;
import com.cloud.vm.SecondaryStorageVm;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NfsSecondaryStorageResource extends ServerResourceBase implements SecondaryStorageResource {

    public static final Logger s_logger = Logger.getLogger(NfsSecondaryStorageResource.class);

    private static final String TEMPLATE_ROOT_DIR = "template/tmpl";
    private static final String VOLUME_ROOT_DIR = "volumes";
    private static final String POST_UPLOAD_KEY_LOCATION = "/etc/cloudstack/agent/ms-psk";
    private static final String ORIGINAL_FILE_EXTENSION = ".orig";

    private static final Map<String, String> updatableConfigData = Maps.newHashMap();
    static {

        updatableConfigData.put(PUBLIC_KEYS_FILE, METATDATA_DIR);
        updatableConfigData.put(USERDATA_FILE, USERDATA_DIR);
        updatableConfigData.put(PASSWORD_FILE, PASSWORD_DIR);
    }

    int _timeout;

    public int getTimeout() {
        return _timeout;
    }

    public void setTimeout(int timeout) {
        _timeout = timeout;
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
    private String _nfsVersion;
    private final List<String> nfsIps = new ArrayList<String>();
    protected String _parent = "/mnt/SecStorage";
    final private String _tmpltpp = "template.properties";
    protected String createTemplateFromSnapshotXenScript;
    private HashMap<String, UploadEntity> uploadEntityStateMap = new HashMap<String, UploadEntity>();
    private String _ssvmPSK = null;
    private long processTimeout;

    public void setParentPath(String path) {
        _parent = path;
    }

    public String getMountingRoot() {
        return _parent;
    }

    @Override
    public void disconnected() {
    }

    public void setInSystemVM(boolean inSystemVM) {
        _inSystemVM = inSystemVM;
    }

    /**
     * Retrieve converted "nfsVersion" value from params
     * @param params
     * @return nfsVersion value if exists, null in other case
     */
    public static String retrieveNfsVersionFromParams(Map<String, Object> params) {
        return (String)params.get("nfsVersion");
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadProgressCommand) {
            return _dlMgr.handleDownloadCommand(this, (DownloadProgressCommand)cmd);
        } else if (cmd instanceof DownloadCommand) {
            return execute((DownloadCommand)cmd);
        } else if (cmd instanceof UploadCommand) {
            return _upldMgr.handleUploadCommand(this, (UploadCommand)cmd);
        } else if (cmd instanceof CreateEntityDownloadURLCommand) {
            return _upldMgr.handleCreateEntityURLCommand((CreateEntityDownloadURLCommand)cmd);
        } else if (cmd instanceof DeleteEntityDownloadURLCommand) {
            return _upldMgr.handleDeleteEntityDownloadURLCommand((DeleteEntityDownloadURLCommand)cmd);
        } else if (cmd instanceof GetStorageStatsCommand) {
            return execute((GetStorageStatsCommand)cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return new CheckHealthAnswer((CheckHealthCommand)cmd, true);
        } else if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand)cmd);
        } else if (cmd instanceof SecStorageFirewallCfgCommand) {
            return execute((SecStorageFirewallCfgCommand)cmd);
        } else if (cmd instanceof SecStorageVMSetupCommand) {
            return execute((SecStorageVMSetupCommand)cmd);
        } else if (cmd instanceof SecStorageSetupCommand) {
            return execute((SecStorageSetupCommand)cmd);
        } else if (cmd instanceof ComputeChecksumCommand) {
            return execute((ComputeChecksumCommand)cmd);
        } else if (cmd instanceof ListTemplateCommand) {
            return execute((ListTemplateCommand)cmd);
        } else if (cmd instanceof ListVolumeCommand) {
            return execute((ListVolumeCommand)cmd);
        } else if (cmd instanceof DeleteSnapshotsDirCommand) {
            return execute((DeleteSnapshotsDirCommand)cmd);
        } else if (cmd instanceof CopyCommand) {
            return execute((CopyCommand)cmd);
        } else if (cmd instanceof DeleteCommand) {
            return execute((DeleteCommand)cmd);
        } else if (cmd instanceof UploadStatusCommand) {
            return execute((UploadStatusCommand)cmd);
        } else if (cmd instanceof HandleConfigDriveIsoCommand) {
            return execute((HandleConfigDriveIsoCommand)cmd);
        } else if (cmd instanceof GetDatadisksCommand) {
            return execute((GetDatadisksCommand)cmd);
        } else if (cmd instanceof CreateDatadiskTemplateCommand) {
            return execute((CreateDatadiskTemplateCommand)cmd);
        } else if (cmd instanceof MoveVolumeCommand) {
            return execute((MoveVolumeCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private Answer execute(HandleConfigDriveIsoCommand cmd) {
        if (cmd.isCreate()) {
            if (cmd.getIsoData() == null) {
                return new HandleConfigDriveIsoAnswer(cmd, "Invalid config drive ISO data");
            }
            String nfsMountPoint = getRootDir(cmd.getDestStore().getUrl(), _nfsVersion);
            File isoFile = new File(nfsMountPoint, cmd.getIsoFile());
            if(isoFile.exists()) {
                s_logger.debug("config drive iso already exists");
            }
            Path tempDir = null;
            try {
                tempDir = java.nio.file.Files.createTempDirectory(ConfigDrive.CONFIGDRIVEDIR);
                File tmpIsoFile = ConfigDriveBuilder.base64StringToFile(cmd.getIsoData(), tempDir.toAbsolutePath().toString(), cmd.getIsoFile());
                copyLocalToNfs(tmpIsoFile, new File(cmd.getIsoFile()), cmd.getDestStore());
            } catch (IOException | ConfigurationException e) {
                return new HandleConfigDriveIsoAnswer(cmd, "Failed due to exception: " + e.getMessage());
            } finally {
                try {
                    if (tempDir != null) {
                        FileUtils.deleteDirectory(tempDir.toFile());
                    }
                } catch (IOException ioe) {
                    s_logger.warn("Failed to delete ConfigDrive temporary directory: " + tempDir.toString(), ioe);
                }
            }
            return new HandleConfigDriveIsoAnswer(cmd, NetworkElement.Location.SECONDARY, "Successfully saved config drive at secondary storage");
        } else {
            DataStoreTO dstore = cmd.getDestStore();
            if (dstore instanceof NfsTO) {
                NfsTO nfs = (NfsTO) dstore;
                String relativeTemplatePath = new File(cmd.getIsoFile()).getPath();
                String nfsMountPoint = getRootDir(nfs.getUrl(), _nfsVersion);
                File tmpltPath = new File(nfsMountPoint, relativeTemplatePath);
                try {
                    Files.deleteIfExists(tmpltPath.toPath());
                } catch (IOException e) {
                    return new HandleConfigDriveIsoAnswer(cmd, e);
                }
                return new HandleConfigDriveIsoAnswer(cmd);
            } else {
                return new HandleConfigDriveIsoAnswer(cmd, "Not implemented yet");
            }
        }
    }

    protected void copyLocalToNfs(File localFile, File isoFile, DataStoreTO destData) throws ConfigurationException, IOException {
        String scriptsDir = "scripts/storage/secondary";
        String createVolScr = Script.findScript(scriptsDir, "createvolume.sh");
        if (createVolScr == null) {
            throw new ConfigurationException("Unable to find createvolume.sh");
        }
        s_logger.info("createvolume.sh found in " + createVolScr);

        int installTimeoutPerGig = 180 * 60 * 1000;
        int imgSizeGigs = (int) Math.ceil(localFile.length() * 1.0d / (1024 * 1024 * 1024));
        imgSizeGigs++; // add one just in case
        long timeout = imgSizeGigs * installTimeoutPerGig;

        Script scr = new Script(createVolScr, timeout, s_logger);
        scr.add("-s", Integer.toString(imgSizeGigs));
        scr.add("-n", isoFile.getName());
        scr.add("-t", getRootDir(destData.getUrl(), _nfsVersion) + "/" + isoFile.getParent());
        scr.add("-f", localFile.getAbsolutePath());
        scr.add("-d", "configDrive");
        String result;
        result = scr.execute();

        if (result != null) {
            // script execution failure
            throw new CloudRuntimeException("Failed to run script " + createVolScr);
        }
    }

    public Answer execute(GetDatadisksCommand cmd) {
        DataTO srcData = cmd.getData();
        String configurationId = cmd.getConfigurationId();
        TemplateObjectTO template = (TemplateObjectTO)srcData;
        DataStoreTO srcStore = srcData.getDataStore();
        if (!(srcStore instanceof NfsTO)) {
            return new CreateDatadiskTemplateAnswer("Unsupported protocol");
        }
        NfsTO nfsImageStore = (NfsTO)srcStore;
        String secondaryStorageUrl = nfsImageStore.getUrl();
        assert (secondaryStorageUrl != null);
        String templateUrl = secondaryStorageUrl + File.separator + srcData.getPath();
        Pair<String, String> templateInfo = decodeTemplateRelativePathAndNameFromUrl(secondaryStorageUrl, templateUrl, template.getName());
        String templateRelativeFolderPath = templateInfo.first();

        try {
            String secondaryMountPoint = getRootDir(secondaryStorageUrl, _nfsVersion);
            s_logger.info("MDOVE Secondary storage mount point: " + secondaryMountPoint);

            String srcOVAFileName = getTemplateOnSecStorageFilePath(secondaryMountPoint, templateRelativeFolderPath, templateInfo.second(), ImageFormat.OVA.getFileExtension());

            String ovfFilePath = getOVFFilePath(srcOVAFileName);
            if (ovfFilePath == null) {
                Script command = new Script("tar", 0, s_logger);
                command.add("--no-same-owner");
                command.add("--no-same-permissions");
                command.add("-xf", srcOVAFileName);
                command.setWorkDir(secondaryMountPoint + File.separator + templateRelativeFolderPath);
                s_logger.info("Executing command: " + command.toString());
                String result = command.execute();
                if (result != null) {
                    String msg = "Unable to unpack snapshot OVA file at: " + srcOVAFileName;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

                command = new Script("chmod", 0, s_logger);
                command.add("-R");
                command.add("666", secondaryMountPoint + File.separator + templateRelativeFolderPath);
                result = command.execute();
                if (result != null) {
                    s_logger.warn("Unable to set permissions for " + secondaryMountPoint + File.separator + templateRelativeFolderPath + " due to " + result);
                }
            }

            Script command = new Script("cp", _timeout, s_logger);
            command.add(ovfFilePath);
            command.add(ovfFilePath + ORIGINAL_FILE_EXTENSION);
            String result = command.execute();
            if (result != null) {
                String msg = "Unable to rename original OVF, error msg: " + result;
                s_logger.error(msg);
            }

            s_logger.debug("Reading OVF " + ovfFilePath + " to retrive the number of disks present in OVA");
            OVFHelper ovfHelper = new OVFHelper();

            List<DatadiskTO> disks = ovfHelper.getOVFVolumeInfoFromFile(ovfFilePath, configurationId);
            return new GetDatadisksAnswer(disks);
        } catch (Exception e) {
            String msg = "Get Datadisk Template Count failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new GetDatadisksAnswer(msg);
        }
    }

    public Answer execute(CreateDatadiskTemplateCommand cmd) {
        TemplateObjectTO diskTemplate = new TemplateObjectTO();
        TemplateObjectTO dataDiskTemplate = (TemplateObjectTO)cmd.getDataDiskTemplate();
        DataStoreTO dataStore = dataDiskTemplate.getDataStore();
        if (!(dataStore instanceof NfsTO)) {
            return new CreateDatadiskTemplateAnswer("Unsupported protocol");
        }
        NfsTO nfsImageStore = (NfsTO)dataStore;
        String secondaryStorageUrl = nfsImageStore.getUrl();
        assert (secondaryStorageUrl != null);

        try {
            String secondaryMountPoint = getRootDir(secondaryStorageUrl, _nfsVersion);

            long templateId = dataDiskTemplate.getId();
            String templateUniqueName = dataDiskTemplate.getUniqueName();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("no cmd? %s", cmd.stringRepresentation()));
            }
            String origDisk = cmd.getPath();
            long virtualSize = dataDiskTemplate.getSize();
            String diskName = origDisk.substring((origDisk.lastIndexOf(File.separator)) + 1);
            long physicalSize = new File(origDisk).length();
            String newTmplDir = getTemplateRelativeDirInSecStorage(dataDiskTemplate.getAccountId(), dataDiskTemplate.getId());
            String newTmplDirAbsolute = secondaryMountPoint + File.separator + newTmplDir;

            String ovfFilePath = getOVFFilePath(origDisk);
            if (!cmd.getBootable()) {
                // Create folder to hold datadisk template
                synchronized (newTmplDir.intern()) {
                    Script command = new Script("mkdir", _timeout, s_logger);
                    command.add("-p");
                    command.add(newTmplDirAbsolute);
                    String result = command.execute();
                    if (result != null) {
                        String msg = "Unable to prepare template directory: " + newTmplDir + ", storage: " + secondaryStorageUrl + ", error msg: " + result;
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }
                }
                // Move Datadisk VMDK from parent template folder to Datadisk template folder
                synchronized (origDisk.intern()) {
                    Script command = new Script("mv", _timeout, s_logger);
                    command.add(origDisk);
                    command.add(newTmplDirAbsolute);
                    String result = command.execute();
                    if (result != null) {
                        String msg = "Unable to copy VMDK from parent template folder to datadisk template folder" + ", error msg: " + result;
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }
                    command = new Script("cp", _timeout, s_logger);
                    command.add(ovfFilePath + ORIGINAL_FILE_EXTENSION);
                    command.add(newTmplDirAbsolute);
                    result = command.execute();
                    if (result != null) {
                        String msg = "Unable to copy VMDK from parent template folder to datadisk template folder" + ", error msg: " + result;
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }
                }
            }

            // Create OVF for the disk
            String newOvfFilePath = newTmplDirAbsolute + File.separator + ovfFilePath.substring(ovfFilePath.lastIndexOf(File.separator) + 1);
            OVFHelper ovfHelper = new OVFHelper();
            ovfHelper.rewriteOVFFileForSingleDisk(ovfFilePath + ORIGINAL_FILE_EXTENSION, newOvfFilePath, diskName);

            postCreatePrivateTemplate(newTmplDirAbsolute, templateId, templateUniqueName, physicalSize, virtualSize);
            writeMetaOvaForTemplate(newTmplDirAbsolute, ovfFilePath.substring(ovfFilePath.lastIndexOf(File.separator) + 1), diskName, templateUniqueName, physicalSize);

            diskTemplate.setId(templateId);
            if (diskName.endsWith("iso")){
                diskTemplate.setPath(newTmplDir + File.separator + diskName);
            }
            else {
                diskTemplate.setPath(newTmplDir + File.separator + templateUniqueName + ".ova");
            }
            diskTemplate.setSize(virtualSize);
            diskTemplate.setPhysicalSize(physicalSize);
        } catch (Exception e) {
            String msg = "Create Datadisk template failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new CreateDatadiskTemplateAnswer(msg);
        }
        return new CreateDatadiskTemplateAnswer(diskTemplate);
    }

    public Answer execute(MoveVolumeCommand cmd) {
        String volumeToString = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "volumeUuid", "volumeName");

        String rootDir = getRootDir(cmd.getDatastoreUri(), _nfsVersion);

        if (!rootDir.endsWith("/")) {
            rootDir += "/";
        }

        Path srcPath = Paths.get(rootDir + cmd.getSrcPath());
        Path destPath = Paths.get(rootDir + cmd.getDestPath());

        try {
            s_logger.debug(String.format("Trying to create missing directories (if any) to move volume [%s].", volumeToString));
            Files.createDirectories(destPath.getParent());
            s_logger.debug(String.format("Trying to move volume [%s] to [%s].", volumeToString, destPath));
            Files.move(srcPath, destPath);

            String msg = String.format("Moved volume [%s] from [%s] to [%s].", volumeToString, srcPath, destPath);
            s_logger.debug(msg);

            return new Answer(cmd, true, msg);

        } catch (IOException ioException) {
            s_logger.error(String.format("Failed to move volume [%s] from [%s] to [%s] due to [%s].", volumeToString, srcPath, destPath, ioException.getMessage()),
                    ioException);
            return new Answer(cmd, ioException);
        }
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

    public static String getTemplateOnSecStorageFilePath(String secStorageMountPoint, String templateRelativeFolderPath, String templateName, String fileExtension) {

        StringBuffer sb = new StringBuffer();
        sb.append(secStorageMountPoint);
        if (!secStorageMountPoint.endsWith("/")) {
            sb.append("/");
        }

        sb.append(templateRelativeFolderPath);
        if (!secStorageMountPoint.endsWith("/")) {
            sb.append("/");
        }

        sb.append(templateName);
        if (!fileExtension.startsWith(".")) {
            sb.append(".");
        }
        sb.append(fileExtension);

        return sb.toString();
    }

    public static String getSecondaryDatastoreUUID(String storeUrl) {
        return UUID.nameUUIDFromBytes(storeUrl.getBytes()).toString();
    }

    private static String getTemplateRelativeDirInSecStorage(long accountId, long templateId) {
        return "template/tmpl/" + accountId + "/" + templateId;
    }

    private void postCreatePrivateTemplate(final String installFullPath, final long templateId, final String templateName, final long size, final long virtualSize) throws Exception {
        // TODO a bit ugly here
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/template.properties"), "UTF-8"));) {
            out.write("filename=" + templateName + ".ova");
            out.newLine();
            out.write("description=privateTemplate");
            out.newLine();
            out.write("hvm=false");
            out.newLine();
            out.write("size=" + size);
            out.newLine();
            out.write("ova=false");
            out.newLine();
            out.write("id=" + templateId);
            out.newLine();
            out.write("ova.filename=" + templateName + ".ova");
            out.newLine();
            out.write("uniquename=" + templateName);
            out.newLine();
            out.write("ova.virtualsize=" + virtualSize);
            out.newLine();
            out.write("virtualsize=" + virtualSize);
            out.newLine();
            out.write("ova.size=" + size);
            out.newLine();
            out.write("checksum=");
            out.newLine();
            out.write("public=false");
            out.newLine();
        }
    }

    private void writeMetaOvaForTemplate(final String installFullPath, final String ovfFilename, final String vmdkFilename, final String templateName, final long diskSize) throws Exception {

        // TODO a bit ugly here
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/" + templateName + ".ova.meta"), "UTF-8"));
            out.write("ova.filename=" + templateName + ".ova");
            out.newLine();
            out.write("version=1.0");
            out.newLine();
            out.write("ovf=" + ovfFilename);
            out.newLine();
            out.write("numDisks=1");
            out.newLine();
            out.write("disk1.name=" + vmdkFilename);
            out.newLine();
            out.write("disk1.size=" + diskSize);
            out.newLine();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private String getOVFFilePath(String srcOVAFileName) {
        File file = new File(srcOVAFileName);
        assert (_storage != null);
        String[] files = _storage.listFiles(file.getParent());
        if (files != null) {
            for (String fileName : files) {
                if (fileName.toLowerCase().endsWith(".ovf")) {
                    File ovfFile = new File(fileName);
                    return file.getParent() + File.separator + ovfFile.getName();
                }
            }
        }
        return null;
    }

    protected CopyCmdAnswer postProcessing(File destFile, String downloadPath, String destPath, DataTO srcData, DataTO destData) throws ConfigurationException {
        if (destData.getObjectType() == DataObjectType.SNAPSHOT) {
            SnapshotObjectTO snapshot = new SnapshotObjectTO();
            snapshot.setPath(destPath + File.separator + destFile.getName());

            CopyCmdAnswer answer = new CopyCmdAnswer(snapshot);
            return answer;
        }
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
        long imgSizeGigs = (long)Math.ceil(destFile.length() * 1.0d / (1024 * 1024 * 1024));
        imgSizeGigs++; // add one just in case
        long timeout = imgSizeGigs * installTimeoutPerGig;

        String origPath = destFile.getAbsolutePath();
        String extension = null;
        if (srcData.getObjectType() == DataObjectType.TEMPLATE) {
            extension = ((TemplateObjectTO)srcData).getFormat().getFileExtension();
        } else if (srcData.getObjectType() == DataObjectType.VOLUME) {
            extension = ((VolumeObjectTO)srcData).getFormat().getFileExtension();
        }

        String templateName = UUID.randomUUID().toString();
        String templateFilename = templateName + "." + extension;
        Script scr = new Script(script, timeout, s_logger);
        scr.add("-s", Long.toString(imgSizeGigs)); // not used for now
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
        long size = _storage.getSize(downloadPath + File.separator + templateFilename);

        DataTO newDestTO = null;

        if (destData.getObjectType() == DataObjectType.TEMPLATE) {
            TemplateObjectTO newTemplTO = new TemplateObjectTO();
            newTemplTO.setPath(finalDownloadPath);
            newTemplTO.setName(finalFileName);
            newTemplTO.setSize(size);
            newTemplTO.setPhysicalSize(size);
            newDestTO = newTemplTO;
        } else {
            VolumeObjectTO newVolTO = new VolumeObjectTO();
            newVolTO.setPath(finalDownloadPath);
            newVolTO.setName(finalFileName);
            newVolTO.setSize(size);
            newDestTO = newVolTO;
        }

        return new CopyCmdAnswer(newDestTO);
    }

    protected Answer copyFromSwiftToNfs(CopyCommand cmd, DataTO srcData, SwiftTO swiftTO, DataTO destData, NfsTO destImageStore) {
        final String storagePath = destImageStore.getUrl();
        final String destPath = destData.getPath();
        try {
            String downloadPath = determineStorageTemplatePath(storagePath, destPath, _nfsVersion);
            final File downloadDirectory = _storage.getFile(downloadPath);

            if (downloadDirectory.exists()) {
                s_logger.debug("Directory " + downloadPath + " already exists");
            } else {
                if (!downloadDirectory.mkdirs()) {
                    final String errMsg = "Unable to create directory " + downloadPath + " to copy from Swift to cache.";
                    s_logger.error(errMsg);
                    return new CopyCmdAnswer(errMsg);
                }
            }

            File destFile = SwiftUtil.getObject(swiftTO, downloadDirectory, srcData.getPath());
            return postProcessing(destFile, downloadPath, destPath, srcData, destData);
        } catch (Exception e) {
            s_logger.debug("Failed to copy swift to nfs", e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    protected Answer copyFromS3ToNfs(CopyCommand cmd, DataTO srcData, S3TO s3, DataTO destData, NfsTO destImageStore) {
        final String storagePath = destImageStore.getUrl();
        final String destPath = destData.getPath();

        try {

            String downloadPath = determineStorageTemplatePath(storagePath, destPath, _nfsVersion);
            final File downloadDirectory = _storage.getFile(downloadPath);

            if (downloadDirectory.exists()) {
                s_logger.debug("Directory " + downloadPath + " already exists");
            } else {
                if (!downloadDirectory.mkdirs()) {
                    final String errMsg = "Unable to create directory " + downloadPath + " to copy from S3 to cache.";
                    s_logger.error(errMsg);
                    return new CopyCmdAnswer(errMsg);
                }
            }
            File destFile = new File(downloadDirectory, StringUtils.substringAfterLast(srcData.getPath(), S3Utils.SEPARATOR));
            S3Utils.getFile(s3, s3.getBucketName(), srcData.getPath(), destFile).waitForCompletion();

            return postProcessing(destFile, downloadPath, destPath, srcData, destData);
        } catch (Exception e) {

            final String errMsg = String.format("Failed to download" + "due to $1%s", e.getMessage());
            s_logger.error(errMsg, e);
            return new CopyCmdAnswer(errMsg);
        }
    }

    protected Answer copySnapshotToTemplateFromNfsToNfsXenserver(CopyCommand cmd, SnapshotObjectTO srcData, NfsTO srcDataStore, TemplateObjectTO destData, NfsTO destDataStore) {
        String srcMountPoint = getRootDir(srcDataStore.getUrl(), _nfsVersion);
        String snapshotPath = srcData.getPath();
        int index = snapshotPath.lastIndexOf("/");
        String snapshotName = snapshotPath.substring(index + 1);
        if (!snapshotName.startsWith("VHD-") && !snapshotName.endsWith(".vhd")) {
            snapshotName = snapshotName + ".vhd";
        }
        snapshotPath = snapshotPath.substring(0, index);

        snapshotPath = srcMountPoint + File.separator + snapshotPath;
        String destMountPoint = getRootDir(destDataStore.getUrl(), _nfsVersion);
        String destPath = destMountPoint + File.separator + destData.getPath();

        String errMsg = null;
        try {
            _storage.mkdir(destPath);

            String templateUuid = UUID.randomUUID().toString();
            String templateName = templateUuid + ".vhd";
            Script command = new Script(createTemplateFromSnapshotXenScript, cmd.getWait() * 1000L, s_logger);
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
            loc.create(1, true, templateUuid);
            loc.addFormat(info);
            loc.save();
            TemplateProp prop = loc.getTemplateInfo();
            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(destData.getPath() + File.separator + templateName);
            newTemplate.setFormat(ImageFormat.VHD);
            newTemplate.setSize(prop.getSize());
            newTemplate.setPhysicalSize(prop.getPhysicalSize());
            newTemplate.setName(templateUuid);
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

    protected Answer copySnapshotToTemplateFromNfsToNfs(CopyCommand cmd, SnapshotObjectTO srcData, NfsTO srcDataStore, TemplateObjectTO destData, NfsTO destDataStore) {

        if (srcData.getHypervisorType() == HypervisorType.XenServer) {
            return copySnapshotToTemplateFromNfsToNfsXenserver(cmd, srcData, srcDataStore, destData, destDataStore);
        } else if (srcData.getHypervisorType() == HypervisorType.KVM) {
            File srcFile = getFile(srcData.getPath(), srcDataStore.getUrl(), _nfsVersion);
            File destFile = getFile(destData.getPath(), destDataStore.getUrl(), _nfsVersion);

            VolumeObjectTO volumeObjectTO = srcData.getVolume();
            ImageFormat srcFormat = null;
            //TODO: the image format should be stored in snapshot table, instead of getting from volume
            if (volumeObjectTO != null) {
                srcFormat = volumeObjectTO.getFormat();
            } else {
                srcFormat = ImageFormat.QCOW2;
            }

            // get snapshot file name
            String templateName = srcFile.getName();
            // add kvm file extension for copied template name
            String fileName = templateName + "." + srcFormat.getFileExtension();
            String destFileFullPath = destFile.getAbsolutePath() + File.separator + fileName;
            s_logger.debug("copy snapshot " + srcFile.getAbsolutePath() + " to template " + destFileFullPath);
            Script.runSimpleBashScript("cp " + srcFile.getAbsolutePath() + " " + destFileFullPath);
            String metaFileName = destFile.getAbsolutePath() + File.separator + _tmpltpp;
            File metaFile = new File(metaFileName);
            try {
                _storage.create(destFile.getAbsolutePath(), _tmpltpp);
                try ( // generate template.properties file
                        FileWriter writer = new FileWriter(metaFile); BufferedWriter bufferWriter = new BufferedWriter(writer);) {
                    // KVM didn't change template unique name, just used the template name passed from orchestration layer, so no need
                    // to send template name back.
                    bufferWriter.write("uniquename=" + destData.getName());
                    bufferWriter.write("\n");
                    bufferWriter.write("filename=" + fileName);
                }
                try {
                    /**
                     * Snapshots might be in either QCOW2 or RAW image format
                     *
                     * For example RBD snapshots are in RAW format
                     */
                    Processor processor = null;
                    if (srcFormat == ImageFormat.QCOW2) {
                        processor = new QCOW2Processor();
                    } else if (srcFormat == ImageFormat.RAW) {
                        processor = new RawImageProcessor();
                    } else {
                        throw new ConfigurationException("Unknown image format " + srcFormat.toString());
                    }

                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put(StorageLayer.InstanceConfigKey, _storage);

                    processor.configure("template processor", params);
                    String destPath = destFile.getAbsolutePath();

                    FormatInfo info = processor.process(destPath, null, templateName);
                    TemplateLocation loc = new TemplateLocation(_storage, destPath);
                    loc.create(1, true, destData.getName());
                    loc.addFormat(info);
                    loc.save();

                    TemplateProp prop = loc.getTemplateInfo();
                    TemplateObjectTO newTemplate = new TemplateObjectTO();
                    newTemplate.setPath(destData.getPath() + File.separator + fileName);
                    newTemplate.setFormat(srcFormat);
                    newTemplate.setSize(prop.getSize());
                    newTemplate.setPhysicalSize(prop.getPhysicalSize());
                    return new CopyCmdAnswer(newTemplate);
                } catch (ConfigurationException e) {
                    s_logger.debug("Failed to create template:" + e.toString());
                    return new CopyCmdAnswer(e.toString());
                } catch (InternalErrorException e) {
                    s_logger.debug("Failed to create template:" + e.toString());
                    return new CopyCmdAnswer(e.toString());
                }
            } catch (IOException e) {
                s_logger.debug("Failed to create template:" + e.toString());
                return new CopyCmdAnswer(e.toString());
            }
        }

        return new CopyCmdAnswer("");
    }

    protected File getFile(String path, String nfsPath, String nfsVersion) {
        String filePath = getRootDir(nfsPath, nfsVersion) + File.separator + path;
        File f = new File(filePath);
        if (!f.exists()) {
            f = findFile(filePath);
            if (f == null) {
                _storage.mkdirs(filePath);
                f = new File(filePath);
            }
        }
        return f;
    }

    protected Answer createTemplateFromSnapshot(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        DataStoreTO srcDataStore = srcData.getDataStore();
        DataStoreTO destDataStore = destData.getDataStore();
        if (srcDataStore.getRole() == DataStoreRole.Image || srcDataStore.getRole() == DataStoreRole.ImageCache || srcDataStore.getRole() == DataStoreRole.Primary) {
            if (!(srcDataStore instanceof NfsTO)) {
                s_logger.debug("only support nfs storage as src, when create template from snapshot");
                return Answer.createUnsupportedCommandAnswer(cmd);
            }

            if (destDataStore instanceof NfsTO) {
                return copySnapshotToTemplateFromNfsToNfs(cmd, (SnapshotObjectTO)srcData, (NfsTO)srcDataStore, (TemplateObjectTO)destData, (NfsTO)destDataStore);
            } else if (destDataStore instanceof SwiftTO) {
                //create template on the same data store
                CopyCmdAnswer answer = (CopyCmdAnswer)copySnapshotToTemplateFromNfsToNfs(cmd, (SnapshotObjectTO)srcData, (NfsTO)srcDataStore, (TemplateObjectTO)destData,
                        (NfsTO)srcDataStore);
                if (!answer.getResult()) {
                    return answer;
                }
                s_logger.debug("starting copy template to swift");
                TemplateObjectTO newTemplate = (TemplateObjectTO)answer.getNewData();
                newTemplate.setDataStore(srcDataStore);
                CopyCommand newCpyCmd = new CopyCommand(newTemplate, destData, cmd.getWait(), cmd.executeInSequence());
                Answer result = copyFromNfsToSwift(newCpyCmd);

                cleanupStagingNfs(newTemplate);
                return result;

            } else if (destDataStore instanceof S3TO) {
                //create template on the same data store
                CopyCmdAnswer answer = (CopyCmdAnswer)copySnapshotToTemplateFromNfsToNfs(cmd, (SnapshotObjectTO)srcData, (NfsTO)srcDataStore, (TemplateObjectTO)destData,
                        (NfsTO)srcDataStore);
                if (!answer.getResult()) {
                    return answer;
                }
                TemplateObjectTO newTemplate = (TemplateObjectTO)answer.getNewData();
                newTemplate.setDataStore(srcDataStore);
                CopyCommand newCpyCmd = new CopyCommand(newTemplate, destData, cmd.getWait(), cmd.executeInSequence());
                Answer result = copyFromNfsToS3(newCpyCmd);

                cleanupStagingNfs(newTemplate);

                return result;
            }
        }
        s_logger.debug("Failed to create template from snapshot");
        return new CopyCmdAnswer("Unsupported protocol");
    }

    /**
     * clean up template data on staging area
     * @param newTemplate: The template on the secondary storage that needs to be cleaned up
     */
    protected void cleanupStagingNfs(TemplateObjectTO newTemplate) {
        try {
            DeleteCommand deleteCommand = new DeleteCommand(newTemplate);
            execute(deleteCommand);
        } catch (Exception e) {
            s_logger.debug("Failed to clean up staging area:", e);
        }
    }

    protected Answer copyFromNfsToImage(CopyCommand cmd) {
        DataTO destData = cmd.getDestTO();
        DataStoreTO destDataStore = destData.getDataStore();

        if (destDataStore instanceof S3TO) {
            return copyFromNfsToS3(cmd);
        } else if (destDataStore instanceof SwiftTO) {
            return copyFromNfsToSwift(cmd);
        } else {
            return new CopyCmdAnswer("unsupported ");
        }
    }

    private boolean shouldPerformDataMigration(DataTO srcData, DataTO destData) {
        DataStoreTO srcDataStore = srcData.getDataStore();
        DataStoreTO destDataStore = destData.getDataStore();
        if (DataStoreRole.Image == srcDataStore.getRole() && DataStoreRole.Image == destDataStore.getRole() &&
                srcDataStore instanceof NfsTO && destDataStore instanceof NfsTO &&
                ((srcData.getObjectType() == DataObjectType.TEMPLATE && destData.getObjectType() == DataObjectType.TEMPLATE) ||
                        (srcData.getObjectType() == DataObjectType.SNAPSHOT && destData.getObjectType() == DataObjectType.SNAPSHOT) ||
                        (srcData.getObjectType() == DataObjectType.VOLUME && destData.getObjectType() == DataObjectType.VOLUME))) {
            return true;
        }
        return false;
    }

    protected Answer execute(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        DataStoreTO srcDataStore = srcData.getDataStore();
        DataStoreTO destDataStore = destData.getDataStore();
        if (shouldPerformDataMigration(srcData, destData)) {
            return copyFromNfsToNfs(cmd);
        }

        if (srcData.getObjectType() == DataObjectType.SNAPSHOT && destData.getObjectType() == DataObjectType.TEMPLATE) {
            return createTemplateFromSnapshot(cmd);
        }

        if (destDataStore instanceof NfsTO && destDataStore.getRole() == DataStoreRole.ImageCache) {
            NfsTO destImageStore = (NfsTO)destDataStore;
            if (srcDataStore instanceof S3TO) {
                S3TO s3 = (S3TO)srcDataStore;
                return copyFromS3ToNfs(cmd, srcData, s3, destData, destImageStore);
            } else if (srcDataStore instanceof SwiftTO) {
                return copyFromSwiftToNfs(cmd, srcData, (SwiftTO)srcDataStore, destData, destImageStore);
            }
        }

        if (srcDataStore.getRole() == DataStoreRole.ImageCache && destDataStore.getRole() == DataStoreRole.Image) {
            return copyFromNfsToImage(cmd);
        }

        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    protected String determineS3TemplateDirectory(final Long accountId, final Long templateId, final String templateUniqueName) {
        return StringUtils.join(asList(TEMPLATE_ROOT_DIR, accountId, templateId, templateUniqueName), S3Utils.SEPARATOR);
    }

    private String determineS3TemplateNameFromKey(String key) {
        return StringUtils.substringAfterLast(StringUtils.substringBeforeLast(key, S3Utils.SEPARATOR), S3Utils.SEPARATOR);
    }

    protected String determineS3VolumeDirectory(final Long accountId, final Long volId) {
        return StringUtils.join(asList(VOLUME_ROOT_DIR, accountId, volId), S3Utils.SEPARATOR);
    }

    protected Long determineS3VolumeIdFromKey(String key) {
        return Long.parseLong(StringUtils.substringAfterLast(StringUtils.substringBeforeLast(key, S3Utils.SEPARATOR), S3Utils.SEPARATOR));
    }

    private String determineStorageTemplatePath(final String storagePath, String dataPath, String nfsVersion) {
        return StringUtils.join(asList(getRootDir(storagePath, nfsVersion), dataPath), File.separator);
    }

    protected File downloadFromUrlToNfs(String url, NfsTO nfs, String path, String name) {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                s_logger.debug("Faled to get entity");
                throw new CloudRuntimeException("Failed to get url: " + url);
            }

            String nfsMountPath = getRootDir(nfs.getUrl(), _nfsVersion);

            String filePath = nfsMountPath + File.separator + path;
            File directory = new File(filePath);
            if (!directory.exists()) {
                _storage.mkdirs(filePath);
            }
            File destFile = new File(filePath + File.separator + name);
            if (!destFile.createNewFile()) {
                s_logger.warn("Reusing existing file " + destFile.getPath());
            }
            try (FileOutputStream outputStream = new FileOutputStream(destFile);) {
                entity.writeTo(outputStream);
            } catch (IOException e) {
                s_logger.debug("downloadFromUrlToNfs:Exception:" + e.getMessage(), e);
            }
            return new File(destFile.getAbsolutePath());
        } catch (IOException e) {
            s_logger.debug("Failed to get url: " + url + ", due to " + e.toString());
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
            String fileName = cmd.getName() + "." + cmd.getFormat().getFileExtension();
            file = downloadFromUrlToNfs(cmd.getUrl(), nfsCacheStore, path, fileName);
            String container = "T-" + cmd.getId();
            String swiftPath = SwiftUtil.putObject(swiftTO, file, container, null);

            long virtualSize = getVirtualSize(file, getTemplateFormat(file.getName()));
            long size = file.length();
            String uniqueName = cmd.getName();

            //put metda file
            File uniqDir = _storage.createUniqDir();
            String metaFileName = uniqDir.getAbsolutePath() + File.separator + _tmpltpp;
            _storage.create(uniqDir.getAbsolutePath(), _tmpltpp);

            File metaFile = swiftWriteMetadataFile(metaFileName, uniqueName, fileName, size, virtualSize);

            SwiftUtil.putObject(swiftTO, metaFile, container, _tmpltpp);
            metaFile.delete();
            uniqDir.delete();
            String md5sum = null;
            try (FileInputStream fs = new FileInputStream(file)) {
                md5sum = DigestUtils.md5Hex(fs);
            } catch (IOException e) {
                s_logger.debug("Failed to get md5sum: " + file.getAbsoluteFile());
            }

            DownloadAnswer answer = new DownloadAnswer(null, 100, null, VMTemplateStorageResourceAssoc.Status.DOWNLOADED, swiftPath, swiftPath, virtualSize, file.length(), md5sum);
            return answer;
        } catch (IOException e) {
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

    private ImageFormat getTemplateFormat(String filePath) {
        String ext = null;
        int extensionPos = filePath.lastIndexOf('.');
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        int i = lastSeparator > extensionPos ? -1 : extensionPos;
        if (i > 0) {
            ext = filePath.substring(i + 1);
        }
        if (ext != null) {
            if (ext.equalsIgnoreCase("vhd")) {
                return ImageFormat.VHD;
            } else if (ext.equalsIgnoreCase("vhdx")) {
                return ImageFormat.VHDX;
            } else if (ext.equalsIgnoreCase("qcow2")) {
                return ImageFormat.QCOW2;
            } else if (ext.equalsIgnoreCase("ova")) {
                return ImageFormat.OVA;
            } else if (ext.equalsIgnoreCase("tar")) {
                return ImageFormat.TAR;
            } else if (ext.equalsIgnoreCase("img") || ext.equalsIgnoreCase("raw")) {
                return ImageFormat.RAW;
            } else if (ext.equalsIgnoreCase("vmdk")) {
                return ImageFormat.VMDK;
            } else if (ext.equalsIgnoreCase("vdi")) {
                return ImageFormat.VDI;
            }
        }

        return null;

    }

    protected long getVirtualSize(File file, ImageFormat format) {
        Processor processor = null;
        try {
            if (format == null) {
                return file.length();
            } else if (format == ImageFormat.QCOW2) {
                processor = new QCOW2Processor();
            } else if (format == ImageFormat.OVA) {
                processor = new OVAProcessor();
            } else if (format == ImageFormat.VHD) {
                processor = new VhdProcessor();
            } else if (format == ImageFormat.RAW) {
                processor = new RawImageProcessor();
            } else if (format == ImageFormat.VMDK) {
                processor = new VmdkProcessor();
            }
            if (format == ImageFormat.TAR) {
                processor = new TARProcessor();
            }

            if (processor == null) {
                return file.length();
            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            processor.configure("template processor", params);
            return processor.getVirtualSize(file);
        } catch (Exception e) {
            s_logger.warn("Failed to get virtual size of file " + file.getPath() + ", returning file size instead: ", e);
            return file.length();
        }

    }

    protected File findFile(String path) {
        File srcFile = _storage.getFile(path);
        if (!srcFile.exists()) {
            srcFile = _storage.getFile(path + ".qcow2");
            if (!srcFile.exists()) {
                srcFile = _storage.getFile(path + ".vhd");
                if (!srcFile.exists()) {
                    srcFile = _storage.getFile(path + ".ova");
                    if (!srcFile.exists()) {
                        srcFile = _storage.getFile(path + ".vmdk");
                        if (!srcFile.exists()) {
                            return null;
                        }
                    }
                }
            }
        }

        return srcFile;
    }

    protected Answer copyFromNfsToNfs(CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        DataStoreTO srcDataStore = srcData.getDataStore();
        NfsTO srcStore = (NfsTO)srcDataStore;
        DataStoreTO destDataStore = destData.getDataStore();
        final NfsTO destStore = (NfsTO) destDataStore;
        try {
            File srcFile = new File(getDir(srcStore.getUrl(), _nfsVersion), srcData.getPath());
            File destFile = new File(getDir(destStore.getUrl(), _nfsVersion), destData.getPath());

            if (srcFile == null) {
                return new CopyCmdAnswer("Can't find source file at path: "+ srcData.getPath() +" on datastore: "+ srcDataStore.getUuid() +" to initiate file transfer");
            }
            ImageFormat format = getTemplateFormat(srcFile.getName());
            if (srcData instanceof TemplateObjectTO || srcData instanceof VolumeObjectTO) {
                File srcDir = null;
                if (srcFile.isFile() || srcFile.getName().contains(".")) {
                    srcDir = new File(srcFile.getParent());
                } else if (!srcFile.isDirectory()) {
                    srcDir = new File(srcFile.getParent());
                } else if (srcFile.isDirectory() && Arrays.stream(srcData.getPath().split(File.separator)).count() == 4) {
                    destFile = new File(destFile.getPath(), srcFile.getName());
                }
                File destDir = null;
                if (destFile.isFile()) {
                    destDir = new File(destFile.getParent());
                }
                try {
                    FileUtils.copyDirectory((srcDir == null ? srcFile : srcDir), (destDir == null? destFile : destDir));
                } catch (IOException e) {
                    String msg = "Failed to copy file to destination";
                    s_logger.info(msg);
                    return new CopyCmdAnswer(msg);
                }
            } else {
                destFile = new File(destFile, srcFile.getName());
                try {
                if (srcFile.isFile()) {
                    FileUtils.copyFile(srcFile, destFile);
                } else {
                    // for vmware
                    srcFile = new File(srcFile.getParent());
                    FileUtils.copyDirectory(srcFile, destFile);
                }
                } catch (IOException e) {
                    String msg = "Failed to copy file to destination";
                    s_logger.info(msg);
                    return new CopyCmdAnswer(msg);
                }
            }

            DataTO retObj = null;
            if (destData.getObjectType() == DataObjectType.TEMPLATE) {
                TemplateObjectTO newTemplate = new TemplateObjectTO();
                newTemplate.setPath(destData.getPath() + File.separator + srcFile.getName());
                newTemplate.setSize(getVirtualSize(srcFile, format));
                newTemplate.setPhysicalSize(srcFile.length());
                newTemplate.setFormat(format);
                retObj = newTemplate;
            } else if (destData.getObjectType() == DataObjectType.VOLUME) {
                VolumeObjectTO newVol = new VolumeObjectTO();
                if (srcFile.isFile()) {
                    newVol.setPath(destData.getPath() + File.separator + srcFile.getName());
                } else {
                    newVol.setPath(srcData.getPath());
                }
                newVol.setSize(getVirtualSize(srcFile, format));
                retObj = newVol;
            } else if (destData.getObjectType() == DataObjectType.SNAPSHOT) {
                SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
                if (srcFile.isFile()) {
                    newSnapshot.setPath(destData.getPath() + File.separator + destFile.getName());
                } else {
                    newSnapshot.setPath(destData.getPath() + File.separator + destFile.getName() + File.separator + destFile.getName());
                }
                retObj = newSnapshot;
            }
            return new CopyCmdAnswer(retObj);
            } catch (Exception e) {
                s_logger.error("failed to copy file" + srcData.getPath(), e);
                return new CopyCmdAnswer("failed to copy file" + srcData.getPath() + e.toString());
        }
    }

    protected Answer copyFromNfsToS3(CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        DataStoreTO srcDataStore = srcData.getDataStore();
        NfsTO srcStore = (NfsTO)srcDataStore;
        DataStoreTO destDataStore = destData.getDataStore();

        final S3TO s3 = (S3TO)destDataStore;

        try {
            final String templatePath = determineStorageTemplatePath(srcStore.getUrl(), srcData.getPath(), _nfsVersion);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + srcData.getObjectType() + " from directory " + templatePath + " to upload to S3.");
            }

            final String bucket = s3.getBucketName();
            File srcFile = findFile(templatePath);
            if (srcFile == null) {
                return new CopyCmdAnswer("Can't find src file:" + templatePath);
            }

            ImageFormat format = getTemplateFormat(srcFile.getName());
            String key = destData.getPath() + S3Utils.SEPARATOR + srcFile.getName();

            putFile(s3, srcFile, bucket, key).waitForCompletion();

            DataTO retObj = null;
            if (destData.getObjectType() == DataObjectType.TEMPLATE) {
                TemplateObjectTO newTemplate = new TemplateObjectTO();
                newTemplate.setPath(key);
                newTemplate.setSize(getVirtualSize(srcFile, format));
                newTemplate.setPhysicalSize(srcFile.length());
                newTemplate.setFormat(format);
                retObj = newTemplate;
            } else if (destData.getObjectType() == DataObjectType.VOLUME) {
                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(key);
                newVol.setSize(srcFile.length());
                retObj = newVol;
            } else if (destData.getObjectType() == DataObjectType.SNAPSHOT) {
                SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
                newSnapshot.setPath(key);
                retObj = newSnapshot;
            }

            return new CopyCmdAnswer(retObj);
        } catch (Exception e) {
            s_logger.error("failed to upload" + srcData.getPath(), e);
            return new CopyCmdAnswer("failed to upload" + srcData.getPath() + e.toString());
        }
    }

    /***
     *This method will create a file using the filenName and metaFileName.
     *That file will contain the given attributes (unique name, file name, size, and virtualSize).
     *
     * @param metaFileName : The path of the metadata file
     * @param filename      :attribute:  Filename of the template
     * @param uniqueName    :attribute:  Unique name of the template
     * @param size          :attribute:  physical size of the template
     * @param virtualSize   :attribute:  virtual size of the template
     * @return File representing the metadata file
     * @throws IOException
     */

    protected File swiftWriteMetadataFile(String metaFileName, String uniqueName, String filename, long size, long virtualSize) throws IOException {
        File metaFile = new File(metaFileName);
        FileWriter writer = new FileWriter(metaFile);
        BufferedWriter bufferWriter = new BufferedWriter(writer);
        bufferWriter.write("uniquename=" + uniqueName);
        bufferWriter.write("\n");
        bufferWriter.write("filename=" + filename);
        bufferWriter.write("\n");
        bufferWriter.write("size=" + size);
        bufferWriter.write("\n");
        bufferWriter.write("virtualsize=" + virtualSize);
        bufferWriter.close();
        writer.close();
        return metaFile;
    }

    /**
     * Creates a template.properties for Swift with its correct unique name
     *
     * @param swift  The swift object
     * @param srcFile Source file on the staging NFS
     * @param containerName Destination container  @return true on successful write
     * @param uniqueName Unique name identifying the template
     */
    protected boolean swiftUploadMetadataFile(SwiftTO swift, File srcFile, String containerName, String uniqueName) throws IOException {

        File uniqDir = _storage.createUniqDir();
        String metaFileName = uniqDir.getAbsolutePath() + File.separator + _tmpltpp;
        _storage.create(uniqDir.getAbsolutePath(), _tmpltpp);

        long virtualSize = getVirtualSize(srcFile, getTemplateFormat(srcFile.getName()));

        File metaFile = swiftWriteMetadataFile(metaFileName, uniqueName, srcFile.getName(), srcFile.length(), virtualSize);

        SwiftUtil.putObject(swift, metaFile, containerName, _tmpltpp);
        metaFile.delete();
        uniqDir.delete();

        return true;
    }

    /**
     * Copies data from NFS and uploads it into a Swift container
     *
     * @param cmd CopyComand
     * @return CopyCmdAnswer
     */
    protected Answer copyFromNfsToSwift(CopyCommand cmd) {

        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();

        DataStoreTO srcDataStore = srcData.getDataStore();
        NfsTO srcStore = (NfsTO)srcDataStore;
        DataStoreTO destDataStore = destData.getDataStore();
        File srcFile = getFile(srcData.getPath(), srcStore.getUrl(), _nfsVersion);

        SwiftTO swift = (SwiftTO)destDataStore;
        long pathId = destData.getId();

        try {

            if (destData instanceof SnapshotObjectTO) {
                pathId = ((SnapshotObjectTO)destData).getVolume().getId();
            }

            String containerName = SwiftUtil.getContainerName(destData.getObjectType().toString(), pathId);
            String swiftPath = SwiftUtil.putObject(swift, srcFile, containerName, srcFile.getName());

            DataTO retObj = null;
            if (destData.getObjectType() == DataObjectType.TEMPLATE) {
                TemplateObjectTO destTemplateData = (TemplateObjectTO)destData;
                String uniqueName = destTemplateData.getName();
                swiftUploadMetadataFile(swift, srcFile, containerName, uniqueName);
                TemplateObjectTO newTemplate = new TemplateObjectTO();
                newTemplate.setPath(swiftPath);
                newTemplate.setSize(getVirtualSize(srcFile, getTemplateFormat(srcFile.getName())));
                newTemplate.setPhysicalSize(srcFile.length());
                newTemplate.setFormat(getTemplateFormat(srcFile.getName()));
                retObj = newTemplate;
            } else if (destData.getObjectType() == DataObjectType.VOLUME) {
                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(containerName);
                newVol.setSize(getVirtualSize(srcFile, getTemplateFormat(srcFile.getName())));
                retObj = newVol;
            } else if (destData.getObjectType() == DataObjectType.SNAPSHOT) {
                SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
                newSnapshot.setPath(containerName + File.separator + srcFile.getName());
                retObj = newSnapshot;
            }

            return new CopyCmdAnswer(retObj);

        } catch (Exception e) {
            s_logger.error("failed to upload " + srcData.getPath(), e);
            return new CopyCmdAnswer("failed to upload " + srcData.getPath() + e.toString());
        }
    }

    String swiftDownload(SwiftTO swift, String container, String rfilename, String lFullPath) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl() + " -U " + swift.getAccount() + ":" + swift.getUserName()
        + " -K " + swift.getKey() + " download " + container + " " + rfilename + " -o " + lFullPath);
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
                    String errMsg = "swiftDownload failed , err=" + parser.getLines();
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
        command.add("cd " + ldir + ";/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl() + " -U " + swift.getAccount() + ":"
                + swift.getUserName() + " -K " + swift.getKey() + " download " + container);
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
                    String errMsg = "swiftDownloadContainer failed , err=" + parser.getLines();
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
            String[] dir_lst = dir.list();
            if (dir_lst != null) {
                for (String file : dir_lst) {
                    if (file.startsWith(".")) {
                        continue;
                    }
                    files.add(file);
                }
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
                command.add("cd " + lDir + ";/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl() + " -U " + swift.getAccount() + ":"
                        + swift.getUserName() + " -K " + swift.getKey() + " upload " + container + " " + file);
            } else {
                command.add("cd " + lDir + ";/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl() + " -U " + swift.getAccount() + ":"
                        + swift.getUserName() + " -K " + swift.getKey() + " upload -S " + SWIFT_MAX_SIZE + " " + container + " " + file);
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
                        String errMsg = "swiftUpload failed , err=" + parser.getLines();
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
        command.add("/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl() + " -U " + swift.getAccount() + ":" + swift.getUserName()
        + " -K " + swift.getKey() + " list " + container + " " + rFilename);
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
        command.add("/usr/bin/python /usr/local/cloud/systemvm/scripts/storage/secondary/swift -A " + swift.getUrl() + " -U " + swift.getAccount() + ":" + swift.getUserName()
        + " -K " + swift.getKey() + " delete " + container + " " + object);
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
                    String errMsg = "swiftDelete failed , err=" + parser.getLines();
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
            NfsTO nfs = (NfsTO)dstore;
            String relativeSnapshotPath = cmd.getDirectory();
            String parent = getRootDir(nfs.getUrl(), _nfsVersion);

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
                details = "Unable to delete directory " + snapshotDir.getName() + " under snapshot path " + relativeSnapshotPath;
                s_logger.debug(details);
                return new Answer(cmd, false, details);
            }
            return new Answer(cmd, true, null);
        } else if (dstore instanceof S3TO) {
            final S3TO s3 = (S3TO)dstore;
            final String path = cmd.getDirectory();
            final String bucket = s3.getBucketName();
            try {
                S3Utils.deleteDirectory(s3, bucket, path);
                return new Answer(cmd, true, String.format("Deleted snapshot %1%s from bucket %2$s.", path, bucket));
            } catch (Exception e) {
                final String errorMessage = String.format("Failed to delete snapshot %1$s from bucket %2$s due to the following error: %3$s", path, bucket, e.getMessage());
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
            String result = swiftDelete((SwiftTO)dstore, "V-" + volumeId.toString(), "");
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

    private Answer execute(ComputeChecksumCommand cmd) {

        String relativeTemplatePath = cmd.getTemplatePath();
        DataStoreTO store = cmd.getStore();
        if (!(store instanceof NfsTO)) {
            return new Answer(cmd, false, "can't handle non nfs data store");
        }
        NfsTO nfsStore = (NfsTO)store;
        String parent = getRootDir(nfsStore.getUrl(), _nfsVersion);

        if (relativeTemplatePath.startsWith(File.separator)) {
            relativeTemplatePath = relativeTemplatePath.substring(1);
        }

        if (!parent.endsWith(File.separator)) {
            parent += File.separator;
        }
        String absoluteTemplatePath = parent + relativeTemplatePath;
        String algorithm = cmd.getAlgorithm();
        File f = new File(absoluteTemplatePath);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("parent path " + parent + " relative template path " + relativeTemplatePath);
        }
        String checksum = null;

        try (InputStream is = new FileInputStream(f);){
            checksum = DigestHelper.digest(algorithm, is).toString();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Successfully calculated checksum for file " + absoluteTemplatePath + " - " + checksum);
            }
        } catch (IOException e) {
            String logMsg = "Unable to process file for " + algorithm + " - " + absoluteTemplatePath;
            s_logger.error(logMsg);
            return new Answer(cmd, false, checksum);
        } catch (NoSuchAlgorithmException e) {
            return new Answer(cmd, false, checksum);
        }

        return new Answer(cmd, true, checksum);
    }

    private void configCerts(KeystoreManager.Certificates certs) {
        if (certs == null) {
            configureSSL();
        } else {
            String prvKey = certs.getPrivKey();
            String pubCert = certs.getPrivCert();
            String certChain = certs.getCertChain();
            String rootCACert = certs.getRootCACert();

            try {
                File prvKeyFile = File.createTempFile("prvkey", null);
                String prvkeyPath = prvKeyFile.getAbsolutePath();
                try (BufferedWriter prvt_key_file = new BufferedWriter(new FileWriter(prvKeyFile));) {
                    prvt_key_file.write(prvKey);
                } catch (IOException e) {
                    s_logger.debug("Failed to config ssl: " + e.toString());
                }

                File pubCertFile = File.createTempFile("pubcert", null);
                String pubCertFilePath = pubCertFile.getAbsolutePath();

                try (BufferedWriter pub_cert_file = new BufferedWriter(new FileWriter(pubCertFile));) {
                    pub_cert_file.write(pubCert);
                } catch (IOException e) {
                    s_logger.debug("Failed to config ssl: " + e.toString());
                }

                String certChainFilePath = null, rootCACertFilePath = null;
                File certChainFile = null, rootCACertFile = null;
                if (certChain != null) {
                    certChainFile = File.createTempFile("certchain", null);
                    certChainFilePath = certChainFile.getAbsolutePath();
                    try (BufferedWriter cert_chain_out = new BufferedWriter(new FileWriter(certChainFile));) {
                        cert_chain_out.write(certChain);
                    } catch (IOException e) {
                        s_logger.debug("Failed to config ssl: " + e.toString());
                    }
                }

                if (rootCACert != null) {
                    rootCACertFile = File.createTempFile("rootcert", null);
                    rootCACertFilePath = rootCACertFile.getAbsolutePath();
                    try (BufferedWriter root_ca_cert_file = new BufferedWriter(new FileWriter(rootCACertFile));) {
                        root_ca_cert_file.write(rootCACert);
                    } catch (IOException e) {
                        s_logger.debug("Failed to config ssl: " + e.toString());
                    }
                }

                configureSSL(prvkeyPath, pubCertFilePath, certChainFilePath, rootCACertFilePath);

                prvKeyFile.delete();
                pubCertFile.delete();
                if (certChainFile != null) {
                    certChainFile.delete();
                }
                if (rootCACertFile != null) {
                    rootCACertFile.delete();
                }

            } catch (IOException e) {
                s_logger.debug("Failed to config ssl: " + e.toString());
            }
        }
    }

    private Answer execute(SecStorageSetupCommand cmd) {
        if (!_inSystemVM) {
            return new Answer(cmd, true, null);
        }
        Answer answer = null;
        DataStoreTO dStore = cmd.getDataStore();
        if (dStore instanceof NfsTO) {
            String secUrl = cmd.getSecUrl();
            try {
                URI uri = new URI(secUrl);
                String nfsHostIp = getUriHostIp(uri);

                addRouteToInternalIpOrCidr(_storageGateway, _storageIp, _storageNetmask, nfsHostIp);

                String dir = mountUri(uri, cmd.getNfsVersion());

                configCerts(cmd.getCerts());

                nfsIps.add(nfsHostIp);
                answer = new SecStorageSetupAnswer(dir);
            } catch (Exception e) {
                String msg = "GetRootDir for " + secUrl + " failed due to " + e.toString();
                s_logger.error(msg);
                answer = new Answer(cmd, false, msg);

            }
        } else {
            // TODO: what do we need to setup for S3/Swift, maybe need to mount
            // to some cache storage
            answer = new Answer(cmd, true, null);
        }

        savePostUploadPSK(cmd.getPostUploadKey());
        startPostUploadServer();
        return answer;
    }

    private void startPostUploadServer() {
        final int PORT = 8210;
        final int NO_OF_WORKERS = 15;
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(NO_OF_WORKERS);
        final ServerBootstrap b = new ServerBootstrap();
        final NfsSecondaryStorageResource storageResource = this;
        b.group(bossGroup, workerGroup);
        b.channel(NioServerSocketChannel.class);
        b.handler(new LoggingHandler(LogLevel.INFO));
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpRequestDecoder());
                pipeline.addLast(new HttpResponseEncoder());
                pipeline.addLast(new HttpContentCompressor());
                pipeline.addLast(new HttpUploadServerHandler(storageResource));
            }
        });
        new Thread() {
            @Override
            public void run() {
                try {
                    Channel ch = b.bind(PORT).sync().channel();
                    s_logger.info(String.format("Started post upload server on port %d with %d workers", PORT, NO_OF_WORKERS));
                    ch.closeFuture().sync();
                } catch (InterruptedException e) {
                    s_logger.info("Failed to start post upload server");
                    s_logger.debug("Exception while starting post upload server", e);
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                    s_logger.info("shutting down post upload server");
                }
            }
        }.start();
        s_logger.info("created a thread to start post upload server");
    }

    private void savePostUploadPSK(String psk) {
        try {
            FileUtils.writeStringToFile(new File(POST_UPLOAD_KEY_LOCATION), psk, "utf-8");
        } catch (IOException ex) {
            s_logger.debug("Failed to copy PSK to the file.", ex);
        }
    }

    protected Answer deleteSnapshot(final DeleteCommand cmd) {
        DataTO obj = cmd.getData();
        DataStoreTO dstore = obj.getDataStore();
        if (dstore instanceof NfsTO) {
            NfsTO nfs = (NfsTO)dstore;
            String parent = getRootDir(nfs.getUrl(), _nfsVersion);
            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String snapshotPath = obj.getPath();
            if (snapshotPath.startsWith(File.separator)) {
                snapshotPath = snapshotPath.substring(1);
            }
            // check if the passed snapshot path is a directory or not. For ImageCache, path is stored as a directory instead of
            // snapshot file name. If so, since backupSnapshot process has already deleted snapshot in cache, so we just do nothing
            // and return true.
            String fullSnapPath = parent + snapshotPath;
            File snapDir = new File(fullSnapPath);
            if (snapDir.exists() && snapDir.isDirectory()) {
                s_logger.debug("snapshot path " + snapshotPath + " is a directory, already deleted during backup snapshot, so no need to delete");
                return new Answer(cmd, true, null);
            }
            // passed snapshot path is a snapshot file path, then get snapshot directory first
            int index = snapshotPath.lastIndexOf("/");
            String snapshotName = snapshotPath.substring(index + 1);
            snapshotPath = snapshotPath.substring(0, index);
            String absoluteSnapshotPath = parent + snapshotPath;
            // check if snapshot directory exists
            File snapshotDir = new File(absoluteSnapshotPath);
            String details = null;
            if (!snapshotDir.exists()) {
                details = "snapshot directory " + snapshotDir.getName() + " doesn't exist";
                s_logger.debug(details);
                return new Answer(cmd, true, details);
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
            final S3TO s3 = (S3TO)dstore;
            final String path = obj.getPath();
            final String bucket = s3.getBucketName();
            try {
                S3Utils.deleteObject(s3, bucket, path);
                return new Answer(cmd, true, String.format("Deleted snapshot %1%s from bucket %2$s.", path, bucket));
            } catch (Exception e) {
                final String errorMessage = String.format("Failed to delete snapshot %1$s from bucket %2$s due to the following error: %3$s", path, bucket, e.getMessage());
                s_logger.error(errorMessage, e);
                return new Answer(cmd, false, errorMessage);
            }
        } else if (dstore instanceof SwiftTO) {
            SwiftTO swiftTO = (SwiftTO)dstore;
            String path = obj.getPath();
            SwiftUtil.deleteObject(swiftTO, path);

            return new Answer(cmd, true, "Deleted snapshot " + path + " from swift");
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + dstore);
        }

    }

    Map<String, TemplateProp> swiftListTemplate(SwiftTO swift) {
        String[] containers = SwiftUtil.list(swift, "", null);
        if (containers == null) {
            return null;
        }
        Map<String, TemplateProp> tmpltInfos = new HashMap<String, TemplateProp>();
        for (String container : containers) {
            if (container.startsWith("T-")) {
                String[] files = SwiftUtil.list(swift, container, _tmpltpp);
                if (files.length != 1) {
                    continue;
                }
                try {
                    File tempFile = File.createTempFile("template", ".tmp");
                    File tmpFile = SwiftUtil.getObject(swift, tempFile, container + File.separator + _tmpltpp);
                    if (tmpFile == null) {
                        continue;
                    }
                    try (FileReader fr = new FileReader(tmpFile); BufferedReader brf = new BufferedReader(fr);) {
                        String line = null;
                        String uniqName = null;
                        Long size = null;
                        Long physicalSize = null;
                        String name = null;
                        while ((line = brf.readLine()) != null) {
                            if (line.startsWith("uniquename=")) {
                                uniqName = line.split("=")[1];
                            } else if (line.startsWith("size=")) {
                                physicalSize = Long.parseLong(line.split("=")[1]);
                            } else if (line.startsWith("virtualsize=")) {
                                size = Long.parseLong(line.split("=")[1]);
                            } else if (line.startsWith("filename=")) {
                                name = line.split("=")[1];
                            }
                        }

                        //fallback
                        if (size == null) {
                            size = physicalSize;
                        }

                        tempFile.delete();
                        if (uniqName != null) {
                            TemplateProp prop = new TemplateProp(uniqName, container + File.separator + name, size, physicalSize, true, false);
                            tmpltInfos.put(uniqName, prop);
                        }
                    } catch (IOException ex) {
                        s_logger.debug("swiftListTemplate:Exception:" + ex.getMessage());
                        continue;
                    }
                } catch (IOException e) {
                    s_logger.debug("Failed to create templ file:" + e.toString());
                    continue;
                } catch (Exception e) {
                    s_logger.debug("Failed to get properties: " + e.toString());
                    continue;
                }
            }
        }
        return tmpltInfos;
    }

    Map<String, TemplateProp> s3ListTemplate(S3TO s3) {
        String bucket = s3.getBucketName();
        // List the objects in the source directory on S3
        final List<S3ObjectSummary> objectSummaries = S3Utils.listDirectory(s3, bucket, TEMPLATE_ROOT_DIR);
        if (objectSummaries == null) {
            return null;
        }
        Map<String, TemplateProp> tmpltInfos = new HashMap<String, TemplateProp>();
        for (S3ObjectSummary objectSummary : objectSummaries) {
            String key = objectSummary.getKey();
            // String installPath = StringUtils.substringBeforeLast(key,
            // S3Utils.SEPARATOR);
            String uniqueName = determineS3TemplateNameFromKey(key);
            // TODO: isPublic value, where to get?
            TemplateProp tInfo = new TemplateProp(uniqueName, key, objectSummary.getSize(), objectSummary.getSize(), true, false);
            tmpltInfos.put(uniqueName, tInfo);
        }
        return tmpltInfos;

    }

    Map<Long, TemplateProp> s3ListVolume(S3TO s3) {
        String bucket = s3.getBucketName();
        // List the objects in the source directory on S3
        final List<S3ObjectSummary> objectSummaries = S3Utils.listDirectory(s3, bucket, VOLUME_ROOT_DIR);
        if (objectSummaries == null) {
            return null;
        }
        Map<Long, TemplateProp> tmpltInfos = new HashMap<Long, TemplateProp>();
        for (S3ObjectSummary objectSummary : objectSummaries) {
            String key = objectSummary.getKey();
            // String installPath = StringUtils.substringBeforeLast(key,
            // S3Utils.SEPARATOR);
            Long id = determineS3VolumeIdFromKey(key);
            // TODO: how to get volume template name
            TemplateProp tInfo = new TemplateProp(id.toString(), key, objectSummary.getSize(), objectSummary.getSize(), true, false);
            tmpltInfos.put(id, tInfo);
        }
        return tmpltInfos;

    }

    private Answer execute(ListTemplateCommand cmd) {
        if (!_inSystemVM) {
            return new ListTemplateAnswer(null, null);
        }

        DataStoreTO store = cmd.getDataStore();
        if (store instanceof NfsTO) {
            NfsTO nfs = (NfsTO)store;
            String secUrl = nfs.getUrl();
            String root = getRootDir(secUrl, cmd.getNfsVersion());
            Map<String, TemplateProp> templateInfos = _dlMgr.gatherTemplateInfo(root);
            return new ListTemplateAnswer(secUrl, templateInfos);
        } else if (store instanceof SwiftTO) {
            SwiftTO swift = (SwiftTO)store;
            Map<String, TemplateProp> templateInfos = swiftListTemplate(swift);
            return new ListTemplateAnswer(swift.toString(), templateInfos);
        } else if (store instanceof S3TO) {
            S3TO s3 = (S3TO)store;
            Map<String, TemplateProp> templateInfos = s3ListTemplate(s3);
            return new ListTemplateAnswer(s3.getBucketName(), templateInfos);
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + store);
        }
    }

    private Answer execute(ListVolumeCommand cmd) {
        if (!_inSystemVM) {
            return new ListVolumeAnswer(cmd.getSecUrl(), null);
        }
        DataStoreTO store = cmd.getDataStore();
        if (store instanceof NfsTO) {
            String root = getRootDir(cmd.getSecUrl(), _nfsVersion);
            Map<Long, TemplateProp> templateInfos = _dlMgr.gatherVolumeInfo(root);
            return new ListVolumeAnswer(cmd.getSecUrl(), templateInfos);
        } else if (store instanceof S3TO) {
            S3TO s3 = (S3TO)store;
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

    private String deleteLocalFile(String fullPath) {
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("rm -rf " + fullPath);
        String result = command.execute();
        if (result != null) {
            String errMsg = "Failed to delete file " + fullPath + ", err=" + result;
            s_logger.warn(errMsg);
            return errMsg;
        }
        return null;
    }

    /**
    * allow *only one* setting of an outgoing destination at a time
    *
    * @destCidr the destination network that will be allowed for outgoing traffic.
    * @return any error message that might be helpful or <null> on success or when called anywhere but in the router VM.
    */
    public synchronized String allowOutgoingOnPrivate(String destCidr) {
        if (!_inSystemVM) {
            return null;
        }
        Script command = new Script("/bin/bash", s_logger);
        String intf = "eth1";
        command.add("-c");
        command.add("iptables -I OUTPUT -o " + intf + " -d " + destCidr + " -p tcp -m state --state NEW -m tcp  -j ACCEPT");

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
        if (result != null) {
            success = false;
        }

        return new Answer(cmd, success, result);
    }

    private UploadStatusAnswer execute(UploadStatusCommand cmd) {
        String entityUuid = cmd.getEntityUuid();
        if (uploadEntityStateMap.containsKey(entityUuid)) {
            UploadEntity uploadEntity = uploadEntityStateMap.get(entityUuid);
            if (uploadEntity.getUploadState() == UploadEntity.Status.ERROR) {
                uploadEntityStateMap.remove(entityUuid);
                return new UploadStatusAnswer(cmd, UploadStatus.ERROR, uploadEntity.getErrorMessage());
            } else if (uploadEntity.getUploadState() == UploadEntity.Status.COMPLETED) {
                UploadStatusAnswer answer = new UploadStatusAnswer(cmd, UploadStatus.COMPLETED);
                answer.setVirtualSize(uploadEntity.getVirtualSize());
                answer.setInstallPath(uploadEntity.getTmpltPath());
                answer.setPhysicalSize(uploadEntity.getPhysicalSize());
                answer.setDownloadPercent(100);
                if (uploadEntity.getOvfInformationTO() != null) {
                    answer.setOvfInformationTO(uploadEntity.getOvfInformationTO());
                }
                uploadEntityStateMap.remove(entityUuid);
                return answer;
            } else if (uploadEntity.getUploadState() == UploadEntity.Status.IN_PROGRESS) {
                UploadStatusAnswer answer = new UploadStatusAnswer(cmd, UploadStatus.IN_PROGRESS);
                long downloadedSize = FileUtils.sizeOfDirectory(new File(uploadEntity.getInstallPathPrefix()));
                int downloadPercent = (int)(100 * downloadedSize / uploadEntity.getContentLength());
                answer.setDownloadPercent(Math.min(downloadPercent, 100));
                return answer;
            }
        }
        return new UploadStatusAnswer(cmd, UploadStatus.UNKNOWN);
    }

    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        DataStoreTO store = cmd.getStore();
        if (store instanceof S3TO || store instanceof SwiftTO) {
            long infinity = Integer.MAX_VALUE;
            return new GetStorageStatsAnswer(cmd, infinity, 0L);
        }

        String rootDir = getRootDir(((NfsTO)store).getUrl(), cmd.getNfsVersion());
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
        if (obj.getPath() == null) {
            // account for those fake entries for NFS migration to object store
            return new Answer(cmd, true, "Object with null install path does not exist on image store , no need to delete");
        }
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
            NfsTO nfs = (NfsTO)dstore;
            String relativeTemplatePath = obj.getPath();
            String parent = getRootDir(nfs.getUrl(), _nfsVersion);

            if (relativeTemplatePath.startsWith(File.separator)) {
                relativeTemplatePath = relativeTemplatePath.substring(1);
            }

            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String absoluteTemplatePath = parent + relativeTemplatePath;
            File tmpltPath = new File(absoluteTemplatePath);
            File tmpltParent = null;
            if (tmpltPath.exists() && tmpltPath.isDirectory()) {
                tmpltParent = tmpltPath;
            } else if (absoluteTemplatePath.endsWith(File.separator + obj.getId())) {
                // the path ends with <account id>/<template id>, if upload fails
                tmpltParent = tmpltPath;
            } else {
                tmpltParent = tmpltPath.getParentFile();
            }

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
                    if (!found && f.getName().equals(_tmpltpp)) {
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
                        return new Answer(cmd, false, "Unable to delete file " + f.getName() + " under Template path " + relativeTemplatePath);
                    }
                }

                if (!found) {
                    details = "Can not find template.properties under " + tmpltParent.getName();
                    s_logger.debug(details);
                }
            }
            if (!tmpltParent.delete()) {
                details = "Unable to delete directory " + tmpltParent.getName() + " under Template path " + relativeTemplatePath;
                s_logger.debug(details);
                return new Answer(cmd, false, details);
            }
            return new Answer(cmd, true, null);
        } else if (dstore instanceof S3TO) {
            final S3TO s3 = (S3TO)dstore;
            final String path = obj.getPath();
            final String bucket = s3.getBucketName();
            try {
                S3Utils.deleteDirectory(s3, bucket, path);
                return new Answer(cmd, true, String.format("Deleted template %1$s from bucket %2$s.", path, bucket));
            } catch (Exception e) {
                final String errorMessage = String.format("Failed to delete template %1$s from bucket %2$s due to the following error: %3$s", path, bucket, e.getMessage());
                s_logger.error(errorMessage, e);
                return new Answer(cmd, false, errorMessage);
            }
        } else if (dstore instanceof SwiftTO) {
            SwiftTO swift = (SwiftTO)dstore;
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
            NfsTO nfs = (NfsTO)dstore;
            String relativeVolumePath = obj.getPath();
            String parent = getRootDir(nfs.getUrl(), _nfsVersion);

            if (relativeVolumePath.startsWith(File.separator)) {
                relativeVolumePath = relativeVolumePath.substring(1);
            }

            if (!parent.endsWith(File.separator)) {
                parent += File.separator;
            }
            String absoluteVolumePath = parent + relativeVolumePath;
            File volPath = new File(absoluteVolumePath);
            File tmpltParent = null;
            if (volPath.exists() && volPath.isDirectory()) {
                // for vmware, absoluteVolumePath represents a directory where volume files are located.
                tmpltParent = volPath;
            } else if (absoluteVolumePath.endsWith(File.separator + obj.getId())) {
                // the path ends with <account id>/<volume id>, if upload fails
                tmpltParent = volPath;
            } else {
                // for other hypervisors, the volume .vhd or .qcow2 file path is passed
                tmpltParent = new File(absoluteVolumePath).getParentFile();
            }
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
                        return new Answer(cmd, false, "Unable to delete file " + f.getName() + " under Volume path " + tmpltParent.getPath());
                    }
                }
                if (!found) {
                    details = "Can not find volume.properties under " + tmpltParent.getName();
                    s_logger.debug(details);
                }
            }
            if (!tmpltParent.delete()) {
                details = "Unable to delete directory " + tmpltParent.getName() + " under Volume path " + tmpltParent.getPath();
                s_logger.debug(details);
                return new Answer(cmd, false, details);
            }
            return new Answer(cmd, true, null);
        } else if (dstore instanceof S3TO) {
            final S3TO s3 = (S3TO)dstore;
            final String path = obj.getPath();
            final String bucket = s3.getBucketName();
            try {
                S3Utils.deleteDirectory(s3, bucket, path);
                return new Answer(cmd, true, String.format("Deleted volume %1%s from bucket %2$s.", path, bucket));
            } catch (Exception e) {
                final String errorMessage = String.format("Failed to delete volume %1$s from bucket %2$s due to the following error: %3$s", path, bucket, e.getMessage());
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
            String result = swiftDelete((SwiftTO)dstore, "V-" + volumeId.toString(), filename);
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

    private String getDir(String secUrl, String nfsVersion) {
        try {
            URI uri = new URI(secUrl);
            String dir = mountUri(uri, nfsVersion);
            return _parent + "/" + dir;
        } catch (Exception e) {
            String msg = "GetRootDir for " + secUrl + " failed due to " + e.toString();
            s_logger.error(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    synchronized public String getRootDir(String secUrl, String nfsVersion) {
        if (!_inSystemVM) {
            return _parent;
        }
        try {
            URI uri = new URI(secUrl);
            String dir = mountUri(uri, nfsVersion);
            return _parent + "/" + dir;
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

        return (long)(Double.parseDouble(size.substring(0, size.length() - 1)) * multiplier);
    }

    @Override
    public Type getType() {
        if (SecondaryStorageVm.Role.templateProcessor.toString().equals(_role)) {
            return Host.Type.SecondaryStorage;
        }

        return Host.Type.SecondaryStorageCmdExecutor;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingStorageCommand(Host.Type.Storage, id, new HashMap<String, Boolean>());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _eth1ip = (String)params.get("eth1ip");
        _eth1mask = (String)params.get("eth1mask");
        if (_eth1ip != null) { // can only happen inside service vm
            params.put("private.network.device", "eth1");
        } else {
            s_logger.warn("eth1ip parameter has not been configured, assuming that we are not inside a system vm");
        }
        String eth2ip = (String)params.get("eth2ip");
        if (eth2ip != null) {
            params.put("public.network.device", "eth2");
        }
        _publicIp = (String)params.get("eth2ip");
        _hostname = (String)params.get("name");

        String inSystemVM = (String)params.get("secondary.storage.vm");
        if (inSystemVM == null || "true".equalsIgnoreCase(inSystemVM)) {
            s_logger.debug("conf secondary.storage.vm is true, act as if executing in SSVM");
            _inSystemVM = true;
        }

        _storageIp = (String)params.get("storageip");
        if (_storageIp == null && _inSystemVM) {
            s_logger.warn("There is no storageip in /proc/cmdline, something wrong!");
        }
        _storageNetmask = (String)params.get("storagenetmask");
        _storageGateway = (String)params.get("storagegateway");
        super.configure(name, params);

        _params = params;
        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 1440) * 1000;

        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        configureStorageLayerClass(params);

        if (_inSystemVM) {
            _storage.mkdirs(_parent);
        }

        _configSslScr = Script.findScript(getDefaultScriptsDir(), "config_ssl.sh");
        if (_configSslScr != null) {
            s_logger.info("config_ssl.sh found in " + _configSslScr);
        }

        _configAuthScr = Script.findScript(getDefaultScriptsDir(), "config_auth.sh");
        if (_configAuthScr != null) {
            s_logger.info("config_auth.sh found in " + _configAuthScr);
        }

        _configIpFirewallScr = Script.findScript(getDefaultScriptsDir(), "ipfirewall.sh");
        if (_configIpFirewallScr != null) {
            s_logger.info("_configIpFirewallScr found in " + _configIpFirewallScr);
        }

        createTemplateFromSnapshotXenScript = Script.findScript(getDefaultScriptsDir(), "create_privatetemplate_from_snapshot_xen.sh");
        if (createTemplateFromSnapshotXenScript == null) {
            throw new ConfigurationException("create_privatetemplate_from_snapshot_xen.sh not found in " + getDefaultScriptsDir());
        }

        _role = (String)params.get("role");
        if (_role == null) {
            _role = SecondaryStorageVm.Role.templateProcessor.toString();
        }
        s_logger.info("Secondary storage runs in role " + _role);

        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        _dc = (String)params.get("zone");
        if (_dc == null) {
            throw new ConfigurationException("Unable to find the zone");
        }
        _pod = (String)params.get("pod");

        _instance = (String)params.get("instance");

        if (!_inSystemVM) {
            _parent = (String)params.get("mount.path");
        }

        if (_inSystemVM) {
            _localgw = (String)params.get("localgw");
            if (_localgw != null) { // can only happen inside service vm
                String mgmtHosts = (String)params.get("host");
                for (final String mgmtHost : mgmtHosts.split(",")) {
                    addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, mgmtHost);
                }

                String internalDns1 = (String)params.get("internaldns1");
                if (internalDns1 == null) {
                    s_logger.warn("No DNS entry found during configuration of NfsSecondaryStorage");
                } else {
                    addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, internalDns1);
                }

                String internalDns2 = (String)params.get("internaldns2");
                if (internalDns2 != null) {
                    addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, internalDns2);
                }

            }

            startAdditionalServices();
            _params.put("install.numthreads", "50");
            _params.put("secondary.storage.vm", "true");
            _nfsVersion = retrieveNfsVersionFromParams(params);
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

    protected void configureStorageLayerClass(Map<String, Object> params) throws ConfigurationException {
        String value;
        if (_storage == null) {
            value = (String)params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                value = "com.cloud.storage.JavaStorageLayer";
            }

            try {
                Class<?> clazz = Class.forName(value);
                _storage = (StorageLayer)clazz.newInstance();
                _storage.configure("StorageLayer", params);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to find class " + value);
            } catch (InstantiationException e) {
                throw new ConfigurationException("Unable to find class " + value);
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Unable to find class " + value);
            }
        }
    }

    private void startAdditionalServices() {
        if (!_inSystemVM) {
            return;
        }
        Script command = new Script("/bin/systemctl", s_logger);
        command.add("restart");
        command.add("ssh");
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
        s_logger.debug("addRouteToInternalIp: localgw=" + localgw + ", eth1ip=" + eth1ip + ", eth1mask=" + eth1mask + ",destIp=" + destIpOrCidr);
        if (destIpOrCidr == null) {
            s_logger.debug("addRouteToInternalIp: destIp is null");
            return;
        }
        if (!NetUtils.isValidIp4(destIpOrCidr) && !NetUtils.isValidIp4Cidr(destIpOrCidr)) {
            s_logger.warn(" destIp is not a valid ip address or cidr destIp=" + destIpOrCidr);
            return;
        }
        boolean inSameSubnet = false;
        if (NetUtils.isValidIp4(destIpOrCidr)) {
            if (eth1ip != null && eth1mask != null) {
                inSameSubnet = NetUtils.sameSubnet(eth1ip, destIpOrCidr, eth1mask);
            } else {
                s_logger.warn("addRouteToInternalIp: unable to determine same subnet: _eth1ip=" + eth1ip + ", dest ip=" + destIpOrCidr + ", _eth1mask=" + eth1mask);
            }
        } else {
            inSameSubnet = NetUtils.isNetworkAWithinNetworkB(destIpOrCidr, NetUtils.ipAndNetMaskToCidr(eth1ip, eth1mask));
        }
        if (inSameSubnet) {
            s_logger.debug("addRouteToInternalIp: dest ip " + destIpOrCidr + " is in the same subnet as eth1 ip " + eth1ip);
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

    private void configureSSL(String prvkeyPath, String prvCertPath, String certChainPath, String rootCACert) {
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
        if (rootCACert != null) {
            command.add("-u", rootCACert);
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

    /**
     * Mount remote device named on local file system on subfolder of _parent
     * field.
     * <p>
     *
     * Supported schemes are "nfs" and "cifs".
     * <p>
     *
     * CIFS parameters are documented with mount.cifs at
     * http://linux.die.net/man/8/mount.cifs
     * For simplicity, when a URI is used to specify a CIFS share,
     * options such as domain,user,password are passed as query parameters.
     *
     * @param uri
     *            crresponding to the remote device. Will throw for unsupported
     *            scheme.
     * @param nfsVersion NFS version to use in mount command
     * @return name of folder in _parent that device was mounted.
     * @throws UnknownHostException
     */
    protected String mountUri(URI uri, String nfsVersion) throws UnknownHostException {
        String uriHostIp = getUriHostIp(uri);
        String nfsPath = uriHostIp + ":" + uri.getPath();

        // Single means of calculating mount directory regardless of scheme
        String dir = UUID.nameUUIDFromBytes(nfsPath.getBytes(com.cloud.utils.StringUtils.getPreferredCharset())).toString();
        String localRootPath = _parent + "/" + dir;

        // remote device syntax varies by scheme.
        String remoteDevice;
        if (uri.getScheme().equals("cifs")) {
            remoteDevice = "//" + uriHostIp + uri.getPath();
            s_logger.debug("Mounting device with cifs-style path of " + remoteDevice);
        } else {
            remoteDevice = nfsPath;
            s_logger.debug("Mounting device with nfs-style path of " + remoteDevice);
        }
        mount(localRootPath, remoteDevice, uri, nfsVersion);
        return dir;
    }

    protected void mount(String localRootPath, String remoteDevice, URI uri, String nfsVersion) {
        s_logger.debug("mount " + uri.toString() + " on " + localRootPath + ((nfsVersion != null) ? " nfsVersion=" + nfsVersion : ""));
        ensureLocalRootPathExists(localRootPath, uri);

        if (mountExists(localRootPath, uri)) {
            return;
        }

        attemptMount(localRootPath, remoteDevice, uri, nfsVersion);

        // XXX: Adding the check for creation of snapshots dir here. Might have
        // to move it somewhere more logical later.
        checkForSnapshotsDir(localRootPath);
        checkForVolumesDir(localRootPath);
    }

    protected void attemptMount(String localRootPath, String remoteDevice, URI uri, String nfsVersion) {
        String result;
        s_logger.debug("Make cmdline call to mount " + remoteDevice + " at " + localRootPath + " based on uri " + uri + ((nfsVersion != null) ? " nfsVersion=" + nfsVersion : ""));
        Script command = new Script(!_inSystemVM, "mount", _timeout, s_logger);

        String scheme = uri.getScheme().toLowerCase();
        command.add("-t", scheme);

        if (scheme.equals("nfs")) {
            if ("Mac OS X".equalsIgnoreCase(System.getProperty("os.name"))) {
                // See http://wiki.qnap.com/wiki/Mounting_an_NFS_share_from_OS_X
                command.add("-o", "resvport");
            }
            if (_inSystemVM) {
                command.add("-o", "soft,timeo=133,retrans=2147483647,tcp,acdirmax=0,acdirmin=0" + ((nfsVersion != null) ? ",vers=" + nfsVersion : ""));
            }
        } else if (scheme.equals("cifs")) {
            String extraOpts = parseCifsMountOptions(uri);

            // nfs acdirmax / acdirmin correspoonds to CIFS actimeo (see
            // http://linux.die.net/man/8/mount.cifs)
            // no equivalent to nfs timeo, retrans or tcp in CIFS
            // todo: allow security mode to be set.
            command.add("-o", extraOpts + "soft,actimeo=0");
        } else {
            String errMsg = "Unsupported storage device scheme " + scheme + " in uri " + uri.toString();
            s_logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        command.add(remoteDevice);
        command.add(localRootPath);
        result = command.execute();
        if (result != null) {
            // Fedora Core 12 errors out with any -o option executed from java
            String errMsg = "Unable to mount " + remoteDevice + " at " + localRootPath + " due to " + result;
            s_logger.error(errMsg);
            File file = new File(localRootPath);
            if (file.exists()) {
                file.delete();
            }
            throw new CloudRuntimeException(errMsg);
        }
        s_logger.debug("Successfully mounted " + remoteDevice + " at " + localRootPath);
    }

    protected String parseCifsMountOptions(URI uri) {
        List<NameValuePair> args = URLEncodedUtils.parse(uri, "UTF-8");
        boolean foundUser = false;
        boolean foundPswd = false;
        StringBuilder extraOpts = new StringBuilder();
        for (NameValuePair nvp : args) {
            String name = nvp.getName();
            if (name.equals("user")) {
                foundUser = true;
                s_logger.debug("foundUser is" + foundUser);
            } else if (name.equals("password")) {
                foundPswd = true;
                s_logger.debug("password is present in uri");
            }

            extraOpts.append(name + "=" + nvp.getValue() + ",");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.error("extraOpts now " + extraOpts);
        }

        if (!foundUser || !foundPswd) {
            String errMsg = "Missing user and password from URI. Make sure they" + "are in the query string and separated by '&'.  E.g. "
                    + "cifs://example.com/some_share?user=foo&password=bar";
            s_logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        return extraOpts.toString();
    }

    protected boolean mountExists(String localRootPath, URI uri) {
        Script script = null;
        script = new Script(!_inSystemVM, "mount", _timeout, s_logger);

        List<String> res = new ArrayList<String>();
        PathParser parser = new PathParser(localRootPath);
        script.execute(parser);
        res.addAll(parser.getPaths());
        for (String s : res) {
            if (s.contains(localRootPath)) {
                s_logger.debug("Some device already mounted at " + localRootPath + ", no need to mount " + uri.toString());
                return true;
            }
        }
        return false;
    }

    protected void ensureLocalRootPathExists(String localRootPath, URI uri) {
        s_logger.debug("making available " + localRootPath + " on " + uri.toString());
        File file = new File(localRootPath);
        s_logger.debug("local folder for mount will be " + file.getPath());
        if (!file.exists()) {
            s_logger.debug("create mount point: " + file.getPath());
            _storage.mkdir(file.getPath());

            // Need to check after mkdir to allow O/S to complete operation
            if (!file.exists()) {
                String errMsg = "Unable to create local folder for: " + localRootPath + " in order to mount " + uri.toString();
                s_logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        }
    }

    protected String getUriHostIp(URI uri) throws UnknownHostException {
        String nfsHost = uri.getHost();
        InetAddress nfsHostAddr = InetAddress.getByName(nfsHost);
        String nfsHostIp = nfsHostAddr.getHostAddress();
        s_logger.info("Determined host " + nfsHost + " corresponds to IP " + nfsHostIp);
        return nfsHostIp;
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
        if (_publicIp != null) {
            cmd.setPublicIpAddress(_publicIp);
        }

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
        return new StartupCommand[] {cmd};
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

    private String getScriptLocation(UploadEntity.ResourceType resourceType) {

        String scriptsDir = (String)_params.get("template.scripts.dir");
        if (scriptsDir == null) {
            scriptsDir = "scripts/storage/secondary";
        }
        String scriptname = null;
        if (resourceType == UploadEntity.ResourceType.VOLUME) {
            scriptname = "createvolume.sh";
        } else if (resourceType == UploadEntity.ResourceType.TEMPLATE) {
            scriptname = "createtmplt.sh";
        } else {
            throw new InvalidParameterValueException("cannot find script for resource type: " + resourceType);
        }
        return Script.findScript(scriptsDir, scriptname);
    }

    public UploadEntity createUploadEntity(String uuid, String metadata, long contentLength) {
        TemplateOrVolumePostUploadCommand cmd = getTemplateOrVolumePostUploadCmd(metadata);
        UploadEntity uploadEntity = null;
        if (cmd == null) {
            String errorMessage = "unable decode and deserialize metadata.";
            updateStateMapWithError(uuid, errorMessage);
            throw new InvalidParameterValueException(errorMessage);
        } else {
            uuid = cmd.getEntityUUID();
            processTimeout = cmd.getProcessTimeout();
            if (isOneTimePostUrlUsed(cmd)) {
                uploadEntity = uploadEntityStateMap.get(uuid);
                StringBuilder errorMessage = new StringBuilder("The one time post url is already used");
                if (uploadEntity != null) {
                    errorMessage.append(" and the upload is in ").append(uploadEntity.getUploadState()).append(" state.");
                }
                throw new InvalidParameterValueException(errorMessage.toString());
            }
            int maxSizeInGB = Integer.parseInt(cmd.getMaxUploadSize());
            int contentLengthInGB = getSizeInGB(contentLength);
            if (contentLengthInGB > maxSizeInGB) {
                String errorMessage = "Maximum file upload size exceeded. Content Length received: " + contentLengthInGB + "GB. Maximum allowed size: " + maxSizeInGB + "GB.";
                updateStateMapWithError(uuid, errorMessage);
                throw new InvalidParameterValueException(errorMessage);
            }
            checkSecondaryStorageResourceLimit(cmd, contentLengthInGB);
            try {
                String absolutePath = cmd.getAbsolutePath();
                uploadEntity = new UploadEntity(uuid, cmd.getEntityId(), UploadEntity.Status.IN_PROGRESS, cmd.getName(), absolutePath);
                uploadEntity.setMetaDataPopulated(true);
                uploadEntity.setResourceType(UploadEntity.ResourceType.valueOf(cmd.getType()));
                uploadEntity.setProcessTimeout(processTimeout);
                uploadEntity.setFormat(Storage.ImageFormat.valueOf(cmd.getImageFormat()));
                //relative path with out ssvm mount info.
                uploadEntity.setTemplatePath(absolutePath);
                String dataStoreUrl = cmd.getDataTo();
                String installPathPrefix = this.getRootDir(dataStoreUrl, cmd.getNfsVersion()) + File.separator + absolutePath;
                uploadEntity.setInstallPathPrefix(installPathPrefix);
                uploadEntity.setHvm(cmd.getRequiresHvm());
                uploadEntity.setChksum(cmd.getChecksum());
                uploadEntity.setMaxSizeInGB(maxSizeInGB);
                uploadEntity.setDescription(cmd.getDescription());
                uploadEntity.setContentLength(contentLength);
                // create a install dir
                if (!_storage.exists(installPathPrefix)) {
                    _storage.mkdir(installPathPrefix);
                }
                uploadEntityStateMap.put(uuid, uploadEntity);
            } catch (Exception e) {
                //upload entity will be null incase an exception occurs and the handler will not proceed.
                s_logger.error("exception occurred while creating upload entity ", e);
                updateStateMapWithError(uuid, e.getMessage());
            }
        }
        return uploadEntity;
    }

    private synchronized void checkSecondaryStorageResourceLimit(TemplateOrVolumePostUploadCommand cmd, int contentLengthInGB) {
        String rootDir = this.getRootDir(cmd.getDataTo(), cmd.getNfsVersion()) + File.separator;
        long accountId = cmd.getAccountId();

        long accountTemplateDirSize = 0;
        File accountTemplateDir = new File(rootDir + getTemplatePathForAccount(accountId));
        if (accountTemplateDir.exists()) {
            accountTemplateDirSize = FileUtils.sizeOfDirectory(accountTemplateDir);
        }
        long accountVolumeDirSize = 0;
        File accountVolumeDir = new File(rootDir + getVolumePathForAccount(accountId));
        if (accountVolumeDir.exists()) {
            accountVolumeDirSize = FileUtils.sizeOfDirectory(accountVolumeDir);
        }
        long accountSnapshotDirSize = 0;
        File accountSnapshotDir = new File(rootDir + getSnapshotPathForAccount(accountId));
        if (accountSnapshotDir.exists()) {
            accountSnapshotDirSize = FileUtils.sizeOfDirectory(accountSnapshotDir);
        }
        s_logger.debug(
                "accountTemplateDirSize: " + accountTemplateDirSize + " accountSnapshotDirSize: " + accountSnapshotDirSize + " accountVolumeDirSize: " + accountVolumeDirSize);

        int accountDirSizeInGB = getSizeInGB(accountTemplateDirSize + accountSnapshotDirSize + accountVolumeDirSize);
        long defaultMaxSecondaryStorageInGB = cmd.getDefaultMaxSecondaryStorageInGB();

        if (defaultMaxSecondaryStorageInGB != Resource.RESOURCE_UNLIMITED && (accountDirSizeInGB + contentLengthInGB) > defaultMaxSecondaryStorageInGB) {
            s_logger.error("accountDirSizeInGb: " + accountDirSizeInGB + " defaultMaxSecondaryStorageInGB: " + defaultMaxSecondaryStorageInGB + " contentLengthInGB:"
                    + contentLengthInGB); // extra attention
            String errorMessage = "Maximum number of resources of type secondary_storage for account/project has exceeded";
            updateStateMapWithError(cmd.getEntityUUID(), errorMessage);
            throw new InvalidParameterValueException(errorMessage);
        }
    }

    private String getVolumePathForAccount(long accountId) {
        return TemplateConstants.DEFAULT_VOLUME_ROOT_DIR + "/" + accountId;
    }

    private String getTemplatePathForAccount(long accountId) {
        return TemplateConstants.DEFAULT_TMPLT_ROOT_DIR + "/" + TemplateConstants.DEFAULT_TMPLT_FIRST_LEVEL_DIR + accountId;
    }

    private String getSnapshotPathForAccount(long accountId) {
        return TemplateConstants.DEFAULT_SNAPSHOT_ROOT_DIR + "/" + accountId;
    }

    private boolean isOneTimePostUrlUsed(TemplateOrVolumePostUploadCommand cmd) {
        String uuid = cmd.getEntityUUID();
        String uploadPath = this.getRootDir(cmd.getDataTo(), cmd.getNfsVersion()) + File.separator + cmd.getAbsolutePath();
        return uploadEntityStateMap.containsKey(uuid) || new File(uploadPath).exists();
    }

    private int getSizeInGB(long sizeInBytes) {
        return (int)Math.ceil(sizeInBytes * 1.0d / (1024 * 1024 * 1024));
    }

    public String postUpload(String uuid, String filename, long processTimeout) {
        UploadEntity uploadEntity = uploadEntityStateMap.get(uuid);
        int installTimeoutPerGig = 180 * 60 * 1000;

        String resourcePath = uploadEntity.getInstallPathPrefix();
        String finalResourcePath = uploadEntity.getTmpltPath(); // template download
        UploadEntity.ResourceType resourceType = uploadEntity.getResourceType();

        String fileSavedTempLocation = uploadEntity.getInstallPathPrefix() + "/" + filename;
        String dummyFileName = "dummy." + uploadEntity.getFormat().getFileExtension();
        if (ImageStoreUtil.isCompressedExtension(filename)) {
            String uploadedFileExtension = FilenameUtils.getExtension(filename);
            dummyFileName += "." + uploadedFileExtension;
        }

        String formatError = ImageStoreUtil.checkTemplateFormat(fileSavedTempLocation, dummyFileName);
        if (StringUtils.isNotBlank(formatError)) {
            String errorString = "File type mismatch between uploaded file and selected format. Selected file format: " + uploadEntity.getFormat() + ". Received: " + formatError;
            s_logger.error(errorString);
            return errorString;
        }

        int imgSizeGigs = getSizeInGB(_storage.getSize(fileSavedTempLocation));
        int maxSize = uploadEntity.getMaxSizeInGB();
        if (imgSizeGigs > maxSize) {
            String errorMessage = "Maximum file upload size exceeded. Physical file size: " + imgSizeGigs + "GB. Maximum allowed size: " + maxSize + "GB.";
            s_logger.error(errorMessage);
            return errorMessage;
        }
        imgSizeGigs++; // add one just in case
        long timeout = (long)imgSizeGigs * installTimeoutPerGig;
        Script scr = new Script(getScriptLocation(resourceType), timeout, s_logger);
        scr.add("-s", Integer.toString(imgSizeGigs));
        scr.add("-S", Long.toString(UploadEntity.s_maxTemplateSize));
        if (uploadEntity.getDescription() != null && uploadEntity.getDescription().length() > 1) {
            scr.add("-d", uploadEntity.getDescription());
        }
        if (uploadEntity.isHvm()) {
            scr.add("-h");
        }
        String checkSum = uploadEntity.getChksum();
        if (StringUtils.isNotBlank(checkSum)) {
            scr.add("-c", checkSum);
        }

        // add options common to ISO and template
        String extension = uploadEntity.getFormat().getFileExtension();
        String templateName = "";
        if (extension.equals("iso")) {
            templateName = uploadEntity.getUuid().trim().replace(" ", "_");
        } else {
            try {
                templateName = UUID.nameUUIDFromBytes((uploadEntity.getFilename() + System.currentTimeMillis()).getBytes("UTF-8")).toString();
            } catch (UnsupportedEncodingException e) {
                templateName = uploadEntity.getUuid().trim().replace(" ", "_");
            }
        }

        // run script to mv the temporary template file to the final template
        // file
        String templateFilename = templateName + "." + extension;
        uploadEntity.setTemplatePath(finalResourcePath + "/" + templateFilename);
        scr.add("-n", templateFilename);

        scr.add("-t", resourcePath);
        scr.add("-f", fileSavedTempLocation); // this is the temporary
        // template file downloaded
        if (uploadEntity.getChksum() != null && uploadEntity.getChksum().length() > 1) {
            scr.add("-c", uploadEntity.getChksum());
        }
        scr.add("-u"); // cleanup
        String result;
        result = scr.execute();

        if (result != null) {
            return result;
        }

        // Set permissions for the downloaded template
        File downloadedTemplate = new File(resourcePath + "/" + templateFilename);
        _storage.setWorldReadableAndWriteable(downloadedTemplate);

        // Set permissions for template/volume.properties
        String propertiesFile = resourcePath;
        if (resourceType == UploadEntity.ResourceType.TEMPLATE) {
            propertiesFile += "/template.properties";
        } else {
            propertiesFile += "/volume.properties";
        }
        File templateProperties = new File(propertiesFile);
        _storage.setWorldReadableAndWriteable(templateProperties);

        TemplateLocation loc = new TemplateLocation(_storage, resourcePath);
        try {
            loc.create(uploadEntity.getEntityId(), true, uploadEntity.getFilename());
        } catch (IOException e) {
            s_logger.warn("Something is wrong with template location " + resourcePath, e);
            loc.purge();
            return "Unable to upload due to " + e.getMessage();
        }

        Map<String, Processor> processors = _dlMgr.getProcessors();
        for (Processor processor : processors.values()) {
            FormatInfo info = null;
            try {
                info = processor.process(resourcePath, null, templateName, processTimeout * 1000);
            } catch (InternalErrorException e) {
                s_logger.error("Template process exception ", e);
                return e.toString();
            }
            if (info != null) {
                loc.addFormat(info);
                uploadEntity.setVirtualSize(info.virtualSize);
                uploadEntity.setPhysicalSize(info.size);
                if (info.ovfInformationTO != null) {
                    uploadEntity.setOvfInformationTO(info.ovfInformationTO);
                }
                break;
            }
        }

        if (!loc.save()) {
            s_logger.warn("Cleaning up because we're unable to save the formats");
            loc.purge();
        }
        uploadEntity.setStatus(UploadEntity.Status.COMPLETED);
        uploadEntityStateMap.put(uploadEntity.getUuid(), uploadEntity);
        return null;
    }

    private String getPostUploadPSK() {
        if (_ssvmPSK == null) {
            try {
                _ssvmPSK = FileUtils.readFileToString(new File(POST_UPLOAD_KEY_LOCATION), "utf-8");
            } catch (IOException e) {
                s_logger.debug("Error while reading SSVM PSK from location " + POST_UPLOAD_KEY_LOCATION, e);
            }
        }
        return _ssvmPSK;
    }

    public void updateStateMapWithError(String uuid, String errorMessage) {
        UploadEntity uploadEntity = null;
        if (uploadEntityStateMap.get(uuid) != null) {
            uploadEntity = uploadEntityStateMap.get(uuid);
        } else {
            uploadEntity = new UploadEntity();
        }
        uploadEntity.setStatus(UploadEntity.Status.ERROR);
        uploadEntity.setErrorMessage(errorMessage);
        uploadEntityStateMap.put(uuid, uploadEntity);
    }

    public void validatePostUploadRequest(String signature, String metadata, String timeout, String hostname, long contentLength, String uuid)
            throws InvalidParameterValueException {
        // check none of the params are empty
        if (StringUtils.isAnyEmpty(signature, metadata, timeout)) {
            updateStateMapWithError(uuid, "signature, metadata and expires are compulsory fields.");
            throw new InvalidParameterValueException("signature, metadata and expires are compulsory fields.");
        }

        //check that contentLength exists and is greater than zero
        if (contentLength <= 0) {
            throw new InvalidParameterValueException("content length is not set in the request or has invalid value.");
        }

        //validate signature
        String fullUrl = "https://" + hostname + "/upload/" + uuid;
        String computedSignature = EncryptionUtil.generateSignature(metadata + fullUrl + timeout, getPostUploadPSK());
        boolean isSignatureValid = computedSignature.equals(signature);
        if (!isSignatureValid) {
            updateStateMapWithError(uuid, "signature validation failed.");
            throw new InvalidParameterValueException("signature validation failed.");
        }

        //validate timeout
        DateTime timeoutDateTime = DateTime.parse(timeout, ISODateTimeFormat.dateTime());
        if (timeoutDateTime.isBeforeNow()) {
            updateStateMapWithError(uuid, "request not valid anymore.");
            throw new InvalidParameterValueException("request not valid anymore.");
        }
    }

    private TemplateOrVolumePostUploadCommand getTemplateOrVolumePostUploadCmd(String metadata) {
        TemplateOrVolumePostUploadCommand cmd = null;
        try {
            Gson gson = new GsonBuilder().create();
            cmd = gson.fromJson(EncryptionUtil.decodeData(metadata, getPostUploadPSK()), TemplateOrVolumePostUploadCommand.class);
        } catch (Exception ex) {
            s_logger.error("exception while decoding and deserialising metadata", ex);
        }
        return cmd;
    }

}
