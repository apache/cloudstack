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

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TemplateTest {

    @Test
    public void toStringTestReturnsValidJson() {
        Template variable = new Template();
        variable.setName("test name");
        variable.setTemplateType(Storage.TemplateType.USER);
        variable.setHypervisorType(Hypervisor.HypervisorType.KVM);
        variable.setFormat(Storage.ImageFormat.QCOW2);

        String expected = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(variable, "name", "templateType",
                "hypervisorType", "format");
        String result = variable.toString();

        Assert.assertEquals(expected, result);
    }

}
