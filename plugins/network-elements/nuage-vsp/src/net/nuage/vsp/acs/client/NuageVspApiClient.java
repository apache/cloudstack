//
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
//

package net.nuage.vsp.acs.client;

import java.util.concurrent.ExecutionException;

public interface NuageVspApiClient {

    void login() throws ExecutionException;

    void setNuageVspHost(String restRelativePath, String[] cmsUserInfo, int noofRetry, int retryInterval, String nuageVspCmsId);

    String executeRestApi(String method, String resource, String resourceId, String childResource, Object entityDetails, String resourceFilter, String proxyUserUuid,
                                 String proxyUserDomainuuid) throws ExecutionException;
}
