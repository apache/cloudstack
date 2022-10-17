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

package org.apache.cloudstack.quota.activationrule.presetvariables;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GenericPresetVariableTest {

    @Test
    public void setIdTestAddFieldIdToCollection() {
        GenericPresetVariable variable = new GenericPresetVariable();
        variable.setId("test");
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("id"));
    }

    @Test
    public void setNameTestAddFieldNameToCollection() {
        GenericPresetVariable variable = new GenericPresetVariable();
        variable.setName("test");
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("name"));
    }

    @Test
    public void toStringTestSetAllFieldsAndReturnAJson() {
        GenericPresetVariable variable = new GenericPresetVariable();
        variable.setId("test id");
        variable.setName("test name");

        String expected = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(variable, "id", "name");
        String result = variable.toString();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void toStringTestSetSomeFieldsAndReturnAJson() {
        GenericPresetVariable variable = new GenericPresetVariable();
        variable.setId("test id");

        String expected = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(variable, "id");
        String result = variable.toString();

        Assert.assertEquals(expected, result);

        variable = new GenericPresetVariable();
        variable.setName("test name");

        expected = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(variable, "name");
        result = variable.toString();

        Assert.assertEquals(expected, result);
    }
}
