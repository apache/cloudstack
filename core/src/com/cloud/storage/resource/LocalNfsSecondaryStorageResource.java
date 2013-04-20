package com.cloud.storage.resource;

import static com.cloud.utils.StringUtils.join;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DownloadSystemTemplateCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.utils.S3Utils;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class LocalNfsSecondaryStorageResource extends
		NfsSecondaryStorageResource {

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadSystemTemplateCommand){
            return execute((DownloadSystemTemplateCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
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
            String key = join(asList(determineS3TemplateDirectory(cmd.getAccountId(), cmd.getResourceId()), urlObj.getFile()), S3Utils.SEPARATOR);
            S3Utils.putObject(s3, in, bucket, key);
            return new Answer(cmd, true, format("Uploaded the contents of input stream from %1$s for template id %2$s to S3 bucket %3$s", url,
                    cmd.getResourceId(), bucket));
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
