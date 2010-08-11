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

public class TemplateInfo {
    String templateName;
    String installPath;
    long size;
    long id;

    boolean isPublic;
    
    public static TemplateInfo getDefaultSystemVmTemplateInfo() {
        TemplateInfo routingInfo = new TemplateInfo(TemplateConstants.DEFAULT_SYSTEM_VM_TMPLT_NAME, TemplateConstants.DEFAULT_SYSTEM_VM_TEMPLATE_PATH, false);
        return routingInfo;
    }

    protected TemplateInfo() {
        
    }
    
    public TemplateInfo(String templateName, String installPath, long size, boolean isPublic) {
        this.templateName = templateName;
        this.installPath = installPath;
        this.size = size;
        this.isPublic = isPublic;
    }

    public TemplateInfo(String templateName, String installPath, boolean isPublic) {
        this.templateName = templateName;
        this.installPath = installPath;
        this.size = 0;
        this.isPublic = isPublic;
    }
    
    public long getId() {
        return id;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public String getInstallPath() {
        return installPath;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
}
