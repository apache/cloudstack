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
package org.apache.cloudstack.storage.image.db;

import com.cloud.storage.VMTemplateStorageResourceAssoc;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TemplateDataStoreDaoImplTest {

    private final TemplateDataStoreDaoImpl dao = new TemplateDataStoreDaoImpl();

    private static final long templateId = 1;
    private static final long templateSize = 9999;

    @Test
    public void testGetValidGreaterSizeBypassedTemplateEmptyList() {
        TemplateDataStoreVO ref = dao.getValidGreaterSizeBypassedTemplate(null, templateId);
        Assert.assertNull(ref);
    }

    private TemplateDataStoreVO createNewTestReferenceRecord() {
        TemplateDataStoreVO ref = new TemplateDataStoreVO();
        ref.setTemplateId(templateId);
        ref.setState(ObjectInDataStoreStateMachine.State.Ready);
        ref.setDownloadState(VMTemplateStorageResourceAssoc.Status.BYPASSED);
        ref.setSize(templateSize);
        return ref;
    }

    @Test
    public void testGetValidGreaterSizeBypassedTemplateMultipleRecords() {
        TemplateDataStoreVO ref1 = createNewTestReferenceRecord();
        TemplateDataStoreVO ref2 = createNewTestReferenceRecord();
        TemplateDataStoreVO ref3 = createNewTestReferenceRecord();
        ref1.setSize(1111L);
        ref3.setSize(2L);
        TemplateDataStoreVO ref = dao.getValidGreaterSizeBypassedTemplate(Arrays.asList(ref1, ref2, ref3), templateId);
        Assert.assertNotNull(ref);
        Assert.assertEquals(templateSize, ref.getSize());
    }

    @Test
    public void testGetValidGreaterSizeBypassedTemplateOneRecord() {
        TemplateDataStoreVO ref1 = createNewTestReferenceRecord();
        TemplateDataStoreVO ref = dao.getValidGreaterSizeBypassedTemplate(List.of(ref1), templateId);
        Assert.assertNotNull(ref);
        Assert.assertEquals(templateSize, ref.getSize());
    }

    @Test
    public void testGetValidGreaterSizeBypassedTemplateOneRecordInvalidState() {
        TemplateDataStoreVO ref1 = createNewTestReferenceRecord();
        ref1.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        TemplateDataStoreVO ref = dao.getValidGreaterSizeBypassedTemplate(List.of(ref1), templateId);
        Assert.assertNull(ref);
    }
}
