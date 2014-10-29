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
package org.apache.cloudstack.framework.jobs;

public interface AsyncJobMBean {
    public long getAccountId();

    public long getUserId();

    public String getCmd();

    public String getCmdInfo();

    public String getStatus();

    public int getProcessStatus();

    public int getResultCode();

    public String getResult();

    public String getInstanceType();

    public String getInstanceId();

    public String getInitMsid();

    public String getCreateTime();

    public String getLastUpdateTime();

    public String getLastPollTime();

    public String getSyncQueueId();

    public String getSyncQueueContentType();

    public String getSyncQueueContentId();
}
