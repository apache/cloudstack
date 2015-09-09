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

package com.cloud.utils.storage;

import java.io.IOException;
import java.io.InputStream;

import com.cloud.utils.NumbersUtil;

public final class QCOW2Utils {
    private static final int VIRTUALSIZE_HEADER_LOCATION = 24;
    private static final int VIRTUALSIZE_HEADER_LENGTH = 8;

    /**
     * Private constructor ->  This utility class cannot be instantiated.
     */
    private QCOW2Utils() {}

    /**
     * @return the header location of the virtual size field.
     */
    public static int getVirtualSizeHeaderLocation() {
        return VIRTUALSIZE_HEADER_LOCATION;
    }

    /**
     * @param inputStream The QCOW2 object in stream format.
     * @return The virtual size of the QCOW2 object.
     */
    public static long getVirtualSize(InputStream inputStream) throws IOException {
        byte[] bytes = new byte[VIRTUALSIZE_HEADER_LENGTH];

        if (inputStream.skip(VIRTUALSIZE_HEADER_LOCATION) != VIRTUALSIZE_HEADER_LOCATION) {
            throw new IOException("Unable to skip to the virtual size header");
        }

        if (inputStream.read(bytes) != VIRTUALSIZE_HEADER_LENGTH) {
            throw new IOException("Unable to properly read the size");
        }

        return NumbersUtil.bytesToLong(bytes);
    }
}