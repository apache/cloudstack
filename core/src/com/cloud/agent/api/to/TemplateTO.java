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
package com.cloud.agent.api.to;

import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.ImageFormat;

public class TemplateTO {
    private long id;
    private String uniqueName;
    private ImageFormat format;
    
    protected TemplateTO() {
    }
    
    public TemplateTO(VMTemplateVO template, VMTemplateStoragePoolVO storedAt) {
        this.id = template.getId();
        this.uniqueName = template.getUniqueName();
        this.format = template.getFormat();
    }
    
    public long getId() {
        return id;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public ImageFormat getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return new StringBuilder("Tmpl[").append(id).append("|").append(uniqueName).append("]").toString();
    }
}
