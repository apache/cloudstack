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
package com.cloud.ovm.object;

import org.apache.xmlrpc.XmlRpcException;

public class OvmVolume extends OvmObject {
    public static class Details {
        public String name;
        public String uuid;
        public String poolUuid;
        public Long size;
        public String path;

        public String toJson() {
            return Coder.toJson(this);
        }
    }

    public static Details createDataDsik(Connection c, String poolUuid, String size, Boolean isRoot) throws XmlRpcException {
        Object[] params = {poolUuid, size, isRoot};
        String res = (String)c.call("OvmVolume.createDataDisk", params);
        Details result = Coder.fromJson(res, Details.class);
        return result;
    }

    public static Details createFromTemplate(Connection c, String poolUuid, String templateUrl) throws XmlRpcException {
        Object[] params = {poolUuid, templateUrl};
        String res = (String)c.callTimeoutInSec("OvmVolume.createFromTemplate", params, 3600 * 3);
        Details result = Coder.fromJson(res, Details.class);
        return result;
    }

    public static void destroy(Connection c, String poolUuid, String path) throws XmlRpcException {
        Object[] params = {poolUuid, path};
        c.call("OvmVolume.destroy", params);
    }

}
