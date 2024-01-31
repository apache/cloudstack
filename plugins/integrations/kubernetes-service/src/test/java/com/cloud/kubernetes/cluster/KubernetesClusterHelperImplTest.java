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
package com.cloud.kubernetes.cluster;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterHelperImplTest {

    private final KubernetesClusterHelperImpl helper = new KubernetesClusterHelperImpl();

    @Test
    public void testIsValidNodeTypeEmptyNodeType() {
        Assert.assertFalse(helper.isValidNodeType(null));
    }

    @Test
    public void testIsValidNodeTypeInvalidNodeType() {
        String nodeType = "invalidNodeType";
        Assert.assertFalse(helper.isValidNodeType(nodeType));
    }

    @Test
    public void testIsValidNodeTypeValidNodeTypeLowercase() {
        String nodeType = KubernetesClusterHelper.KubernetesClusterNodeType.WORKER.name().toLowerCase();
        Assert.assertTrue(helper.isValidNodeType(nodeType));
    }
}
