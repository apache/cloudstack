/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.DownloadCommand;
import com.cloud.agent.api.storage.DownloadProgressCommand;
import com.cloud.agent.api.storage.DownloadProgressCommand.RequestType;
import com.cloud.exception.InternalErrorException;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.StorageResource;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.storage.template.TemplateDownloader.DownloadCompleteCallback;
import com.cloud.storage.template.TemplateDownloader.Status;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.UUID;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

/**
 * @author chiradeep
 * 
 */
@Local(value = DownloadManager.class)
public class DownloadManagerImpl implements DownloadManager {
    private String _name;
    StorageLayer _storage;
    Adapters<Processor> _processors;

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

    private static class DownloadJob {
        private final TemplateDownloader td;
        private final String jobId;
        private final String tmpltName;
        private final boolean hvm;
        private final ImageFormat format;
        private String tmpltPath;
        private String description;
        private String checksum;
        private Long accountId;
        private String installPathPrefix;
        private long templatesize;
        private long id;

        public DownloadJob(TemplateDownloader td, String jobId, long id, String tmpltName, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum, String installPathPrefix) {
            super();
            this.td = td;
            this.jobId = jobId;
            this.tmpltName = tmpltName;
            this.format = format;
            this.hvm = hvm;
            this.accountId = accountId;
            this.description = descr;
            this.checksum = cksum;
            this.installPathPrefix = installPathPrefix;
            this.templatesize = 0;
            this.id = id;
        }

        public TemplateDownloader getTd() {
            return td;
        }

        public String getDescription() {
            return description;
        }

        public String getChecksum() {
            return checksum;
        }

        public DownloadJob(TemplateDownloader td, String jobId, DownloadCommand cmd) {
            this.td = td;
            this.jobId = jobId;
            this.tmpltName = cmd.getName();
            this.format = cmd.getFormat();
            this.hvm = cmd.isHvm();
        }

        public TemplateDownloader getTemplateDownloader() {
            return td;
        }

        public String getJobId() {
            return jobId;
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

        public Long getAccountId() {
            return accountId;
        }

        public long getId() {
            return id;
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
                    if (dir != null)
                        dir.delete();
                }
            }

        }

        public void setTemplatesize(long templatesize) {
            this.templatesize = templatesize;
        }

