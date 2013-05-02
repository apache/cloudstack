package com.cloud.storage.resource;

import static com.cloud.utils.StringUtils.join;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.DownloadSystemTemplateCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.template.DownloadManagerImpl;
import com.cloud.utils.S3Utils;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class LocalNfsSecondaryStorageResource extends
		NfsSecondaryStorageResource {

    public LocalNfsSecondaryStorageResource(){
        _dlMgr = new DownloadManagerImpl();
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadSystemTemplateCommand){
            return execute((DownloadSystemTemplateCommand)cmd);
        } else {
            //return Answer.createUnsupportedCommandAnswer(cmd);
            return super.executeRequest(cmd);
        }
    }

    private Answer execute(DownloadSystemTemplateCommand cmd){
        DataStoreTO dstore = cmd.getDataStore();
        if ( dstore instanceof S3TO ){
            //TODO: how to handle download progress for S3
            S3TO s3 = (S3TO)cmd.getDataStore();
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
            // convention is no / in the end for install path based on S3Utils implementation.
            String path = determineS3TemplateDirectory(cmd.getAccountId(), cmd.getResourceId(), cmd.getName());
            // template key is
            // TEMPLATE_ROOT_DIR/account_id/template_id/template_name
            String key = join(asList(path, urlObj.getFile()), S3Utils.SEPARATOR);
            S3Utils.putObject(s3, in, bucket, key);
            List<S3ObjectSummary> s3Obj = S3Utils.getDirectory(s3, bucket, path);
            if (s3Obj == null || s3Obj.size() == 0) {
                return new Answer(cmd, false, "Failed to download to S3 bucket: " + bucket + " with key: " + key);
            } else {
                return new DownloadAnswer(null, 100, null, Status.DOWNLOADED, path, path, s3Obj.get(0).getSize(), s3Obj.get(0).getSize(), s3Obj.get(0)
                        .getETag());
            }
        }
        else if ( dstore instanceof NfsTO ){
            return new Answer(cmd, false, "Nfs needs to be pre-installed with system vm templates");
        }
        else if ( dstore instanceof SwiftTO ){
            //TODO: need to move code from execute(uploadTemplateToSwiftFromSecondaryStorageCommand) here, but we need to handle
            // source is url, most likely we need to modify our existing swiftUpload python script.
            return new Answer(cmd, false, "Swift is not currently support DownloadCommand");
        }
        else{
            return new Answer(cmd, false, "Unsupported image data store: " + dstore);
        }
    }
}
