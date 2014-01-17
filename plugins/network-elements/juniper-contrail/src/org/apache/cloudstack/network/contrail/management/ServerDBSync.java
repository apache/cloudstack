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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;

import com.cloud.domain.DomainVO;
import com.cloud.projects.ProjectVO;

public interface ServerDBSync {

    public final static short SYNC_STATE_IN_SYNC = 0;
    public final static short SYNC_STATE_OUT_OF_SYNC = 1;
    public final static short SYNC_STATE_UNKNOWN = -1;

    /*
     * API for syncing all classes of vnc objects with cloudstack
     * Sync cloudstack and vnc objects.
     */
    public short syncAll(short syncMode);

    public void syncClass(Class<?> cls);

    public void createProject(ProjectVO project, StringBuffer syncLogMesg) throws IOException;

    public void createDomain(DomainVO domain, StringBuffer logMesg) throws IOException;
}
