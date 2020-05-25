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

package org.apache.cloudstack.acl;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.google.common.base.Enums;
import com.google.common.base.Strings;

public interface ProjectRole extends RoleEntity, InternalIdentity, Identity {

    Long getProjectId();

    public enum ProjectRoleType {
        Admin, Regular;

        public static ProjectRoleType fromString(final String name) {
            if (!Strings.isNullOrEmpty(name)
                    && Enums.getIfPresent(RoleType.class, name).isPresent()) {
                return ProjectRoleType.valueOf(name);
            }
            throw new IllegalStateException("Illegal ProjectRoleType name provided");
        }
    }
}
