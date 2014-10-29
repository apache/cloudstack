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
import java.io.IOException;

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
     *
     * @param templatePath path to the templates to process.
     * @param format Format of the original file.  If null, it means unknown.  If not null,
     *        there is already a file with thte template name and image format extension
     *        that exists in case a conversion can be done.
     */
    FormatInfo process(String templatePath, ImageFormat format, String templateName) throws InternalErrorException;

    public static class FormatInfo {
        public ImageFormat format;
        public long size;
        public long virtualSize;
        public String filename;
        public boolean isCorrupted;
    }

    long getVirtualSize(File file) throws IOException;

}
