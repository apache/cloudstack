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
package org.apache.cloudstack.region;

import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import org.apache.cloudstack.api.command.admin.account.DeleteAccountCmd;
import org.apache.cloudstack.api.command.admin.account.DisableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.EnableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.domain.DeleteDomainCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.DisableUserCmd;
import org.apache.cloudstack.api.command.admin.user.EnableUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.command.user.region.ListRegionsCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

@Component
@Local(value = { RegionService.class })
public class RegionServiceImpl extends ManagerBase implements RegionService, Manager {
    public static final Logger s_logger = Logger.getLogger(RegionServiceImpl.class);

    @Inject
    private RegionManager _regionMgr;

    private String _name;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public Region addRegion(int id, String name, String endPoint) {
        //Check for valid Name
        //Check valid end_point url
        return _regionMgr.addRegion(id, name, endPoint);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Region updateRegion(int id, String name, String endPoint) {
        //Check for valid Name
        //Check valid end_point url
        return _regionMgr.updateRegion(id, name, endPoint);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public boolean removeRegion(int id) {
        return _regionMgr.removeRegion(id);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public List<? extends Region> listRegions(ListRegionsCmd cmd) {
        return _regionMgr.listRegions(cmd.getId(), cmd.getName());
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public boolean deleteUserAccount(DeleteAccountCmd cmd) {
        return _regionMgr.deleteUserAccount(cmd.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Account updateAccount(UpdateAccountCmd cmd) {
        return _regionMgr.updateAccount(cmd);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Account disableAccount(DisableAccountCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException {
        return _regionMgr.disableAccount(cmd.getAccountName(), cmd.getDomainId(), cmd.getId(), cmd.getLockRequested());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Account enableAccount(EnableAccountCmd cmd) {
        return _regionMgr.enableAccount(cmd.getAccountName(), cmd.getDomainId(), cmd.getId());
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public boolean deleteUser(DeleteUserCmd cmd) {
        return _regionMgr.deleteUser(cmd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Domain updateDomain(UpdateDomainCmd cmd) {
        return _regionMgr.updateDomain(cmd);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public boolean deleteDomain(DeleteDomainCmd cmd) {
        return _regionMgr.deleteDomain(cmd.getId(), cmd.getCleanup());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserAccount updateUser(UpdateUserCmd cmd){
        return _regionMgr.updateUser(cmd);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public UserAccount disableUser(DisableUserCmd cmd) {
        return _regionMgr.disableUser(cmd.getId());
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public UserAccount enableUser(EnableUserCmd cmd) {
        return _regionMgr.enableUser(cmd.getId());
    }

}
