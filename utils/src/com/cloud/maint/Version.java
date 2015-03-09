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

package com.cloud.maint;

public class Version {
    /**
     * Compares two version strings and see which one is higher version.
     * @param ver1
     * @param ver2
     * @return positive if ver1 is higher.  negative if ver1 is lower; zero if the same.
     */
    public static int compare(String ver1, String ver2) {
        String[] tokens1 = ver1.split("[.]");
        String[] tokens2 = ver2.split("[.]");
//        assert(tokens1.length <= tokens2.length);

        int compareLength = Math.min(tokens1.length, tokens2.length);
        for (int i = 0; i < compareLength; i++) {
            long version1 = Long.parseLong(tokens1[i]);
            long version2 = Long.parseLong(tokens2[i]);
            if (version1 != version2) {
                return version1 < version2 ? -1 : 1;
            }
        }

        if (tokens1.length > tokens2.length) {
            return 1;
        } else if (tokens1.length < tokens2.length) {
            return -1;
        }

        return 0;
    }

    public static String trimToPatch(String version) {
        int index = version.indexOf("-");

        if (index > 0)
            version = version.substring(0, index);

        String[] tokens = version.split("[.]");

        if (tokens.length < 3)
            return "0";
        return tokens[0] + "." + tokens[1] + "." + tokens[2];
    }

    public static String trimRouterVersion(String version) {
        String[] tokens = version.split(" ");
        if (tokens.length >= 3 && tokens[2].matches("[0-9]+(\\.[0-9]+)*")) {
            return tokens[2];
        }
        return "0";
    }

    public static void main(String[] args) {
        System.out.println("Result is " + compare(args[0], args[1]));
    }

}
