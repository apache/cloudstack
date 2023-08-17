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
package com.cloud.api.query.dao;

import java.util.Date;
import java.util.Map;

import org.apache.cloudstack.api.response.TemplateResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ApiDBUtils.class)
public class TemplateJoinDaoImplTest extends GenericDaoBaseWithTagInformationBaseTest<TemplateJoinVO, TemplateResponse> {

    @InjectMocks
    private TemplateJoinDaoImpl _templateJoinDaoImpl;

    private TemplateJoinVO template = new TemplateJoinVO();
    private TemplateResponse templateResponse = new TemplateResponse();

    //TemplateJoinVO fields
    private String uuid = "1234567890abc";
    private String name = TemplateManager.XS_TOOLS_ISO;
    private String displayText = "xen-pv-drv-iso";
    private boolean publicTemplate = true;
    private Date created = new Date();
    private Storage.ImageFormat format = Storage.ImageFormat.ISO;
    private String guestOSUuid = "987654321cba";
    private String guestOSName = "CentOS 4.5 (32-bit)";
    private boolean bootable = true;
    private Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.XenServer;
    private boolean dynamicallyScalable = true;
    private Account.Type accountType = Account.Type.NORMAL;
    private String accountName = "system";
    private String domainUuid = "abcde1234567890";
    private String domainName = "ROOT";
    private String detailName = "detail_name1";
    private String detailValue = "detail_val";

    @Before
    public void setup() {
        prepareSetup();
        populateTemplateJoinVO();
    }

    @Test
    public void testUpdateTemplateTagInfo(){
        testUpdateTagInformation(_templateJoinDaoImpl, template, templateResponse);
    }

    @Test
    public void testNewUpdateResponse() {
        final TemplateResponse response = _templateJoinDaoImpl.newUpdateResponse(template);
        Assert.assertEquals(uuid, response.getId());
        Assert.assertEquals(name, ReflectionTestUtils.getField(response, "name"));
        Assert.assertEquals(displayText, ReflectionTestUtils.getField(response, "displayText"));
        Assert.assertTrue((Boolean) ReflectionTestUtils.getField(response, "isPublic"));
        Assert.assertEquals(created, ReflectionTestUtils.getField(response, "created"));
        Assert.assertEquals(format, ReflectionTestUtils.getField(response, "format"));
        Assert.assertEquals(guestOSUuid, ReflectionTestUtils.getField(response, "osTypeId"));
        Assert.assertEquals(guestOSName, ReflectionTestUtils.getField(response, "osTypeName"));
        Assert.assertTrue((Boolean) ReflectionTestUtils.getField(response, "bootable"));
        Assert.assertEquals(hypervisorType, Hypervisor.HypervisorType.getType(ReflectionTestUtils.getField(response, "hypervisor").toString()));
        Assert.assertTrue((Boolean) ReflectionTestUtils.getField(response, "isDynamicallyScalable"));
        Assert.assertEquals(accountName, ReflectionTestUtils.getField(response, "account"));
        Assert.assertEquals(domainUuid, ReflectionTestUtils.getField(response, "domainId"));
        Assert.assertEquals(domainName, ReflectionTestUtils.getField(response, "domainName"));
        Assert.assertTrue(((Map)ReflectionTestUtils.getField(response, "details")).containsKey(detailName));
        Assert.assertEquals(detailValue, ((Map)ReflectionTestUtils.getField(response, "details")).get(detailName));
    }

    private void populateTemplateJoinVO() {
        ReflectionTestUtils.setField(template, "uuid", uuid);
        ReflectionTestUtils.setField(template, "name", name);
        ReflectionTestUtils.setField(template, "displayText", displayText);
        ReflectionTestUtils.setField(template, "publicTemplate", publicTemplate);
        ReflectionTestUtils.setField(template, "created", created);
        ReflectionTestUtils.setField(template, "format", format);
        ReflectionTestUtils.setField(template, "guestOSUuid", guestOSUuid);
        ReflectionTestUtils.setField(template, "guestOSName", guestOSName);
        ReflectionTestUtils.setField(template, "bootable", bootable);
        ReflectionTestUtils.setField(template, "hypervisorType", hypervisorType);
        ReflectionTestUtils.setField(template, "dynamicallyScalable", dynamicallyScalable);
        ReflectionTestUtils.setField(template, "accountType", accountType);
        ReflectionTestUtils.setField(template, "accountName", accountName);
        ReflectionTestUtils.setField(template, "domainUuid", domainUuid);
        ReflectionTestUtils.setField(template, "domainName", domainName);
        ReflectionTestUtils.setField(template, "detailName", detailName);
        ReflectionTestUtils.setField(template, "detailValue", detailValue);
    }
}
