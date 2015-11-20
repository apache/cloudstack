/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.Map;
import java.util.Set;

public interface DataStoreProvider {
    // constants for provider names
    String NFS_IMAGE = "NFS";
    String S3_IMAGE = "S3";
    String SWIFT_IMAGE = "Swift";
    String SAMPLE_IMAGE = "Sample";
    String SMB = "NFS";
    String DEFAULT_PRIMARY = "DefaultPrimary";

    enum DataStoreProviderType {
        PRIMARY, IMAGE, ImageCache
    }

    DataStoreLifeCycle getDataStoreLifeCycle();

    DataStoreDriver getDataStoreDriver();

    HypervisorHostListener getHostListener();

    String getName();

    boolean configure(Map<String, Object> params);

    Set<DataStoreProviderType> getTypes();
}
