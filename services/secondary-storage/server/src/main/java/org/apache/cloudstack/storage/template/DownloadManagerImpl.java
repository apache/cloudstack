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
package org.apache.cloudstack.storage.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.storage.OVFPropertyTO;
import com.cloud.storage.template.Processor;
import com.cloud.storage.template.S3TemplateDownloader;
import com.cloud.storage.template.TemplateDownloader;
import com.cloud.storage.template.TemplateLocation;
import com.cloud.storage.template.MetalinkTemplateDownloader;
import com.cloud.storage.template.HttpTemplateDownloader;
import com.cloud.storage.template.LocalTemplateDownloader;
import com.cloud.storage.template.ScpTemplateDownloader;
import com.cloud.storage.template.TemplateProp;
import com.cloud.storage.template.OVAProcessor;
import com.cloud.storage.template.IsoProcessor;
import com.cloud.storage.template.QCOW2Processor;
import com.cloud.storage.template.VmdkProcessor;
import com.cloud.storage.template.RawImageProcessor;
import com.cloud.storage.template.TARProcessor;
import com.cloud.storage.template.VhdProcessor;
import com.cloud.storage.template.TemplateConstants;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand.RequestType;
import org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource;
import org.apache.cloudstack.storage.resource.SecondaryStorageResource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.utils.net.Proxy;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.storage.template.TemplateDownloader.DownloadCompleteCallback;
import com.cloud.storage.template.TemplateDownloader.Status;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.utils.storage.QCOW2Utils;
import org.apache.cloudstack.utils.security.ChecksumValue;
import org.apache.cloudstack.utils.security.DigestHelper;

public class DownloadManagerImpl extends ManagerBase implements DownloadManager {
    private String _name;
    StorageLayer _storage;
    public Map<String, Processor> _processors;
    private long _processTimeout;
    private Integer _nfsVersion;

    public class Completion implements DownloadCompleteCallback {
        private final String jobId;

        public Completion(String jobId) {
            this.jobId = jobId;
        }

        @Override
        public void downloadComplete(Status status) {
            setDownloadStatus(jobId, status);
        }
    }

    @Override
    public Map<String, Processor> getProcessors() {
        return _processors;
    }

    private static class DownloadJob {
        private final TemplateDownloader td;
        private final String tmpltName;
        private final boolean hvm;
        private final ImageFormat format;
        private String tmpltPath;
        private final String description;
        private String checksum;
        private final String installPathPrefix;
        private long templatesize;
        private long templatePhysicalSize;
        private final long id;
        private final ResourceType resourceType;
        private List<OVFPropertyTO> ovfProperties;

        public DownloadJob(TemplateDownloader td, String jobId, long id, String tmpltName, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum,
                String installPathPrefix, ResourceType resourceType) {
            super();
            this.td = td;
            this.tmpltName = tmpltName;
            this.format = format;
            this.hvm = hvm;
            this.description = descr;
            this.checksum = cksum;
            this.installPathPrefix = installPathPrefix;
            this.templatesize = 0;
            this.id = id;
            this.resourceType = resourceType;
        }

        public String getDescription() {
            return description;
        }

        public String getChecksum() {
            return checksum;
        }

        public TemplateDownloader getTemplateDownloader() {
            return td;
        }

        public String getTmpltName() {
            return tmpltName;
        }

        public ImageFormat getFormat() {
            return format;
        }

        public boolean isHvm() {
            return hvm;
        }

        public long getId() {
            return id;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        public void setTmpltPath(String tmpltPath) {
            this.tmpltPath = tmpltPath;
        }

        public String getTmpltPath() {
            return tmpltPath;
        }

        public String getInstallPathPrefix() {
            return installPathPrefix;
        }

        public void cleanup() {
            if (td != null) {
                String dnldPath = td.getDownloadLocalPath();
                if (dnldPath != null) {
                    File f = new File(dnldPath);
                    File dir = f.getParentFile();
                    f.delete();
                    if (dir != null) {
                        dir.delete();
                    }
                }
            }

        }

        public void setTemplatesize(long templatesize) {
            this.templatesize = templatesize;
        }

        public long getTemplatesize() {
            return templatesize;
        }

        public void setTemplatePhysicalSize(long templatePhysicalSize) {
            this.templatePhysicalSize = templatePhysicalSize;
        }

        public long getTemplatePhysicalSize() {
            return templatePhysicalSize;
        }

        public void setCheckSum(String checksum) {
            this.checksum = checksum;
        }

        public List<OVFPropertyTO> getOvfProperties() {
            return ovfProperties;
        }

        public void setOvfProperties(List<OVFPropertyTO> ovfProperties) {
            this.ovfProperties = ovfProperties;
        }
    }

