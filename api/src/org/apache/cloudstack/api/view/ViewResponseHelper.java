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
package org.apache.cloudstack.api.view;

import org.apache.cloudstack.api.response.ControlledViewEntityResponse;
import org.apache.cloudstack.api.view.vo.ControlledViewEntity;

import com.cloud.user.Account;

/**
 * Some helper routine in generating response from db view.
 * @author minc
 *
 */
public class ViewResponseHelper {

    public static void populateOwner(ControlledViewEntityResponse response, ControlledViewEntity object) {

        if (object.getAccountType() == Account.ACCOUNT_TYPE_PROJECT) {
            response.setProjectId(object.getProjectUuid());
            response.setProjectName(object.getProjectName());
        } else {
            response.setAccountName(object.getAccountName());
        }

        response.setDomainId(object.getDomainUuid());
        response.setDomainName(object.getDomainName());
    }
}
