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

import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Assert;
import org.junit.Test;

public class DiskOfferingDaoImplTest {

    private final DiskOfferingDaoImpl dao = new DiskOfferingDaoImpl();

    @Test(expected = CloudRuntimeException.class)
    public void testGetClosestDiskSizeInGBNegativeSize() {
        long size = -4 * DiskOfferingDaoImpl.GB_UNIT_BYTES;
        dao.getClosestDiskSizeInGB(size);
    }

    @Test
    public void testGetClosestDiskSizeInGBSizeGB() {
        int gbUnits = 5;
        long size = gbUnits * DiskOfferingDaoImpl.GB_UNIT_BYTES;
        long sizeInGB = dao.getClosestDiskSizeInGB(size);
        Assert.assertEquals(gbUnits, sizeInGB);
    }

    @Test
    public void testGetClosestDiskSizeInGBSizeGBRest() {
        int gbUnits = 5;
        long size = gbUnits * DiskOfferingDaoImpl.GB_UNIT_BYTES + 12345;
        long sizeInGB = dao.getClosestDiskSizeInGB(size);
        Assert.assertEquals(gbUnits + 1, sizeInGB);
    }

    @Test
    public void testGetClosestDiskSizeInGBSizeLessOneGB() {
        int gbUnits = 1;
        long size = gbUnits * DiskOfferingDaoImpl.GB_UNIT_BYTES - 12345;
        long sizeInGB = dao.getClosestDiskSizeInGB(size);
        Assert.assertEquals(gbUnits, sizeInGB);
    }
}
