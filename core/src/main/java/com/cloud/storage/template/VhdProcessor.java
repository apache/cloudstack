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

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import javax.naming.ConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * VhdProcessor processes the downloaded template for VHD.  It
 * currently does not handle any type of template conversion
 * into the VHD format.
 *
 */
public class VhdProcessor extends AdapterBase implements Processor {

    StorageLayer _storage;
    private int vhdFooterSize = 512;
    private int vhdCookieOffset = 8;
    private int vhdFooterCreatorAppOffset = 28;
    private int vhdFooterCreatorVerOffset = 32;
    private int vhdFooterCurrentSizeOffset = 48;
    private byte[][] citrixCreatorApp = { {0x74, 0x61, 0x70, 0x00}, {0x43, 0x54, 0x58, 0x53}}; /*"tap ", and "CTXS"*/
    private String vhdIdentifierCookie = "conectix";

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException {
     return process(templatePath, format, templateName, 0);
    }

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName, long processTimeout) throws InternalErrorException {
        if (format != null) {
            logger.debug("We currently don't handle conversion from " + format + " to VHD.");
            return null;
        }

        String vhdPath = templatePath + File.separator + templateName + "." + ImageFormat.VHD.getFileExtension();
        if (!_storage.exists(vhdPath)) {
            logger.debug("Unable to find the vhd file: " + vhdPath);
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
            logger.error("Unable to get the virtual size for " + vhdPath);
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
            logger.info("[ignored]" + "failed to get template virtual size for VHD: " + e.getLocalizedMessage());
        }
        return file.length();
    }

    protected long getTemplateVirtualSize(File file) throws IOException {
        byte[] currentSize = new byte[8];
        byte[] cookie = new byte[8];
        byte[] creatorApp = new byte[4];


        BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(file));
        InputStream strm = fileStream;

        boolean isCompressed = checkCompressed(file.getAbsolutePath());

        if ( isCompressed ) {
            try {
                strm = new CompressorStreamFactory().createCompressorInputStream(fileStream);
            } catch (CompressorException e) {
                logger.info("error opening compressed VHD file " + file.getName());
                return file.length();
            }
        } try {

            //read the backup footer present at the top of the VHD file
            strm.read(cookie);
            if (! new String(cookie).equals(vhdIdentifierCookie)) {
                strm.close();
                return  file.length();
            }

            long skipped = strm.skip(vhdFooterCreatorAppOffset - vhdCookieOffset);
            if (skipped == -1) {
                throw new IOException("Unexpected end-of-file");
            }
            long read = strm.read(creatorApp);
            if (read == -1) {
                throw new IOException("Unexpected end-of-file");
            }
            skipped = strm.skip(vhdFooterCurrentSizeOffset - vhdFooterCreatorVerOffset - vhdCookieOffset);
            if (skipped == -1) {
                throw new IOException("Unexpected end-of-file");
            }
            read = strm.read(currentSize);
            if (read == -1) {
                throw new IOException("Unexpected end-of-file");
            }
        } catch (IOException e) {
            logger.warn("Error reading virtual size from VHD file " + e.getMessage() + " VHD: " + file.getName());
            return file.length();
        } finally {
            if (strm != null) {
                strm.close();
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

    private boolean checkCompressed(String fileName) throws IOException {

        FileInputStream fin = null;
        BufferedInputStream bin = null;
        CompressorInputStream cin = null;

        try {
            fin = new FileInputStream(fileName);
            bin = new BufferedInputStream(fin);
            cin = new CompressorStreamFactory().createCompressorInputStream(bin);

        } catch (CompressorException e) {
            logger.warn(e.getMessage());
            return false;

        } catch (FileNotFoundException e) {
            logger.warn(e.getMessage());
            return false;
        } finally {
            if (cin != null)
                cin.close();
            else if (bin != null)
                bin.close();
        }
        return true;
    }
}
