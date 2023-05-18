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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.log4j.Logger;

import com.cloud.utils.NumbersUtil;

public final class QCOW2Utils {
    public static final Logger LOGGER = Logger.getLogger(QCOW2Utils.class.getName());

    private static final int VIRTUALSIZE_HEADER_LOCATION = 24;
    private static final int VIRTUALSIZE_HEADER_LENGTH = 8;
    private static final int MAGIC_HEADER_LENGTH = 4;

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

    public static long getVirtualSize(String urlStr) {
        InputStream inputStream = null;

        try {
            URL url = new URL(urlStr);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(url.openStream());
            inputStream = bufferedInputStream;

            try {
                CompressorInputStream compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(bufferedInputStream);
                inputStream = compressorInputStream;
            } catch (CompressorException e) {
                LOGGER.warn(e.getMessage());
                inputStream = bufferedInputStream;
            }

            byte[] inputBytes = inputStream.readNBytes(VIRTUALSIZE_HEADER_LOCATION + VIRTUALSIZE_HEADER_LENGTH);

            ByteBuffer inputMagicBytes = ByteBuffer.allocate(MAGIC_HEADER_LENGTH);
            inputMagicBytes.put(inputBytes, 0, MAGIC_HEADER_LENGTH);

            ByteBuffer qcow2MagicBytes = ByteBuffer.allocate(MAGIC_HEADER_LENGTH);
            qcow2MagicBytes.put("QFI".getBytes(Charset.forName("UTF-8")));
            qcow2MagicBytes.put((byte)0xfb);

            long virtualSize = 0L;
            // Validate the header magic bytes
            if (qcow2MagicBytes.compareTo(inputMagicBytes) == 0) {
                ByteBuffer virtualSizeBytes = ByteBuffer.allocate(VIRTUALSIZE_HEADER_LENGTH);
                virtualSizeBytes.put(inputBytes, VIRTUALSIZE_HEADER_LOCATION, VIRTUALSIZE_HEADER_LENGTH);
                virtualSize = virtualSizeBytes.getLong(0);
            }

            return virtualSize;
        } catch (MalformedURLException e) {
            LOGGER.warn("Failed to validate for qcow2, malformed URL: " + urlStr + ", error: " + e.getMessage());
            throw new IllegalArgumentException("Invalid URL: " + urlStr);
        }  catch (IOException e) {
            LOGGER.warn("Failed to validate for qcow2, error: " + e.getMessage());
            throw new IllegalArgumentException("Failed to connect URL: " + urlStr);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    LOGGER.warn("Failed to close input stream due to: " + e.getMessage());
                }
            }
        }
    }
}