    public static final Logger s_logger = Logger.getLogger(DownloadManagerImpl.class);
    private String _templateDir;
    private String _volumeDir;
    private String createTmpltScr;
    private String createVolScr;

    private ExecutorService threadPool;

    private final Map<String, DownloadJob> jobs = new ConcurrentHashMap<String, DownloadJob>();
    private String listTmpltScr;
    private String listVolScr;
    private int installTimeoutPerGig = 180 * 60 * 1000;

    public void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    public void setStorageLayer(StorageLayer storage) {
        _storage = storage;
    }

    /**
     * Get notified of change of job status. Executed in context of downloader
     * thread
     *
     * @param jobId
     *            the id of the job
     * @param status
     *            the status of the job
     */
    public void setDownloadStatus(String jobId, Status status) {
        DownloadJob dj = jobs.get(jobId);
        if (dj == null) {
            s_logger.warn("setDownloadStatus for jobId: " + jobId + ", status=" + status + " no job found");
            return;
        }
        TemplateDownloader td = dj.getTemplateDownloader();
        s_logger.info("Download Completion for jobId: " + jobId + ", status=" + status);
        s_logger.info("local: " + td.getDownloadLocalPath() + ", bytes=" + td.getDownloadedBytes() + ", error=" + td.getDownloadError() + ", pct=" +
                td.getDownloadPercent());

        switch (status) {
        case ABORTED:
        case NOT_STARTED:
        case UNRECOVERABLE_ERROR:
            // TODO
            dj.cleanup();
            break;
        case UNKNOWN:
            return;
        case IN_PROGRESS:
            s_logger.info("Resuming jobId: " + jobId + ", status=" + status);
            td.setResume(true);
            threadPool.execute(td);
            break;
        case RECOVERABLE_ERROR:
            threadPool.execute(td);
            break;
        case DOWNLOAD_FINISHED:
            if(td instanceof S3TemplateDownloader) {
                // For S3 and Swift, which are considered "remote",
                // as in the file cannot be accessed locally,
                // we run the postRemoteDownload() method.
                td.setDownloadError("Download success, starting install ");
                String result = postRemoteDownload(jobId);
                if (result != null) {
                    s_logger.error("Failed post download install: " + result);
                    td.setStatus(Status.UNRECOVERABLE_ERROR);
                    td.setDownloadError("Failed post download install: " + result);
                    ((S3TemplateDownloader) td).cleanupAfterError();
                } else {
                    td.setStatus(Status.POST_DOWNLOAD_FINISHED);
                    td.setDownloadError("Install completed successfully at " + new SimpleDateFormat().format(new Date()));
                }
            }
            else {
                // For other TemplateDownloaders where files are locally available,
                // we run the postLocalDownload() method.
                td.setDownloadError("Download success, starting install ");
                String result = postLocalDownload(jobId);
                if (result != null) {
                    s_logger.error("Failed post download script: " + result);
                    td.setStatus(Status.UNRECOVERABLE_ERROR);
                    td.setDownloadError("Failed post download script: " + result);
                } else {
                    td.setStatus(Status.POST_DOWNLOAD_FINISHED);
                    td.setDownloadError("Install completed successfully at " + new SimpleDateFormat().format(new Date()));
                }
            }
            dj.cleanup();
            break;
        default:
            break;
        }
    }

