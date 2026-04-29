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
package org.apache.cloudstack.utils.security;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class ChecksumValue {
    String checksum;
    String algorithm = "MD5";
    public ChecksumValue(String algorithm, String checksum) {
        this.algorithm = algorithm;
        this.checksum = checksum;
    }
    public ChecksumValue(String digest) {
        digest = StringUtils.strip(digest);
        this.algorithm = algorithmFromDigest(digest);
        this.checksum = stripAlgorithmFromDigest(digest);
    }

    @Override
    public String toString() {
        return '{' + algorithm + '}'+ checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ChecksumValue that = (ChecksumValue)o;
        return Objects.equals(getChecksum(), that.getChecksum()) && Objects.equals(getAlgorithm(), that.getAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getChecksum(), getAlgorithm());
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    private static String stripAlgorithmFromDigest(String digest) {
        if(StringUtils.isNotEmpty(digest)) {
            int s = digest.indexOf('{');// only assume a
            int e = digest.indexOf('}');
            if (s == 0 && e > s) { // we have an algorithm name of at least 1 char
                return digest.substring(e+1);
            }
        }
        // we assume digest is alright if there is no algorithm at the start
        return digest;
    }

    private static String algorithmFromDigest(String digest) {
        if(StringUtils.isNotEmpty(digest)) {
            int s = digest.indexOf('{');
            int e = digest.indexOf('}');
            if (s == 0 && e > s+1) { // we have an algorithm name of at least 1 char
                return digest.substring(s+1,e);
            } // else if no algoritm
            return "MD5";
        } // or if no digest at all
        return "SHA-512";
    }
}
