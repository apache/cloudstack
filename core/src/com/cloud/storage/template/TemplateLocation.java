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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.cloud.storage.StorageLayer;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.utils.NumbersUtil;

public class TemplateLocation {
    private static final Logger s_logger = Logger.getLogger(TemplateLocation.class);
    public final static String Filename = "template.properties";
    
    StorageLayer _storage;
    String _templatePath;
    
    File _file;
    Properties _props;
    
    ArrayList<FormatInfo> _formats;
    
    public TemplateLocation(StorageLayer storage, String templatePath) {
        _storage = storage;
        _templatePath = templatePath;
        if (!_templatePath.endsWith(File.separator)) {
            _templatePath += File.separator;
        }
        _formats = new ArrayList<FormatInfo>(5);
        _props = new Properties();
        _file = _storage.getFile(_templatePath + Filename);
    }
    
    public boolean create(long id, boolean isPublic, String uniqueName) throws IOException {
        boolean result = load();
        _props.setProperty("id", Long.toString(id));
        _props.setProperty("public", Boolean.toString(isPublic));
        _props.setProperty("uniquename", uniqueName);
        
        return result;
    }
    
    public boolean purge() {
        boolean purged = true;
        String[] files = _storage.listFiles(_templatePath);
        for (String file : files) {
            boolean r = _storage.delete(file);
            if (!r) {
                purged = false;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug((r ? "R" : "Unable to r") + "emove " + file);
            }
        }
        
        return purged;
    }
    
    public boolean load() throws IOException {
        FileInputStream strm = null;
        try {
            strm = new FileInputStream(_file);
            _props.load(strm);
        } finally {
            if (strm != null) {
                try {
                    strm.close();
                } catch (IOException e) {
                }
            }
        }
        
        for (ImageFormat format : ImageFormat.values()) {
            String ext = _props.getProperty(format.getFileExtension());
            if (ext != null) {
                FormatInfo info = new FormatInfo();
                info.format = format;
                info.filename = _props.getProperty(format.getFileExtension() + ".filename");
                info.size = NumbersUtil.parseLong(_props.getProperty(format.getFileExtension() + ".size"), -1);
                info.virtualSize = NumbersUtil.parseLong(_props.getProperty(format.getFileExtension() + ".virtualsize"), -1);
                
                _formats.add(info);
                
                if (!checkFormatValidity(info)) {
                    s_logger.warn("Cleaning up inconsistent information for " + format);
                    cleanup(format);
                }
            }
        }
        
        if (_props.getProperty("uniquename") == null || _props.getProperty("virtualsize") == null) {
            return false;
        }
        
        return _formats.size() > 0;
    }
    
    public boolean save() {
        for (FormatInfo info : _formats) {
            _props.setProperty(info.format.getFileExtension(), "true");
            _props.setProperty(info.format.getFileExtension() + ".filename", info.filename);
            _props.setProperty(info.format.getFileExtension() + ".size", Long.toString(info.size));
            _props.setProperty(info.format.getFileExtension() + ".virtualsize", Long.toString(info.virtualSize));
        }
        FileOutputStream strm = null;
        try {
            strm = new FileOutputStream(_file);
            _props.store(strm, "");
        } catch (IOException e) {
            s_logger.warn("Unable to save the template properties ", e);
            return false;
        } finally {
            if (strm != null) {
                try {
                    strm.close();
                } catch (IOException e) {
                }
            }
        }
        
        return true;
    }
    
    public TemplateInfo getTemplateInfo() {
        TemplateInfo tmplInfo = new TemplateInfo();
        
        String[] tokens = _templatePath.split(File.separator);
        tmplInfo.id = Long.parseLong(_props.getProperty("id"));
        tmplInfo.installPath = _templatePath + File.separator + _props.getProperty("filename");
        tmplInfo.installPath = tmplInfo.installPath.substring(tmplInfo.installPath.indexOf("template"));
        tmplInfo.isPublic = Boolean.parseBoolean(_props.getProperty("public"));
        tmplInfo.templateName = _props.getProperty("uniquename");
        tmplInfo.size = Long.parseLong(_props.getProperty("virtualsize"));
        
        return tmplInfo;
    }
    
    protected void cleanup(ImageFormat format) {
        FormatInfo info = deleteFormat(format);
        if (info != null && info.filename != null) {
            boolean r = _storage.delete(_templatePath + info.filename);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug((r ? "R" : "Unable to r") + "emove " + _templatePath + info.filename);
            }
        }
    }
    
    public FormatInfo getFormat(ImageFormat format) {
        for (FormatInfo info : _formats) {
            if (info.format == format) {
                return info;
            }
        }
        
        return null;
    }
    
    public boolean addFormat(FormatInfo newInfo) {
        deleteFormat(newInfo.format);
        
        if (!checkFormatValidity(newInfo)) {
            s_logger.warn("Format is invalid ");
            return false;
        }
        
        _props.setProperty("virtualsize", Long.toString(newInfo.virtualSize));
        _formats.add(newInfo);
        return true;
    }
    
    protected boolean checkFormatValidity(FormatInfo info) {
        return (info.format != null && info.size > 0 && info.virtualSize > 0 && info.filename != null && _storage.exists(_templatePath + info.filename) && _storage.getSize(_templatePath + info.filename) == info.size);
    }
    
    protected FormatInfo deleteFormat(ImageFormat format) {
        Iterator<FormatInfo> it = _formats.iterator();
        while (it.hasNext()) {
            FormatInfo info = it.next();
            if (info.format == format) {
                it.remove();
                _props.remove(format.getFileExtension());
                _props.remove(format.getFileExtension() + ".filename");
                _props.remove(format.getFileExtension() + ".size");
                _props.remove(format.getFileExtension() + ".virtualsize");
                return info;
            }
        }
        
        return null;
    }
}
