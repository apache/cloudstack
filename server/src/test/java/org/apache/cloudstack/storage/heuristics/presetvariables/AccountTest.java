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

package org.apache.cloudstack.storage.heuristics.presetvariables;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AccountTest {

    @Test
    public void toStringTestReturnsValidJson() {
        Account variable = new Account();
        variable.setName("test name");
        variable.setId("test id");

        Domain domainVariable = new Domain();
        domainVariable.setId("domain id");
        domainVariable.setName("domain name");
        variable.setDomain(domainVariable);

        String expected = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(variable, "name", "id", "domain");
        String result = variable.toString();

        Assert.assertEquals(expected, result);
    }

}
