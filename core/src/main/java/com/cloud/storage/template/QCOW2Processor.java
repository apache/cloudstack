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
import java.io.IOException;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.exception.InternalErrorException;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;

public class QCOW2Processor extends AdapterBase implements Processor {
    private static final int VIRTUALSIZE_HEADER_LOCATION = 24;

    private StorageLayer _storage;

   @Override
   public FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException  {
     return process(templatePath, format, templateName, 0);
   }

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName, long processTimeout) throws InternalErrorException {
        if (format != null) {
            logger.debug("We currently don't handle conversion from " + format + " to QCOW2.");
            return null;
        }

        String qcow2Path = templatePath + File.separator + templateName + "." + ImageFormat.QCOW2.getFileExtension();

        if (!_storage.exists(qcow2Path)) {
            logger.debug("Unable to find the qcow2 file: " + qcow2Path);
            return null;
        }

        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.QCOW2;
        info.filename = templateName + "." + ImageFormat.QCOW2.getFileExtension();

        File qcow2File = _storage.getFile(qcow2Path);

        info.size = _storage.getSize(qcow2Path);

        try {
            info.virtualSize = getTemplateVirtualSize(qcow2File);
        } catch (IOException e) {
            logger.error("Unable to get virtual size from " + qcow2File.getName());
            throw new InternalErrorException("unable to get virtual size from qcow2 file");
        }

        return info;
    }

    @Override
    public long getVirtualSize(File file) throws IOException {
        try {
            long size = getTemplateVirtualSize(file);
            return size;
        } catch (Exception e) {
            logger.info("[ignored]" + "failed to get template virtual size for QCOW2: " + e.getLocalizedMessage());
        }
        return file.length();
    }

    protected long getTemplateVirtualSize(File file) throws IOException {
        byte[] b = new byte[8];
        try (FileInputStream strm = new FileInputStream(file)) {
            if (strm.skip(VIRTUALSIZE_HEADER_LOCATION) != VIRTUALSIZE_HEADER_LOCATION) {
                throw new IOException("Unable to skip to the virtual size header");
            }
            if (strm.read(b) != 8) {
                throw new IOException("Unable to properly read the size");
            }
        }

        return NumbersUtil.bytesToLong(b);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            throw new ConfigurationException("Unable to get storage implementation");
        }

        return true;
    }
}