        public long getTemplatesize() {
            return templatesize;
        }
    }

    public static final Logger s_logger = Logger.getLogger(DownloadManagerImpl.class);
    private String parentDir;
    private String publicTemplateRepo;
    private String createTmpltScr;
    private Adapters<Processor> processors;

    private ExecutorService threadPool;

    private final Map<String, DownloadJob> jobs = new ConcurrentHashMap<String, DownloadJob>();
    private String listTmpltScr;
    private int installTimeoutPerGig = 180 * 60 * 1000;
	private boolean _sslCopy;

    public String setRootDir(String rootDir, StorageResource storage) {
        /*
         * if (!storage.existPath(rootDir + templateDownloadDir)) { s_logger.info("Creating template download path: " +
         * rootDir + templateDownloadDir); String result = storage.createPath(rootDir + templateDownloadDir); if (result
         * != null) { return "Cannot create " + rootDir + templateDownloadDir + " due to " + result; } }
         * this.templateDownloadDir = rootDir + templateDownloadDir;
         */
        this.publicTemplateRepo = rootDir + publicTemplateRepo;

        return null;
    }

    /**
     * Get notified of change of job status. Executed in context of downloader thread
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
        s_logger.info("local: " + td.getDownloadLocalPath() + ", bytes=" + td.getDownloadedBytes() + ", error=" + td.getDownloadError() + ", pct=" + td.getDownloadPercent());

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
            td.setDownloadError("Download success, starting install ");
            String result = postDownload(jobId);
            if (result != null) {
                s_logger.error("Failed post download script: " + result);
                td.setStatus(Status.UNRECOVERABLE_ERROR);
                td.setDownloadError("Failed post download script: " + result);
            } else {
                td.setStatus(Status.POST_DOWNLOAD_FINISHED);
                td.setDownloadError("Install completed successfully at " + new SimpleDateFormat().format(new Date()));
            }
            dj.cleanup();
            break;
        default:
            break;
        }
    }

    /**
     * Post download activity (install and cleanup). Executed in context of downloader thread
     * 
     * @throws IOException
     */
    private String postDownload(String jobId) {
        DownloadJob dnld = jobs.get(jobId);
        TemplateDownloader td = dnld.getTemplateDownloader();
        String templatePath = null;
        templatePath = dnld.getInstallPathPrefix() + dnld.getAccountId() + File.separator + dnld.getId() + File.separator;// dnld.getTmpltName();

        _storage.mkdirs(templatePath);
        
        // once template path is set, remove the parent dir so that the template is installed with a relative path
        String finalTemplatePath = templatePath.substring(parentDir.length());
        dnld.setTmpltPath(finalTemplatePath);

        int imgSizeGigs = (int) Math.ceil(_storage.getSize(td.getDownloadLocalPath()) * 1.0d / (1024 * 1024 * 1024));
        imgSizeGigs++; // add one just in case
        long timeout = imgSizeGigs * installTimeoutPerGig;
        Script scr = null;
        scr = new Script(createTmpltScr, timeout, s_logger);
        scr.add("-s", Integer.toString(imgSizeGigs));
        scr.add("-S", Long.toString(td.getMaxTemplateSizeInBytes()));
        if (dnld.getDescription() != null && dnld.getDescription().length() > 1) {
            scr.add("-d", dnld.getDescription());
        }
        if (dnld.isHvm()) {
            scr.add("-h");
        }

        // add options common to ISO and template
        String extension = dnld.getFormat().toString().toLowerCase();
        String templateName = "";
        if( extension.equals("iso")) {
            templateName = jobs.get(jobId).getTmpltName().trim().replace(" ", "_");
        } else {
            templateName = java.util.UUID.nameUUIDFromBytes((jobs.get(jobId).getTmpltName() + System.currentTimeMillis()).getBytes()).toString();
        }
        
        String templateFilename = templateName + "." + extension; 
        dnld.setTmpltPath(finalTemplatePath + "/" + templateFilename);
        scr.add("-n", templateFilename);

        scr.add("-t", templatePath);
        scr.add("-f", td.getDownloadLocalPath());
        if (dnld.getChecksum() != null && dnld.getChecksum().length() > 1) {
            scr.add("-c", dnld.getChecksum());
        }
        scr.add("-u"); // cleanup
        String result;
        result = scr.execute();

        if (result != null) {
            return result;
        }
        
        // Set permissions for the downloaded template
        File downloadedTemplate = new File(templatePath + "/" + templateFilename);
        _storage.setWorldReadableAndWriteable(downloadedTemplate);
        
        // Set permissions for template.properties
        File templateProperties = new File(templatePath + "/template.properties");
        _storage.setWorldReadableAndWriteable(templateProperties);

        TemplateLocation loc = new TemplateLocation(_storage, templatePath);
        try {
            loc.create(dnld.getId(), true, dnld.getTmpltName());
        } catch (IOException e) {
            s_logger.warn("Something is wrong with template location " + templatePath, e);
            loc.purge();
            return "Unable to download due to " + e.getMessage();
        }
        
        Enumeration<Processor> en = _processors.enumeration();
        while (en.hasMoreElements()) {
            Processor processor = en.nextElement();
            
            FormatInfo info = null;
			try {
				info = processor.process(templatePath, null, templateName);
			} catch (InternalErrorException e) {
				return e.toString();
			}
            if (info != null) {
                loc.addFormat(info);
                dnld.setTemplatesize(info.virtualSize);
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
    public String downloadPublicTemplate(long id, String url, String name, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum, String installPathPrefix, String user, String password, long maxTemplateSizeInBytes) {
        UUID uuid = new UUID();
        String jobId = uuid.toString();
        String tmpDir = installPathPrefix + File.separator + accountId + File.separator + id;

        try {
            
            if (!_storage.mkdirs(tmpDir)) {
                s_logger.warn("Unable to create " + tmpDir);
                return "Unable to create " + tmpDir;
            }

            File file = _storage.getFile(tmpDir + File.separator + TemplateLocation.Filename);
            
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
                if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
                    td = new HttpTemplateDownloader(_storage, url, tmpDir, new Completion(jobId), maxTemplateSizeInBytes, user, password);
                } else if (uri.getScheme().equalsIgnoreCase("file")) {
                    td = new LocalTemplateDownloader(_storage, url, tmpDir, maxTemplateSizeInBytes, new Completion(jobId));
                } else if (uri.getScheme().equalsIgnoreCase("scp")) {
                    td = new ScpTemplateDownloader(_storage, url, tmpDir, maxTemplateSizeInBytes, new Completion(jobId));
                } else if (uri.getScheme().equalsIgnoreCase("nfs")) {
                    td = null;
                    // TODO: implement this.
                    throw new CloudRuntimeException("Scheme is not supported " + url);
                } else {
                    throw new CloudRuntimeException("Scheme is not supported " + url);
                }
            } else {
                throw new CloudRuntimeException("Unable to download from URL: " + url);
            }
            DownloadJob dj = new DownloadJob(td, jobId, id, name, format, hvm, accountId, descr, cksum, installPathPrefix);
            jobs.put(jobId, dj);
            threadPool.execute(td);

            return jobId;
        } catch (IOException e) {
            s_logger.warn("Unable to download to " + tmpDir, e);
            return null;
        }
    }

    public String getPublicTemplateRepo() {
        return publicTemplateRepo;
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
    public DownloadAnswer handleDownloadCommand(DownloadCommand cmd) {
        if (cmd instanceof DownloadProgressCommand) {
            return handleDownloadProgressCmd((DownloadProgressCommand) cmd);
        }

        if (cmd.getUrl() == null) {
            return new DownloadAnswer(null, 0, "Template is corrupted on storage due to an invalid url , cannot download", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR, "", "", 0);
        }

        if (cmd.getName() == null) {
            return new DownloadAnswer(null, 0, "Invalid Name", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR, "", "", 0);
        }

        String installPathPrefix = null;
        installPathPrefix = publicTemplateRepo;

        String user = null;
        String password = null;
        if (cmd.getAuth() != null) {
        	user = cmd.getAuth().getUserName();
        	password = new String(cmd.getAuth().getPassword());
        }
        
        long maxDownloadSizeInBytes = (cmd.getMaxDownloadSizeInBytes() == null) ? TemplateDownloader.DEFAULT_MAX_TEMPLATE_SIZE_IN_BYTES : (cmd.getMaxDownloadSizeInBytes());
        String jobId = downloadPublicTemplate(cmd.getId(), cmd.getUrl(), cmd.getName(), cmd.getFormat(), cmd.isHvm(), cmd.getAccountId(), cmd.getDescription(), cmd.getChecksum(), installPathPrefix, user, password, maxDownloadSizeInBytes);
        sleep();
        if (jobId == null) {
            return new DownloadAnswer(null, 0, "Internal Error", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR, "", "", 0);
        }
        return new DownloadAnswer(jobId, getDownloadPct(jobId), getDownloadError(jobId), getDownloadStatus2(jobId), getDownloadLocalPath(jobId), getInstallPath(jobId),
                getDownloadTemplateSize(jobId));
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private DownloadAnswer handleDownloadProgressCmd(DownloadProgressCommand cmd) {
        String jobId = cmd.getJobId();
        DownloadAnswer answer;
        DownloadJob dj = null;
        if (jobId != null)
            dj = jobs.get(jobId);
        if (dj == null) {
            if (cmd.getRequest() == RequestType.GET_OR_RESTART) {
                DownloadCommand dcmd = new DownloadCommand(cmd);
                return handleDownloadCommand(dcmd);
            } else {
                return new DownloadAnswer(null, 0, "Cannot find job", com.cloud.storage.VMTemplateHostVO.Status.UNKNOWN, "", "", 0);
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
            answer = new DownloadAnswer(jobId, getDownloadPct(jobId), getDownloadError(jobId), getDownloadStatus2(jobId), getDownloadLocalPath(jobId), getInstallPath(jobId), getDownloadTemplateSize(jobId));
            jobs.remove(jobId);
            return answer;
        default:
            break; // TODO
        }
        return new DownloadAnswer(jobId, getDownloadPct(jobId), getDownloadError(jobId), getDownloadStatus2(jobId), getDownloadLocalPath(jobId), getInstallPath(jobId),
                getDownloadTemplateSize(jobId));
    }

    private String getInstallPath(String jobId) {
        DownloadJob dj = jobs.get(jobId);
        if (dj != null) {
            return dj.getTmpltPath();
        }
        return null;
    }

    private String createTempDir(File rootDir, String name) throws IOException {

        File f = File.createTempFile(name, "", rootDir);
        f.delete();
        f.mkdir();
        _storage.setWorldReadableAndWriteable(f);
        return f.getAbsolutePath();

    }

    @Override
    public List<String> listPublicTemplates() {
        return listTemplates(publicTemplateRepo);
    }

    private List<String> listTemplates(String rootdir) {
        List<String> result = new ArrayList<String>();
        Script script = new Script(listTmpltScr, s_logger);
        script.add("-r", rootdir);
        ZfsPathParser zpp = new ZfsPathParser(rootdir);
        script.execute(zpp);
        result.addAll(zpp.getPaths());
        s_logger.info("found " + zpp.getPaths().size() + " templates");
        return result;
    }

    @Override
    public Map<String, TemplateInfo> gatherTemplateInfo() {
        Map<String, TemplateInfo> result = new HashMap<String, TemplateInfo>();
        List<String> publicTmplts = listPublicTemplates();
        for (String tmplt : publicTmplts) {
            String path = tmplt.substring(0, tmplt.lastIndexOf(File.separator));
            TemplateLocation loc = new TemplateLocation(_storage, path);
            try {
                if (!loc.load()) {
                    s_logger.warn("Post download installation was not completed for " + path);
                    loc.purge();
                    _storage.cleanup(path, publicTemplateRepo);
                    continue;
                }
            } catch (IOException e) {
                s_logger.warn("Unable to load template location " + path, e);
                loc.purge();
                try {
                    _storage.cleanup(path, publicTemplateRepo);
                } catch (IOException e1) {
                    s_logger.warn("Unable to cleanup " + path, e1);
                }
                continue;
            }
            
            TemplateInfo tInfo = loc.getTemplateInfo();

            result.put(tInfo.templateName, tInfo);
            s_logger.debug("Added template name: " + tInfo.templateName + ", path: " + tmplt);
        }
        /*
        for (String tmplt : isoTmplts) {
            String tmp[];
            tmp = tmplt.split("/");
            String tmpltName = tmp[tmp.length - 2];
            tmplt = tmplt.substring(tmplt.lastIndexOf("iso/"));
            TemplateInfo tInfo = new TemplateInfo(tmpltName, tmplt, false);
            s_logger.debug("Added iso template name: " + tmpltName + ", path: " + tmplt);
            result.put(tmpltName, tInfo);
        }
        */
        return result;
    }

    private int deleteDownloadDirectories(File downloadPath, int deleted) {
        try {
            if (downloadPath.exists()) {
                File[] files = downloadPath.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDownloadDirectories(files[i], deleted);
                        files[i].delete();
                        deleted++;
                    } else {
                        files[i].delete();
                        deleted++;
                    }
                }
            }
        } catch (Exception ex) {
            s_logger.info("Failed to clean up template downloads directory " + ex.toString());
        }
        return deleted;
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
    }

    public DownloadManagerImpl() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        String value = null;

        _storage = (StorageLayer) params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            value = (String) params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                throw new ConfigurationException("Unable to find the storage layer");
            }

            Class<StorageLayer> clazz;
            try {
                clazz = (Class<StorageLayer>) Class.forName(value);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to instantiate " + value);
            }
            _storage = ComponentLocator.inject(clazz);
        }
        String useSsl = (String)params.get("sslcopy");
        if (useSsl != null) {
        	_sslCopy = Boolean.parseBoolean(useSsl);
        	
        }
        configureFolders(name, params);
        String inSystemVM = (String)params.get("secondary.storage.vm");
        if (inSystemVM != null && "true".equalsIgnoreCase(inSystemVM)) {
        	s_logger.info("DownloadManager: starting additional services since we are inside system vm");
        	startAdditionalServices();
        	blockOutgoingOnPrivate();
        }

        value = (String) params.get("install.timeout.pergig");
        this.installTimeoutPerGig = NumbersUtil.parseInt(value, 15 * 60) * 1000;

        value = (String) params.get("install.numthreads");
        final int numInstallThreads = NumbersUtil.parseInt(value, 10);        

        String scriptsDir = (String) params.get("template.scripts.dir");
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

        List<Processor> processors = new ArrayList<Processor>();
        _processors = new Adapters<Processor>("processors", processors);
        Processor processor = new VhdProcessor();
        
        processor.configure("VHD Processor", params);
        processors.add(processor);
        
        processor = new IsoProcessor();
        processor.configure("ISO Processor", params);
        processors.add(processor);
        
        processor = new QCOW2Processor();
        processor.configure("QCOW2 Processor", params);
        processors.add(processor);
        // Add more processors here.
        threadPool = Executors.newFixedThreadPool(numInstallThreads);
        return true;
    }

    private void blockOutgoingOnPrivate() {
    	Script command = new Script("/bin/bash", s_logger);
    	String intf = "eth1";
    	command.add("-c");
    	command.add("iptables -A OUTPUT -o " + intf + " -p tcp -m state --state NEW -m tcp --dport " + "80" + " -j REJECT;" +
    			"iptables -A OUTPUT -o " + intf + " -p tcp -m state --state NEW -m tcp --dport " + "443" + " -j REJECT;");

    	String result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in blocking outgoing to port 80/443 err=" + result );
    		return;
    	}		
	}

	protected void configureFolders(String name, Map<String, Object> params) throws ConfigurationException {
        parentDir = (String) params.get("template.parent");
        if (parentDir == null) {
            throw new ConfigurationException("Unable to find the parent root for the templates");
        }

        String value = (String) params.get("public.templates.root.dir");
        if (value == null) {
            value = TemplateConstants.DEFAULT_TMPLT_ROOT_DIR;
        }
        
        if (value.startsWith(File.separator)) {
            publicTemplateRepo = value;
        } else {
            publicTemplateRepo = parentDir + File.separator + value;
        }
        
        if (!publicTemplateRepo.endsWith(File.separator)) {
            publicTemplateRepo += File.separator;
        }
        
        publicTemplateRepo += TemplateConstants.DEFAULT_TMPLT_FIRST_LEVEL_DIR;
        
        if (!_storage.mkdirs(publicTemplateRepo)) {
            throw new ConfigurationException("Unable to create public templates directory");
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
    	
    	Script command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("service httpd stop ");
    	String result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in stopping httpd service err=" + result );
    	}
    	String port = Integer.toString(TemplateConstants.DEFAULT_TMPLT_COPY_PORT);
    	String intf = TemplateConstants.DEFAULT_TMPLT_COPY_INTF;
    	
    	command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("iptables -D INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + port + " -j DROP;" +
			        "iptables -D INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + port + " -j HTTP;" +
			        "iptables -D INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + "443" + " -j DROP;" +
			        "iptables -D INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + "443" + " -j HTTP;" +
			        "iptables -F HTTP;" +
			        "iptables -X HTTP;" +
			        "iptables -N HTTP;" +
    			    "iptables -I INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + port + " -j DROP;" +
    			    "iptables -I INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + "443" + " -j DROP;" +
    			    "iptables -I INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + port + " -j HTTP;" +
    	            "iptables -I INPUT -i " + intf + " -p tcp -m state --state NEW -m tcp --dport " + "443" + " -j HTTP;");

    	result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in opening up httpd port err=" + result );
    		return;
    	}
    	
    	command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("service httpd start ");
    	result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in starting httpd service err=" + result );
    		return;
    	}
    	command = new Script("mkdir", s_logger);
		command.add("-p");
    	command.add("/var/www/html/copy/template");
    	result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in creating directory =" + result );
    		return;
    	}
    	
    	command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("ln -sf " + publicTemplateRepo + " /var/www/html/copy/template");
    	result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in linking  err=" + result );
    		return;
    	}
	}
}
