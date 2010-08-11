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
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.storage.StorageLayer;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.NumbersUtil;

public class QCOW2Processor implements Processor {
    private static final Logger s_logger = Logger.getLogger(QCOW2Processor.class);
    String _name;
    StorageLayer _storage;

	@Override
	public FormatInfo process(String templatePath, ImageFormat format,
			String templateName) {
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
        FileInputStream strm = null;
        byte[] b = new byte[8];
        try {
            strm = new FileInputStream(qcow2File);
            strm.skip(24);
            strm.read(b);
        } catch (Exception e) {
            s_logger.warn("Unable to read qcow2 file " + qcow2Path, e);
            return null;
        } finally {
            if (strm != null) {
                try {
                    strm.close();
                } catch (IOException e) {
                }
            }
        }
        
        long templateSize = NumbersUtil.bytesToLong(b);
        info.virtualSize = templateSize;

        return info;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
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
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return true;
	}

}
