package org.apache.cloudstack.storage.resource;

import static com.cloud.utils.StringUtils.join;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.cloudstack.storage.command.DownloadSystemTemplateCommand;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import org.apache.cloudstack.storage.template.DownloadManagerImpl;
import org.apache.cloudstack.storage.template.DownloadManagerImpl.ZfsPathParser;
import com.cloud.utils.S3Utils;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@Component
public class LocalNfsSecondaryStorageResource extends NfsSecondaryStorageResource {

    private static final Logger s_logger = Logger.getLogger(NfsSecondaryStorageResource.class);

    public LocalNfsSecondaryStorageResource() {
        this._dlMgr = new DownloadManagerImpl();
        ((DownloadManagerImpl) _dlMgr).setThreadPool(Executors.newFixedThreadPool(10));
        _storage = new JavaStorageLayer();
        this._inSystemVM = false;
        System.setProperty("paths.script", "/Users/minc/dev/cloud-asf"); // This
                                                                         // is
                                                                         // just
                                                                         // for
                                                                         // my
                                                                         // testing,
                                                                         // not
                                                                         // for
                                                                         // QA
                                                                         // build
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadSystemTemplateCommand) {
            return execute((DownloadSystemTemplateCommand) cmd);
        } else {
            // return Answer.createUnsupportedCommandAnswer(cmd);
            return super.executeRequest(cmd);
        }
    }

    private Answer execute(DownloadSystemTemplateCommand cmd) {
        DataStoreTO dstore = cmd.getDataStore();
        if (dstore instanceof S3TO) {
            // TODO: how to handle download progress for S3
            S3TO s3 = (S3TO) cmd.getDataStore();
            String url = cmd.getUrl();
            String user = null;
            String password = null;
            if (cmd.getAuth() != null) {
                user = cmd.getAuth().getUserName();
                password = new String(cmd.getAuth().getPassword());
            }
            // get input stream from the given url
            InputStream in = UriUtils.getInputStreamFromUrl(url, user, password);
            URI uri;
            URL urlObj;
            try {
                uri = new URI(url);
                urlObj = new URL(url);
            } catch (URISyntaxException e) {
                throw new CloudRuntimeException("URI is incorrect: " + url);
            } catch (MalformedURLException e) {
                throw new CloudRuntimeException("URL is incorrect: " + url);
            }

            final String bucket = s3.getBucketName();
            // convention is no / in the end for install path based on S3Utils
            // implementation.
            String path = determineS3TemplateDirectory(cmd.getAccountId(), cmd.getResourceId(), cmd.getName());
            // template key is
            // TEMPLATE_ROOT_DIR/account_id/template_id/template_name
            String key = join(asList(path, urlObj.getFile()), S3Utils.SEPARATOR);
            S3Utils.putObject(s3, in, bucket, key);
            List<S3ObjectSummary> s3Obj = S3Utils.getDirectory(s3, bucket, path);
            if (s3Obj == null || s3Obj.size() == 0) {
                return new Answer(cmd, false, "Failed to download to S3 bucket: " + bucket + " with key: " + key);
            } else {
                return new DownloadAnswer(null, 100, null, Status.DOWNLOADED, path, path, s3Obj.get(0).getSize(), s3Obj.get(0).getSize(), s3Obj
                        .get(0).getETag());
            }
        } else if (dstore instanceof NfsTO) {
            return new Answer(cmd, false, "Nfs needs to be pre-installed with system vm templates");
        } else if (dstore instanceof SwiftTO) {
            // TODO: need to move code from
            // execute(uploadTemplateToSwiftFromSecondaryStorageCommand) here,
            // but we need to handle
            // source is url, most likely we need to modify our existing
            // swiftUpload python script.
            return new Answer(cmd, false, "Swift is not currently support DownloadCommand");
        } else {
            return new Answer(cmd, false, "Unsupported image data store: " + dstore);
        }
    }

    @Override
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
        if ("Mac OS X".equalsIgnoreCase(System.getProperty("os.name"))) {
            command.add("-o", "resvport");
        }
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

        // Change permissions for the mountpoint
        script = new Script(true, "chmod", _timeout, s_logger);
        script.add("777", root);
        result = script.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for " + root + " due to " + result);
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

}
