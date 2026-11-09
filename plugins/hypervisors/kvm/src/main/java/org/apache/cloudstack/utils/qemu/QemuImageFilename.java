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

package org.apache.cloudstack.utils.qemu;

import org.json.JSONException;
import org.json.JSONObject;

// qemu reports an image opened with options that have no place in a plain path, such as the key secret of an
// encrypted qcow2, under a json:{...} pseudo-filename whose innermost file.filename is the path.
public final class QemuImageFilename {
    private static final String JSON_PREFIX = "json:";

    private QemuImageFilename() {
    }

    // The plain path behind a qemu file name: the innermost file.filename of a json: pseudo-filename, else the name.
    public static String plain(String filename) {
        if (filename == null || !filename.startsWith(JSON_PREFIX)) {
            return filename;
        }
        try {
            JSONObject options = new JSONObject(filename.substring(JSON_PREFIX.length()));
            while (options.has("file") && options.optJSONObject("file") != null) {
                options = options.getJSONObject("file");
            }
            String plain = options.optString("filename", null);
            return plain != null ? plain : filename;
        } catch (JSONException e) {
            return filename;
        }
    }
}
