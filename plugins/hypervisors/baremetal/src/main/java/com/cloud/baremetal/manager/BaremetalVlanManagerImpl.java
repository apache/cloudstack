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
package com.cloud.baremetal.manager;

import com.cloud.baremetal.database.BaremetalRctDao;
import com.cloud.baremetal.database.BaremetalRctVO;
import com.cloud.baremetal.networkservice.BaremetalRctResponse;
import com.cloud.baremetal.networkservice.BaremetalSwitchBackend;
import com.cloud.baremetal.networkservice.BaremetalVlanStruct;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineProfile;
import com.google.gson.Gson;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.AddBaremetalRctCmd;
import org.apache.cloudstack.api.DeleteBaremetalRctCmd;
import org.apache.cloudstack.api.ListBaremetalRctCmd;
import org.apache.cloudstack.utils.baremetal.BaremetalUtils;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by frank on 5/8/14.
 */
public class BaremetalVlanManagerImpl extends ManagerBase implements BaremetalVlanManager {
    private Gson gson = new Gson();

    @Inject
    private BaremetalRctDao rctDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private AccountDao acntDao;
    @Inject
    private UserDao userDao;
    @Inject
    private AccountManager acntMgr;

    private Map<String, BaremetalSwitchBackend> backends;

    private class RackPair {
        BaremetalRct.Rack rack;
        BaremetalRct.HostEntry host;
    }

    public void setBackends(Map<String, BaremetalSwitchBackend> backends) {
        this.backends = backends;
    }