    private ChecksumValue computeCheckSum(String algorithm, File f) throws NoSuchAlgorithmException {
        try (InputStream is = new FileInputStream(f);) {
            return DigestHelper.digest(algorithm, is);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Post remote download activity (install and cleanup). Executed in context of the downloader thread.
     */
    private String postRemoteDownload(String jobId) {
        String result = null;
        DownloadJob dnld = jobs.get(jobId);
        S3TemplateDownloader td = (S3TemplateDownloader)dnld.getTemplateDownloader();

        if (td.getFileExtension().equalsIgnoreCase("QCOW2")) {
            // The QCOW2 is the only format with a header,
            // and as such can be easily read.

            try (InputStream inputStream = td.getS3ObjectInputStream();) {
                dnld.setTemplatesize(QCOW2Utils.getVirtualSize(inputStream));
            }
            catch (IOException e) {
                result = "Couldn't read QCOW2 virtual size. Error: " + e.getMessage();
            }

        }
        else {
            // For the other formats, both the virtual
            // and actual file size are set the same.
            dnld.setTemplatesize(td.getTotalBytes());
        }

        dnld.setTemplatePhysicalSize(td.getTotalBytes());
        dnld.setTmpltPath(td.getDownloadLocalPath());

        return result;
    }

    /**
     * Post local download activity (install and cleanup). Executed in context of
     * downloader thread
     *
     * @throws IOException
     */
    private String postLocalDownload(String jobId) {
        DownloadJob dnld = jobs.get(jobId);
        TemplateDownloader td = dnld.getTemplateDownloader();
        String resourcePath = dnld.getInstallPathPrefix(); // path with mount
        // directory
        String finalResourcePath = dnld.getTmpltPath(); // template download
        // path on secondary
        // storage
        ResourceType resourceType = dnld.getResourceType();

        File originalTemplate = new File(td.getDownloadLocalPath());
        ChecksumValue oldValue = new ChecksumValue(dnld.getChecksum());
        ChecksumValue newValue = null;
        try {
            newValue = computeCheckSum(oldValue.getAlgorithm(), originalTemplate);
        } catch (NoSuchAlgorithmException e) {
            return "checksum algorithm not recognised: " + oldValue.getAlgorithm();
        }
        if(StringUtils.isNotBlank(dnld.getChecksum()) && ! oldValue.equals(newValue)) {
            return "checksum \"" + newValue +"\" didn't match the given value, \"" + oldValue + "\"";
        }
        String checksum = newValue.getChecksum();
        if (checksum == null) {
            s_logger.warn("Something wrong happened when try to calculate the checksum of downloaded template!");
        }

        dnld.setCheckSum(checksum);

        int imgSizeGigs = (int)Math.ceil(_storage.getSize(td.getDownloadLocalPath()) * 1.0d / (1024 * 1024 * 1024));
        imgSizeGigs++; // add one just in case
        long timeout = (long)imgSizeGigs * installTimeoutPerGig;
        Script scr = null;
        String script = resourceType == ResourceType.TEMPLATE ? createTmpltScr : createVolScr;
        scr = new Script(script, timeout, s_logger);
        scr.add("-s", Integer.toString(imgSizeGigs));
        scr.add("-S", Long.toString(td.getMaxTemplateSizeInBytes()));
        if (dnld.getDescription() != null && dnld.getDescription().length() > 1) {
            scr.add("-d", dnld.getDescription());
        }
        if (dnld.isHvm()) {
            scr.add("-h");
        }

        // add options common to ISO and template
        String extension = dnld.getFormat().getFileExtension();
        String templateName = "";
        if (extension.equals("iso")) {
            templateName = jobs.get(jobId).getTmpltName().trim().replace(" ", "_");
        } else {
            templateName = java.util.UUID.nameUUIDFromBytes((jobs.get(jobId).getTmpltName() + System.currentTimeMillis()).getBytes(StringUtils.getPreferredCharset())).toString();
        }

        // run script to mv the temporary template file to the final template
        // file
        String templateFilename = templateName + "." + extension;
        dnld.setTmpltPath(finalResourcePath + "/" + templateFilename);
        scr.add("-n", templateFilename);

        scr.add("-t", resourcePath);
        scr.add("-f", td.getDownloadLocalPath()); // this is the temporary template file downloaded
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
        if (resourceType == ResourceType.TEMPLATE) {
            propertiesFile += "/template.properties";
        } else {
            propertiesFile += "/volume.properties";
        }
        File templateProperties = new File(propertiesFile);
        _storage.setWorldReadableAndWriteable(templateProperties);

        TemplateLocation loc = new TemplateLocation(_storage, resourcePath);
        try {
            loc.create(dnld.getId(), true, dnld.getTmpltName());
        } catch (IOException e) {
            s_logger.warn("Something is wrong with template location " + resourcePath, e);
            loc.purge();
            return "Unable to download due to " + e.getMessage();
        }

        Iterator<Processor> en = _processors.values().iterator();
        while (en.hasNext()) {
            Processor processor = en.next();

            FormatInfo info = null;
            try {
                info = processor.process(resourcePath, null, templateName, this._processTimeout);
            } catch (InternalErrorException e) {
                s_logger.error("Template process exception ", e);
                return e.toString();
            }
            if (info != null) {
                if(!loc.addFormat(info)) {
                    loc.purge();
                    return "Unable to install due to invalid file format";
                }
                dnld.setTemplatesize(info.virtualSize);
                dnld.setTemplatePhysicalSize(info.size);
                if (CollectionUtils.isNotEmpty(info.ovfProperties)) {
                    dnld.setOvfProperties(info.ovfProperties);
                }
                break;
            }
        }

        if (!loc.save()) {
            s_logger.warn("Cleaning up because we're unable to save the formats");
            loc.purge();
        }

        return null;
    }

    @Override
    public Status getDownloadStatus(String jobId) {
        DownloadJob job = jobs.get(jobId);
        if (job != null) {
            TemplateDownloader td = job.getTemplateDownloader();
            if (td != null) {
                return td.getStatus();
            }
        }
        return Status.UNKNOWN;
    }

    @Override
    public String downloadS3Template(S3TO s3, long id, String url, String name, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum,
            String installPathPrefix, String user, String password, long maxTemplateSizeInBytes, Proxy proxy, ResourceType resourceType) {
        UUID uuid = UUID.randomUUID();
        String jobId = uuid.toString();

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("URI is incorrect: " + url);
        }
        TemplateDownloader td;
        if ((uri != null) && (uri.getScheme() != null)) {
            if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
                td = new S3TemplateDownloader(s3, url, installPathPrefix, new Completion(jobId), maxTemplateSizeInBytes, user, password, proxy, resourceType);
            } else {
                throw new CloudRuntimeException("Scheme is not supported " + url);
            }
        } else {
            throw new CloudRuntimeException("Unable to download from URL: " + url);
        }
        DownloadJob dj = new DownloadJob(td, jobId, id, name, format, hvm, accountId, descr, cksum, installPathPrefix, resourceType);
        dj.setTmpltPath(installPathPrefix);
        jobs.put(jobId, dj);
        threadPool.execute(td);

        return jobId;
    }

