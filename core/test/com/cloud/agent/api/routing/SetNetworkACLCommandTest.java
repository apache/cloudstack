package com.cloud.agent.api.routing;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.cloud.agent.api.to.NetworkACLTO;
import com.google.common.collect.Lists;

public class SetNetworkACLCommandTest {

    @Test
    public void testNetworkAclRuleOrdering(){

        //given
        List<NetworkACLTO> aclList = Lists.newArrayList();

        aclList.add(new NetworkACLTO(3, null, null, null, null, false, false, null, null, null, null, false, 3));
        aclList.add(new NetworkACLTO(1, null, null, null, null, false, false, null, null, null, null, false, 1));
        aclList.add(new NetworkACLTO(2, null, null, null, null, false, false, null, null, null, null, false, 2));

        SetNetworkACLCommand cmd = new SetNetworkACLCommand(aclList, null);

        //when
        cmd.orderNetworkAclRulesByRuleNumber(aclList);

        //then
        for(int i=0; i< aclList.size();i++){
            assertEquals(aclList.get(i).getNumber(), i+1);
        }
    }
}
