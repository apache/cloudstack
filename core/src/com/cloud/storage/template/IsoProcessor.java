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

import java.io.File;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.component.AdapterBase;

@Local(value = Processor.class)
public class IsoProcessor extends AdapterBase implements Processor {
    private static final Logger s_logger = Logger.getLogger(IsoProcessor.class);

    StorageLayer _storage;

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) {
        if (format != null) {
            s_logger.debug("We don't handle conversion from " + format + " to ISO.");
            return null;
        }

        String isoPath = templatePath + File.separator + templateName + "." + ImageFormat.ISO.getFileExtension();

        if (!_storage.exists(isoPath)) {
            s_logger.debug("Unable to find the iso file: " + isoPath);
            return null;
        }

        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.ISO;
        info.filename = templateName + "." + ImageFormat.ISO.getFileExtension();
        info.size = _storage.getSize(isoPath);
        info.virtualSize = info.size;

        return info;
    }

    @Override
    public Long getVirtualSize(File file) {
        return file.length();
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
