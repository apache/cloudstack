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
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.storage.StorageLayer;
import com.cloud.storage.Storage.ImageFormat;

@Local(value=Processor.class)
public class IsoProcessor implements Processor {
    private static final Logger s_logger = Logger.getLogger(IsoProcessor.class);
    
    String _name;
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
