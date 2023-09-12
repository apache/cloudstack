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

package com.cloud.utils;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.xerces.impl.xpath.regex.RegularExpression;

public class UuidUtils {

    private static final RegularExpression uuidRegex = new RegularExpression("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

    public static String first(String uuid) {
        return uuid.substring(0, uuid.indexOf('-'));
    }

    /**
     * Checks if the parameter is a valid UUID (based on {@link UuidUtils#uuidRegex}).
     * <br/>
     * Example: 24abcb8f-4211-374f-a2e1-e5c0b7e88a2d -> true
     *          24abcb8f4211374fa2e1e5c0b7e88a2dda23 -> false
     */
    public static boolean isUuid(String uuid) {
        return uuidRegex.matches(uuid);
    }

    /**
     * Returns a valid UUID in string format from a 32 digit UUID string without hyphens.
     * Example: 24abcb8f4211374fa2e1e5c0b7e88a2d -> 24abcb8f-4211-374f-a2e1-e5c0b7e88a2d
     */
    public static String normalize(String noHyphen) {
        if (noHyphen.length() != 32 || noHyphen.contains("-")) {
            throw new CloudRuntimeException("Invalid string format");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(noHyphen.substring(0, 8)).append("-")
                .append(noHyphen.substring(8, 12)).append("-")
                .append(noHyphen.substring(12, 16)).append("-")
                .append(noHyphen.substring(16, 20)).append("-")
                .append(noHyphen.substring(20, 32));
        String uuid = stringBuilder.toString();
        if (!isUuid(uuid)) {
            throw new CloudRuntimeException("Error generating UUID");
        }
        return uuid;
    }

    public static RegularExpression getUuidRegex() {
        return uuidRegex;
    }
}
