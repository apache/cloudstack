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
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;

import com.cloud.agent.api.storage.OVFPropertyTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.cloud.agent.api.storage.OVFHelper;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.script.Script;

public class OVAProcessor extends AdapterBase implements Processor {
    private static final Logger s_logger = Logger.getLogger(OVAProcessor.class);
    StorageLayer _storage;

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException {
        return process(templatePath, format, templateName, 0);
    }

    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName, long processTimeout) throws InternalErrorException {
        if (format != null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("We currently don't handle conversion from " + format + " to OVA.");
            }
            return null;
        }

        s_logger.info("Template processing. templatePath: " + templatePath + ", templateName: " + templateName);
        String templateFilePath = templatePath + File.separator + templateName + "." + ImageFormat.OVA.getFileExtension();
        if (!_storage.exists(templateFilePath)) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Unable to find the vmware template file: " + templateFilePath);
            }
            return null;
        }

        s_logger.info("Template processing - untar OVA package. templatePath: " + templatePath + ", templateName: " + templateName);
        String templateFileFullPath = templatePath + File.separator + templateName + "." + ImageFormat.OVA.getFileExtension();
        File templateFile = new File(templateFileFullPath);
        Script command = new Script("tar", processTimeout, s_logger);
        command.add("--no-same-owner");
        command.add("--no-same-permissions");
        command.add("-xf", templateFileFullPath);
        command.setWorkDir(templateFile.getParent());
        String result = command.execute();
        if (result != null) {
            s_logger.info("failed to untar OVA package due to " + result + ". templatePath: " + templatePath + ", templateName: " + templateName);
            throw new InternalErrorException("failed to untar OVA package");
        }

        command = new Script("chmod", 0, s_logger);
        command.add("-R");
        command.add("666", templatePath);
        result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for files in " + templatePath + " due to " + result);
        }
        command = new Script("chmod", 0, s_logger);
        command.add("777", templatePath);
        result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for " + templatePath + " due to " + result);
        }

        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.OVA;
        info.filename = templateName + "." + ImageFormat.OVA.getFileExtension();
        info.size = _storage.getSize(templateFilePath);
        info.virtualSize = getTemplateVirtualSize(templatePath, info.filename);

        //vaidate ova
        String ovfFile = getOVFFilePath(templateFileFullPath);
        try {
            OVFHelper ovfHelper = new OVFHelper();
            List<DatadiskTO> disks = ovfHelper.getOVFVolumeInfo(ovfFile);
            List<OVFPropertyTO> ovfProperties = ovfHelper.getOVFPropertiesFromFile(ovfFile);
            if (CollectionUtils.isNotEmpty(ovfProperties)) {
                s_logger.info("Found " + ovfProperties.size() + " configurable OVF properties");
                info.ovfProperties = ovfProperties;
            }
        } catch (Exception e) {
            s_logger.info("The ovf file " + ovfFile + " is invalid ", e);
            throw new InternalErrorException("OVA package has bad ovf file " + e.getMessage(), e);
        }
        // delete original OVA file
        // templateFile.delete();
        return info;
    }

    @Override
    public long getVirtualSize(File file) {
        try {
            long size = getTemplateVirtualSize(file.getParent(), file.getName());
            return size;
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "failed to get virtual template size for ova: " + e.getLocalizedMessage());
        }
        return file.length();
    }

    public long getTemplateVirtualSize(String templatePath, String templateName) throws InternalErrorException {
        // get the virtual size from the OVF file meta data
        long virtualSize = 0;
        String templateFileFullPath = templatePath.endsWith(File.separator) ? templatePath : templatePath + File.separator;
        templateFileFullPath += templateName.endsWith(ImageFormat.OVA.getFileExtension()) ? templateName : templateName + "." + ImageFormat.OVA.getFileExtension();
        String ovfFileName = getOVFFilePath(templateFileFullPath);
        if (ovfFileName == null) {
            String msg = "Unable to locate OVF file in template package directory: " + templatePath;
            s_logger.error(msg);
            throw new InternalErrorException(msg);
        }
        try {
            Document ovfDoc = null;
            ovfDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(ovfFileName));
            Element disk = (Element)ovfDoc.getElementsByTagName("Disk").item(0);
            virtualSize = Long.parseLong(disk.getAttribute("ovf:capacity"));
            String allocationUnits = disk.getAttribute("ovf:capacityAllocationUnits");
            virtualSize = OVFHelper.getDiskVirtualSize(virtualSize, allocationUnits, ovfFileName);
            return virtualSize;
        } catch (Exception e) {
            String msg = "getTemplateVirtualSize: Unable to parse OVF XML document " + templatePath + " to get the virtual disk " + templateName + " size due to " + e;
            s_logger.error(msg);
            throw new InternalErrorException(msg);
        }
    }

    public Pair<Long, Long> getDiskDetails(String ovfFilePath, String diskName) throws InternalErrorException {
        long virtualSize = 0;
        long fileSize = 0;
        String fileId = null;
        try {
            Document ovfDoc = null;
            ovfDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(ovfFilePath));
            NodeList disks = ovfDoc.getElementsByTagName("Disk");
            NodeList files = ovfDoc.getElementsByTagName("File");
            for (int j = 0; j < files.getLength(); j++) {
                Element file = (Element)files.item(j);
                if (file.getAttribute("ovf:href").equals(diskName)) {
                    fileSize = Long.parseLong(file.getAttribute("ovf:size"));
                    fileId = file.getAttribute("ovf:id");
                    break;
                }
            }
            for (int i = 0; i < disks.getLength(); i++) {
                Element disk = (Element)disks.item(i);
                if (disk.getAttribute("ovf:fileRef").equals(fileId)) {
                    virtualSize = Long.parseLong(disk.getAttribute("ovf:capacity"));
                    String allocationUnits = disk.getAttribute("ovf:capacityAllocationUnits");
                    virtualSize = OVFHelper.getDiskVirtualSize(virtualSize, allocationUnits, ovfFilePath);
                    break;
                }
            }
            return new Pair<Long, Long>(virtualSize, fileSize);
        } catch (Exception e) {
            String msg = "getDiskDetails: Unable to parse OVF XML document " + ovfFilePath + " to get the virtual disk " + diskName + " size due to " + e;
            s_logger.error(msg);
            throw new InternalErrorException(msg);
        }
    }

    private String getOVFFilePath(String srcOVAFileName) {
        File file = new File(srcOVAFileName);
        assert (_storage != null);
        String[] files = _storage.listFiles(file.getParent());
        if (files != null) {
            for (String fileName : files) {
                if (fileName.toLowerCase().endsWith(".ovf")) {
                    File ovfFile = new File(fileName);
                    return file.getParent() + File.separator + ovfFile.getName();
                }
            }
        }
        return null;
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
