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

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.EnumSet;

import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VnfTemplateDetailVO;
import com.cloud.storage.VnfTemplateNicVO;
import com.cloud.storage.dao.VnfTemplateDetailsDao;
import com.cloud.storage.dao.VnfTemplateNicDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.dao.UserVmDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class UserVmJoinDaoImplTest extends GenericDaoBaseWithTagInformationBaseTest<UserVmJoinVO, UserVmResponse> {

    @InjectMocks
    private UserVmJoinDaoImpl _userVmJoinDaoImpl;

    @Mock
    private UserDao userDao;

    @Mock
    private AnnotationDao annotationDao;

    @Mock
    private AccountManager accountMgr;

    @Mock
    private UserVmDetailsDao _userVmDetailsDao;

    @Mock
    private UserStatisticsDao userStatsDao;

    @Mock
    private VnfTemplateNicDao vnfTemplateNicDao;

    @Mock
    private VnfTemplateDetailsDao vnfTemplateDetailsDao;

    private UserVmJoinVO userVm = new UserVmJoinVO();
    private UserVmResponse userVmResponse = new UserVmResponse();

    @Mock
    Account caller;

    @Mock
    UserVmJoinVO userVmMock;

    private Long vmId = 100L;

    private Long templateId = 101L;
    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = openMocks(this);
        prepareSetup();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeable.close();
        super.tearDown();
    }

    @Test
    public void testUpdateUserVmTagInfo(){
        testUpdateTagInformation(_userVmJoinDaoImpl, userVm, userVmResponse);
    }

    private void prepareNewUserVmResponseForVnfAppliance() {
        Mockito.when(userVmMock.getId()).thenReturn(vmId);
        Mockito.when(userVmMock.getTemplateId()).thenReturn(templateId);
        Mockito.when(userVmMock.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        Mockito.when(userVmMock.getTemplateFormat()).thenReturn(Storage.ImageFormat.OVA);

        Mockito.when(caller.getId()).thenReturn(2L);
        Mockito.when(accountMgr.isRootAdmin(nullable(Long.class))).thenReturn(true);

        SearchBuilder<UserStatisticsVO> searchBuilderMock = Mockito.mock(SearchBuilder.class);
        Mockito.doReturn(searchBuilderMock).when(userStatsDao).createSearchBuilder();
        UserStatisticsVO userStatisticsVOMock = Mockito.mock(UserStatisticsVO.class);
        Mockito.when(searchBuilderMock.entity()).thenReturn(userStatisticsVOMock);
        SearchCriteria<UserStatisticsVO> searchCriteriaMock = Mockito.mock(SearchCriteria.class);
        Mockito.doReturn(searchCriteriaMock).when(searchBuilderMock).create();
        Mockito.doReturn(Arrays.asList()).when(userStatsDao).search(searchCriteriaMock, null);

        VnfTemplateNicVO vnfNic1 = new VnfTemplateNicVO(templateId, 0L, "eth0", true, true, "first");
        VnfTemplateNicVO vnfNic2 = new VnfTemplateNicVO(templateId, 1L, "eth1", true, true, "second");
        Mockito.doReturn(Arrays.asList(vnfNic1, vnfNic2)).when(vnfTemplateNicDao).listByTemplateId(templateId);

        VnfTemplateDetailVO detail1 = new VnfTemplateDetailVO(templateId, "name1", "value1", true);
        VnfTemplateDetailVO detail2 = new VnfTemplateDetailVO(templateId, "name2", "value2", true);
        VnfTemplateDetailVO detail3 = new VnfTemplateDetailVO(templateId, "name3", "value3", true);
        Mockito.doReturn(Arrays.asList(detail1, detail2, detail3)).when(vnfTemplateDetailsDao).listDetails(templateId);
    }

    @Test
    public void testNewUserVmResponseForVnfAppliance() {
        prepareNewUserVmResponseForVnfAppliance();

        UserVmResponse response = _userVmJoinDaoImpl.newUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVmMock,
                EnumSet.of(ApiConstants.VMDetails.all), null, null, caller);

        Assert.assertEquals(2, response.getVnfNics().size());
        Assert.assertEquals(3, response.getVnfDetails().size());
    }

    @Test
    public void testNewUserVmResponseForVnfApplianceVnfNics() {
        prepareNewUserVmResponseForVnfAppliance();

        UserVmResponse response = _userVmJoinDaoImpl.newUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVmMock,
                EnumSet.of(ApiConstants.VMDetails.vnfnics), null, null, caller);

        Assert.assertEquals(2, response.getVnfNics().size());
        Assert.assertEquals(3, response.getVnfDetails().size());
    }
}
