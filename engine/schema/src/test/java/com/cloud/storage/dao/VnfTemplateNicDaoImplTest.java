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
package com.cloud.storage.dao;

import com.cloud.storage.VnfTemplateNicVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class VnfTemplateNicDaoImplTest {

    @Mock
    SearchBuilder<VnfTemplateNicVO> searchBuilderVnfTemplateNicVOMock;

    @Mock
    SearchCriteria<VnfTemplateNicVO> searchCriteriaVnfTemplateNicVOMock;

    @Mock
    List<VnfTemplateNicVO> listVnfTemplateNicVOMock;

    @Mock
    private TransactionLegacy transactionMock;

    @Spy
    VnfTemplateNicDaoImpl vnfTemplateNicDaoImplSpy;

    @Before
    public void setUp() {
        vnfTemplateNicDaoImplSpy.TemplateSearch = searchBuilderVnfTemplateNicVOMock;
        Mockito.doReturn(searchCriteriaVnfTemplateNicVOMock).when(searchBuilderVnfTemplateNicVOMock).create();
        Mockito.doNothing().when(searchCriteriaVnfTemplateNicVOMock).setParameters(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testListByTemplateId() {
        Mockito.doReturn(listVnfTemplateNicVOMock).when(vnfTemplateNicDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));
        long templateId = 100L;

        List<VnfTemplateNicVO> result = vnfTemplateNicDaoImplSpy.listByTemplateId(templateId);

        Assert.assertEquals(listVnfTemplateNicVOMock, result);
        Mockito.verify(searchCriteriaVnfTemplateNicVOMock).setParameters("templateId", templateId);
    }

    @Test
    public void testDeleteByTemplateId() {
        Mockito.doReturn(0).when(vnfTemplateNicDaoImplSpy).remove(searchCriteriaVnfTemplateNicVOMock);
        long templateId = 100L;

        try (MockedStatic<TransactionLegacy> ignore = Mockito.mockStatic(TransactionLegacy.class)) {
            Mockito.when(TransactionLegacy.currentTxn()).thenReturn(transactionMock);
            Mockito.doNothing().when(transactionMock).start();
            Mockito.doReturn(true).when(transactionMock).commit();

            vnfTemplateNicDaoImplSpy.deleteByTemplateId(templateId);

            Mockito.verify(transactionMock, Mockito.times(1)).start();
            Mockito.verify(vnfTemplateNicDaoImplSpy, Mockito.times(1)).remove(searchCriteriaVnfTemplateNicVOMock);
            Mockito.verify(transactionMock, Mockito.times(1)).commit();
        }
    }
}