    @Override
    public String downloadPublicTemplate(long id, String url, String name, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum,
            String installPathPrefix, String templatePath, String user, String password, long maxTemplateSizeInBytes, Proxy proxy, ResourceType resourceType) {
        UUID uuid = UUID.randomUUID();
        String jobId = uuid.toString();
        String tmpDir = installPathPrefix;

        try {

            if (!_storage.mkdirs(tmpDir)) {
                s_logger.warn("Unable to create " + tmpDir);
                return "Unable to create " + tmpDir;
            }
            // TO DO - define constant for volume properties.
            File file =
                    ResourceType.TEMPLATE == resourceType ? _storage.getFile(tmpDir + File.separator + TemplateLocation.Filename) : _storage.getFile(tmpDir + File.separator +
                            "volume.properties");
                    if (file.exists()) {
                        if(! file.delete()) {
                            s_logger.warn("Deletion of file '" + file.getAbsolutePath() + "' failed.");
                        }
                    }

                    if (!file.createNewFile()) {
                        s_logger.warn("Unable to create new file: " + file.getAbsolutePath());
                        return "Unable to create new file: " + file.getAbsolutePath();
                    }

                    URI uri;
                    try {
                        uri = new URI(url);
                    } catch (URISyntaxException e) {
                        throw new CloudRuntimeException("URI is incorrect: " + url);
                    }
                    TemplateDownloader td;
                    if ((uri != null) && (uri.getScheme() != null)) {
                        if (uri.getPath().endsWith(".metalink")) {
                            td = new MetalinkTemplateDownloader(_storage, url, tmpDir, new Completion(jobId), maxTemplateSizeInBytes);
                        } else if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
                            td = new HttpTemplateDownloader(_storage, url, tmpDir, new Completion(jobId), maxTemplateSizeInBytes, user, password, proxy, resourceType);
                        } else if (uri.getScheme().equalsIgnoreCase("file")) {
                            td = new LocalTemplateDownloader(_storage, url, tmpDir, maxTemplateSizeInBytes, new Completion(jobId));
                        } else if (uri.getScheme().equalsIgnoreCase("scp")) {
                            td = new ScpTemplateDownloader(_storage, url, tmpDir, maxTemplateSizeInBytes, new Completion(jobId));
                        } else if (uri.getScheme().equalsIgnoreCase("nfs") || uri.getScheme().equalsIgnoreCase("cifs")) {
                            td = null;
                            // TODO: implement this.
                            throw new CloudRuntimeException("Scheme is not supported " + url);
                        } else {
                            throw new CloudRuntimeException("Scheme is not supported " + url);
                        }
                    } else {
                        throw new CloudRuntimeException("Unable to download from URL: " + url);
                    }
                    // NOTE the difference between installPathPrefix and templatePath
                    // here. instalPathPrefix is the absolute path for template
                    // including mount directory
                    // on ssvm, while templatePath is the final relative path on
                    // secondary storage.
                    DownloadJob dj = new DownloadJob(td, jobId, id, name, format, hvm, accountId, descr, cksum, installPathPrefix, resourceType);
                    dj.setTmpltPath(templatePath);
                    jobs.put(jobId, dj);
                    threadPool.execute(td);

                    return jobId;
        } catch (IOException e) {
            s_logger.warn("Unable to download to " + tmpDir, e);
            return null;
        }
    }

