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

import com.cloud.exception.UnavailableCommandException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import java.util.Map;

public abstract class QuotaBaseCmd extends BaseCmd {

    @Override
    public void validateCommandSpecificPermissionsAndParameters(final Map<String, String> params) {
        validateQuotaAccountEnabled(this);
        super.validateCommandSpecificPermissionsAndParameters(params);
    }

    public static void validateQuotaAccountEnabled(BaseCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();

        if (caller.getType().equals(Account.Type.ADMIN)) {
            return;
        }

        if (!QuotaConfig.QuotaPluginEnabled.value() || !QuotaConfig.QuotaAccountEnabled.valueIn(caller.getAccountId())){
            throw new UnavailableCommandException(String.format("The API [%s] is not available for account %s.", cmd.getActualCommandName(),
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(caller, "accountName", "uuid")));
        }
    }

}
