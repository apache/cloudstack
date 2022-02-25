package com.cloud.user;

import org.junit.Assert;
import org.junit.Test;

public class AccountTypeTest {

    @Test
    public void ordinalTestIfInCorrectOrder(){
        Assert.assertEquals(0, Account.Type.NORMAL.ordinal());
        Assert.assertEquals(1, Account.Type.ADMIN.ordinal());
        Assert.assertEquals(2, Account.Type.DOMAIN_ADMIN.ordinal());
        Assert.assertEquals(3, Account.Type.RESOURCE_DOMAIN_ADMIN.ordinal());
        Assert.assertEquals(4, Account.Type.READ_ONLY_ADMIN.ordinal());
        Assert.assertEquals(5, Account.Type.PROJECT.ordinal());
    }

    @Test
    public void getFromValueTestIfAllValuesAreReturned(){
        for (Account.Type accountType: Account.Type.values()) {
            Assert.assertEquals(Account.Type.getFromValue(accountType.ordinal()), accountType);
        }
    }

    @Test
    public void getFromValueTestInvalidValue(){
        Assert.assertEquals(Account.Type.getFromValue(null),null);
    }

    @Test
    public void stateToStringTestAllValues(){
        Assert.assertEquals(Account.State.ENABLED.toString(), "enabled");
        Assert.assertEquals(Account.State.DISABLED.toString(), "disabled");
        Assert.assertEquals(Account.State.LOCKED.toString(), "locked");
    }
}