    @Override
    public String getDownloadError(String jobId) {
        DownloadJob dj = jobs.get(jobId);
        if (dj != null) {
            return dj.getTemplateDownloader().getDownloadError();
        }
        return null;
    }

    public long getDownloadTemplateSize(String jobId) {
        DownloadJob dj = jobs.get(jobId);
        if (dj != null) {
            return dj.getTemplatesize();
        }
        return 0;
    }

    public String getDownloadCheckSum(String jobId) {
        DownloadJob dj = jobs.get(jobId);
        if (dj != null) {
            return dj.getChecksum();
        }
        return null;
    }

    public long getDownloadTemplatePhysicalSize(String jobId) {
        DownloadJob dj = jobs.get(jobId);
        if (dj != null) {
            return dj.getTemplatePhysicalSize();
        }
        return 0;
    }

    // @Override
    public String getDownloadLocalPath(String jobId) {
        DownloadJob dj = jobs.get(jobId);
        if (dj != null) {
            return dj.getTemplateDownloader().getDownloadLocalPath();
        }
        return null;
    }

    @Override
    public int getDownloadPct(String jobId) {
        DownloadJob dj = jobs.get(jobId);
        if (dj != null) {
            return dj.getTemplateDownloader().getDownloadPercent();
        }
        return 0;
    }

    public static VMTemplateHostVO.Status convertStatus(Status tds) {
        switch (tds) {
        case ABORTED:
            return VMTemplateHostVO.Status.NOT_DOWNLOADED;
        case DOWNLOAD_FINISHED:
            return VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS;
        case IN_PROGRESS:
            return VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS;
        case NOT_STARTED:
            return VMTemplateHostVO.Status.NOT_DOWNLOADED;
        case RECOVERABLE_ERROR:
            return VMTemplateHostVO.Status.NOT_DOWNLOADED;
        case UNKNOWN:
            return VMTemplateHostVO.Status.UNKNOWN;
        case UNRECOVERABLE_ERROR:
            return VMTemplateHostVO.Status.DOWNLOAD_ERROR;
        case POST_DOWNLOAD_FINISHED:
            return VMTemplateHostVO.Status.DOWNLOADED;
        default:
            return VMTemplateHostVO.Status.UNKNOWN;
        }
    }

    @Override
    public com.cloud.storage.VMTemplateHostVO.Status getDownloadStatus2(String jobId) {
        return convertStatus(getDownloadStatus(jobId));
    }

