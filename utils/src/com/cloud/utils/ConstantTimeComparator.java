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

public class ConstantTimeComparator {

    public static boolean compareBytes(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < b1.length; i++) {
            result |= b1[i] ^ b2[i];
        }
        return result == 0;
    }

    public static boolean compareStrings(String s1, String s2) {
        return compareBytes(s1.getBytes(), s2.getBytes());
    }
}
