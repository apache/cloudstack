// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.utils.component.ComponentContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/com/cloud/utils/db/transactioncontextBuilderTest.xml")
public class TransactionContextBuilderTest {

    @Inject
    DbAnnotatedBaseDerived _derived;

    DbAnnotatedBase _base;

    @Inject
    List<DbAnnotatedBase> _list;

    @Test
    public void test() {
        // _derived.DbAnnotatedMethod();
        // _base.MethodWithClassDbAnnotated();

        // test @DB injection on dynamically constructed objects
        DbAnnotatedBase base = ComponentContext.inject(new DbAnnotatedBase());
        base.MethodWithClassDbAnnotated();

        /*
                Map<String, DbAnnotatedBase> components = ComponentContext.getApplicationContext().getBeansOfType(DbAnnotatedBase.class);
                for(Map.Entry<String, DbAnnotatedBase> entry : components.entrySet()) {
                    System.out.println(entry.getKey());
                    entry.getValue().MethodWithClassDbAnnotated();
                }
        */
        for (DbAnnotatedBase entry : _list) {
            entry.MethodWithClassDbAnnotated();
        }
    }
}
