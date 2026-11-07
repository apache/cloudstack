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
package com.cloud.service.dao;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.service.ServiceOfferingCategoryVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class ServiceOfferingDaoImplTest {
    @Mock
    private ServiceOfferingCategoryDao categoryDao;

    @InjectMocks
    private ServiceOfferingDaoImpl offeringDao = new ServiceOfferingDaoImpl();

    private ServiceOfferingVO newOffering() {
        return new ServiceOfferingVO("test", 1, 512, 500, null, null, false, "test", false, null, false);
    }

    @Test
    public void assignsFlaggedDefaultCategory() {
        ServiceOfferingCategoryVO category = new ServiceOfferingCategoryVO("Renamed");
        category.setId(42L);
        category.setDefault(true);
        when(categoryDao.findDefault()).thenReturn(category);
        ServiceOfferingVO offering = newOffering();

        offeringDao.assignDefaultCategory(offering);

        Assert.assertEquals(Long.valueOf(42L), offering.getCategoryId());
    }

    @Test
    public void preservesExplicitCategory() {
        ServiceOfferingVO offering = newOffering();
        offering.setCategoryId(7L);

        offeringDao.assignDefaultCategory(offering);

        Assert.assertEquals(Long.valueOf(7L), offering.getCategoryId());
        verify(categoryDao, never()).findDefault();
    }

    @Test
    public void rejectsMissingDefaultCategory() {
        when(categoryDao.findDefault()).thenReturn(null);

        Assert.assertThrows(CloudRuntimeException.class, () -> offeringDao.assignDefaultCategory(newOffering()));
    }
}
