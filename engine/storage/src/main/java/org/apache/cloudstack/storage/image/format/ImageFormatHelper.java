/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.image.format;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class ImageFormatHelper {
    private static List<ImageFormat> formats;
    private static final ImageFormat defaultFormat = new Unknown();

    @Inject
    public void setFormats(List<ImageFormat> formats) {
        ImageFormatHelper.initFormats(formats);
    }

    private static synchronized void initFormats(List<ImageFormat> newFormats) {
        formats = newFormats;
    }

    public static ImageFormat getFormat(String format) {
        for (ImageFormat fm : formats) {
            if (fm.toString().equals(format)) {
                return fm;
            }
        }
        return ImageFormatHelper.defaultFormat;
    }
}
