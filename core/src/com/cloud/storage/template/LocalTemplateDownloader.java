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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.cloud.storage.StorageLayer;



public class LocalTemplateDownloader extends TemplateDownloaderBase implements TemplateDownloader {
    public static final Logger s_logger = Logger.getLogger(LocalTemplateDownloader.class);
    
    public LocalTemplateDownloader(StorageLayer storageLayer, String downloadUrl, String toDir, long maxTemplateSizeInBytes, DownloadCompleteCallback callback) {
        super(storageLayer, downloadUrl, toDir, maxTemplateSizeInBytes, callback);
        String filename = downloadUrl.substring(downloadUrl.lastIndexOf(File.separator));
        _toFile = toDir.endsWith(File.separator) ? (toDir + filename) : (toDir + File.separator + filename);
    }

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (_status == Status.ABORTED ||
            _status == Status.UNRECOVERABLE_ERROR ||
            _status == Status.DOWNLOAD_FINISHED) {
            return 0;
        }

        _start = System.currentTimeMillis();
        _resume = resume;
        
        File src;
        try {
            src = new File(new URI(_downloadUrl));
        } catch (URISyntaxException e1) {
            s_logger.warn("Invalid URI " + _downloadUrl);
            _status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }
        File dst = new File(_toFile);
        
        FileChannel fic = null;
        FileChannel foc = null;
        
        try {
        	if (_storage != null) {
        		dst.createNewFile();
            	_storage.setWorldReadableAndWriteable(dst);
        	}   	
        	
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 512);
            FileInputStream fis;
            try {
                fis = new FileInputStream(src);
            } catch (FileNotFoundException e) {
                s_logger.warn("Unable to find " + _downloadUrl);
                _errorString = "Unable to find " + _downloadUrl;
                return -1;
            }
            fic = fis.getChannel();
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(dst);
            } catch (FileNotFoundException e) {
                s_logger.warn("Unable to find " + _toFile);
                return -1;
            }
            foc = fos.getChannel();
            
            _remoteSize = src.length();
            this._totalBytes = 0;
            _status = TemplateDownloader.Status.IN_PROGRESS;
            
            try {
                while (_status != Status.ABORTED && fic.read(buffer) != -1) {
                    buffer.flip();
                    int count = foc.write(buffer);
                    _totalBytes += count;
                    buffer.clear();
                }
            } catch (IOException e) {
                s_logger.warn("Unable to download", e);
            }
            
            String downloaded = "(incomplete download)";
            if (_totalBytes == _remoteSize) {
                _status = TemplateDownloader.Status.DOWNLOAD_FINISHED;
                downloaded = "(download complete)";
            }
            
            _errorString = "Downloaded " + _remoteSize + " bytes " + downloaded;
            _downloadTime += System.currentTimeMillis() - _start;
            return _totalBytes;
        } catch (Exception e) {
            _status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            _errorString = e.getMessage();
            return 0;
        } finally {
            if (fic != null) {
                try {
                    fic.close();
                } catch (IOException e) {
                }
            }
            
            if (foc != null) {
                try {
                    foc.close();
                } catch (IOException e) {
                }
            }
            
            if (_status == Status.UNRECOVERABLE_ERROR && dst.exists()) {
                dst.delete();
            }
            if (callback != null) {
                callback.downloadComplete(_status);
            }
        }
    }
    
    public static void main(String[] args) {
        String url ="file:///home/ahuang/Download/E3921_P5N7A-VM_manual.zip";
        TemplateDownloader td = new LocalTemplateDownloader(null, url,"/tmp/mysql", TemplateDownloader.DEFAULT_MAX_TEMPLATE_SIZE_IN_BYTES, null);
        long bytes = td.download(true, null);
        if (bytes > 0) {
            System.out.println("Downloaded  (" + bytes + " bytes)" + " in " + td.getDownloadTime()/1000 + " secs");
        } else {
            System.out.println("Failed download");
        }

    }
}
