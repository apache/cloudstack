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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import org.apache.cloudstack.api.AddBaremetalRctCmd;
import org.apache.cloudstack.api.AddBaremetalSwitchCmd;
import org.apache.cloudstack.api.DeleteBaremetalRctCmd;
import org.apache.cloudstack.api.DeleteBaremetalSwitchCmd;
import org.apache.cloudstack.api.ListBaremetalRctCmd;
import org.apache.cloudstack.api.ListBaremetalSwitchesCmd;
import org.apache.cloudstack.api.UpdateBaremetalSwitchCmd;
import org.apache.cloudstack.utils.baremetal.BaremetalUtils;

import com.cloud.baremetal.database.BaremetalRctDao;
import com.cloud.baremetal.database.BaremetalRctVO;
import com.cloud.baremetal.database.BaremetalSwitchDao;
import com.cloud.baremetal.database.BaremetalSwitchVO;
import com.cloud.baremetal.networkservice.BaremetalRctResponse;
import com.cloud.baremetal.networkservice.BaremetalSwitchBackend;
import com.cloud.baremetal.networkservice.BaremetalSwitchResponse;
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
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineProfile;

/**
 * Created by frank on 5/8/14.
 */
public class BaremetalVlanManagerImpl extends ManagerBase implements BaremetalVlanManager {
    private final Gson gson = new Gson();

    @Inject
    private BaremetalRctDao rctDao;
    @Inject
    private BaremetalSwitchDao switchDao;
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
    public BaremetalSwitchResponse addSwitch(AddBaremetalSwitchCmd cmd) {
        // check whether specified switch type is valid
        getSwitchBackend(cmd.getType());

        BaremetalSwitchVO vo = new BaremetalSwitchVO();
        vo.setIp(cmd.getIp());
        vo.setUsername(cmd.getUsername());
        vo.setPassword(cmd.getPassword());
        vo.setType(cmd.getType());
        vo = switchDao.persist(vo);

        return switchDao.newBaremetalSwitchResponse(vo);
    }

    @Override
    public BaremetalSwitchResponse updateSwitch(UpdateBaremetalSwitchCmd cmd) {
        BaremetalSwitchVO vo = switchDao.findById(cmd.getId());
        if (null != cmd.getIp()) {
            vo.setIp(cmd.getIp());
        }
        if (null != cmd.getUsername()) {
            vo.setUsername(cmd.getUsername());
        }
        if (null != cmd.getPassword()) {
            vo.setPassword(cmd.getPassword());
        }

        if (!switchDao.update(vo.getId(), vo)) {
            throw new CloudRuntimeException("Unable to update baremetal switch");
        }

        return switchDao.newBaremetalSwitchResponse(vo);
    }

    /**
     * Generates a vlan struct for the specified network and host.
     * Uses relevant baremetal switch unless specified host does not have details
     * on its switch and/or vlan port, in which case, the method will fall back
     * on using RCT for obtaining switch information.
     *
     * @param nw
     * @param host
     * @return BaremetalVlanStruct for the specified network and host
     */
    protected BaremetalVlanStruct generateVlanStruct(Network nw, HostVO host) {
        if (null == host.getDetails()) {
            hostDao.loadDetails(host);
        }

        String switchUuid = null;
        String switchPort = host.getDetail("switchPort");
        if (null != switchPort) { // only check for switch uuid if host has vlan port specified
            switchUuid = host.getDetail("switchUuid");
        }

        BaremetalVlanStruct struct = null;
        if (null != switchUuid) {
            BaremetalSwitchVO sw = switchDao.findByUuid(switchUuid);
            if (null == sw) {
                throw new CloudRuntimeException(String.format("switch with uuid %s not found (associated with host with uuid %s)", switchUuid, host.getUuid()));
            }

            struct = new BaremetalVlanStruct();
            struct.setSwitchIp(sw.getIp());
            struct.setSwitchUsername(sw.getUsername());
            struct.setSwitchPassword(DBEncryptionUtil.decrypt(sw.getPassword()));
            struct.setSwitchType(sw.getType());
        } else {
            List<BaremetalRctVO> vos = rctDao.listAll();
            if (vos.isEmpty()) {
                throw new CloudRuntimeException("no rack configuration found, please call addBaremetalRct to add one");
            }

            BaremetalRctVO vo = vos.get(0);
            BaremetalRct rct = gson.fromJson(vo.getRct(), BaremetalRct.class);
            RackPair rp = findRack(rct, host.getPrivateMacAddress());
            if (rp == null) {
                throw new CloudRuntimeException(
                        String.format("cannot find any rack contains host[mac:%s], please double check your rack configuration file, update it and call addBaremetalRct again",
                                host.getPrivateMacAddress()));
            }

            struct = new BaremetalVlanStruct();
            struct.setSwitchIp(rp.rack.getL2Switch().getIp());
            struct.setSwitchPassword(rp.rack.getL2Switch().getPassword());
            struct.setSwitchType(rp.rack.getL2Switch().getType());
            struct.setSwitchUsername(rp.rack.getL2Switch().getUsername());
            switchPort = rp.host.getPort();
        }

        int vlan = Integer.parseInt(Networks.BroadcastDomainType.getValue(nw.getBroadcastUri()));
        struct.setHostMac(host.getPrivateMacAddress());
        struct.setPort(switchPort);
        struct.setVlan(vlan);

        return struct;
    }

    @Override
    public void prepareVlan(Network nw, DeployDestination destHost) {
        HostVO host = null;
        if (destHost.getHost() instanceof HostVO) {
            host = (HostVO)destHost.getHost();
        } else {
            host = hostDao.findById(destHost.getHost().getId());
        }

        BaremetalVlanStruct struct = generateVlanStruct(nw, host);
        BaremetalSwitchBackend backend = getSwitchBackend(struct.getSwitchType());
        backend.prepareVlan(struct);
    }

    @Override
    public void releaseVlan(Network nw, VirtualMachineProfile vm) {
        HostVO host = hostDao.findById(vm.getVirtualMachine().getHostId());
        BaremetalVlanStruct struct = generateVlanStruct(nw, host);
        BaremetalSwitchBackend backend = getSwitchBackend(struct.getSwitchType());
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
    public void deleteSwitch(DeleteBaremetalSwitchCmd cmd) {
        switchDao.remove(cmd.getId());
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

    @Override
    public List<BaremetalSwitchResponse> listSwitches() {
        List<BaremetalSwitchVO> vos = switchDao.listAll();
        List<BaremetalSwitchResponse> rsp = new ArrayList<BaremetalSwitchResponse>();
        for (BaremetalSwitchVO vo : vos) {
            rsp.add(switchDao.newBaremetalSwitchResponse(vo));
        }
        return rsp;
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
        cmds.add(AddBaremetalSwitchCmd.class);
        cmds.add(UpdateBaremetalSwitchCmd.class);
        cmds.add(ListBaremetalSwitchesCmd.class);
        cmds.add(DeleteBaremetalSwitchCmd.class);
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
        acnt.setState(Account.State.enabled);
        acnt.setDomainId(1);
        acnt = acntDao.persist(acnt);

        UserVO user = new UserVO();
        user.setState(Account.State.enabled);
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
