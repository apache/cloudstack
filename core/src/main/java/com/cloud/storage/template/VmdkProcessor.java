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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.component.AdapterBase;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

public class VmdkProcessor extends AdapterBase implements Processor {
    private static final Logger s_logger = Logger.getLogger(VmdkProcessor.class);

    StorageLayer _storage;

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException {
     return process(templatePath, format, templateName, 0);
    }

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName, long processTimeout) throws InternalErrorException {
        if (format != null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("We currently don't handle conversion from " + format + " to VMDK.");
            }
            return null;
        }

        s_logger.info("Template processing. templatePath: " + templatePath + ", templateName: " + templateName);
        String templateFilePath = templatePath + File.separator + templateName + "." + ImageFormat.VMDK.getFileExtension();
        if (!_storage.exists(templateFilePath)) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Unable to find the vmware template file: " + templateFilePath);
            }
            return null;
        }

        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.VMDK;
        info.filename = templateName + "." + ImageFormat.VMDK.getFileExtension();
        info.size = _storage.getSize(templateFilePath);
        info.virtualSize = getTemplateVirtualSize(templatePath, info.filename);

        return info;
    }

    @Override
    public long getVirtualSize(File file) {
        try {
            long size = getTemplateVirtualSize(file.getParent(), file.getName());
            return size;
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "failed to get template virtual size for vmdk: " + e.getLocalizedMessage());
        }
        return file.length();
    }

    public long getTemplateVirtualSize(String templatePath, String templateName) throws InternalErrorException {
        long virtualSize = 0;
        String templateFileFullPath = templatePath.endsWith(File.separator) ? templatePath : templatePath + File.separator;
        templateFileFullPath += templateName.endsWith(ImageFormat.VMDK.getFileExtension()) ? templateName : templateName + "." + ImageFormat.VMDK.getFileExtension();
        try (
                FileReader fileReader = new FileReader(templateFileFullPath);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
            ) {
            Pattern regex = Pattern.compile("(RW|RDONLY|NOACCESS) (\\d+) (FLAT|SPARSE|ZERO|VMFS|VMFSSPARSE|VMFSDRM|VMFSRAW)");
            String line = null;
            while((line = bufferedReader.readLine()) != null) {
                Matcher m = regex.matcher(line);
                if (m.find( )) {
                    long sectors = Long.parseLong(m.group(2));
                    virtualSize = sectors * 512;
                    break;
                }
            }
        } catch(FileNotFoundException ex) {
            String msg = "Unable to open file '" + templateFileFullPath + "' " + ex.toString();
            s_logger.error(msg);
            throw new InternalErrorException(msg);
        } catch(IOException ex) {
            String msg = "Unable read open file '" + templateFileFullPath + "' " + ex.toString();
            s_logger.error(msg);
            throw new InternalErrorException(msg);
        }

        s_logger.debug("vmdk file had size=" + toHumanReadableSize(virtualSize));
        return virtualSize;
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
