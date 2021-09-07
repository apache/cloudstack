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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.Answer;
import org.apache.log4j.Logger;

import org.apache.cloudstack.storage.resource.SecondaryStorageResource;

import com.cloud.agent.api.storage.CreateEntityDownloadURLAnswer;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.DeleteEntityDownloadURLCommand;
import com.cloud.agent.api.storage.UploadAnswer;
import com.cloud.agent.api.storage.UploadCommand;
import com.cloud.agent.api.storage.UploadProgressCommand;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Upload;
import com.cloud.storage.UploadVO;
import com.cloud.storage.template.FtpTemplateUploader;
import com.cloud.storage.template.TemplateUploader;
import com.cloud.storage.template.TemplateUploader.Status;
import com.cloud.storage.template.TemplateUploader.UploadCompleteCallback;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

public class UploadManagerImpl extends ManagerBase implements UploadManager {

    public class Completion implements UploadCompleteCallback {
        private final String jobId;

        public Completion(String jobId) {
            this.jobId = jobId;
        }

        @Override
        public void uploadComplete(Status status) {
            setUploadStatus(jobId, status);
        }
    }

    private static class UploadJob {
        private final TemplateUploader tu;

        public UploadJob(TemplateUploader tu, String jobId, long id, String name, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum,
                String installPathPrefix) {
            super();
            this.tu = tu;
        }

        public TemplateUploader getTemplateUploader() {
            return tu;
        }

        public void cleanup() {
            if (tu != null) {
                String upldPath = tu.getUploadLocalPath();
                if (upldPath != null) {
                    File f = new File(upldPath);
                    f.delete();
                }
            }
        }

    }

    public static final Logger s_logger = Logger.getLogger(UploadManagerImpl.class);
    private ExecutorService threadPool;
    private final Map<String, UploadJob> jobs = new ConcurrentHashMap<String, UploadJob>();
    private String parentDir;
    private final String extractMountPoint = "/mnt/SecStorage/extractmnt";
    private StorageLayer _storage;
    private boolean hvm;

    @Override
    public String uploadPublicTemplate(long id, String url, String name, ImageFormat format, Long accountId, String descr, String cksum, String installPathPrefix,
            String userName, String passwd, long templateSizeInBytes) {

        UUID uuid = UUID.randomUUID();
        String jobId = uuid.toString();

        String completePath = parentDir + File.separator + installPathPrefix;
        s_logger.debug("Starting upload from " + completePath);

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            s_logger.error("URI is incorrect: " + url);
            throw new CloudRuntimeException("URI is incorrect: " + url);
        }
        TemplateUploader tu;
        if ((uri != null) && (uri.getScheme() != null)) {
            if (uri.getScheme().equalsIgnoreCase("ftp")) {
                tu = new FtpTemplateUploader(completePath, url, new Completion(jobId), templateSizeInBytes);
            } else {
                s_logger.error("Scheme is not supported " + url);
                throw new CloudRuntimeException("Scheme is not supported " + url);
            }
        } else {
            s_logger.error("Unable to download from URL: " + url);
            throw new CloudRuntimeException("Unable to download from URL: " + url);
        }
        UploadJob uj = new UploadJob(tu, jobId, id, name, format, hvm, accountId, descr, cksum, installPathPrefix);
        jobs.put(jobId, uj);
        threadPool.execute(tu);

