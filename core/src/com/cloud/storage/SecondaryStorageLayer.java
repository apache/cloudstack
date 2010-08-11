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
package com.cloud.storage;

import java.net.URI;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.component.Adapter;

public interface SecondaryStorageLayer extends Adapter {
    
    /**
     * Mounts a template
     * 
     * @param poolId the pool to mount it to.
     * @param poolUuid the pool's uuid if it is needed.
     * @param name unique name to the template.
     * @param url url to access the template.
     * @param format format of the template.
     * @param accountId account id the template belongs to.
     * @return a String that unique identifies the reference the template once it is mounted.
     */
    String mountTemplate(long poolId, String poolUuid, String name, URI url, ImageFormat format, long accountId);
    
}
