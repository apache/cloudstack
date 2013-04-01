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

public class LibvirtStorageVolumeDef {
    public enum volFormat {
        RAW("raw"), QCOW2("qcow2"), DIR("dir"), TAR("tar");
        private String _format;

        volFormat(String format) {
            _format = format;
        }

        @Override
        public String toString() {
            return _format;
        }

        public static volFormat getFormat(String format) {
            if (format == null) {
                return null;
            }
            if (format.equalsIgnoreCase("raw")) {
                return RAW;
            } else if (format.equalsIgnoreCase("qcow2")) {
                return QCOW2;
            } else if (format.equalsIgnoreCase("dir")) {
                return DIR;
            } else if (format.equalsIgnoreCase("tar")) {
                return TAR;
            }
            return null;
        }
    }

    private String _volName;
    private Long _volSize;
    private volFormat _volFormat;
    private String _backingPath;
    private volFormat _backingFormat;

    public LibvirtStorageVolumeDef(String volName, Long size, volFormat format,
            String tmplPath, volFormat tmplFormat) {
        _volName = volName;
        _volSize = size;
        _volFormat = format;
        _backingPath = tmplPath;
        _backingFormat = tmplFormat;
    }

    public volFormat getFormat() {
        return this._volFormat;
    }

    @Override
    public String toString() {
        StringBuilder storageVolBuilder = new StringBuilder();
        storageVolBuilder.append("<volume>\n");
        storageVolBuilder.append("<name>" + _volName + "</name>\n");
        if (_volSize != null) {
            storageVolBuilder
                    .append("<capacity >" + _volSize + "</capacity>\n");
        }
        storageVolBuilder.append("<target>\n");
        storageVolBuilder.append("<format type='" + _volFormat + "'/>\n");
        storageVolBuilder.append("<permissions>");
        storageVolBuilder.append("<mode>0744</mode>");
        storageVolBuilder.append("</permissions>");
        storageVolBuilder.append("</target>\n");
        if (_backingPath != null) {
            storageVolBuilder.append("<backingStore>\n");
            storageVolBuilder.append("<path>" + _backingPath + "</path>\n");
            storageVolBuilder.append("<format type='" + _backingFormat
                    + "'/>\n");
            storageVolBuilder.append("</backingStore>\n");
        }
        storageVolBuilder.append("</volume>\n");
        return storageVolBuilder.toString();
    }

}
