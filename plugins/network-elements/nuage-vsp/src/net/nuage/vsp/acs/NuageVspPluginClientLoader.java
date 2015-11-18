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

import net.nuage.vsp.acs.client.NuageVspApiClient;
import net.nuage.vsp.acs.client.NuageVspElementClient;
import net.nuage.vsp.acs.client.NuageVspGuruClient;
import net.nuage.vsp.acs.client.NuageVspManagerClient;
import net.nuage.vsp.acs.client.NuageVspSyncClient;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class NuageVspPluginClientLoader {

    private ClassLoader _loader = null;
    private static final Logger s_logger = Logger.getLogger(NuageVspPluginClientLoader.class);

    private NuageVspApiClient _nuageVspApiClient;
    private NuageVspElementClient _nuageVspElementClient;
    private NuageVspGuruClient _nuageVspGuruClient;
    private NuageVspManagerClient _nuageVspManagerClient;
    private NuageVspSyncClient _nuageVspSyncClient;

    private static final String NUAGE_PLUGIN_CLIENT_JAR_FILE = "/usr/share/nuagevsp/lib/nuage-vsp-acs-client.jar";
    private static final String NUAGE_VSP_API_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspApiClientImpl";
    private static final String NUAGE_VSP_SYNC_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspSyncClientImpl";
    private static final String NUAGE_VSP_ELEMENT_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspElementClientImpl";
    private static final String NUAGE_VSP_GURU_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspGuruClientImpl";
    private static final String NUAGE_VSP_MANAGER_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspManagerClientImpl";

    private NuageVspPluginClientLoader(String nuagePluginClientJarLocation) {
        try {
            _loader = URLClassLoader.newInstance(new URL[] {new URL("jar:file:" + nuagePluginClientJarLocation + "!/")},
                    getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static NuageVspPluginClientLoader getClientLoader(String relativePath, String[] cmsUserInfo, int numRetries, int retryInterval,
            String nuageVspCmsId) throws ConfigurationException {
        NuageVspPluginClientLoader nuageVspPluginClientClassloader = new NuageVspPluginClientLoader(NUAGE_PLUGIN_CLIENT_JAR_FILE);
        nuageVspPluginClientClassloader.loadClasses(relativePath, cmsUserInfo, numRetries, retryInterval, nuageVspCmsId);
        return nuageVspPluginClientClassloader;
    }

    private void loadClasses(String relativePath, String[] cmsUserInfo, int numRetries, int retryInterval, String nuageVspCmsId) throws ConfigurationException {
        try {
            Class<?> nuageVspApiClientClass = Class.forName(NUAGE_VSP_API_CLIENT_IMPL, true, _loader);
            Class<?> nuageVspSyncClientClass = Class.forName(NUAGE_VSP_SYNC_CLIENT_IMPL, true, _loader);
            Class<?> nuageVspGuruClientClass = Class.forName(NUAGE_VSP_GURU_CLIENT_IMPL, true, _loader);
            Class<?> nuageVspElementClientClass = Class.forName(NUAGE_VSP_ELEMENT_CLIENT_IMPL, true, _loader);
            Class<?> nuageVspManagerClientClass = Class.forName(NUAGE_VSP_MANAGER_CLIENT_IMPL, true, _loader);

            //Instantiate the instances
            _nuageVspApiClient = (NuageVspApiClient)nuageVspApiClientClass.newInstance();
            _nuageVspApiClient.setNuageVspHost(relativePath, cmsUserInfo, numRetries, retryInterval, nuageVspCmsId);
            _nuageVspSyncClient = (NuageVspSyncClient)nuageVspSyncClientClass.newInstance();
            _nuageVspSyncClient.setNuageVspApiClient(_nuageVspApiClient);
            _nuageVspGuruClient = (NuageVspGuruClient)nuageVspGuruClientClass.newInstance();
            _nuageVspGuruClient.setNuageVspApiClient(_nuageVspApiClient);
            _nuageVspElementClient = (NuageVspElementClient)nuageVspElementClientClass.newInstance();
            _nuageVspElementClient.setNuageVspApiClient(_nuageVspApiClient);
            _nuageVspManagerClient = (NuageVspManagerClient)nuageVspManagerClientClass.newInstance();
            _nuageVspManagerClient.setNuageVspApiClient(_nuageVspApiClient);
        } catch (ClassNotFoundException cnfe) {
            s_logger.error("Error while loading classes of Nuage VSP client", cnfe);
            throw new ConfigurationException("Error while loading classes of Nuage VSP client");
        } catch (InstantiationException ie) {
            s_logger.error("Error while initializing classes of Nuage VSP client", ie);
            throw new ConfigurationException("Error while initializing classes of Nuage VSP client");
        } catch (IllegalAccessException iae) {
            s_logger.error("Error while accessing classes of Nuage VSP client", iae);
            throw new ConfigurationException("Error while accessing classes of Nuage VSP client");
        }

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

    public NuageVspSyncClient getNuageVspSyncClient() {
        return _nuageVspSyncClient;
    }
}
