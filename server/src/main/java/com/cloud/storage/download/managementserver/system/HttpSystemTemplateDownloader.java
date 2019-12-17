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

package com.cloud.storage.download.managementserver.system;

import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HttpSystemTemplateDownloader extends SystemTemplateDownloader {

    @Inject
    private VMTemplateDao _templateDao;

    protected HttpClient client;
    public static final Logger logger = Logger.getLogger(HttpSystemTemplateDownloader.class.getName());

    public HttpSystemTemplateDownloader(VirtualMachineTemplate template,VMTemplateVO templateVO, String destPoolPath) {
        super(template, templateVO, destPoolPath);
        String downloadDir = getDownloadPath(template.getId());
        createDownloadDirectory(downloadDir);
        setDownloadedFilePath(getDestPoolPath() + File.separator + downloadDir + File.separator + getFileNameFromUrl());
    }

    protected void createDownloadDirectory(String downloadDir) {
        createFolder(getDestPoolPath() + File.separator + downloadDir);
    }

    public boolean downloadTemplate() {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(getTemplate().getUrl());
        try {
            HttpResponse response = client.execute(get);
            org.apache.http.HttpEntity entity = response.getEntity();
            if (entity == null) {
                s_logger.debug("Failed to get entity");
                throw new CloudRuntimeException("Failed to get url: " + getTemplate().getUrl());
            }
            File destFile = new File(getDownloadedFilePath());
            if (!destFile.createNewFile()) {
                s_logger.warn("Reusing existing file " + destFile.getPath());
            }
            try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
                entity.writeTo(outputStream);
            } catch (IOException e) {
                s_logger.debug("downloadFromUrlToNfs:Exception:" + e.getMessage(), e);
            }
        } catch (IOException e) {
            s_logger.debug("Failed to get url:" + getTemplate().getUrl() + ", due to " + e.toString(), e);
            throw new CloudRuntimeException(e);
        }
        return true;
    }
}