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

public class KubernetesClusterAffinityGroupMapVOTest {

    @Test
    public void testConstructorAndGetters() {
        KubernetesClusterAffinityGroupMapVO vo =
            new KubernetesClusterAffinityGroupMapVO(1L, "CONTROL", 100L);

        Assert.assertEquals(1L, vo.getClusterId());
        Assert.assertEquals("CONTROL", vo.getNodeType());
        Assert.assertEquals(100L, vo.getAffinityGroupId());
    }

    @Test
    public void testDefaultConstructor() {
        KubernetesClusterAffinityGroupMapVO vo = new KubernetesClusterAffinityGroupMapVO();
        Assert.assertNotNull(vo);
    }

    @Test
    public void testSetClusterId() {
        KubernetesClusterAffinityGroupMapVO vo = new KubernetesClusterAffinityGroupMapVO();
        vo.setClusterId(2L);
        Assert.assertEquals(2L, vo.getClusterId());
    }

    @Test
    public void testSetNodeType() {
        KubernetesClusterAffinityGroupMapVO vo = new KubernetesClusterAffinityGroupMapVO();
        vo.setNodeType("WORKER");
        Assert.assertEquals("WORKER", vo.getNodeType());
    }

    @Test
    public void testSetAffinityGroupId() {
        KubernetesClusterAffinityGroupMapVO vo = new KubernetesClusterAffinityGroupMapVO();
        vo.setAffinityGroupId(200L);
        Assert.assertEquals(200L, vo.getAffinityGroupId());
    }

    @Test
    public void testAllNodeTypes() {
        KubernetesClusterAffinityGroupMapVO controlVo =
            new KubernetesClusterAffinityGroupMapVO(1L, "CONTROL", 10L);
        KubernetesClusterAffinityGroupMapVO workerVo =
            new KubernetesClusterAffinityGroupMapVO(1L, "WORKER", 20L);
        KubernetesClusterAffinityGroupMapVO etcdVo =
            new KubernetesClusterAffinityGroupMapVO(1L, "ETCD", 30L);

        Assert.assertEquals("CONTROL", controlVo.getNodeType());
        Assert.assertEquals("WORKER", workerVo.getNodeType());
        Assert.assertEquals("ETCD", etcdVo.getNodeType());
    }

    @Test
    public void testSettersChain() {
        KubernetesClusterAffinityGroupMapVO vo = new KubernetesClusterAffinityGroupMapVO();

        vo.setClusterId(5L);
        vo.setNodeType("ETCD");
        vo.setAffinityGroupId(500L);

        Assert.assertEquals(5L, vo.getClusterId());
        Assert.assertEquals("ETCD", vo.getNodeType());
        Assert.assertEquals(500L, vo.getAffinityGroupId());
    }
}
