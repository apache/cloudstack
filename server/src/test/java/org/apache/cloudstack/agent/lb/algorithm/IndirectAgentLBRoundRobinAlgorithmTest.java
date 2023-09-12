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
package org.apache.cloudstack.agent.lb.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm;
import org.junit.Assert;
import org.junit.Test;

public class IndirectAgentLBRoundRobinAlgorithmTest {
    private IndirectAgentLBAlgorithm algorithm = new IndirectAgentLBRoundRobinAlgorithm();

    private List<String> msList = Arrays.asList("10.1.1.1", "10.1.1.2", "10.1.1.3");
    private List<Long> hostList = new ArrayList<>(Arrays.asList(1L, 5L, 10L, 20L, 50L, 60L, 70L, 80L));

    @Test
    public void testGetMSListForNewHost() throws Exception {
        List<String> startList = algorithm.sort(msList, hostList, null);
        Assert.assertNotEquals(msList, startList);
        Assert.assertFalse(algorithm.compare(msList, startList));

        hostList.add(100L);
        List<String> nextList = algorithm.sort(msList, hostList, null);
        List<String> expectedList = startList.subList(1, startList.size());
        expectedList.addAll(startList.subList(0, 1));
        Assert.assertEquals(nextList, expectedList);
    }

    @Test
    public void testGetMSListForExistingHost() throws Exception {
        List<String> startList = new ArrayList<>(msList);
        for (Long hostId : hostList.subList(1, hostList.size())) {
            List<String> nextList = new ArrayList<>(startList.subList(1, msList.size()));
            nextList.addAll(startList.subList(0, 1));
            List<String> expectedList = algorithm.sort(msList, hostList, hostId);
            Assert.assertEquals(expectedList, nextList);
            startList = nextList;
        }
    }

    @Test
    public void testName() throws Exception {
        Assert.assertEquals(algorithm.getName(), "roundrobin");
    }

    @Test
    public void testListComparison() throws Exception {
        Assert.assertTrue(algorithm.compare(Collections.singletonList("10.1.1.1"), Collections.singletonList("10.1.1.1")));
        Assert.assertTrue(algorithm.compare(Arrays.asList("10.1.1.2", "10.1.1.1"), Arrays.asList("10.1.1.2", "10.1.1.1")));
        Assert.assertTrue(algorithm.compare(msList, Arrays.asList("10.1.1.1", "10.1.1.2", "10.1.1.3")));

        Assert.assertFalse(algorithm.compare(msList, Arrays.asList("10.1.1.3", "10.1.1.2", "10.1.1.1")));
        Assert.assertFalse(algorithm.compare(msList, Arrays.asList("10.1.1.0", "10.2.2.2")));
        Assert.assertFalse(algorithm.compare(msList, new ArrayList<String>()));
        Assert.assertFalse(algorithm.compare(msList, null));
    }

}
