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
package org.apache.cloudstack.storage;

import java.util.HashMap;

import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource;
import org.apache.cloudstack.storage.template.DownloadManagerImpl;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.script.Script;

@Component
public class MockLocalNfsSecondaryStorageResource extends NfsSecondaryStorageResource {

    public MockLocalNfsSecondaryStorageResource() {
        _dlMgr = new DownloadManagerImpl();
        _storage = new JavaStorageLayer();
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(StorageLayer.InstanceConfigKey, _storage);
        try {
            _dlMgr.configure("downloadMgr", params);
        } catch (ConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        createTemplateFromSnapshotXenScript = Script.findScript(getDefaultScriptsDir(), "create_privatetemplate_from_snapshot_xen.sh");

    }

    @Override
    public String getRootDir(String secUrl) {
        return "/mnt";
    }

    @Override
    public Answer executeRequest(Command cmd) {
        // return Answer.createUnsupportedCommandAnswer(cmd);
        return super.executeRequest(cmd);
    }

}