        return jobId;

    }

    @Override
    public String getUploadError(String jobId) {
        UploadJob uj = jobs.get(jobId);
        if (uj != null) {
            return uj.getTemplateUploader().getUploadError();
        }
        return null;
    }

    @Override
    public int getUploadPct(String jobId) {
        UploadJob uj = jobs.get(jobId);
        if (uj != null) {
            return uj.getTemplateUploader().getUploadPercent();
        }
        return 0;
    }

    @Override
    public Status getUploadStatus(String jobId) {
        UploadJob job = jobs.get(jobId);
        if (job != null) {
            TemplateUploader tu = job.getTemplateUploader();
            if (tu != null) {
                return tu.getStatus();
            }
        }
        return Status.UNKNOWN;
    }

    public static UploadVO.Status convertStatus(Status tds) {
        switch (tds) {
        case ABORTED:
            return UploadVO.Status.NOT_UPLOADED;
        case UPLOAD_FINISHED:
            return UploadVO.Status.UPLOAD_IN_PROGRESS;
        case IN_PROGRESS:
            return UploadVO.Status.UPLOAD_IN_PROGRESS;
        case NOT_STARTED:
            return UploadVO.Status.NOT_UPLOADED;
        case RECOVERABLE_ERROR:
            return UploadVO.Status.NOT_UPLOADED;
        case UNKNOWN:
            return UploadVO.Status.UNKNOWN;
        case UNRECOVERABLE_ERROR:
            return UploadVO.Status.UPLOAD_ERROR;
        case POST_UPLOAD_FINISHED:
            return UploadVO.Status.UPLOADED;
        default:
            return UploadVO.Status.UNKNOWN;
        }
    }

    @Override
    public com.cloud.storage.UploadVO.Status getUploadStatus2(String jobId) {
        return convertStatus(getUploadStatus(jobId));
    }

    @Override
    public String getPublicTemplateRepo() {
        // TODO Auto-generated method stub
        return null;
    }

    private UploadAnswer handleUploadProgressCmd(UploadProgressCommand cmd) {
        String jobId = cmd.getJobId();
        UploadAnswer answer;
        UploadJob uj = null;
        if (jobId != null)
            uj = jobs.get(jobId);
        if (uj == null) {
            return new UploadAnswer(null, 0, "Cannot find job", com.cloud.storage.UploadVO.Status.UNKNOWN, "", "", 0);
        }
        TemplateUploader td = uj.getTemplateUploader();
        switch (cmd.getRequest()) {
        case GET_STATUS:
            break;
        case ABORT:
            td.stopUpload();
            sleep();
            break;
            /*case RESTART:
            td.stopUpload();
            sleep();
            threadPool.execute(td);
            break;*/
        case PURGE:
            td.stopUpload();
            answer =
                    new UploadAnswer(jobId, getUploadPct(jobId), getUploadError(jobId), getUploadStatus2(jobId), getUploadLocalPath(jobId), getInstallPath(jobId),
                            getUploadTemplateSize(jobId));
            jobs.remove(jobId);
            return answer;
        default:
            break; // TODO
        }
        return new UploadAnswer(jobId, getUploadPct(jobId), getUploadError(jobId), getUploadStatus2(jobId), getUploadLocalPath(jobId), getInstallPath(jobId),
                getUploadTemplateSize(jobId));
    }

    @Override
    public UploadAnswer handleUploadCommand(SecondaryStorageResource resource, UploadCommand cmd) {
        s_logger.warn("Handling the upload " + cmd.getInstallPath() + " " + cmd.getId());
        if (cmd instanceof UploadProgressCommand) {
            return handleUploadProgressCmd((UploadProgressCommand)cmd);
        }

        String user = null;
        String password = null;
        String jobId =
                uploadPublicTemplate(cmd.getId(), cmd.getUrl(), cmd.getName(), cmd.getFormat(), cmd.getAccountId(), cmd.getDescription(), cmd.getChecksum(),
                        cmd.getInstallPath(), user, password, cmd.getTemplateSizeInBytes());
        sleep();
        return new UploadAnswer(jobId, getUploadPct(jobId), getUploadError(jobId), getUploadStatus2(jobId), getUploadLocalPath(jobId), getInstallPath(jobId),
                getUploadTemplateSize(jobId));
    }

    @Override
    public CreateEntityDownloadURLAnswer handleCreateEntityURLCommand(CreateEntityDownloadURLCommand cmd) {

        boolean isApacheUp = checkAndStartApache();
        if (!isApacheUp) {
            String errorString = "Error in starting Apache server ";
            s_logger.error(errorString);
            return new CreateEntityDownloadURLAnswer(errorString, CreateEntityDownloadURLAnswer.RESULT_FAILURE);
        }
        // Create the directory structure so that its visible under apache server root
        String extractDir = "/var/www/html/userdata/";
        Script command = new Script("/bin/su", s_logger);
        command.add("-s");
        command.add("/bin/bash");
        command.add("-c");
        command.add("mkdir -p " + extractDir);
        command.add("www-data");
        String result = command.execute();
        if (result != null) {
            String errorString = "Error in creating directory =" + result;
            s_logger.error(errorString);
            return new CreateEntityDownloadURLAnswer(errorString, CreateEntityDownloadURLAnswer.RESULT_FAILURE);
        }

        // Create a random file under the directory for security reasons.
        String uuid = cmd.getExtractLinkUUID();
        // Create a symbolic link from the actual directory to the template location. The entity would be directly visible under /var/www/html/userdata/cmd.getInstallPath();
        command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("ln -sf /mnt/SecStorage/" + cmd.getParent() + File.separator + cmd.getInstallPath() + " " + extractDir + uuid);
        result = command.execute();
        if (result != null) {
            String errorString = "Error in linking  err=" + result;
            s_logger.error(errorString);
            return new CreateEntityDownloadURLAnswer(errorString, CreateEntityDownloadURLAnswer.RESULT_FAILURE);
        }

        return new CreateEntityDownloadURLAnswer("", CreateEntityDownloadURLAnswer.RESULT_SUCCESS);

    }

    @Override
    public Answer handleDeleteEntityDownloadURLCommand(DeleteEntityDownloadURLCommand cmd) {

        //Delete the soft link. Example path = volumes/8/74eeb2c6-8ab1-4357-841f-2e9d06d1f360.vhd
        s_logger.warn("handleDeleteEntityDownloadURLCommand Path:" + cmd.getPath() + " Type:" + cmd.getType().toString());
        String path = cmd.getPath();
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");

        //We just need to remove the UUID.vhd
        String extractUrl = cmd.getExtractUrl();
        String result;
        if (extractUrl != null) {
            command.add("unlink /var/www/html/userdata/" + extractUrl.substring(extractUrl.lastIndexOf(File.separator) + 1));
            result = command.execute();
            if (result != null) {
                // FIXME - Ideally should bail out if you cant delete symlink. Not doing it right now.
                // This is because the ssvm might already be destroyed and the symlinks do not exist.
                s_logger.warn("Error in deleting symlink :" + result);
            }
        }

        // If its a volume also delete the Hard link since it was created only for the purpose of download.
        if (cmd.getType() == Upload.Type.VOLUME) {
            command = new Script("/bin/bash", s_logger);
            command.add("-c");
            command.add("rm -rf /mnt/SecStorage/" + cmd.getParentPath() + File.separator + path);
            s_logger.warn(" " + parentDir + File.separator + path);
            result = command.execute();
            if (result != null) {
                String errorString = "Error in deleting volume " + path + " : " + result;
                s_logger.warn(errorString);
                return new Answer(cmd, false, errorString);
            }
        }

        return new Answer(cmd, true, "");
    }

    private String getInstallPath(String jobId) {
        // TODO Auto-generated method stub
        return null;
    }

    private String getUploadLocalPath(String jobId) {
        // TODO Auto-generated method stub
        return null;
    }

    private long getUploadTemplateSize(String jobId) {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

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
            s_logger.info("UploadManager: starting additional services since we are inside system vm");
            startAdditionalServices();
            //blockOutgoingOnPrivate();
        }

        value = (String)params.get("install.numthreads");
        final int numInstallThreads = NumbersUtil.parseInt(value, 10);

        String scriptsDir = (String)params.get("template.scripts.dir");
        if (scriptsDir == null) {
            scriptsDir = "scripts/storage/secondary";
        }

        // Add more processors here.
        threadPool = Executors.newFixedThreadPool(numInstallThreads);

        return true;
    }

    private void startAdditionalServices() {

        Script command = new Script("rm", s_logger);
        command.add("-rf");
        command.add(extractMountPoint);
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Error in creating file " + extractMountPoint + " ,error: " + result);
            return;
        }

        command = new Script("touch", s_logger);
        command.add(extractMountPoint);
        result = command.execute();
        if (result != null) {
            s_logger.warn("Error in creating file " + extractMountPoint + " ,error: " + result);
            return;
        }

        command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("ln -sf " + parentDir + " " + extractMountPoint);
        result = command.execute();
        if (result != null) {
            s_logger.warn("Error in linking  err=" + result);
            return;
        }

    }

    /**
     * Get notified of change of job status. Executed in context of uploader thread
     *
     * @param jobId
     *            the id of the job
     * @param status
     *            the status of the job
     */
    public void setUploadStatus(String jobId, Status status) {
        UploadJob uj = jobs.get(jobId);
        if (uj == null) {
            s_logger.warn("setUploadStatus for jobId: " + jobId + ", status=" + status + " no job found");
            return;
        }
        TemplateUploader tu = uj.getTemplateUploader();
        s_logger.warn("Upload Completion for jobId: " + jobId + ", status=" + status);
        s_logger.warn("UploadedBytes=" + toHumanReadableSize(tu.getUploadedBytes()) + ", error=" + tu.getUploadError() + ", pct=" + tu.getUploadPercent());

        switch (status) {
        case ABORTED:
        case NOT_STARTED:
        case UNRECOVERABLE_ERROR:
            // Delete the entity only if its a volume. TO DO - find a better way of finding it a volume.
            if (uj.getTemplateUploader().getUploadLocalPath().indexOf("volume") > -1) {
                uj.cleanup();
            }
            break;
        case UNKNOWN:
            return;
        case IN_PROGRESS:
            s_logger.info("Resuming jobId: " + jobId + ", status=" + status);
            tu.setResume(true);
            threadPool.execute(tu);
            break;
        case RECOVERABLE_ERROR:
            threadPool.execute(tu);
            break;
        case UPLOAD_FINISHED:
            tu.setUploadError("Upload success, starting install ");
            String result = postUpload(jobId);
            if (result != null) {
                s_logger.error("Failed post upload script: " + result);
                tu.setStatus(Status.UNRECOVERABLE_ERROR);
                tu.setUploadError("Failed post upload script: " + result);
            } else {
                s_logger.warn("Upload completed successfully at " + new SimpleDateFormat().format(new Date()));
                tu.setStatus(Status.POST_UPLOAD_FINISHED);
                tu.setUploadError("Upload completed successfully at " + new SimpleDateFormat().format(new Date()));
            }
            // Delete the entity only if its a volume. TO DO - find a better way of finding it a volume.
            if (uj.getTemplateUploader().getUploadLocalPath().indexOf("volume") > -1) {
                uj.cleanup();
            }
            break;
        default:
            break;
        }
    }

    private String postUpload(String jobId) {
        return null;
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private boolean checkAndStartApache() {
        //Check whether the Apache server is running
        Script command = new Script("/bin/systemctl", s_logger);
        command.add("is-active");
        command.add("apache2");
        String result = command.execute();

        //Apache Server is not running. Try to start it.
        if (result != null && !result.equals("active")) {
            command = new Script("/bin/systemctl", s_logger);
            command.add("start");
            command.add("apache2");
            result = command.execute();
            if (result != null) {
                s_logger.warn("Error in starting apache2 service err=" + result);
                return false;
            }
        }

        return true;
    }
}
