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
package com.cloud.hypervisor.kvm.resource;

public class LibvirtSecretDef {

    public enum Usage {
        VOLUME("volume"), CEPH("ceph");
        String _usage;

        Usage(String usage) {
            _usage = usage;
        }

        @Override
        public String toString() {
            return _usage;
        }
    }

    private Usage _usage;
    private boolean _ephemeral;
    private boolean _private;
    private String _uuid;
    private String _description;
    private String _cephName;
    private String _volumeVolume;

    public LibvirtSecretDef(Usage usage, String uuid) {
        _usage = usage;
        _uuid = uuid;
    }

    public LibvirtSecretDef(Usage usage, String uuid, String description) {
        _usage = usage;
        _uuid = uuid;
        _description = description;
    }

    public boolean getEphemeral() {
        return _ephemeral;
    }

    public void setEphemeral(boolean ephemeral) { _ephemeral = ephemeral; }

    public boolean getPrivate() {
        return _private;
    }

    public void setPrivate(boolean isPrivate) { _private = isPrivate; }

    public String getUuid() {
        return _uuid;
    }

    public String getDescription() {
        return _description;
    }

    public String getVolumeVolume() {
        return _volumeVolume;
    }

    public String getCephName() {
        return _cephName;
    }

    public void setVolumeVolume(String volume) {
        _volumeVolume = volume;
    }

    public void setCephName(String name) {
        _cephName = name;
    }

    @Override
    public String toString() {
        StringBuilder secretBuilder = new StringBuilder();
        secretBuilder.append("<secret ephemeral='" + (_ephemeral ? "yes" : "no") + "' private='" + (_private ? "yes" : "no") + "'>\n");
        secretBuilder.append("<uuid>" + _uuid + "</uuid>\n");
        if (_description != null) {
            secretBuilder.append("<description>" + _description + "</description>\n");
        }
        secretBuilder.append("<usage type='" + _usage + "'>\n");
        if (_usage == Usage.VOLUME) {
            secretBuilder.append("<volume>" + _volumeVolume + "</volume>\n");
        }
        if (_usage == Usage.CEPH) {
            secretBuilder.append("<name>" + _cephName + "</name>\n");
        }
        secretBuilder.append("</usage>\n");
        secretBuilder.append("</secret>\n");
        return secretBuilder.toString();
    }

}
