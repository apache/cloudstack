package com.cloud.storage.template;

import java.io.File;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Storage.ImageFormat;

public class VmdkProcessor implements Processor {
    private static final Logger s_logger = Logger.getLogger(VmdkProcessor.class);

    String _name;
    StorageLayer _storage;
	
    @Override
    public FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException {
        if (format != null) {
        	if(s_logger.isInfoEnabled())
        		s_logger.info("We currently don't handle conversion from " + format + " to VMDK.");
            return null;
        }
        
        s_logger.info("Template processing. templatePath: " + templatePath + ", templateName: " + templateName);
        String templateFilePath = templatePath + File.separator + templateName + "." + ImageFormat.VMDK.getFileExtension();
        if (!_storage.exists(templateFilePath)) {
        	if(s_logger.isInfoEnabled())
        		s_logger.info("Unable to find the vmware template file: " + templateFilePath);
            return null;
        }
        
        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.VMDK;
        info.filename = templateName + "." + ImageFormat.VMDK.getFileExtension();
        info.size = _storage.getSize(templateFilePath);
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
