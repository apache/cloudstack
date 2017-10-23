package org.apache.cloudstack.agent.lb.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm;
import org.junit.Assert;
import org.junit.Test;

public class IndirectAgentLBStaticAlgorithmTest {
    private IndirectAgentLBAlgorithm algorithm = new IndirectAgentLBStaticAlgorithm();

    private List<String> msList = Arrays.asList("10.1.1.1", "10.1.1.2", "10.1.1.3");

    @Test
    public void testGetMSList() throws Exception {
        Assert.assertEquals(msList, algorithm.sort(msList, null, null));
    }

    @Test
    public void testName() throws Exception {
        Assert.assertEquals(algorithm.getName(), "static");
    }

    @Test
    public void testListComparison() throws Exception {
        Assert.assertTrue(algorithm.compare(msList, Arrays.asList("10.1.1.1", "10.1.1.2", "10.1.1.3")));
        Assert.assertFalse(algorithm.compare(msList, Arrays.asList("10.1.1.0", "10.2.2.2")));
        Assert.assertFalse(algorithm.compare(msList, new ArrayList<String>()));
        Assert.assertFalse(algorithm.compare(msList, null));
    }
}