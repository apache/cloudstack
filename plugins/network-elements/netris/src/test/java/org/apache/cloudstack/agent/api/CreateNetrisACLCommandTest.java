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
package org.apache.cloudstack.agent.api;

import org.apache.cloudstack.resource.NetrisPortGroup;
import org.junit.Assert;
import org.junit.Test;

public class CreateNetrisACLCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String NAME = "test-network";
    private static final Long ID = 4L;
    private static final String VPC_NAME = "test-vpc";
    private static final Long VPC_ID = 5L;
    private static final boolean IS_VPC = true;
    private static final String ACTION = "allow";
    private static final String SOURCE_PREFIX = "10.0.0.0/24";
    private static final String DEST_PREFIX = "10.0.1.0/24";
    private static final Integer DEST_PORT_START = 80;
    private static final Integer DEST_PORT_END = 80;
    private static final String PROTOCOL = "tcp";
    private static final Integer ICMP_TYPE = 8;
    private static final String NETRIS_ACL_NAME = "test-acl";
    private static final String REASON = "test reason";
    private static final String PORTS = "80,443";

    @Test
    public void testConstructorAndGetters() {
        // Act
        CreateNetrisACLCommand command = new CreateNetrisACLCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, VPC_NAME, VPC_ID, IS_VPC, ACTION,
                SOURCE_PREFIX, DEST_PREFIX, DEST_PORT_START, DEST_PORT_END, PROTOCOL);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(VPC_NAME, command.getVpcName());
        Assert.assertEquals(VPC_ID, command.getVpcId());
        Assert.assertEquals(ACTION, command.getAction());
        Assert.assertEquals(SOURCE_PREFIX, command.getSourcePrefix());
        Assert.assertEquals(DEST_PREFIX, command.getDestPrefix());
        Assert.assertEquals(DEST_PORT_START, command.getDestPortStart());
        Assert.assertEquals(DEST_PORT_END, command.getDestPortEnd());
        Assert.assertEquals(PROTOCOL, command.getProtocol());
    }

    @Test
    public void testConstructorWithNullValues() {
        // Act
        CreateNetrisACLCommand command = new CreateNetrisACLCommand(
                ZONE_ID, null, null, NAME, null, VPC_NAME, null, IS_VPC, ACTION,
                SOURCE_PREFIX, DEST_PREFIX, null, null, PROTOCOL);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertNull(command.getAccountId());
        Assert.assertNull(command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertNull(command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(VPC_NAME, command.getVpcName());
        Assert.assertNull(command.getVpcId());
        Assert.assertEquals(ACTION, command.getAction());
        Assert.assertEquals(SOURCE_PREFIX, command.getSourcePrefix());
        Assert.assertEquals(DEST_PREFIX, command.getDestPrefix());
        Assert.assertNull(command.getDestPortStart());
        Assert.assertNull(command.getDestPortEnd());
        Assert.assertEquals(PROTOCOL, command.getProtocol());
    }

    @Test
    public void testSetters() {
        // Arrange
        CreateNetrisACLCommand command = new CreateNetrisACLCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, VPC_NAME, VPC_ID, IS_VPC, ACTION,
                SOURCE_PREFIX, DEST_PREFIX, DEST_PORT_START, DEST_PORT_END, PROTOCOL);

        NetrisPortGroup portGroup = new NetrisPortGroup(PORTS);
        String newVpcName = "new-vpc";
        Long newVpcId = 6L;

        // Act
        command.setVpcName(newVpcName);
        command.setVpcId(newVpcId);
        command.setPortGroup(portGroup);
        command.setIcmpType(ICMP_TYPE);
        command.setNetrisAclName(NETRIS_ACL_NAME);
        command.setReason(REASON);

        // Assert
        Assert.assertEquals(newVpcName, command.getVpcName());
        Assert.assertEquals(newVpcId, command.getVpcId());
        Assert.assertEquals(portGroup, command.getPortGroup());
        Assert.assertEquals(ICMP_TYPE, command.getIcmpType());
        Assert.assertEquals(NETRIS_ACL_NAME, command.getNetrisAclName());
        Assert.assertEquals(REASON, command.getReason());
        Assert.assertEquals(PORTS, command.getPortGroup().getPorts());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        CreateNetrisACLCommand command = new CreateNetrisACLCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", ID, "", VPC_ID, IS_VPC, "",
                "", "", DEST_PORT_START, DEST_PORT_END, "");

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals("", command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals("", command.getVpcName());
        Assert.assertEquals(VPC_ID, command.getVpcId());
        Assert.assertEquals("", command.getAction());
        Assert.assertEquals("", command.getSourcePrefix());
        Assert.assertEquals("", command.getDestPrefix());
        Assert.assertEquals(DEST_PORT_START, command.getDestPortStart());
        Assert.assertEquals(DEST_PORT_END, command.getDestPortEnd());
        Assert.assertEquals("", command.getProtocol());
    }
}
