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

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;

@Local(value = Processor.class)
public class QCOW2Processor extends AdapterBase implements Processor {
    private static final Logger s_logger = Logger.getLogger(QCOW2Processor.class);
    private static final int VIRTUALSIZE_HEADER_LOCATION = 24;

    private StorageLayer _storage;

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) {
        if (format != null) {
            s_logger.debug("We currently don't handle conversion from " + format + " to QCOW2.");
            return null;
        }

        String qcow2Path = templatePath + File.separator + templateName + "." + ImageFormat.QCOW2.getFileExtension();

        if (!_storage.exists(qcow2Path)) {
            s_logger.debug("Unable to find the qcow2 file: " + qcow2Path);
            return null;
        }

        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.QCOW2;
        info.filename = templateName + "." + ImageFormat.QCOW2.getFileExtension();

        File qcow2File = _storage.getFile(qcow2Path);

        info.size = _storage.getSize(qcow2Path);

        try {
            info.virtualSize = getVirtualSize(qcow2File);
        } catch (IOException e) {
            s_logger.error("Unable to get virtual size from " + qcow2File.getName());
            return null;
        }

        return info;
    }

    @Override
    public long getVirtualSize(File file) throws IOException {
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
