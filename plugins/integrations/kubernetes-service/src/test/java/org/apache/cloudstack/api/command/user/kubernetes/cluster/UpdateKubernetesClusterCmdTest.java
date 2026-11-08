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
package org.apache.cloudstack.api.command.user.kubernetes.cluster;

import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.cluster.KubernetesServiceHelperImpl;
import com.cloud.vm.VmDetailConstants;

public class UpdateKubernetesClusterCmdTest {

    private UpdateKubernetesClusterCmd cmd(String nodeType, String size) {
        UpdateKubernetesClusterCmd cmd = new UpdateKubernetesClusterCmd();
        cmd.kubernetesServiceHelper = new KubernetesServiceHelperImpl();
        ReflectionTestUtils.setField(cmd, "rootDiskSizeNodeTypeMap",
                Map.of("0", Map.of(VmDetailConstants.CKS_NODE_TYPE, nodeType, ApiConstants.SIZE, size)));
        return cmd;
    }

    @Test
    public void rootDiskSizesAreReadByNodeType() {
        Assert.assertEquals(Map.of("WORKER", 50L), cmd("worker", "50").getRootDiskSizeNodeTypeMap());
        Assert.assertTrue(new UpdateKubernetesClusterCmd().getRootDiskSizeNodeTypeMap().isEmpty());
    }

    @Test
    public void badRootDiskSizesAreRefused() {
        for (String[] entry : new String[][] {{"worker", "-1"}, {"worker", "big"}, {"master", "50"}, {"worker", ""}}) {
            Assert.assertThrows(entry[0] + "=" + entry[1], InvalidParameterValueException.class, () -> cmd(entry[0], entry[1]).getRootDiskSizeNodeTypeMap());
        }
    }

    @Test
    public void rolloutAndCancelDefaultToFalse() {
        UpdateKubernetesClusterCmd cmd = new UpdateKubernetesClusterCmd();
        Assert.assertFalse(cmd.isRollout());
        Assert.assertFalse(cmd.isCancelRollout());
    }
}
