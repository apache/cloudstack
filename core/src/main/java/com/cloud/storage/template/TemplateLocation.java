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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Arrays;

import org.apache.log4j.Logger;

import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.utils.NumbersUtil;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

public class TemplateLocation {
    private static final Logger s_logger = Logger.getLogger(TemplateLocation.class);
    public final static String Filename = "template.properties";

    StorageLayer _storage;
    String _templatePath;
    boolean _isCorrupted;
    ResourceType _resourceType = ResourceType.TEMPLATE;

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
        //TO DO - remove this hack
        if (_templatePath.matches(".*" + "volumes" + ".*")) {
            _file = _storage.getFile(_templatePath + "volume.properties");
            _resourceType = ResourceType.VOLUME;
        } else {
            _file = _storage.getFile(_templatePath + Filename);
        }
        _isCorrupted = false;
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
            boolean isRemoved = _storage.delete(file);
            if (!isRemoved) {
                purged = false;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug((isRemoved ? "Removed " : "Unable to remove") + file);
            }
        }

        return purged;
    }

    public boolean load() throws IOException {
        try (FileInputStream strm = new FileInputStream(_file);) {
            _props.load(strm);
        } catch (IOException e) {
            s_logger.warn("Unable to load the template properties for '" + _file + "': ", e);
        }

        for (ImageFormat format : ImageFormat.values()) {
            String currentExtension = format.getFileExtension();
            String ext = _props.getProperty(currentExtension);
            if (ext != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("File extension '" + currentExtension + "' was found in '" + _file + "'.");
                }
                FormatInfo info = new FormatInfo();
                info.format = format;
                info.filename = _props.getProperty(currentExtension + ".filename");
                if (info.filename == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Property '" + currentExtension + ".filename' was not found in '" + _file + "'. Current format is ignored.");
                    }
                    continue;
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Property '" + currentExtension + ".filename' was found in '" + _file + "'. Current format will be parsed.");
                }
                info.size = NumbersUtil.parseLong(_props.getProperty(currentExtension + ".size"), -1);
                _props.setProperty("physicalSize", Long.toString(info.size));
                info.virtualSize = NumbersUtil.parseLong(_props.getProperty(currentExtension + ".virtualsize"), -1);
                _formats.add(info);

                if (!checkFormatValidity(info)) {
                    _isCorrupted = true;
                    s_logger.warn("Cleaning up inconsistent information for " + format);
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Format extension '" + currentExtension + "' wasn't found in '" + _file + "'.");
                }
            }
        }

        if (_props.getProperty("uniquename") == null || _props.getProperty("virtualsize") == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Property 'uniquename' or 'virtualsize' weren't found in '" + _file + "'. Loading failed.");
            }
            return false;
        }
        return (_formats.size() > 0);
    }

    public boolean save() {
        for (FormatInfo info : _formats) {
            String formatExtension = info.format.getFileExtension();
            _props.setProperty(formatExtension, "true");
            _props.setProperty(formatExtension + ".filename", info.filename);
            _props.setProperty(formatExtension + ".size", Long.toString(info.size));
            _props.setProperty(formatExtension + ".virtualsize", Long.toString(info.virtualSize));
        }
        try (FileOutputStream strm =  new FileOutputStream(_file);) {
            _props.store(strm, "");
        } catch (IOException e) {
            s_logger.warn("Unable to save the template properties ", e);
            return false;
        }
        return true;
    }

    public TemplateProp getTemplateInfo() {
        TemplateProp tmplInfo = new TemplateProp();
        tmplInfo.id = Long.parseLong(_props.getProperty("id"));
        tmplInfo.installPath = _templatePath + _props.getProperty("filename"); // _templatePath endsWith /
        if (_resourceType == ResourceType.VOLUME) {
            tmplInfo.installPath = tmplInfo.installPath.substring(tmplInfo.installPath.indexOf("volumes"));
        } else {
            tmplInfo.installPath = tmplInfo.installPath.substring(tmplInfo.installPath.indexOf("template"));
        }
        tmplInfo.isCorrupted = _isCorrupted;
        tmplInfo.isPublic = Boolean.parseBoolean(_props.getProperty("public"));
        tmplInfo.templateName = _props.getProperty("uniquename");
        if (_props.getProperty("virtualsize") != null) {
            tmplInfo.size = Long.parseLong(_props.getProperty("virtualsize"));
        }
        if (_props.getProperty("size") != null) {
            tmplInfo.physicalSize = Long.parseLong(_props.getProperty("size"));
        }

        return tmplInfo;
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
            s_logger.warn("Format is invalid");
            s_logger.debug("Format: " + newInfo.format + " size: " + toHumanReadableSize(newInfo.size) + " virtualsize: " + toHumanReadableSize(newInfo.virtualSize) + " filename: " + newInfo.filename);
            s_logger.debug("format, filename cannot be null and size, virtual size should be  > 0 ");
            return false;
        }

        _props.setProperty("virtualsize", Long.toString(newInfo.virtualSize));
        _props.setProperty("size", Long.toString(newInfo.size));
        _formats.add(newInfo);
        return true;
    }

    public void updateVirtualSize(long virtualSize) {
        _props.setProperty("virtualsize", Long.toString(virtualSize));
    }

    protected boolean checkFormatValidity(FormatInfo info) {
        return (info.format != null && info.size > 0 && info.virtualSize > 0 && info.filename != null);
    }

    protected FormatInfo deleteFormat(ImageFormat format) {
        Iterator<FormatInfo> it = _formats.iterator();
        while (it.hasNext()) {
            FormatInfo info = it.next();
            if (info.format == format) {
                it.remove();
                String formatExtension = format.getFileExtension();
                _props.remove(formatExtension);
                for(String propertySuffix : Arrays.asList("filename","size","virtualsize")) {
                        _props.remove(formatExtension + "." + propertySuffix);
                }
                return info;
            }
        }

        return null;
    }
}
