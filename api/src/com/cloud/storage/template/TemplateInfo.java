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
    long physicalSize;
    long id;
    boolean isPublic;
    boolean isCorrupted;

    protected TemplateInfo() {

    }

    public TemplateInfo(String templateName, String installPath, long size, long physicalSize, boolean isPublic, boolean isCorrupted) {
        this.templateName = templateName;
        this.installPath = installPath;
        this.size = size;
        this.physicalSize = physicalSize;
        this.isPublic = isPublic;
        this.isCorrupted = isCorrupted;
    }

    public TemplateInfo(String templateName, String installPath, boolean isPublic, boolean isCorrupted) {
        this(templateName, installPath, 0, 0, isPublic, isCorrupted);
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

    public boolean isCorrupted() {
        return isCorrupted;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public long getSize() {
        return size;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
