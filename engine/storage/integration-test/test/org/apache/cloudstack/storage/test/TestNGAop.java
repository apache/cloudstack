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
package org.apache.cloudstack.storage.test;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.internal.ConstructorOrMethod;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;

public class TestNGAop implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        for (IMethodInstance methodIns : methods) {
            ITestNGMethod method = methodIns.getMethod();
            ConstructorOrMethod meth = method.getConstructorOrMethod();
            Method m = meth.getMethod();
            if (m != null) {
                DB db = m.getAnnotation(DB.class);
                if (db != null) {
                    TransactionLegacy txn = TransactionLegacy.open(m.getName());
                }
            }
        }

        // TODO Auto-generated method stub
        return methods;
    }

}
