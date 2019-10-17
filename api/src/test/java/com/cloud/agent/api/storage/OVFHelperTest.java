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
package com.cloud.agent.api.storage;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public class OVFHelperTest {

    private String ovfFileProductSection =
        "<ProductSection>" +
            "<Info>VM Arguments</Info>" +
            "<Property ovf:key=\"va-ssh-public-key\" ovf:type=\"string\" ovf:userConfigurable=\"true\" ovf:value=\"\">" +
                "<Label>Set the SSH public key allowed to access the appliance</Label>" +
                "<Description>This will enable the SSHD service and configure the specified public key</Description>" +
            "</Property>" +
            "<Property ovf:key=\"user-data\" ovf:type=\"string\" ovf:userConfigurable=\"true\" ovf:value=\"\">" +
                "<Label>User data to be made available inside the instance</Label>" +
                "<Description>This allows to pass any text to the appliance. The value should be encoded in base64</Description>" +
            "</Property>" +
        "</ProductSection>";

    private OVFHelper ovfHelper = new OVFHelper();

    @Test
    public void testGetOVFPropertiesValidOVF() throws IOException, SAXException, ParserConfigurationException {
        List<OVFPropertyTO> props = ovfHelper.getOVFPropertiesXmlString(ovfFileProductSection);
        Assert.assertEquals(2, props.size());
    }

    @Test(expected = SAXParseException.class)
    public void testGetOVFPropertiesInvalidOVF() throws IOException, SAXException, ParserConfigurationException {
        ovfHelper.getOVFPropertiesXmlString(ovfFileProductSection + "xxxxxxxxxxxxxxxxx");
    }
}
