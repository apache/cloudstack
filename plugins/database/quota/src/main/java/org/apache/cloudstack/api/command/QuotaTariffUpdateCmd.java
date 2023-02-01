//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command;

import com.cloud.user.Account;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;

import javax.inject.Inject;

import java.util.Date;

@APICommand(name = "quotaTariffUpdate", responseObject = QuotaTariffResponse.class, description = "Update the tariff plan for a resource", since = "4.7.0",
requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class QuotaTariffUpdateCmd extends BaseCmd {

    @Inject
    QuotaResponseBuilder _responseBuilder;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, description = "DEPRECATED. Integer value for the usage type of the resource")
    private Integer usageType;

    @Parameter(name = ApiConstants.VALUE, type = CommandType.DOUBLE, description = "The quota tariff value of the resource as per the default unit.")
    private Double value;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "DEPRECATED. The effective start date on/after which the quota tariff is effective. " +
            "Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03.")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "The end date of the quota tariff. Use yyyy-MM-dd as the date format, e.g."
            + " endDate=2009-06-03.", since = "4.18.0.0")
    private Date endDate;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Quota tariff's name", length = 65535, since = "4.18.0.0")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Quota tariff's description. Inform empty to remove the description.", length = 65535,
            since = "4.18.0.0")
    private String description;

    @Parameter(name = ApiConstants.ACTIVATION_RULE, type = CommandType.STRING, description = "Quota tariff's activation rule. It can receive a JS script that results in either " +
            "a boolean or a numeric value: if it results in a boolean value, the tariff value will be applied according to the result; if it results in a numeric value, the " +
            "numeric value will be applied; if the result is neither a boolean nor a numeric value, the tariff will not be applied. If the rule is not informed, the tariff " +
            "value will be applied. Inform empty to remove the activation rule.", length = 65535, since = "4.18.0.0")
    private String activationRule;

    public Integer getUsageType() {
        return usageType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getActivationRule() {
        return activationRule;
    }

    public QuotaTariffUpdateCmd() {
        super();
    }

    @Override
    public void execute() {
        final QuotaTariffVO result = _responseBuilder.updateQuotaTariffPlan(this);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update quota tariff plan");
        }
        final QuotaTariffResponse response = _responseBuilder.createQuotaTariffResponse(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
