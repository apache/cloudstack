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

import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.component.Adapter;

/**
 * Generic interface to process different types of image formats
 * for templates downloaded and for conversion from one format
 * to anther.
 *
 */
public interface Processor extends Adapter {
    
    /**
     * Returns image format if it was able to process the original file and
     * change it to the the image format it requires.
     * 
     * @param templatePath path to the templates to process.
     * @param format Format of the original file.  If null, it means unknown.  If not null,
     *        there is already a file with thte template name and image format extension
     *        that exists in case a conversion can be done.
     * @param templateName file name to call the resulting image file.  The processor is required to add extensions.
     * @return FormatInfo if the file is processed.  null if not.
     */
    FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException;
    
    public static class FormatInfo {
        public ImageFormat format;
        public long size;
        public long virtualSize;
        public String filename;
    }
}
