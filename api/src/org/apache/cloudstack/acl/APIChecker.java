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

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.User;
import com.cloud.utils.component.Adapter;

// APIChecker checks the ownership and access control to API requests
public interface APIChecker extends Adapter {
    // Interface for checking access for a role using apiname
    // If true, apiChecker has checked the operation
    // If false, apiChecker is unable to handle the operation or not implemented
    // On exception, checkAccess failed don't allow
    boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException;
}
