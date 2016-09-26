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

package net.nuage.vsp.acs;

import net.nuage.vsp.acs.client.api.NuageVspApiClient;
import net.nuage.vsp.acs.client.api.NuageVspElementClient;
import net.nuage.vsp.acs.client.api.NuageVspGuruClient;
import net.nuage.vsp.acs.client.api.NuageVspManagerClient;
import net.nuage.vsp.acs.client.api.impl.NuageVspApiClientImpl;
import net.nuage.vsp.acs.client.api.impl.NuageVspElementClientImpl;
import net.nuage.vsp.acs.client.api.impl.NuageVspGuruClientImpl;
import net.nuage.vsp.acs.client.api.impl.NuageVspManagerClientImpl;
import net.nuage.vsp.acs.client.api.model.VspHost;
import org.apache.log4j.Logger;


public class NuageVspPluginClientLoader {

    private static final Logger s_logger = Logger.getLogger(NuageVspPluginClientLoader.class);

    private NuageVspApiClient _nuageVspApiClient;
    private NuageVspElementClient _nuageVspElementClient;
    private NuageVspGuruClient _nuageVspGuruClient;
    private NuageVspManagerClient _nuageVspManagerClient;

    private NuageVspPluginClientLoader() {

    }

    public static NuageVspPluginClientLoader getClientLoader(String relativePath, String cmsUserEnterprise, String cmsUserLogin,
            String cmsUserPassword, int numRetries, int retryInterval, String nuageVspCmsId) {
        NuageVspPluginClientLoader nuageVspPluginClientClassloader = new NuageVspPluginClientLoader();
        nuageVspPluginClientClassloader.loadClasses(relativePath, cmsUserEnterprise, cmsUserLogin, cmsUserPassword, numRetries, retryInterval, nuageVspCmsId);
        return nuageVspPluginClientClassloader;
    }

    private void loadClasses(String relativePath, String cmsUserEnterprise, String cmsUserLogin, String cmsUserPassword, int numRetries,
            int retryInterval, String nuageVspCmsId) {
        VspHost vspHost = new VspHost.Builder()
                .restRelativePath(relativePath)
                .cmsUserEnterprise(cmsUserEnterprise)
                .cmsUserLogin(cmsUserLogin)
                .cmsUserPassword(cmsUserPassword)
                .noofRetry(numRetries)
                .retryInterval(retryInterval)
                .nuageVspCmsId(nuageVspCmsId)
                .build();
        _nuageVspApiClient = new NuageVspApiClientImpl(vspHost);
        _nuageVspElementClient = new NuageVspElementClientImpl(_nuageVspApiClient);
        _nuageVspGuruClient = new NuageVspGuruClientImpl(_nuageVspApiClient);
        _nuageVspManagerClient = new NuageVspManagerClientImpl(_nuageVspApiClient);
    }

    public NuageVspApiClient getNuageVspApiClient() {
        return _nuageVspApiClient;
    }

    public NuageVspElementClient getNuageVspElementClient() {
        return _nuageVspElementClient;
    }

    public NuageVspGuruClient getNuageVspGuruClient() {
        return _nuageVspGuruClient;
    }

    public NuageVspManagerClient getNuageVspManagerClient() {
        return _nuageVspManagerClient;
    }
}
