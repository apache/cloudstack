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
package com.cloud.kubernetes.cluster.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroup;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterNodeGroupUtil.Taint;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterNodeGroupUtilTest {

    private Map<String, Map<String, String>> apiMap(String[]... entries) {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        int i = 0;
        for (String[] entry : entries) {
            Map<String, String> values = new HashMap<>();
            values.put(KubernetesClusterNodeGroupUtil.KEY, entry[0]);
            values.put(KubernetesClusterNodeGroupUtil.VALUE, entry[1]);
            if (entry.length > 2) {
                values.put(KubernetesClusterNodeGroupUtil.EFFECT, entry[2]);
            }
            map.put(String.valueOf(i++), values);
        }
        return map;
    }

    @Test
    public void testValidateNodeGroupNameValid() {
        KubernetesClusterNodeGroupUtil.validateNodeGroupName("gpu-pool-1");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateNodeGroupNameUpperCase() {
        KubernetesClusterNodeGroupUtil.validateNodeGroupName("GPU");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateNodeGroupNameTooLong() {
        KubernetesClusterNodeGroupUtil.validateNodeGroupName("a-very-long-node-group-name");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateNodeGroupNameTrailingDash() {
        KubernetesClusterNodeGroupUtil.validateNodeGroupName("pool-");
    }

    @Test
    public void testGetLabelsFromApiMap() {
        Map<String, String> labels = KubernetesClusterNodeGroupUtil.getLabelsFromApiMap(
                apiMap(new String[]{"disktype", "ssd"}, new String[]{"example.com/team", "data"}, new String[]{"empty", ""}));
        Assert.assertEquals(3, labels.size());
        Assert.assertEquals("ssd", labels.get("disktype"));
        Assert.assertEquals("data", labels.get("example.com/team"));
        Assert.assertEquals("disktype=ssd,example.com/team=data,empty=", KubernetesClusterNodeGroupUtil.labelsToString(labels));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetLabelsFromApiMapRestrictedNamespace() {
        KubernetesClusterNodeGroupUtil.getLabelsFromApiMap(apiMap(new String[]{"node-role.kubernetes.io/worker", ""}));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetLabelsFromApiMapReservedNodeGroupLabel() {
        KubernetesClusterNodeGroupUtil.getLabelsFromApiMap(apiMap(new String[]{KubernetesClusterNodeGroup.NODE_GROUP_LABEL, "other"}));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetLabelsFromApiMapInvalidValue() {
        KubernetesClusterNodeGroupUtil.getLabelsFromApiMap(apiMap(new String[]{"key", "value;rm -rf /"}));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetLabelsFromApiMapDuplicateKey() {
        KubernetesClusterNodeGroupUtil.getLabelsFromApiMap(apiMap(new String[]{"key", "a"}, new String[]{"key", "b"}));
    }

    @Test
    public void testGetTaintsFromApiMap() {
        List<Taint> taints = KubernetesClusterNodeGroupUtil.getTaintsFromApiMap(
                apiMap(new String[]{"gpu", "true", "NoSchedule"}, new String[]{"dedicated", "", "NoExecute"}));
        Assert.assertEquals(2, taints.size());
        Assert.assertEquals("gpu=true:NoSchedule,dedicated:NoExecute", KubernetesClusterNodeGroupUtil.taintsToString(taints));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetTaintsFromApiMapInvalidEffect() {
        KubernetesClusterNodeGroupUtil.getTaintsFromApiMap(apiMap(new String[]{"gpu", "true", "Never"}));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetTaintsFromApiMapDuplicateKeyAndEffect() {
        KubernetesClusterNodeGroupUtil.getTaintsFromApiMap(apiMap(new String[]{"gpu", "a", "NoSchedule"}, new String[]{"gpu", "b", "NoSchedule"}));
    }

    @Test
    public void testTaintsFromStringRoundTrip() {
        String taints = "gpu=true:NoSchedule,dedicated:NoExecute,example.com/key=v:PreferNoSchedule";
        List<Taint> list = KubernetesClusterNodeGroupUtil.taintsFromString(taints);
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(new Taint("dedicated", "", "NoExecute"), list.get(1));
        Assert.assertEquals(taints, KubernetesClusterNodeGroupUtil.taintsToString(list));
    }

    @Test
    public void testLabelsAndTaintsFromEmptyString() {
        Assert.assertTrue(KubernetesClusterNodeGroupUtil.labelsFromString(null).isEmpty());
        Assert.assertTrue(KubernetesClusterNodeGroupUtil.taintsFromString("").isEmpty());
        Assert.assertNull(KubernetesClusterNodeGroupUtil.labelsToString(new HashMap<>()));
        Assert.assertNull(KubernetesClusterNodeGroupUtil.taintsToString(List.of()));
    }

    @Test
    public void testGetKubeletExtraArgs() {
        KubernetesClusterNodeGroup nodeGroup = Mockito.mock(KubernetesClusterNodeGroup.class);
        Mockito.when(nodeGroup.getName()).thenReturn("gpu");
        Mockito.when(nodeGroup.getLabels()).thenReturn("disktype=ssd");
        Mockito.when(nodeGroup.getTaints()).thenReturn("gpu=true:NoSchedule");
        Assert.assertEquals("--node-labels=cloudstack.apache.org/nodegroup=gpu,disktype=ssd --register-with-taints=gpu=true:NoSchedule",
                KubernetesClusterNodeGroupUtil.getKubeletExtraArgs(nodeGroup));
    }

    @Test
    public void testGetKubeletExtraArgsWithoutLabelsAndTaints() {
        KubernetesClusterNodeGroup nodeGroup = Mockito.mock(KubernetesClusterNodeGroup.class);
        Mockito.when(nodeGroup.getName()).thenReturn("pool");
        Assert.assertEquals("--node-labels=cloudstack.apache.org/nodegroup=pool",
                KubernetesClusterNodeGroupUtil.getKubeletExtraArgs(nodeGroup));
    }

    @Test
    public void testGetLabelAndTaintUpdateCommands() {
        List<String> commands = KubernetesClusterNodeGroupUtil.getLabelAndTaintUpdateCommands("node-1", "pool",
                "a=1,b=2", "b=3,c=4",
                "gpu=true:NoSchedule,old:NoExecute", "gpu=false:NoSchedule");
        Assert.assertEquals(List.of(
                "sudo /opt/bin/kubectl label node node-1 a- cloudstack.apache.org/nodegroup=pool b=3 c=4 --overwrite",
                "sudo /opt/bin/kubectl taint node node-1 old:NoExecute- || true",
                "sudo /opt/bin/kubectl taint node node-1 gpu=false:NoSchedule --overwrite"), commands);
    }

    @Test
    public void testGetLabelAndTaintUpdateCommandsCleanup() {
        List<String> commands = KubernetesClusterNodeGroupUtil.getLabelAndTaintUpdateCommands("node-1", "pool",
                "a=1", null, "gpu=true:NoSchedule", null);
        Assert.assertEquals(List.of(
                "sudo /opt/bin/kubectl label node node-1 a- cloudstack.apache.org/nodegroup=pool --overwrite",
                "sudo /opt/bin/kubectl taint node node-1 gpu:NoSchedule- || true"), commands);
    }

    @Test
    public void testGetLabelAndTaintUpdateCommandsOnlyNodeGroupLabel() {
        Assert.assertEquals(List.of("sudo /opt/bin/kubectl label node node-1 cloudstack.apache.org/nodegroup=default --overwrite"),
                KubernetesClusterNodeGroupUtil.getLabelAndTaintUpdateCommands("node-1", "default", null, null, null, null));
    }

    @Test
    public void testHasSchedulingTaints() {
        Assert.assertFalse(KubernetesClusterNodeGroupUtil.hasSchedulingTaints(null));
        Assert.assertFalse(KubernetesClusterNodeGroupUtil.hasSchedulingTaints(List.of(
                new KubernetesClusterNodeGroupUtil.Taint("dedicated", "system", "PreferNoSchedule"))));
        Assert.assertTrue(KubernetesClusterNodeGroupUtil.hasSchedulingTaints(List.of(
                new KubernetesClusterNodeGroupUtil.Taint("dedicated", "system", "PreferNoSchedule"),
                new KubernetesClusterNodeGroupUtil.Taint("gpu", null, "NoExecute"))));
    }
    @Test
    public void testMergeLabelsNodeOverridesNodeGroup() {
        Assert.assertEquals("pool=gpu,disktype=nvme,zone=a",
                KubernetesClusterNodeGroupUtil.mergeLabels("pool=gpu,disktype=ssd", "disktype=nvme,zone=a"));
        Assert.assertEquals("pool=gpu", KubernetesClusterNodeGroupUtil.mergeLabels("pool=gpu", null));
        Assert.assertNull(KubernetesClusterNodeGroupUtil.mergeLabels(null, null));
    }

    @Test
    public void testMergeTaintsNodeOverridesSameKeyAndEffect() {
        Assert.assertEquals("dedicated=gpu:NoExecute,dedicated=team-a:NoSchedule,maintenance:NoSchedule",
                KubernetesClusterNodeGroupUtil.mergeTaints("dedicated=gpu:NoSchedule,dedicated=gpu:NoExecute",
                        "dedicated=team-a:NoSchedule,maintenance:NoSchedule"));
        Assert.assertEquals("gpu=true:NoSchedule", KubernetesClusterNodeGroupUtil.mergeTaints(null, "gpu=true:NoSchedule"));
    }
}
