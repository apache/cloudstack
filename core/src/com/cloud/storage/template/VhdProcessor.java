/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.NumbersUtil;

/**
 * VhdProcessor processes the downloaded template for VHD.  It
 * currently does not handle any type of template conversion
 * into the VHD format.
 *
 */
public class VhdProcessor implements Processor {
    
    private static final Logger s_logger = Logger.getLogger(VhdProcessor.class);
    String _name;
    StorageLayer _storage;
    private int vhd_footer_size = 512;
    private int vhd_footer_creator_app_offset = 28;
    private int vhd_footer_creator_ver_offset = 32;
    private int vhd_footer_current_size_offset = 48;
    private byte[] citrix_creator_app = {0x74, 0x61, 0x70, 0x00}; /*"tap "*/

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
        
        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.VHD;
        info.filename = templateName + "." + ImageFormat.VHD.getFileExtension();
        
        File vhdFile = _storage.getFile(vhdPath);
        
        info.size = _storage.getSize(vhdPath);
        FileInputStream strm = null;
        byte[] currentSize = new byte[8];
        byte[] creatorApp = new byte[4];
        try {
            strm = new FileInputStream(vhdFile);
            strm.skip(info.size - vhd_footer_size + vhd_footer_creator_app_offset);
            strm.read(creatorApp);
            strm.skip(vhd_footer_current_size_offset - vhd_footer_creator_ver_offset);
            strm.read(currentSize);           
        } catch (Exception e) {
            s_logger.warn("Unable to read vhd file " + vhdPath, e);
            throw new InternalErrorException("Unable to read vhd file " + vhdPath + ": " + e);
        } finally {
            if (strm != null) {
                try {
                    strm.close();
                } catch (IOException e) {
                }
            }
        }
        
        if (!Arrays.equals(creatorApp, citrix_creator_app)) {
        	/*Only support VHD image created by citrix xenserver*/
        	throw new InternalErrorException("Image creator is:" + creatorApp.toString() +", is not supported");
        }
        
        long templateSize = NumbersUtil.bytesToLong(currentSize);
        info.virtualSize = templateSize;

        return info;
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

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
