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
package com.cloud.storage.template;


import static com.cloud.utils.StringUtils.join;
import static java.util.Arrays.asList;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;
import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.cloud.agent.api.storage.Proxy;
import com.cloud.agent.api.to.S3TO;
import com.cloud.utils.Pair;
import com.cloud.utils.S3Utils;
import com.cloud.utils.UriUtils;

/**
 * Download a template file using HTTP
 *
 */
public class S3TemplateDownloader implements TemplateDownloader {
	public static final Logger s_logger = Logger.getLogger(S3TemplateDownloader.class.getName());
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

	private String downloadUrl;
	private String installPath;
	private String s3Key;
	private String fileName;
	public TemplateDownloader.Status status= TemplateDownloader.Status.NOT_STARTED;
	public String errorString = " ";
	private long remoteSize = 0;
	public long downloadTime = 0;
	public long totalBytes;
	private final HttpClient client;
	private GetMethod request;
	private boolean resume = false;
	private DownloadCompleteCallback completionCallback;
	S3TO s3;
	boolean inited = true;

	private long MAX_TEMPLATE_SIZE_IN_BYTES;
	private ResourceType resourceType = ResourceType.TEMPLATE;
	private final HttpMethodRetryHandler myretryhandler;



	public S3TemplateDownloader (S3TO storageLayer, String downloadUrl, String installPath, DownloadCompleteCallback callback, long maxTemplateSizeInBytes, String user, String password, Proxy proxy, ResourceType resourceType) {
		this.s3 = storageLayer;
		this.downloadUrl = downloadUrl;
		this.installPath = installPath;
		this.status = TemplateDownloader.Status.NOT_STARTED;
		this.resourceType = resourceType;
		this.MAX_TEMPLATE_SIZE_IN_BYTES = maxTemplateSizeInBytes;

		this.totalBytes = 0;
		this.client = new HttpClient(s_httpClientManager);

		myretryhandler = new HttpMethodRetryHandler() {
		    @Override
            public boolean retryMethod(
		        final HttpMethod method,
		        final IOException exception,
		        int executionCount) {
		        if (executionCount >= 2) {
		            // Do not retry if over max retry count
		            return false;
		        }
		        if (exception instanceof NoHttpResponseException) {
		            // Retry if the server dropped connection on us
		            return true;
		        }
		        if (!method.isRequestSent()) {
		            // Retry if the request has not been sent fully or
		            // if it's OK to retry methods that have been sent
		            return true;
		        }
		        // otherwise do not retry
		        return false;
		    }
		};

		try {
			this.request = new GetMethod(downloadUrl);
			this.request.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, myretryhandler);
			this.completionCallback = callback;

			Pair<String, Integer> hostAndPort = UriUtils.validateUrl(downloadUrl);
            this.fileName = StringUtils.substringAfterLast(downloadUrl, "/");

			if (proxy != null) {
				client.getHostConfiguration().setProxy(proxy.getHost(), proxy.getPort());
				if (proxy.getUserName() != null) {
					Credentials proxyCreds = new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPassword());
					client.getState().setProxyCredentials(AuthScope.ANY, proxyCreds);
				}
			}
			if ((user != null) && (password != null)) {
				client.getParams().setAuthenticationPreemptive(true);
				Credentials defaultcreds = new UsernamePasswordCredentials(user, password);
				client.getState().setCredentials(new AuthScope(hostAndPort.first(), hostAndPort.second(), AuthScope.ANY_REALM), defaultcreds);
				s_logger.info("Added username=" + user + ", password=" + password + "for host " + hostAndPort.first() + ":" + hostAndPort.second());
			} else {
				s_logger.info("No credentials configured for host=" + hostAndPort.first() + ":" + hostAndPort.second());
			}
		} catch (IllegalArgumentException iae) {
			errorString = iae.getMessage();
			status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
			inited = false;
		} catch (Exception ex){
			errorString = "Unable to start download -- check url? ";
			status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
			s_logger.warn("Exception in constructor -- " + ex.toString());
		} catch (Throwable th) {
		    s_logger.warn("throwable caught ", th);
		}
	}


	@Override
	public long download(boolean resume, DownloadCompleteCallback callback) {
		switch (status) {
		case ABORTED:
		case UNRECOVERABLE_ERROR:
		case DOWNLOAD_FINISHED:
			return 0;
		default:

		}


        int bytes=0;
		try {
	        // execute get method
	        int responseCode = HttpStatus.SC_OK;
	        if ((responseCode = client.executeMethod(request)) != HttpStatus.SC_OK) {
	            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
	            errorString = " HTTP Server returned " + responseCode + " (expected 200 OK) ";
	            return 0; //FIXME: retry?
	        }
		    // get the total size of file
            Header contentLengthHeader = request.getResponseHeader("Content-Length");
            boolean chunked = false;
            long remoteSize2 = 0;
            if (contentLengthHeader == null) {
            	Header chunkedHeader = request.getResponseHeader("Transfer-Encoding");
            	if (chunkedHeader == null || !"chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
            		status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            		errorString=" Failed to receive length of download ";
            		return 0; //FIXME: what status do we put here? Do we retry?
            	} else if ("chunked".equalsIgnoreCase(chunkedHeader.getValue())){
            		chunked = true;
            	}
            } else {
            	remoteSize2 = Long.parseLong(contentLengthHeader.getValue());
            }

            if (remoteSize == 0) {
            	remoteSize = remoteSize2;
            }

            if (remoteSize > MAX_TEMPLATE_SIZE_IN_BYTES) {
            	s_logger.info("Remote size is too large: " + remoteSize + " , max=" + MAX_TEMPLATE_SIZE_IN_BYTES);
            	status = Status.UNRECOVERABLE_ERROR;
            	errorString = "Download file size is too large";
            	return 0;
            }

            if (remoteSize == 0) {
            	remoteSize = MAX_TEMPLATE_SIZE_IN_BYTES;
            }

            InputStream in = !chunked?new BufferedInputStream(request.getResponseBodyAsStream())
            						: new ChunkedInputStream(request.getResponseBodyAsStream());

            s_logger.info("Starting download from " + getDownloadUrl() + " to s3 bucket " + s3.getBucketName() + " remoteSize=" + remoteSize + " , max size=" + MAX_TEMPLATE_SIZE_IN_BYTES);

            Date start = new Date();
            // compute s3 key
            s3Key = join(asList(installPath, fileName), S3Utils.SEPARATOR);

            // download using S3 API
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(remoteSize);
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    s3.getBucketName(), s3Key, in, metadata)
                    .withStorageClass(StorageClass.ReducedRedundancy);
            // register progress listenser
            putObjectRequest
                    .setProgressListener(new ProgressListener() {
                        @Override
                        public void progressChanged(
                                ProgressEvent progressEvent) {
                           // s_logger.debug(progressEvent.getBytesTransfered()
                           //         + " number of byte transferd "
                           //         + new Date());
                            totalBytes += progressEvent.getBytesTransfered();
                            if (progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                                s_logger.info("download completed");
                                status = TemplateDownloader.Status.DOWNLOAD_FINISHED;
                            } else if (progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE){
                                status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
                            } else if (progressEvent.getEventCode() == ProgressEvent.CANCELED_EVENT_CODE){
                                status = TemplateDownloader.Status.ABORTED;
                            } else{
                                status = TemplateDownloader.Status.IN_PROGRESS;
                            }
                        }

                    });
            S3Utils.putObject(s3, putObjectRequest);
            while (status != TemplateDownloader.Status.DOWNLOAD_FINISHED &&
                    status != TemplateDownloader.Status.UNRECOVERABLE_ERROR &&
                    status != TemplateDownloader.Status.ABORTED ){
                // wait for completion
            }
            // finished or aborted
            Date finish = new Date();
            String downloaded = "(incomplete download)";
            if (totalBytes >= remoteSize) {
            	status = TemplateDownloader.Status.DOWNLOAD_FINISHED;
            	downloaded = "(download complete remote=" + remoteSize + "bytes)";
            } else {
                errorString = "Downloaded " + totalBytes + " bytes " + downloaded;
            }
            downloadTime += finish.getTime() - start.getTime();
            return totalBytes;
		}catch (HttpException hte) {
			status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
			errorString = hte.getMessage();
		} catch (IOException ioe) {
			status = TemplateDownloader.Status.UNRECOVERABLE_ERROR; //probably a file write error?
			errorString = ioe.getMessage();
		} catch (AmazonClientException ex) {
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR; // S3 api exception
            errorString = ex.getMessage();
		} finally {
		    // close input stream
			request.releaseConnection();
            if (callback != null) {
            	callback.downloadComplete(status);
            }
		}
		return 0;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}


	@Override
    public TemplateDownloader.Status getStatus() {
		return status;
	}


	@Override
    public long getDownloadTime() {
		return downloadTime;
	}


	@Override
    public long getDownloadedBytes() {
		return totalBytes;
	}

	@Override
	@SuppressWarnings("fallthrough")
	public boolean stopDownload() {
		switch (getStatus()) {
		case IN_PROGRESS:
			if (request != null) {
				request.abort();
			}
			status = TemplateDownloader.Status.ABORTED;
			return true;
		case UNKNOWN:
		case NOT_STARTED:
		case RECOVERABLE_ERROR:
		case UNRECOVERABLE_ERROR:
		case ABORTED:
			status = TemplateDownloader.Status.ABORTED;
		case DOWNLOAD_FINISHED:
            try {
                S3Utils.deleteObject(s3, s3.getBucketName(), s3Key);
            } catch (Exception ex) {
                // ignore delete exception if it is not there
            }
			return true;

		default:
			return true;
		}
	}

	@Override
	public int getDownloadPercent() {
		if (remoteSize == 0) {
			return 0;
		}

		return (int)(100.0*totalBytes/remoteSize);
	}

	@Override
	public void run() {
		try {
			download(resume, completionCallback);
		} catch (Throwable t) {
			s_logger.warn("Caught exception during download "+ t.getMessage(), t);
			errorString = "Failed to install: " + t.getMessage();
			status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
		}

	}

	@Override
	public void setStatus(TemplateDownloader.Status status) {
		this.status = status;
	}



	public boolean isResume() {
		return resume;
	}

	@Override
	public String getDownloadError() {
		return errorString;
	}

	@Override
	public String getDownloadLocalPath() {
		return this.s3Key;
	}

	@Override
    public void setResume(boolean resume) {
		this.resume = resume;
	}


	@Override
    public long getMaxTemplateSizeInBytes() {
		return this.MAX_TEMPLATE_SIZE_IN_BYTES;
	}

	public static void main(String[] args) {
		String url ="http://dev.mysql.com/get/Downloads/MySQL-5.0/mysql-noinstall-5.0.77-win32.zip/from/http://mirror.services.wisc.edu/mysql/";
		try {
			URI uri = new java.net.URI(url);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TemplateDownloader td = new S3TemplateDownloader(null, url,"/tmp/mysql", null, TemplateDownloader.DEFAULT_MAX_TEMPLATE_SIZE_IN_BYTES, null, null, null, null);
		long bytes = td.download(true, null);
		if (bytes > 0) {
			System.out.println("Downloaded  (" + bytes + " bytes)" + " in " + td.getDownloadTime()/1000 + " secs");
		} else {
			System.out.println("Failed download");
		}

	}

	@Override
	public void setDownloadError(String error) {
		errorString = error;
	}



	@Override
	public boolean isInited() {
		return inited;
	}


	public ResourceType getResourceType() {
		return resourceType;
	}

}
