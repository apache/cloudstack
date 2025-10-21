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
package org.apache.cloudstack.vm;

import com.cloud.dc.DataCenter;
import com.cloud.host.Host;
import com.cloud.user.Account;
import org.apache.cloudstack.api.command.admin.vm.ListImportVMTasksCmd;
import org.apache.cloudstack.api.response.ImportVMTaskResponse;
import org.apache.cloudstack.api.response.ListResponse;

public interface ImportVmTasksManager {

    ListResponse<ImportVMTaskResponse> listImportVMTasks(ListImportVMTasksCmd cmd);

    ImportVmTask createImportVMTaskRecord(DataCenter zone, Account owner, long userId, String displayName,
                                            String vcenter, String datacenterName, String sourceVMName,
                                            Host convertHost, Host importHost);

    void updateImportVMTaskStep(ImportVmTask importVMTaskVO, DataCenter zone, Account owner, Host convertHost,
                                Host importHost, Long vmId, ImportVmTask.Step step);

    void updateImportVMTaskErrorState(ImportVmTask importVMTaskVO, ImportVmTask.TaskState state, String errorMsg);
}
