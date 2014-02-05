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
package org.apache.cloudstack.api;

import org.apache.cloudstack.acl.RoleType;

public abstract class BaseAsyncCustomIdCmd extends BaseAsyncCmd {
    @Parameter(name = ApiConstants.CUSTOM_ID,
               type = CommandType.STRING,
 description = "an optional field, in case you want to set a custom id to the resource. Allowed to Root Admins only", since = "4.4", authorized = {RoleType.Admin})
    private String customId;

    public String getCustomId() {
        return customId;
    }

    public abstract void checkUuid();

}
