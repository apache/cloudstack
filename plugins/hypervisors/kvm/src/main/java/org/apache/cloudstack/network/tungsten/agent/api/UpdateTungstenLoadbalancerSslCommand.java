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
package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.agent.api.Command;

import java.util.Objects;

public class UpdateTungstenLoadbalancerSslCommand extends Command {
    private final String lbUuid;
    private final String sslCertName;
    private final String certificateKey;
    private final String privateKey;
    private final String privateIp;
    private final String port;

    public UpdateTungstenLoadbalancerSslCommand(final String lbUuid, final String sslCertName,
        final String certificateKey, final String privateKey, final String privateIp, final String port) {
        this.lbUuid = lbUuid;
        this.sslCertName = sslCertName;
        this.certificateKey = certificateKey;
        this.privateKey = privateKey;
        this.privateIp = privateIp;
        this.port = port;
    }

    public String getLbUuid() {
        return lbUuid;
    }

    public String getSslCertName() {
        return sslCertName;
    }

    public String getCertificateKey() {
        return certificateKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPort() {
        return port;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UpdateTungstenLoadbalancerSslCommand that = (UpdateTungstenLoadbalancerSslCommand) o;
        return Objects.equals(lbUuid, that.lbUuid) && Objects.equals(sslCertName, that.sslCertName) && Objects.equals(certificateKey, that.certificateKey) && Objects.equals(privateKey, that.privateKey) && Objects.equals(privateIp, that.privateIp) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lbUuid, sslCertName, certificateKey, privateKey, privateIp, port);
    }
}
