//
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
//

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
        if (_status == Status.ABORTED || _status == Status.UNRECOVERABLE_ERROR || _status == Status.DOWNLOAD_FINISHED) {
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
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            if (_storage != null) {
                dst.createNewFile();
                _storage.setWorldReadableAndWriteable(dst);
            }

            ByteBuffer buffer = ByteBuffer.allocate(1024 * 512);

            try {
                fis = new FileInputStream(src);
            } catch (FileNotFoundException e) {
                s_logger.warn("Unable to find " + _downloadUrl);
                _errorString = "Unable to find " + _downloadUrl;
                return -1;
            }
            fic = fis.getChannel();
            try {
                fos = new FileOutputStream(dst);
            } catch (FileNotFoundException e) {
                s_logger.warn("Unable to find " + _toFile);
                return -1;
            }
            foc = fos.getChannel();

            _remoteSize = src.length();
            _totalBytes = 0;
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
                    s_logger.info("[ignore] error while closing file input channel.");
                }
            }

            if (foc != null) {
                try {
                    foc.close();
                } catch (IOException e) {
                    s_logger.info("[ignore] error while closing file output channel.");
                }
            }

            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    s_logger.info("[ignore] error while closing file input stream.");
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    s_logger.info("[ignore] error while closing file output stream.");
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
        String url = "file:///home/ahuang/Download/E3921_P5N7A-VM_manual.zip";
        TemplateDownloader td = new LocalTemplateDownloader(null, url, "/tmp/mysql", TemplateDownloader.DEFAULT_MAX_TEMPLATE_SIZE_IN_BYTES, null);
        long bytes = td.download(true, null);
        if (bytes > 0) {
            System.out.println("Downloaded  (" + bytes + " bytes)" + " in " + td.getDownloadTime() / 1000 + " secs");
        } else {
            System.out.println("Failed download");
        }

    }
}
