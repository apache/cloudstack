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

package org.apache.cloudstack.storage.fileshare.provider;

import org.apache.cloudstack.storage.fileshare.FileShareLifeCycle;
import org.apache.cloudstack.storage.fileshare.FileShareProvider;
import org.apache.cloudstack.storage.fileshare.lifecycle.StorageFsVmFileShareLifeCycle;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentContext;

public class StorageFsVmFileShareProvider extends AdapterBase implements FileShareProvider {
    protected String name = String.valueOf(FileShareProviderType.STORAGEFSVM);
    protected FileShareLifeCycle lifecycle;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void configure() {
        lifecycle = ComponentContext.inject(StorageFsVmFileShareLifeCycle.class);
    }

    @Override
    public FileShareLifeCycle getFileShareLifeCycle() {
        return lifecycle;
    }
}
