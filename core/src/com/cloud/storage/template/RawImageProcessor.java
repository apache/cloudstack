package com.cloud.storage.template;

import java.io.File;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.template.Processor.FormatInfo;

@Local(value=Processor.class)
public class RawImageProcessor implements Processor {
    private static final Logger s_logger = Logger.getLogger(RawImageProcessor.class);
    String _name;
    StorageLayer _storage;
    
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
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public FormatInfo process(String templatePath, ImageFormat format,
			String templateName) throws InternalErrorException {
		if (format != null) {
            s_logger.debug("We currently don't handle conversion from " + format + " to raw image.");
            return null;
        }
        
		String imgPath = templatePath + File.separator + templateName + "." + ImageFormat.RAW.getFileExtension();
		if (!_storage.exists(imgPath)) {
			s_logger.debug("Unable to find raw image:" + imgPath);
		}
        FormatInfo info = new FormatInfo();
        info.format = ImageFormat.RAW;
        info.filename = templateName + "." + ImageFormat.RAW.getFileExtension();
        info.size = _storage.getSize(imgPath);
        info.virtualSize = info.size;
        s_logger.debug("Process raw image " + info.filename + " successfully");
        return info;
	}

}
