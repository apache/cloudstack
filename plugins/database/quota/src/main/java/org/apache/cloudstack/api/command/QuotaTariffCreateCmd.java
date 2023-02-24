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
import org.apache.log4j.Logger;

import javax.inject.Inject;

import java.util.Date;

@APICommand(name = "quotaTariffCreate", responseObject = QuotaTariffResponse.class, description = "Creates a quota tariff for a resource.", since = "4.18.0.0",
requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class QuotaTariffCreateCmd extends BaseCmd {
    protected Logger logger = Logger.getLogger(getClass());

    @Inject
    QuotaResponseBuilder responseBuilder;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Quota tariff's name", length = 65535)
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Quota tariff's description.", length = 65535)
    private String description;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, required = true, description = "Integer value for the usage type of the resource.")
    private Integer usageType;

    @Parameter(name = "value", type = CommandType.DOUBLE, required = true, description = "The quota tariff value of the resource as per the default unit.")
    private Double value;

    @Parameter(name = ApiConstants.ACTIVATION_RULE, type = CommandType.STRING, description = "Quota tariff's activation rule. It can receive a JS script that results in either " +
            "a boolean or a numeric value: if it results in a boolean value, the tariff value will be applied according to the result; if it results in a numeric value, the " +
            "numeric value will be applied; if the result is neither a boolean nor a numeric value, the tariff will not be applied. If the rule is not informed, the tariff " +
            "value will be applied.", length = 65535)
    private String activationRule;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "The effective start date on/after which the quota tariff is effective. Use yyyy-MM-dd as"
            + " the date format, e.g. startDate=2009-06-03. Inform null to use the current date.")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "The end date of the quota tariff. Use yyyy-MM-dd as the date format, e.g."
            + " endDate=2009-06-03.")
    private Date endDate;

    @Override
    public void execute() {
        QuotaTariffVO result = responseBuilder.createQuotaTariff(this);

        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create new quota tariff.");
        }

        QuotaTariffResponse response = responseBuilder.createQuotaTariffResponse(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getActivationRule() {
        return activationRule;
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

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

}
