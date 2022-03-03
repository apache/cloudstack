package org.apache.cloudstack.api;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.domain.Domain;
import com.cloud.network.Network;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.vm.VirtualMachine;

public class ApiCommandResourceTypeTest {

    @Test
    public void testGetAssociatedClass() {
        Assert.assertNull(ApiCommandResourceType.None.getAssociatedClass());
        Assert.assertEquals(Account.class, ApiCommandResourceType.Account.getAssociatedClass());
        Assert.assertEquals(Domain.class, ApiCommandResourceType.Domain.getAssociatedClass());
        Assert.assertEquals(VirtualMachineTemplate.class, ApiCommandResourceType.Template.getAssociatedClass());
        Assert.assertEquals(VirtualMachine.class, ApiCommandResourceType.VirtualMachine.getAssociatedClass());
        Assert.assertEquals(Network.class, ApiCommandResourceType.Network.getAssociatedClass());
        Assert.assertEquals(Volume.class, ApiCommandResourceType.Volume.getAssociatedClass());
    }

    @Test
    public void testValuesFromAssociatedClass() {
        List<ApiCommandResourceType> types = ApiCommandResourceType.valuesFromAssociatedClass(Account.class);
        Assert.assertNotNull(types);
        Assert.assertTrue(types.size() > 0);
        Assert.assertEquals(types.get(0), ApiCommandResourceType.Account);

        types = ApiCommandResourceType.valuesFromAssociatedClass(ApiCommandResourceTypeTest.class);
        Assert.assertNotNull(types);
        Assert.assertEquals(0, types.size());
    }

    @Test
    public void testValueFromAssociatedClass() {
        Assert.assertEquals(ApiCommandResourceType.valueFromAssociatedClass(VirtualMachine.class), ApiCommandResourceType.VirtualMachine);
        Assert.assertNull(ApiCommandResourceType.valueFromAssociatedClass(ApiCommandResourceTypeTest.class));
    }

    @Test
    public void testFromString() {
        Assert.assertNull(ApiCommandResourceType.fromString(null));
        Assert.assertNull(ApiCommandResourceType.fromString(""));
        Assert.assertNull(ApiCommandResourceType.fromString("Something"));
        Assert.assertEquals(ApiCommandResourceType.Account, ApiCommandResourceType.fromString("Account"));
        Assert.assertEquals(ApiCommandResourceType.Domain, ApiCommandResourceType.fromString("Domain"));
    }
}