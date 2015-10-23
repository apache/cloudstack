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

package org.apache.cloudstack.utils.security;

import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SSLUtils {
    public static final Logger s_logger = Logger.getLogger(SSLUtils.class);

    public static String[] getSupportedProtocols(String[] protocols) {
        Set<String> set = new HashSet<String>();
        for (String s : protocols) {
            if (s.equals("SSLv3") || s.equals("SSLv2Hello")) {
                continue;
            }
            set.add(s);
        }
        return (String[]) set.toArray(new String[set.size()]);
    }

    public static String[] getSupportedCiphers() throws NoSuchAlgorithmException {
        String[] availableCiphers = getSSLContext().getSocketFactory().getSupportedCipherSuites();
        Arrays.sort(availableCiphers);
        return availableCiphers;
    }

    public static SSLContext getSSLContext() throws NoSuchAlgorithmException {
        return SSLContext.getInstance("TLSv1");
    }

    public static SSLContext getSSLContext(String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        return SSLContext.getInstance("TLSv1", provider);
    }
}
