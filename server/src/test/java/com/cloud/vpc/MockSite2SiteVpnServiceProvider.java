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

package com.cloud.vpc;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.utils.component.ManagerBase;
import org.springframework.stereotype.Component;

import javax.naming.ConfigurationException;
import java.util.Map;

@Component
public class MockSite2SiteVpnServiceProvider extends ManagerBase implements Site2SiteVpnServiceProvider {

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Adapter#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Adapter#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "Site2SiteVpnServiceProvider";
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Adapter#start()
     */
    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Adapter#stop()
     */
    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.element.Site2SiteVpnServiceProvider#startSite2SiteVpn(com.cloud.network.Site2SiteVpnConnection)
     */
    @Override
    public boolean startSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.element.Site2SiteVpnServiceProvider#stopSite2SiteVpn(com.cloud.network.Site2SiteVpnConnection)
     */
    @Override
    public boolean stopSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return true;
    }

}
