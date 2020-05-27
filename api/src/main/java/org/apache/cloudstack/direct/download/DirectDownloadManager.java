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

package org.apache.cloudstack.direct.download;

import org.apache.cloudstack.framework.agent.direct.download.DirectDownloadService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.utils.component.PluggableService;

public interface DirectDownloadManager extends DirectDownloadService, PluggableService, Configurable {

    static final int DEFAULT_DIRECT_DOWNLOAD_CONNECT_TIMEOUT = 5000;
    static final int DEFAULT_DIRECT_DOWNLOAD_SOCKET_TIMEOUT = 5000;
    static final int DEFAULT_DIRECT_DOWNLOAD_CONNECTION_REQUEST_TIMEOUT = 5000;

    ConfigKey<Long> DirectDownloadCertificateUploadInterval = new ConfigKey<>("Advanced", Long.class,
            "direct.download.certificate.background.task.interval",
            "0",
            "This interval (in hours) controls a background task to sync hosts within enabled zones " +
                    "missing uploaded certificates for direct download." +
                    "Only certificates which have not been revoked from hosts are uploaded",
            false);

    static final ConfigKey<Integer> DirectDownloadConnectTimeout = new ConfigKey<Integer>("Advanced", Integer.class,
            "direct.download.connect.timeout",
            String.valueOf(DEFAULT_DIRECT_DOWNLOAD_CONNECT_TIMEOUT),
            "Connection establishment timeout in milliseconds for direct download",
            true);

    static final ConfigKey<Integer> DirectDownloadSocketTimeout = new ConfigKey<Integer>("Advanced", Integer.class,
            "direct.download.socket.timeout",
            String.valueOf(DEFAULT_DIRECT_DOWNLOAD_SOCKET_TIMEOUT),
            "Socket timeout (SO_TIMEOUT) in milliseconds for direct download",
            true);

    static final ConfigKey<Integer> DirectDownloadConnectionRequestTimeout = new ConfigKey<Integer>("Hidden", Integer.class,
            "direct.download.connection.request.timeout",
            String.valueOf(DEFAULT_DIRECT_DOWNLOAD_CONNECTION_REQUEST_TIMEOUT),
            "Requesting a connection from connection manager timeout in milliseconds for direct download",
            true);

    /**
     * Revoke direct download certificate with alias 'alias' from hosts of hypervisor type 'hypervisor'
     */
    boolean revokeCertificateAlias(String certificateAlias, String hypervisor, Long zoneId, Long hostId);
}
