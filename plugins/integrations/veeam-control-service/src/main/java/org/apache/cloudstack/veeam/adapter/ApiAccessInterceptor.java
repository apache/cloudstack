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

package org.apache.cloudstack.veeam.adapter;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class ApiAccessInterceptor implements MethodInterceptor {
    @Inject
    AccountManager accountManager;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method m = invocation.getMethod();
        Object target = invocation.getThis();
        if (target == null) {
            return invocation.proceed();
        }

        ApiAccess access = m.getAnnotation(ApiAccess.class);
        if (access == null) {
            m = target.getClass().getMethod(m.getName(), m.getParameterTypes());
            access = m.getAnnotation(ApiAccess.class);
        }
        if (access == null) {
            return invocation.proceed();
        }

        ServerAdapter adapter = (ServerAdapter) target;
        Pair<User, Account> serviceUserAccount = adapter.getServiceAccount();
        String apiName = BaseCmd.getCommandNameByClass(access.command());

        accountManager.checkApiAccess(serviceUserAccount.second(), apiName);

        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            return invocation.proceed();
        } finally {
            CallContext.unregister();
        }
    }
}
