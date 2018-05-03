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

package org.apache.cloudstack.backuprecovery.element;

import org.apache.cloudstack.backuprecovery.resource.DummyBackupRecoveryResource;
import org.apache.cloudstack.framework.backuprecovery.element.BackupRecoveryElement;
import org.apache.cloudstack.framework.backuprecovery.helper.BackupRecoveryHelper;
import org.apache.cloudstack.framework.backuprecovery.resource.BackupRecoveryResource;

import javax.naming.ConfigurationException;
import java.util.Map;

public class DummyBackupRecoveryElement extends BackupRecoveryElement {
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        BackupRecoveryHelper.addProvider("Dummy", this);
        return true;
    }

    @Override
    public BackupRecoveryResource createNewResource() {
        return new DummyBackupRecoveryResource();
    }
}
