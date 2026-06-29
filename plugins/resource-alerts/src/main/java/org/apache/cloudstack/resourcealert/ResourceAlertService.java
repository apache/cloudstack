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

package org.apache.cloudstack.resourcealert;

import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.resourcealert.api.command.admin.CreateResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.DeleteResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.ListResourceAlertRulesCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.ListResourceAlertsCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.UpdateResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.response.ResourceAlertResponse;
import org.apache.cloudstack.resourcealert.api.response.ResourceAlertRuleResponse;

import com.cloud.utils.component.PluggableService;

public interface ResourceAlertService extends PluggableService {

    ResourceAlertRuleResponse createResourceAlertRule(CreateResourceAlertRuleCmd cmd);
    ListResponse<ResourceAlertRuleResponse> listResourceAlertRules(ListResourceAlertRulesCmd cmd);
    ResourceAlertRuleResponse updateResourceAlertRule(UpdateResourceAlertRuleCmd cmd);
    boolean deleteResourceAlertRule(DeleteResourceAlertRuleCmd cmd);
    ListResponse<ResourceAlertResponse> listResourceAlerts(ListResourceAlertsCmd cmd);
}
