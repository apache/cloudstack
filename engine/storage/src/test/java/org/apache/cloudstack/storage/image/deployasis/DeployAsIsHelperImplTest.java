/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.image.deployasis;

import org.junit.Assert;
import org.junit.Test;

public class DeployAsIsHelperImplTest {

    private DeployAsIsHelperImpl deployAsIsHelper = new DeployAsIsHelperImpl();

    private static final String singleHardwareVersionDefinition = "vmx-13";
    private static final String multipleHardwareVersionDefinition = "vmx-10 vmx-11 vmx-13";
    private static final String multipleHardwareVersionDefinitionUnordered = "vmx-13 vmx-8 vmx-10";

    @Test
    public void testGetMinimumSupportedHypervisorVersionForHardwareVersion() {
        String vmwareVersion = deployAsIsHelper.getMinimumSupportedHypervisorVersionForHardwareVersion(singleHardwareVersionDefinition);
        Assert.assertEquals("6.5", vmwareVersion);
    }

    @Test
    public void testGetMinimumSupportedHypervisorVersionForMultipleHardwareVersion() {
        String vmwareVersion = deployAsIsHelper.getMinimumSupportedHypervisorVersionForHardwareVersion(multipleHardwareVersionDefinition);
        Assert.assertEquals("6.0", vmwareVersion);
    }

    @Test
    public void testGetMinimumSupportedHypervisorVersionForMultipleUnorderedHardwareVersion() {
        String vmwareVersion = deployAsIsHelper.getMinimumSupportedHypervisorVersionForHardwareVersion(multipleHardwareVersionDefinitionUnordered);
        Assert.assertEquals("6.0", vmwareVersion);
    }
}
