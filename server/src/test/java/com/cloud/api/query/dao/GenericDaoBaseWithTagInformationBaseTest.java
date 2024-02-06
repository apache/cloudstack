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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.BaseViewWithTagInformationVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.server.ResourceTag.ResourceObjectType;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.junit.After;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

public abstract class GenericDaoBaseWithTagInformationBaseTest<T extends BaseViewWithTagInformationVO,
                                                                Z extends BaseResponseWithTagInformation> {

    private final static long TAG_ID = 1l;
    private final static String TAG_KEY = "type";
    private final static String TAG_VALUE = "supported";
    private final static String TAG_UUID = "aaaa-aaaa-aaaa-aaaa";
    private final static ResourceObjectType TAG_RESOURCE_TYPE = ResourceObjectType.Template;
    private final static String TAG_RESOURCE_TYPE_STR = "Template";
    private final static String TAG_RESOURCE_UUID = "aaaa-aaaa-aaaa-aaaa";
    private final static long TAG_DOMAIN_ID = 123l;
    private final static String TAG_DOMAIN_ID_STR = "123";
    private final static String TAG_DOMAIN_NAME = "aaaa-aaaa-aaaa-aaaa";
    private final static String TAG_CUSTOMER = "aaaa-aaaa-aaaa-aaaa";
    private final static String TAG_ACCOUNT_NAME = "admin";

    private final static String RESPONSE_OBJECT_NAME = "tag";
    private MockedStatic<ApiDBUtils> apiDBUtilsMocked;

    public void prepareSetup(){
        apiDBUtilsMocked = Mockito.mockStatic(ApiDBUtils.class);
        apiDBUtilsMocked.when(ReflectionTestUtils.invokeMethod(ApiDBUtils.class, "newResourceTagResponse", Mockito.any(ResourceTagJoinVO.class), Mockito.anyBoolean())).thenReturn(getResourceTagResponse());
    }

    @After
    public void tearDown() throws Exception {
        if (apiDBUtilsMocked != null) {
            apiDBUtilsMocked.close();
        }
    }

    private ResourceTagResponse getResourceTagResponse(){
        ResourceTagResponse tagResponse = new ResourceTagResponse();
        tagResponse.setKey(TAG_KEY);
        tagResponse.setValue(TAG_VALUE);
        tagResponse.setObjectName(RESPONSE_OBJECT_NAME);
        tagResponse.setResourceType(TAG_RESOURCE_TYPE_STR);
        tagResponse.setResourceId(TAG_RESOURCE_UUID);
        tagResponse.setDomainId(TAG_DOMAIN_ID_STR);
        tagResponse.setDomainName(TAG_DOMAIN_NAME);
        tagResponse.setCustomer(TAG_CUSTOMER);
        tagResponse.setAccountName(TAG_ACCOUNT_NAME);
        return tagResponse;
    }

    private void prepareBaseView(long tagId, T baseView){
        baseView.setTagId(tagId);
        baseView.setTagKey(TAG_KEY);
        baseView.setTagValue(TAG_VALUE);
        baseView.setTagUuid(TAG_UUID);
        baseView.setTagResourceType(TAG_RESOURCE_TYPE);
        baseView.setTagAccountName(TAG_ACCOUNT_NAME);
        baseView.setTagDomainId(TAG_DOMAIN_ID);
        baseView.setTagDomainName(TAG_DOMAIN_NAME);
        baseView.setTagCustomer(TAG_CUSTOMER);
        baseView.setTagAccountName(TAG_ACCOUNT_NAME);
    }

    public void testUpdateTagInformation(GenericDaoBaseWithTagInformation<T, Z> dao, T baseView, Z baseResponse){
        prepareBaseView(TAG_ID, baseView);
        dao.addTagInformation(baseView, baseResponse);
        ResourceTagResponse[] responseArray = new ResourceTagResponse[baseResponse.getTags().size()];
        baseResponse.getTags().toArray(responseArray);
        assertEquals(1, responseArray.length);
        assertEquals(TAG_KEY, responseArray[0].getKey());
        assertEquals(TAG_VALUE, responseArray[0].getValue());
        assertEquals(RESPONSE_OBJECT_NAME, responseArray[0].getObjectName());
    }

}
