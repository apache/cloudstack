package com.cloud.storage.template;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.log4j.Logger;


public class FtpTemplateUploader implements TemplateUploader {
	
	public static final Logger s_logger = Logger.getLogger(FtpTemplateUploader.class.getName());
	public TemplateUploader.Status status = TemplateUploader.Status.NOT_STARTED;
	public String errorString = "";
	public long totalBytes = 0;
	public long templateSizeinBytes;
	private String sourcePath;
	private String ftpUrl;	
	private UploadCompleteCallback completionCallback;
	private boolean resume;
    private BufferedInputStream inputStream = null;
    private BufferedOutputStream outputStream = null;
	private static final int CHUNK_SIZE = 1024*1024; //1M
	
	public FtpTemplateUploader(String sourcePath, String url, UploadCompleteCallback callback, long templateSizeinBytes){
		
		this.sourcePath = sourcePath;
		this.ftpUrl = url;
		this.completionCallback = callback;
		this.templateSizeinBytes = templateSizeinBytes;
		
	}
	
	public long upload(UploadCompleteCallback callback )
		   {
		
				switch (status) {
				case ABORTED:
				case UNRECOVERABLE_ERROR:
				case UPLOAD_FINISHED:
					return 0;
				default:
		
				}
				
	             Date start = new Date();
				 
		         StringBuffer sb = new StringBuffer();
		         // check for authentication else assume its anonymous access.
		        /* if (user != null && password != null)
		         {
		            sb.append( user );
		            sb.append( ':' );
		            sb.append( password );
		            sb.append( '@' );
		         }*/
		         sb.append( ftpUrl );
		         /*sb.append( '/' );
		         sb.append( fileName ); filename where u want to dld it */
		         /*ftp://10.91.18.14/
		          * type ==> a=ASCII mode, i=image (binary) mode, d= file directory
		          * listing
		          */
		         sb.append( ";type=i" );

		         try
		         {
		            URL url = new URL( sb.toString() );
		            URLConnection urlc = url.openConnection();
		            File sourceFile = new File(sourcePath);
		            templateSizeinBytes = sourceFile.length();

		            outputStream = new BufferedOutputStream( urlc.getOutputStream() );
		            inputStream = new BufferedInputStream( new FileInputStream(sourceFile) );            

		            status = TemplateUploader.Status.IN_PROGRESS;

		            int bytes = 0;
		            byte[] block = new byte[CHUNK_SIZE];
		            boolean done=false;
		            while (!done && status != Status.ABORTED ) {
		            	if ( (bytes = inputStream.read(block, 0, CHUNK_SIZE)) > -1) {
		            		outputStream.write(block,0, bytes);		            			            				            			            		
		            		totalBytes += bytes;
		            	} else {
		            		done = true;
		            	}
		            }		            
		            status = TemplateUploader.Status.UPLOAD_FINISHED;		            
		            return totalBytes;
		         } catch (MalformedURLException e) {
		        	status = TemplateUploader.Status.UNRECOVERABLE_ERROR;
		 			errorString = e.getMessage();
		 			s_logger.error(errorString);
				} catch (IOException e) {
					status = TemplateUploader.Status.UNRECOVERABLE_ERROR;
		 			errorString = e.getMessage();
		 			s_logger.error(errorString);
				}
		         finally
		         {
		           try		         
	               {
		            if (inputStream != null){		               
		            	inputStream.close();
		            }
		            if (outputStream != null){		               
		                  outputStream.close();
		            }
	               }catch (IOException ioe){
	            	   s_logger.error(" Caught exception while closing the resources" ); 		                  
	               }
				   if (callback != null) {
					   callback.uploadComplete(status);
				   }
		         }

				return 0;
		   }

	@Override
	public void run() {
		try {
			upload(completionCallback);
		} catch (Throwable t) {
			s_logger.warn("Caught exception during upload "+ t.getMessage(), t);
			errorString = "Failed to install: " + t.getMessage();
			status = TemplateUploader.Status.UNRECOVERABLE_ERROR;
		}
		
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public String getUploadError() {
		return errorString;
	}

	@Override
	public String getUploadLocalPath() {
		return null;
	}

	@Override
	public int getUploadPercent() {
		if (templateSizeinBytes == 0) {
			return 0;
		}		
		return (int)(100.0*totalBytes/templateSizeinBytes);
	}

	@Override
	public long getUploadTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getUploadedBytes() {
		return totalBytes;
	}

	@Override
	public boolean isInited() {
		return false;
	}

	@Override
	public void setResume(boolean resume) {
		this.resume = resume;
		
	}

	@Override
	public void setStatus(Status status) {
		this.status = status;		
	}

	@Override
	public void setUploadError(String string) {
		errorString = string;		
	}

	@Override
	public boolean stopUpload() {
		switch (getStatus()) {
		case IN_PROGRESS:
			try {
				if(outputStream != null) {
					outputStream.close();
				}
				if (inputStream != null){				
					inputStream.close();					
				}
			} catch (IOException e) {
				s_logger.error(" Caught exception while closing the resources" );
			}
			status = TemplateUploader.Status.ABORTED;
			return true;
		case UNKNOWN:
		case NOT_STARTED:
		case RECOVERABLE_ERROR:
		case UNRECOVERABLE_ERROR:
		case ABORTED:
			status = TemplateUploader.Status.ABORTED;
		case UPLOAD_FINISHED:
			return true;

		default:
			return true;
		}
	}
	

}
