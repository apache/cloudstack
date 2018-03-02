package org.apache.cloudstack.agent.lb.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm;
import org.junit.Assert;
import org.junit.Test;

public class IndirectAgentLBShuffleAlgorithmTest {
    private IndirectAgentLBAlgorithm algorithm = new IndirectAgentLBShuffleAlgorithm();

    private List<String> msList = Arrays.asList("10.1.1.1", "10.1.1.2", "10.1.1.3");

    @Test
    public void testGetMSList() throws Exception {
        for (int i = 0; i < 100; i++) {
            final List<String> newList = algorithm.sort(msList, null, null);
            if (!msList.equals(newList)) {
                return;
            }
            Thread.sleep(10);
        }
        Assert.fail("Shuffle failed to produce a randomly sorted management server list");
    }

    @Test
    public void testName() throws Exception {
        Assert.assertEquals(algorithm.getName(), "shuffle");
    }

    @Test
    public void testListComparison() throws Exception {
        Assert.assertTrue(algorithm.compare(Collections.singletonList("10.1.1.1"), Collections.singletonList("10.1.1.1")));
        Assert.assertTrue(algorithm.compare(msList, Arrays.asList("10.1.1.1", "10.1.1.2", "10.1.1.3")));
        Assert.assertTrue(algorithm.compare(msList, Arrays.asList("10.1.1.3", "10.1.1.2", "10.1.1.1")));

        Assert.assertFalse(algorithm.compare(msList, Arrays.asList("10.1.1.0", "10.2.2.2")));
        Assert.assertFalse(algorithm.compare(msList, new ArrayList<String>()));
        Assert.assertFalse(algorithm.compare(msList, null));
    }
}