    @Override
    public DownloadAnswer handleDownloadCommand(SecondaryStorageResource resource, DownloadCommand cmd) {
        int timeout = NumbersUtil.parseInt(cmd.getContextParam("vmware.package.ova.timeout"), 3600000);
        this._processTimeout = timeout;
        ResourceType resourceType = cmd.getResourceType();
        if (cmd instanceof DownloadProgressCommand) {
            return handleDownloadProgressCmd(resource, (DownloadProgressCommand)cmd);
        }

        if (cmd.getUrl() == null) {
            return new DownloadAnswer(resourceType.toString() + " is corrupted on storage due to an invalid url , cannot download",
                    VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
        }

        if (cmd.getName() == null) {
            return new DownloadAnswer("Invalid Name", VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
        }

        if(! DigestHelper.isAlgorithmSupported(cmd.getChecksum())) {
            return new DownloadAnswer("invalid algorithm: " + cmd.getChecksum(), VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED);
        }

        DataStoreTO dstore = cmd.getDataStore();
        String installPathPrefix = cmd.getInstallPath();
        // for NFS, we need to get mounted path
        if (dstore instanceof NfsTO) {
            installPathPrefix = resource.getRootDir(((NfsTO)dstore).getUrl(), _nfsVersion) + File.separator + installPathPrefix;
        }
        String user = null;
        String password = null;
        if (cmd.getAuth() != null) {
            user = cmd.getAuth().getUserName();
            password = cmd.getAuth().getPassword();
        }
        // TO DO - Define Volume max size as well
        long maxDownloadSizeInBytes =
                (cmd.getMaxDownloadSizeInBytes() == null) ? TemplateDownloader.DEFAULT_MAX_TEMPLATE_SIZE_IN_BYTES : (cmd.getMaxDownloadSizeInBytes());
        String jobId = null;
        if (dstore instanceof S3TO) {
            jobId =
                    downloadS3Template((S3TO)dstore, cmd.getId(), cmd.getUrl(), cmd.getName(), cmd.getFormat(), cmd.isHvm(), cmd.getAccountId(), cmd.getDescription(),
                            cmd.getChecksum(), installPathPrefix, user, password, maxDownloadSizeInBytes, cmd.getProxy(), resourceType);
        } else {
            jobId =
                    downloadPublicTemplate(cmd.getId(), cmd.getUrl(), cmd.getName(), cmd.getFormat(), cmd.isHvm(), cmd.getAccountId(), cmd.getDescription(),
                            cmd.getChecksum(), installPathPrefix, cmd.getInstallPath(), user, password, maxDownloadSizeInBytes, cmd.getProxy(), resourceType);
        }
        sleep();
        if (jobId == null) {
            return new DownloadAnswer("Internal Error", VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
        }
        return new DownloadAnswer(jobId, getDownloadPct(jobId), getDownloadError(jobId), getDownloadStatus2(jobId), getDownloadLocalPath(jobId), getInstallPath(jobId),
                getDownloadTemplateSize(jobId), getDownloadTemplateSize(jobId), getDownloadCheckSum(jobId));
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private DownloadAnswer handleDownloadProgressCmd(SecondaryStorageResource resource, DownloadProgressCommand cmd) {
        String jobId = cmd.getJobId();
        DownloadAnswer answer;
        DownloadJob dj = null;
        if (jobId != null) {
            dj = jobs.get(jobId);
        }
        if (dj == null) {
            if (cmd.getRequest() == RequestType.GET_OR_RESTART) {
                DownloadCommand dcmd = new DownloadCommand(cmd);
                return handleDownloadCommand(resource, dcmd);
            } else {
                return new DownloadAnswer("Cannot find job", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.UNKNOWN);
            }
        }
        TemplateDownloader td = dj.getTemplateDownloader();
        switch (cmd.getRequest()) {
        case GET_STATUS:
            break;
        case ABORT:
            td.stopDownload();
            sleep();
            break;
        case RESTART:
            td.stopDownload();
            sleep();
            threadPool.execute(td);
            break;
        case PURGE:
            td.stopDownload();
            answer =
                    new DownloadAnswer(jobId, getDownloadPct(jobId), getDownloadError(jobId), getDownloadStatus2(jobId), getDownloadLocalPath(jobId),
                            getInstallPath(jobId), getDownloadTemplateSize(jobId), getDownloadTemplatePhysicalSize(jobId), getDownloadCheckSum(jobId));
            if (CollectionUtils.isNotEmpty(dj.getOvfProperties())) {
                answer.setOvfProperties(dj.getOvfProperties());
            }
            jobs.remove(jobId);
            return answer;
        default:
            break; // TODO
        }
        return new DownloadAnswer(jobId, getDownloadPct(jobId), getDownloadError(jobId), getDownloadStatus2(jobId), getDownloadLocalPath(jobId), getInstallPath(jobId),
                getDownloadTemplateSize(jobId), getDownloadTemplatePhysicalSize(jobId), getDownloadCheckSum(jobId));
    }

    private String getInstallPath(String jobId) {
        DownloadJob dj = jobs.get(jobId);
        if (dj != null) {
            return dj.getTmpltPath();
        }
        return null;
    }

    private List<String> listVolumes(String rootdir) {
        List<String> result = new ArrayList<String>();

        Script script = new Script(listVolScr, s_logger);
        script.add("-r", rootdir);
        ZfsPathParser zpp = new ZfsPathParser(rootdir);
        script.execute(zpp);
        result.addAll(zpp.getPaths());
        s_logger.info("found " + zpp.getPaths().size() + " volumes" + zpp.getPaths());
        return result;
    }

    private List<String> listTemplates(String rootdir) {
        List<String> result = new ArrayList<String>();

        Script script = new Script(listTmpltScr, s_logger);
        script.add("-r", rootdir);
        ZfsPathParser zpp = new ZfsPathParser(rootdir);
        script.execute(zpp);
        result.addAll(zpp.getPaths());
        s_logger.info("found " + zpp.getPaths().size() + " templates" + zpp.getPaths());
        return result;
    }

    @Override
    public Map<String, TemplateProp> gatherTemplateInfo(String rootDir) {
        Map<String, TemplateProp> result = new HashMap<String, TemplateProp>();
        String templateDir = rootDir + File.separator + _templateDir;

        if (!_storage.exists(templateDir)) {
            _storage.mkdirs(templateDir);
        }

        List<String> publicTmplts = listTemplates(templateDir);
        for (String tmplt : publicTmplts) {
            String path = tmplt.substring(0, tmplt.lastIndexOf(File.separator));
            TemplateLocation loc = new TemplateLocation(_storage, path);
            try {
                if (!loc.load()) {
                    s_logger.warn("Post download installation was not completed for " + path);
                    // loc.purge();
                    _storage.cleanup(path, templateDir);
                    continue;
                }
            } catch (IOException e) {
                s_logger.warn("Unable to load template location " + path, e);
                continue;
            }

            TemplateProp tInfo = loc.getTemplateInfo();

            if ((tInfo.getSize() == tInfo.getPhysicalSize()) && (tInfo.getInstallPath().endsWith(ImageFormat.OVA.getFileExtension()))) {
                try {
                    Processor processor = _processors.get("OVA Processor");
                    OVAProcessor vmdkProcessor = (OVAProcessor)processor;
                    long vSize = vmdkProcessor.getTemplateVirtualSize(path, tInfo.getInstallPath().substring(tInfo.getInstallPath().lastIndexOf(File.separator) + 1));
                    tInfo.setSize(vSize);
                    loc.updateVirtualSize(vSize);
                    loc.save();
                } catch (Exception e) {
                    s_logger.error("Unable to get the virtual size of the template: " + tInfo.getInstallPath() + " due to " + e.getMessage());
                }
            }

            result.put(tInfo.getTemplateName(), tInfo);
            s_logger.debug("Added template name: " + tInfo.getTemplateName() + ", path: " + tmplt);
        }
        return result;
    }

    @Override
    public Map<Long, TemplateProp> gatherVolumeInfo(String rootDir) {
        Map<Long, TemplateProp> result = new HashMap<Long, TemplateProp>();
        String volumeDir = rootDir + File.separator + _volumeDir;

        if (!_storage.exists(volumeDir)) {
            _storage.mkdirs(volumeDir);
        }

        List<String> vols = listVolumes(volumeDir);
        for (String vol : vols) {
            String path = vol.substring(0, vol.lastIndexOf(File.separator));
            TemplateLocation loc = new TemplateLocation(_storage, path);
            try {
                if (!loc.load()) {
                    s_logger.warn("Post download installation was not completed for " + path);
                    // loc.purge();
                    _storage.cleanup(path, volumeDir);
                    continue;
                }
            } catch (IOException e) {
                s_logger.warn("Unable to load volume location " + path, e);
                continue;
            }

            TemplateProp vInfo = loc.getTemplateInfo();

            if ((vInfo.getSize() == vInfo.getPhysicalSize()) && (vInfo.getInstallPath().endsWith(ImageFormat.OVA.getFileExtension()))) {
                try {
                    Processor processor = _processors.get("OVA Processor");
                    OVAProcessor vmdkProcessor = (OVAProcessor)processor;
                    long vSize = vmdkProcessor.getTemplateVirtualSize(path, vInfo.getInstallPath().substring(vInfo.getInstallPath().lastIndexOf(File.separator) + 1));
                    vInfo.setSize(vSize);
                    loc.updateVirtualSize(vSize);
                    loc.save();
                } catch (Exception e) {
                    s_logger.error("Unable to get the virtual size of the volume: " + vInfo.getInstallPath() + " due to " + e.getMessage());
                }
            }

            result.put(vInfo.getId(), vInfo);
            s_logger.debug("Added volume name: " + vInfo.getTemplateName() + ", path: " + vol);
        }
        return result;
    }

    public static class ZfsPathParser extends OutputInterpreter {
        String _parent;
        List<String> paths = new ArrayList<String>();

        public ZfsPathParser(String parent) {
            _parent = parent;
        }

        @Override
        public String interpret(BufferedReader reader) throws IOException {
            String line = null;
            while ((line = reader.readLine()) != null) {
                paths.add(line);
            }
            return null;
        }

        public List<String> getPaths() {
            return paths;
        }

        @Override
        public boolean drain() {
            return true;
        }
    }

    public DownloadManagerImpl() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        String value = null;

        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            value = (String)params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                throw new ConfigurationException("Unable to find the storage layer");
            }

            Class<StorageLayer> clazz;
            try {
                clazz = (Class<StorageLayer>)Class.forName(value);
                _storage = clazz.newInstance();
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to instantiate " + value);
            } catch (InstantiationException e) {
                throw new ConfigurationException("Unable to instantiate " + value);
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Unable to instantiate " + value);
            }
        }

        String inSystemVM = (String)params.get("secondary.storage.vm");
        if (inSystemVM != null && "true".equalsIgnoreCase(inSystemVM)) {
            s_logger.info("DownloadManager: starting additional services since we are inside system vm");
            _nfsVersion = NfsSecondaryStorageResource.retrieveNfsVersionFromParams(params);
            startAdditionalServices();
            blockOutgoingOnPrivate();
        }

        value = (String)params.get("install.timeout.pergig");
        installTimeoutPerGig = NumbersUtil.parseInt(value, 15 * 60) * 1000;

        value = (String)params.get("install.numthreads");
        final int numInstallThreads = NumbersUtil.parseInt(value, 10);

        String scriptsDir = (String)params.get("template.scripts.dir");
        if (scriptsDir == null) {
            scriptsDir = "scripts/storage/secondary";
        }

        listTmpltScr = Script.findScript(scriptsDir, "listvmtmplt.sh");
        if (listTmpltScr == null) {
            throw new ConfigurationException("Unable to find the listvmtmplt.sh");
        }
        s_logger.info("listvmtmplt.sh found in " + listTmpltScr);

        createTmpltScr = Script.findScript(scriptsDir, "createtmplt.sh");
        if (createTmpltScr == null) {
            throw new ConfigurationException("Unable to find createtmplt.sh");
        }
        s_logger.info("createtmplt.sh found in " + createTmpltScr);

        listVolScr = Script.findScript(scriptsDir, "listvolume.sh");
        if (listVolScr == null) {
            throw new ConfigurationException("Unable to find the listvolume.sh");
        }
        s_logger.info("listvolume.sh found in " + listVolScr);

        createVolScr = Script.findScript(scriptsDir, "createvolume.sh");
        if (createVolScr == null) {
            throw new ConfigurationException("Unable to find createvolume.sh");
        }
        s_logger.info("createvolume.sh found in " + createVolScr);

        _processors = new HashMap<String, Processor>();

        Processor processor = new VhdProcessor();
        processor.configure("VHD Processor", params);
        _processors.put("VHD Processor", processor);

        processor = new IsoProcessor();
        processor.configure("ISO Processor", params);
        _processors.put("ISO Processor", processor);

        processor = new QCOW2Processor();
        processor.configure("QCOW2 Processor", params);
        _processors.put("QCOW2 Processor", processor);

        processor = new OVAProcessor();
        processor.configure("OVA Processor", params);
        _processors.put("OVA Processor", processor);

        processor = new VmdkProcessor();
        processor.configure("VMDK Processor", params);
        _processors.put("VMDK Processor", processor);

        processor = new RawImageProcessor();
        processor.configure("Raw Image Processor", params);
        _processors.put("Raw Image Processor", processor);

        processor = new TARProcessor();
        processor.configure("TAR Processor", params);
        _processors.put("TAR Processor", processor);

        _templateDir = (String)params.get("public.templates.root.dir");
        if (_templateDir == null) {
            _templateDir = TemplateConstants.DEFAULT_TMPLT_ROOT_DIR;
        }
        _templateDir += File.separator + TemplateConstants.DEFAULT_TMPLT_FIRST_LEVEL_DIR;
        _volumeDir = TemplateConstants.DEFAULT_VOLUME_ROOT_DIR + File.separator;
        // Add more processors here.
        threadPool = Executors.newFixedThreadPool(numInstallThreads);
        return true;
    }

    private void blockOutgoingOnPrivate() {
        Script command = new Script("/bin/bash", s_logger);
        String intf = "eth1";
        command.add("-c");
        command.add("iptables -A OUTPUT -o " + intf + " -p tcp -m state --state NEW -m tcp --dport " + "80" + " -j REJECT;" + "iptables -A OUTPUT -o " + intf +
                " -p tcp -m state --state NEW -m tcp --dport " + "443" + " -j REJECT;");

        String result = command.execute();
        if (result != null) {
            s_logger.warn("Error in blocking outgoing to port 80/443 err=" + result);
            return;
        }
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void startAdditionalServices() {
        Script command = new Script("/bin/systemctl", s_logger);
        command.add("stop");
        command.add("apache2");
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Error in stopping httpd service err=" + result);
        }
        String port = Integer.toString(TemplateConstants.DEFAULT_TMPLT_COPY_PORT);
        String intf = TemplateConstants.DEFAULT_TMPLT_COPY_INTF;

        command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("iptables -I INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + port + " -j ACCEPT;" + "iptables -I INPUT -i " + intf +
                " -p tcp -m state --state NEW -m tcp --dport " + "443" + " -j ACCEPT;");

        result = command.execute();
        if (result != null) {
            s_logger.warn("Error in opening up apache2 port err=" + result);
            return;
        }

        command = new Script("/bin/systemctl", s_logger);
        command.add("start");
        command.add("apache2");
        result = command.execute();
        if (result != null) {
            s_logger.warn("Error in starting apache2 service err=" + result);
            return;
        }

        command = new Script("/bin/su", s_logger);
        command.add("-s");
        command.add("/bin/bash");
        command.add("-c");
        command.add("mkdir -p /var/www/html/copy/template");
        command.add("www-data");
        result = command.execute();
        if (result != null) {
            s_logger.warn("Error in creating directory =" + result);
            return;
        }
    }

}
