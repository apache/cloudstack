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

package org.apache.cloudstack.quota.activationrule.presetvariables;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValueTest {

    @Test
    public void setIdTestAddFieldIdToCollection() {
        Value variable = new Value();
        variable.setId(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("id"));
    }

    @Test
    public void setNameTestAddFieldNameToCollection() {
        Value variable = new Value();
        variable.setName(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("name"));
    }

    @Test
    public void setHostTestAddFieldHostToCollection() {
        Value variable = new Value();
        variable.setHost(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("host"));
    }

    @Test
    public void setOsNameTestAddFieldOsNameToCollection() {
        Value variable = new Value();
        variable.setOsName(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("osName"));
    }

    @Test
    public void setAccountResourcesTestAddFieldAccountResourcesToCollection() {
        Value variable = new Value();
        variable.setAccountResources(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("accountResources"));
    }

    @Test
    public void setTagsTestAddFieldTagsToCollection() {
        Value variable = new Value();
        variable.setTags(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("tags"));
    }

    @Test
    public void setTagTestAddFieldTagToCollection() {
        Value variable = new Value();
        variable.setTag(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("tag"));
    }

    @Test
    public void setSizeTestAddFieldSizeToCollection() {
        Value variable = new Value();
        variable.setSize(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("size"));
    }

    @Test
    public void setProvisioningTypeTestAddFieldProvisioningTypeToCollection() {
        Value variable = new Value();
        variable.setProvisioningType(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("provisioningType"));
    }

    @Test
    public void setSnapshotTypeTestAddFieldSnapshotTypeToCollection() {
        Value variable = new Value();
        variable.setSnapshotType(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("snapshotType"));
    }

    @Test
    public void setVmSnapshotTypeTestAddFieldVmSnapshotTypeToCollection() {
        Value variable = new Value();
        variable.setVmSnapshotType(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("vmSnapshotType"));
    }

    @Test
    public void setComputeOfferingTestAddFieldComputeOfferingToCollection() {
        Value variable = new Value();
        variable.setComputeOffering(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("computeOffering"));
    }

    @Test
    public void setTemplateTestAddFieldTemplateToCollection() {
        Value variable = new Value();
        variable.setTemplate(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("template"));
    }

    @Test
    public void setDiskOfferingTestAddFieldDiskOfferingToCollection() {
        Value variable = new Value();
        variable.setDiskOffering(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("diskOffering"));
    }

    @Test
    public void setStorageTestAddFieldStorageToCollection() {
        Value variable = new Value();
        variable.setStorage(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("storage"));
    }

    @Test
    public void setComputingResourcesTestAddFieldComputingResourcesToCollection() {
        Value variable = new Value();
        variable.setComputingResources(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("computingResources"));
    }

    @Test
    public void setVirtualSizeTestAddFieldVirtualSizeToCollection() {
        Value variable = new Value();
        variable.setVirtualSize(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("virtualSize"));
    }

    @Test
    public void setBackupOfferingTestAddFieldBackupOfferingToCollection() {
        Value variable = new Value();
        variable.setBackupOffering(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("backupOffering"));
    }

    @Test
    public void setHypervisorTypeTestAddFieldHypervisorTypeToCollection() {
        Value variable = new Value();
        variable.setHypervisorType(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("hypervisorType"));
    }

    @Test
    public void setVolumeFormatTestAddFieldVolumeFormatToCollection() {
        Value variable = new Value();
        variable.setVolumeFormat(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("volumeFormat"));
    }

    @Test
    public void setStateTestAddFieldStateToCollection() {
        Value variable = new Value();
        variable.setState(null);
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("state"));
    }

}