    @Override
    public BaremetalRctResponse addRct(AddBaremetalRctCmd cmd) {
        try {
            List<BaremetalRctVO> existings = rctDao.listAll();
            if (!existings.isEmpty()) {
                throw new CloudRuntimeException(String.format("there is some RCT existing. A CloudStack deployment accepts only one RCT"));
            }
            URL url = new URL(cmd.getRctUrl());
            RestTemplate rest = new RestTemplate();
            String rctStr = rest.getForObject(url.toString(), String.class);

            // validate it's right format
            BaremetalRct rct = gson.fromJson(rctStr, BaremetalRct.class);
            QueryBuilder<BaremetalRctVO> sc = QueryBuilder.create(BaremetalRctVO.class);
            sc.and(sc.entity().getUrl(), SearchCriteria.Op.EQ, cmd.getRctUrl());
            BaremetalRctVO vo =  sc.find();
            if (vo == null) {
                vo = new BaremetalRctVO();
                vo.setRct(gson.toJson(rct));
                vo.setUrl(cmd.getRctUrl());
                vo = rctDao.persist(vo);
            } else {
                vo.setRct(gson.toJson(rct));
                rctDao.update(vo.getId(), vo);
            }

            BaremetalRctResponse rsp = new BaremetalRctResponse();
            rsp.setUrl(vo.getUrl());
            rsp.setId(vo.getUuid());
            rsp.setObjectName("baremetalrct");
            return rsp;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("%s is not a legal http url", cmd.getRctUrl()));
        }
    }

    @Override
    public void prepareVlan(Network nw, DeployDestination destHost) {
        List<BaremetalRctVO> vos = rctDao.listAll();
        if (vos.isEmpty()) {
            throw new CloudRuntimeException("no rack configuration found, please call addBaremetalRct to add one");
        }

        BaremetalRctVO vo = vos.get(0);
        BaremetalRct rct = gson.fromJson(vo.getRct(), BaremetalRct.class);

        RackPair rp = findRack(rct, destHost.getHost().getPrivateMacAddress());
        if (rp == null) {
            throw new CloudRuntimeException(String.format("cannot find any rack contains host[mac:%s], please double check your rack configuration file, update it and call addBaremetalRct again", destHost.getHost().getPrivateMacAddress()));
        }

        int vlan = Integer.parseInt(Networks.BroadcastDomainType.getValue(nw.getBroadcastUri()));
        BaremetalSwitchBackend backend = getSwitchBackend(rp.rack.getL2Switch().getType());
        BaremetalVlanStruct struct = new BaremetalVlanStruct();
        struct.setHostMac(rp.host.getMac());
        struct.setPort(rp.host.getPort());
        struct.setSwitchIp(rp.rack.getL2Switch().getIp());
        struct.setSwitchPassword(rp.rack.getL2Switch().getPassword());
        struct.setSwitchType(rp.rack.getL2Switch().getType());
        struct.setSwitchUsername(rp.rack.getL2Switch().getUsername());
        struct.setVlan(vlan);
        backend.prepareVlan(struct);
    }

    @Override
    public void releaseVlan(Network nw, VirtualMachineProfile vm) {
        List<BaremetalRctVO> vos = rctDao.listAll();
        if (vos.isEmpty()) {
            throw new CloudRuntimeException("no rack configuration found, please call addBaremetalRct to add one");
        }

        BaremetalRctVO vo = vos.get(0);
        BaremetalRct rct = gson.fromJson(vo.getRct(), BaremetalRct.class);
        HostVO host = hostDao.findById(vm.getVirtualMachine().getHostId());
        RackPair rp = findRack(rct, host.getPrivateMacAddress());
        assert rp != null : String.format("where is my rack???");

        int vlan = Integer.parseInt(Networks.BroadcastDomainType.getValue(nw.getBroadcastUri()));
        BaremetalVlanStruct struct = new BaremetalVlanStruct();
        struct.setHostMac(rp.host.getMac());
        struct.setPort(rp.host.getPort());
        struct.setSwitchIp(rp.rack.getL2Switch().getIp());
        struct.setSwitchPassword(rp.rack.getL2Switch().getPassword());
        struct.setSwitchType(rp.rack.getL2Switch().getType());
        struct.setSwitchUsername(rp.rack.getL2Switch().getUsername());
        struct.setVlan(vlan);
        BaremetalSwitchBackend backend = getSwitchBackend(rp.rack.getL2Switch().getType());
        backend.removePortFromVlan(struct);
    }

    @Override
    public void registerSwitchBackend(BaremetalSwitchBackend backend) {
        backends.put(backend.getSwitchBackendType(), backend);
    }

    @Override
    public void deleteRct(DeleteBaremetalRctCmd cmd) {
        rctDao.remove(cmd.getId());
    }

    @Override
    public BaremetalRctResponse listRct() {
        List<BaremetalRctVO> vos = rctDao.listAll();
        if (!vos.isEmpty()) {
            BaremetalRctVO vo = vos.get(0);
            BaremetalRctResponse rsp = new BaremetalRctResponse();
            rsp.setId(vo.getUuid());
            rsp.setUrl(vo.getUrl());
            rsp.setObjectName("baremetalrct");
            return rsp;
        }
        return null;
    }

    private BaremetalSwitchBackend getSwitchBackend(String type) {
        BaremetalSwitchBackend backend = backends.get(type);
        if (backend == null) {
            throw new CloudRuntimeException(String.format("cannot find switch backend[type:%s]", type));
        }
        return backend;
    }

    private RackPair findRack(BaremetalRct rct, String mac) {
        for (BaremetalRct.Rack rack : rct.getRacks()) {
            for (BaremetalRct.HostEntry host : rack.getHosts()) {
                if (mac.equals(host.getMac())) {
                    RackPair p = new RackPair();
                    p.host = host;
                    p.rack = rack;
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Baremetal Vlan Manager";
    }


    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<Class<?>>();
        cmds.add(AddBaremetalRctCmd.class);
        cmds.add(ListBaremetalRctCmd.class);
        cmds.add(DeleteBaremetalRctCmd.class);
        return cmds;
    }

    @Override
    public boolean start() {
        QueryBuilder<AccountVO> acntq = QueryBuilder.create(AccountVO.class);
        acntq.and(acntq.entity().getAccountName(), SearchCriteria.Op.EQ, BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME);
        AccountVO acnt = acntq.find();
        if (acnt != null) {
            return true;
        }

        acnt = new AccountVO();
        acnt.setAccountName(BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME);
        acnt.setUuid(UUID.randomUUID().toString());
        acnt.setState(Account.State.ENABLED);
        acnt.setDomainId(1);
        acnt.setType(RoleType.User.getAccountType());
        acnt.setRoleId(RoleType.User.getId());
        acnt = acntDao.persist(acnt);

        UserVO user = new UserVO();
        user.setState(Account.State.ENABLED);
        user.setUuid(UUID.randomUUID().toString());
        user.setAccountId(acnt.getAccountId());
        user.setUsername(BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME);
        user.setFirstname(BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME);
        user.setLastname(BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME);
        user.setPassword(UUID.randomUUID().toString());
        user.setSource(User.Source.UNKNOWN);
        user = userDao.persist(user);

        String[] keys = acntMgr.createApiKeyAndSecretKey(user.getId());
        user.setApiKey(keys[0]);
        user.setSecretKey(keys[1]);
        userDao.update(user.getId(), user);
        return true;
    }
}
