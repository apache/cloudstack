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

package com.cloud.agent.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.storage.Storage.ImageFormat;

public class CreatePrivateTemplateAnswer extends Answer {
    private String _path;
    private long _virtualSize;
    private String _uniqueName;
    private ImageFormat _format;

    public CreatePrivateTemplateAnswer() {}

    public CreatePrivateTemplateAnswer(Command cmd, boolean success, String result, String path, long virtualSize, String uniqueName, ImageFormat format) {
        super(cmd, success, result);
        _path = path;
        _virtualSize = virtualSize;
        _uniqueName = uniqueName;
        _format = format;
    }

    public String getPath() {
        return _path;
    }
    
    public void setPath(String path) {
        _path = path;
    }
    
    public long getVirtualSize() {
    	return _virtualSize;
    }
    
    public void setVirtualSize(long virtualSize) {
    	_virtualSize = virtualSize;
    }
    
    public String getUniqueName() {
    	return _uniqueName;
    }
    
    public ImageFormat getImageFormat() {
    	return _format;
    }
}
