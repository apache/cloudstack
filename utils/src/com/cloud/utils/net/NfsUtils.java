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

package com.cloud.utils.net;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class NfsUtils {

    public static String url2Mount(String urlStr) throws URISyntaxException {
        URI url;
        url = new URI(urlStr);
        return url.getHost() + ":" + url.getPath();
    }

    public static String uri2Mount(URI uri) {
        return uri.getHost() + ":" + uri.getPath();
    }

    public static String url2PathSafeString(String urlStr) {
        String safe = urlStr.replace(File.separatorChar, '-');
        safe = safe.replace("?", "");
        safe = safe.replace("*", "");
        safe = safe.replace("\\", "");
        safe = safe.replace("/", "");
        return safe;
    }

    public static String getHostPart(String nfsPath) {
        String toks[] = nfsPath.split(":");
        if (toks != null && toks.length == 2) {
            return toks[0];
        }
        return null;
    }

}
