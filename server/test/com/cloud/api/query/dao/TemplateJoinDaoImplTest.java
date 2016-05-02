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

import org.apache.cloudstack.api.response.TemplateResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.TemplateJoinVO;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ApiDBUtils.class)
public class TemplateJoinDaoImplTest extends GenericDaoBaseWithTagInformationBaseTest<TemplateJoinVO, TemplateResponse> {

    @InjectMocks
    private TemplateJoinDaoImpl _templateJoinDaoImpl;

    private TemplateJoinVO template = new TemplateJoinVO();
    private TemplateResponse templateResponse = new TemplateResponse();

    @Before
    public void setup() {
        prepareSetup();
    }

    @Test
    public void testUpdateTemplateTagInfo(){
        testUpdateTagInformation(_templateJoinDaoImpl, template, templateResponse);
    }

}