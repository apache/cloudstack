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
import java.util.Map;

import javax.naming.ConfigurationException;


import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.component.AdapterBase;

public class RawImageProcessor extends AdapterBase implements Processor {
    StorageLayer _storage;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            throw new ConfigurationException("Unable to get storage implementation");
        }

        return true;
    }

   @Override
   public FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException {
      return process(templatePath, format, templateName, 0);
   }

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName, long processTimeout) throws InternalErrorException {
        if (format != null) {
            logger.debug("We currently don't handle conversion from " + format + " to raw image.");
            return null;
        }

        String imgPath = templatePath + File.separator + templateName + "." + ImageFormat.RAW.getFileExtension();
        if (!_storage.exists(imgPath)) {
            logger.debug("Unable to find raw image:" + imgPath);
            return null;
        }
        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.RAW;
        info.filename = templateName + "." + ImageFormat.RAW.getFileExtension();
        info.size = _storage.getSize(imgPath);
        info.virtualSize = info.size;
        logger.debug("Process raw image " + info.filename + " successfully");
        return info;
    }

    @Override
    public long getVirtualSize(File file) {
        return file.length();
    }

}
