/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.service.model;

import org.apache.cloudstack.storage.feign.model.ExportRule;

/**
 * Three-way translation for a single CloudStack NFS {@link ProtocolType}:
 * the ONTAP REST export-rule token, the Linux mount option, and the SVM
 * capability field that gates pool creation.
 *
 * <p>One place to change when adding a new NFS variant (v4.2, Kerberized v4.1,
 * etc.) so the driver, lifecycle, host listener, and export-policy builder all
 * stay in agreement.</p>
 */
public final class NfsProtocolSpec {

    public static final NfsProtocolSpec NFS3 = new NfsProtocolSpec(
            ProtocolType.NFS3,
            ExportRule.ProtocolsEnum.NFS3,
            "vers=3",
            "nfs.enabled");

    public static final NfsProtocolSpec NFS4_1 = new NfsProtocolSpec(
            ProtocolType.NFS4_1,
            ExportRule.ProtocolsEnum.NFS4,
            "vers=4.1",
            "nfs.protocol.v41_enabled");

    private final ProtocolType protocol;
    private final ExportRule.ProtocolsEnum exportToken;
    private final String mountOptions;
    private final String svmCapabilityField;

    private NfsProtocolSpec(ProtocolType protocol,
                            ExportRule.ProtocolsEnum exportToken,
                            String mountOptions,
                            String svmCapabilityField) {
        this.protocol = protocol;
        this.exportToken = exportToken;
        this.mountOptions = mountOptions;
        this.svmCapabilityField = svmCapabilityField;
    }

    public ProtocolType protocol() {
        return protocol;
    }

    public ExportRule.ProtocolsEnum exportToken() {
        return exportToken;
    }

    public String mountOptions() {
        return mountOptions;
    }

    public String svmCapabilityField() {
        return svmCapabilityField;
    }

    public static NfsProtocolSpec forProtocol(ProtocolType protocol) {
        switch (protocol) {
            case NFS3:
                return NFS3;
            case NFS4_1:
                return NFS4_1;
            default:
                throw new IllegalArgumentException("Not an NFS protocol: " + protocol);
        }
    }

    public static boolean isNfs(ProtocolType protocol) {
        return protocol == ProtocolType.NFS3 || protocol == ProtocolType.NFS4_1;
    }

    public static boolean isNfs(String protocolName) {
        if (protocolName == null) {
            return false;
        }
        return ProtocolType.NFS3.name().equalsIgnoreCase(protocolName)
                || ProtocolType.NFS4_1.name().equalsIgnoreCase(protocolName);
    }
}
