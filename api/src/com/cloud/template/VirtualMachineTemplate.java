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
package com.cloud.template;

import java.util.Date;
import java.util.Map;

import com.cloud.acl.ControlledEntity;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;

public interface VirtualMachineTemplate extends ControlledEntity {

    public static enum BootloaderType { PyGrub, HVM, External, CD };
    public enum TemplateFilter {
        featured,           // returns templates that have been marked as featured and public
        self,               // returns templates that have been registered or created by the calling user
        selfexecutable,     // same as self, but only returns templates that are ready to be deployed with
        sharedexecutable,   // ready templates that have been granted to the calling user by another user
        executable,         // templates that are owned by the calling user, or public templates, that can be used to deploy a new VM
        community,          // returns templates that have been marked as public but not featured
        all                 // all templates (only usable by ROOT admins)
    }

    /**
     * @return id.
     */
    long getId();

    boolean isFeatured();

    /**
     * @return public or private template
     */
    boolean isPublicTemplate();

    boolean isExtractable();

    /**
     * @return name
     */
    String getName();

    ImageFormat getFormat();

    boolean isRequiresHvm();

    String getDisplayText();

    boolean getEnablePassword();
    
    boolean getEnableSshKey();

    boolean isCrossZones();

    Date getCreated();

    long getGuestOSId();

    boolean isBootable();

    TemplateType getTemplateType();

    HypervisorType getHypervisorType();

    int getBits();

    String getUniqueName();

    String getUrl();

    String getChecksum();

    Long getSourceTemplateId();

    String getTemplateTag();
    
    Map getDetails();
}
