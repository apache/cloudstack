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
import org.apache.log4j.Logger;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;

/**
 * VhdProcessor processes the downloaded template for VHD.  It
 * currently does not handle any type of template conversion
 * into the VHD format.
 *
 */
public class VhdProcessor extends AdapterBase implements Processor {

    private static final Logger s_logger = Logger.getLogger(VhdProcessor.class);
    StorageLayer _storage;
    private int vhdFooterSize = 512;
    private int vhdFooterCreatorAppOffset = 28;
    private int vhdFooterCreatorVerOffset = 32;
    private int vhdFooterCurrentSizeOffset = 48;
    private byte[][] citrixCreatorApp = { {0x74, 0x61, 0x70, 0x00}, {0x43, 0x54, 0x58, 0x53}}; /*"tap ", and "CTXS"*/

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException {
        if (format != null) {
            s_logger.debug("We currently don't handle conversion from " + format + " to VHD.");
            return null;
        }

        String vhdPath = templatePath + File.separator + templateName + "." + ImageFormat.VHD.getFileExtension();
        if (!_storage.exists(vhdPath)) {
            s_logger.debug("Unable to find the vhd file: " + vhdPath);
            return null;
        }

        File vhdFile = _storage.getFile(vhdPath);

        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.VHD;
        info.filename = templateName + "." + ImageFormat.VHD.getFileExtension();
        info.size = _storage.getSize(vhdPath);

        try {
            info.virtualSize = getTemplateVirtualSize(vhdFile);
        } catch (IOException e) {
            s_logger.error("Unable to get the virtual size for " + vhdPath);
            throw new InternalErrorException("unable to get virtual size from vhd file");
        }

        return info;
    }

    @Override
    public long getVirtualSize(File file) throws IOException {
        try {
            long size = getTemplateVirtualSize(file);
            return size;
        } catch (Exception e) {
            s_logger.info("[ignored]" + "failed to get template virtual size for VHD: " + e.getLocalizedMessage());
        }
        return file.length();
    }

    protected long getTemplateVirtualSize(File file) throws IOException {
        byte[] currentSize = new byte[8];
        byte[] creatorApp = new byte[4];

        try (FileInputStream strm = new FileInputStream(file)) {
            long skipped = strm.skip(file.length() - vhdFooterSize + vhdFooterCreatorAppOffset);
            if (skipped == -1) {
                throw new IOException("Unexpected end-of-file");
            }
            long read = strm.read(creatorApp);
            if (read == -1) {
                throw new IOException("Unexpected end-of-file");
            }
            skipped = strm.skip(vhdFooterCurrentSizeOffset - vhdFooterCreatorVerOffset);
            if (skipped == -1) {
                throw new IOException("Unexpected end-of-file");
            }
            read = strm.read(currentSize);
            if (read == -1) {
                throw new IOException("Unexpected end-of-file");
            }
        }

        return NumbersUtil.bytesToLong(currentSize);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            throw new ConfigurationException("Unable to get storage implementation");
        }

        return true;
    }

}
