package org.apache.cloudstack.hypervisor.xenserver;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class ExtraConfigurationUtilityTest {

    @Test
    public void prepareKeyValuePairTest() {
        // Map params
        verifyKeyValuePairForConfigParam("platform:exp-nested-hvm=true", "platform:exp-nested-hvm", "true");
        verifyKeyValuePairForConfigParam("other_config:my_key=my_value", "other_config:my_key", "my_value");
        verifyKeyValuePairForConfigParam("test-config:test-key=test-value", "test_config:test-key", "test-value");

        // Params
        verifyKeyValuePairForConfigParam("is_a_template=true", "is_a_template", "true");
        verifyKeyValuePairForConfigParam("is-a-template=true", "is_a_template", "true");
        verifyKeyValuePairForConfigParam("memory_dynamic_min=536870912", "memory_dynamic_min", "536870912");
        verifyKeyValuePairForConfigParam("VCPUs_at_startup=2", "VCPUs_at_startup", "2");
        verifyKeyValuePairForConfigParam("VCPUs-max=4", "VCPUs_max", "4");
    }

    private void verifyKeyValuePairForConfigParam(String cfg, String expectedKey, String expectedValue) {
        Pair<String, String> keyValuePair = ExtraConfigurationUtility.prepareKeyValuePair(cfg);
        Assert.assertEquals(expectedKey, keyValuePair.first());
        Assert.assertEquals(expectedValue, keyValuePair.second());
    }
}
