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

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.component.AdapterBase;

import javax.naming.ConfigurationException;
import java.io.File;
import java.util.Map;

public class TARProcessor extends AdapterBase implements Processor {

    private StorageLayer _storage;

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) {
       return process(templatePath, format, templateName, 0);
    }

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName, long processTimeout) {
        if (format != null) {
            logger.debug("We currently don't handle conversion from " + format + " to TAR.");
            return null;
        }

        String tarPath = templatePath + File.separator + templateName + "." + ImageFormat.TAR.getFileExtension();

        if (!_storage.exists(tarPath)) {
            logger.debug("Unable to find the tar file: " + tarPath);
            return null;
        }

        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.TAR;
        info.filename = templateName + "." + ImageFormat.TAR.getFileExtension();

        File tarFile = _storage.getFile(tarPath);

        info.size = _storage.getSize(tarPath);

        info.virtualSize = getVirtualSize(tarFile);

        return info;
    }

    @Override
    public long getVirtualSize(File file) {
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
