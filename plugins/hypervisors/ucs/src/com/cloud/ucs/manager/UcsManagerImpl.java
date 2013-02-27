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
package com.cloud.ucs.manager;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cxf.helpers.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceService;
import com.cloud.ucs.database.UcsBladeDao;
import com.cloud.ucs.database.UcsBladeVO;
import com.cloud.ucs.database.UcsManagerDao;
import com.cloud.ucs.database.UcsManagerVO;
import com.cloud.ucs.structure.ComputeBlade;
import com.cloud.ucs.structure.UcsProfile;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.xmlobject.XmlObject;
import com.cloud.utils.xmlobject.XmlObjectParser;

@Local(value = { UcsManager.class })
@Component
public class UcsManagerImpl implements UcsManager {
    public static final Logger s_logger = Logger.getLogger(UcsManagerImpl.class);

    @Inject
    private UcsManagerDao ucsDao;
    @Inject
    private ResourceService resourceService;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private ClusterDetailsDao clusterDetailsDao;
    @Inject
    private UcsBladeDao bladeDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private DataCenterDao dcDao;

    private Map<Long, String> cookies = new HashMap<Long, String>();
    private String name;
    private int runLevel;
    private Map<String, Object> params;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
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
        return name;
    }

    private void discoverBlades(UcsManagerVO ucsMgrVo) {
        List<ComputeBlade> blades = listBlades(ucsMgrVo.getId());
        for (ComputeBlade b : blades) {
            UcsBladeVO vo = new UcsBladeVO();
            vo.setDn(b.getDn());
            vo.setUcsManagerId(ucsMgrVo.getId());
            vo.setUuid(UUID.randomUUID().toString());
            bladeDao.persist(vo);
        }
    }
    
    @Override
    @DB
    public UcsManagerResponse addUcsManager(AddUcsManagerCmd cmd) {
        UcsManagerVO vo = new UcsManagerVO();
        vo.setUuid(UUID.randomUUID().toString());
        vo.setPassword(cmd.getPassword());
        vo.setUrl(cmd.getUrl());
        vo.setUsername(cmd.getUsername());
        vo.setZoneId(cmd.getZoneId());
        vo.setName(cmd.getName());

        Transaction txn = Transaction.currentTxn();
        txn.start();
        ucsDao.persist(vo);
        txn.commit();
        UcsManagerResponse rsp = new UcsManagerResponse();
        rsp.setId(String.valueOf(vo.getId()));
        rsp.setName(vo.getName());
        rsp.setUrl(vo.getUrl());
        rsp.setZoneId(String.valueOf(vo.getZoneId()));
        
        discoverBlades(vo);
        
        return rsp;
    }

    private String getCookie(Long ucsMgrId) {
        try {
            String cookie = cookies.get(ucsMgrId);
            if (cookie == null) {
                UcsManagerVO mgrvo = ucsDao.findById(ucsMgrId);
                UcsHttpClient client = new UcsHttpClient(mgrvo.getUrl());
                String login = UcsCommands.loginCmd(mgrvo.getUsername(), mgrvo.getPassword());
                cookie = client.call(login);
                cookies.put(ucsMgrId, cookie);
            }

            return cookie;
        } catch (Exception e) {
            throw new CloudRuntimeException("Cannot get cookie", e);
        }
    }

    private List<ComputeBlade> listBlades(Long ucsMgrId) {
        String cookie = getCookie(ucsMgrId);
        UcsManagerVO mgrvo = ucsDao.findById(ucsMgrId);
        UcsHttpClient client = new UcsHttpClient(mgrvo.getUrl());
        String cmd = UcsCommands.listComputeBlades(cookie);
        String ret = client.call(cmd);
        return ComputeBlade.fromXmString(ret);
    }

    private List<UcsProfile> getUcsProfiles(Long ucsMgrId) {
        String cookie = getCookie(ucsMgrId);
        UcsManagerVO mgrvo = ucsDao.findById(ucsMgrId);
        String cmd = UcsCommands.listProfiles(cookie);
        UcsHttpClient client = new UcsHttpClient(mgrvo.getUrl());
        String res = client.call(cmd);
        List<UcsProfile> profiles = UcsProfile.fromXmlString(res);
        return profiles;
    }

    @Override
    public ListResponse<UcsProfileResponse> listUcsProfiles(ListUcsProfileCmd cmd) {
        List<UcsProfile> profiles = getUcsProfiles(cmd.getUcsManagerId());
        ListResponse<UcsProfileResponse> response = new ListResponse<UcsProfileResponse>();
        List<UcsProfileResponse> rs = new ArrayList<UcsProfileResponse>();
        for (UcsProfile p : profiles) {
            UcsProfileResponse r = new UcsProfileResponse();
            r.setObjectName("ucsprofile");
            r.setDn(p.getDn());
            rs.add(r);
        }
        response.setResponses(rs);
        return response;
    }

    private String cloneProfile(Long ucsMgrId, String srcDn, String newProfileName) {
        UcsManagerVO mgrvo = ucsDao.findById(ucsMgrId);
        UcsHttpClient client = new UcsHttpClient(mgrvo.getUrl());
        String cookie = getCookie(ucsMgrId);
        String cmd = UcsCommands.cloneProfile(cookie, srcDn, newProfileName);
        String res = client.call(cmd);
        XmlObject xo = XmlObjectParser.parseFromString(res);
        return xo.get("lsClone.outConfig.lsServer.dn");
    }

    private boolean isProfileAssociated(Long ucsMgrId, String dn) {
        UcsManagerVO mgrvo = ucsDao.findById(ucsMgrId);
        UcsHttpClient client = new UcsHttpClient(mgrvo.getUrl());
        String cookie = getCookie(ucsMgrId);
        String cmd = UcsCommands.configResolveDn(cookie, dn);
        String res = client.call(cmd);
        XmlObject xo = XmlObjectParser.parseFromString(res);
        return xo.get("outConfig.lsServer.assocState").equals("associated");
    }

    @Override
    public void associateProfileToBlade(AssociateUcsProfileToBladeCmd cmd) {
        SearchCriteriaService<UcsBladeVO, UcsBladeVO> q = SearchCriteria2.create(UcsBladeVO.class);
        q.addAnd(q.getEntity().getUcsManagerId(), Op.EQ, cmd.getUcsManagerId());
        q.addAnd(q.getEntity().getId(), Op.EQ, cmd.getBladeId());
        UcsBladeVO bvo = q.find();
        if (bvo == null) {
            throw new IllegalArgumentException(String.format("cannot find UCS blade[id:%s, ucs manager id:%s]", cmd.getBladeId(), cmd.getUcsManagerId()));
        }
        
        if (bvo.getHostId() != null) {
            throw new CloudRuntimeException(String.format("blade[id:%s,  dn:%s] has been associated with host[id:%s]", bvo.getId(), bvo.getDn(), bvo.getHostId()));
        }
        
        UcsManagerVO mgrvo = ucsDao.findById(cmd.getUcsManagerId());
        String cookie = getCookie(cmd.getUcsManagerId());
        String pdn = cloneProfile(mgrvo.getId(), cmd.getProfileDn(), "profile-for-blade-" + bvo.getId());
        String ucscmd = UcsCommands.associateProfileToBlade(cookie, pdn, bvo.getDn());
        UcsHttpClient client = new UcsHttpClient(mgrvo.getUrl());
        String res = client.call(ucscmd);
        int count = 0;
        int timeout = 600;
        while (count < timeout) {
            if (isProfileAssociated(mgrvo.getId(), bvo.getDn())) {
                break;
            }
            
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new CloudRuntimeException(e);
            }
            
            count += 2;
        }
        
        if (count >= timeout) {
            throw new CloudRuntimeException(String.format("associating profile[%s] to balde[%s] timeout after 600 seconds", pdn, bvo.getDn()));
        }
        
        s_logger.debug(String.format("successfully associated profile[%s] to blade[%s]", pdn, bvo.getDn()));
    }
    
    private String hostIdToUuid(Long hostId) {
        HostVO vo = hostDao.findById(hostId);
        return vo.getUuid();
    }
    
    private String zoneIdToUuid(Long zoneId) {
        DataCenterVO vo = dcDao.findById(zoneId);
        return vo.getUuid();
    }
    
    private String ucsManagerIdToUuid(Long ucsMgrId) {
        UcsManagerVO vo = ucsDao.findById(ucsMgrId); 
        return vo.getUuid();
    }

    @Override
    public ListResponse<UcsManagerResponse> listUcsManager(ListUcsManagerCmd cmd) {
        SearchCriteriaService<UcsManagerVO, UcsManagerVO> serv = SearchCriteria2.create(UcsManagerVO.class);
        serv.addAnd(serv.getEntity().getZoneId(), Op.EQ, cmd.getZoneId());
        List<UcsManagerVO> vos = serv.list();

        List<UcsManagerResponse> rsps = new ArrayList<UcsManagerResponse>(vos.size());
        for (UcsManagerVO vo : vos) {
            UcsManagerResponse rsp = new UcsManagerResponse();
            rsp.setObjectName("ucsmanager");
            rsp.setId(vo.getUuid());
            rsp.setName(vo.getName());
            rsp.setUrl(vo.getUrl());
            rsp.setZoneId(zoneIdToUuid(vo.getZoneId()));
            rsps.add(rsp);
        }
        ListResponse<UcsManagerResponse> response = new ListResponse<UcsManagerResponse>();
        response.setResponses(rsps);
        return response;
    }
    
    public ListResponse<UcsBladeResponse> listUcsBlades(ListUcsBladeCmd cmd) {
        SearchCriteriaService<UcsBladeVO, UcsBladeVO> serv = SearchCriteria2.create(UcsBladeVO.class);
        serv.addAnd(serv.getEntity().getUcsManagerId(), Op.EQ, cmd.getUcsManagerId());
        List<UcsBladeVO> vos = serv.list();
        
        List<UcsBladeResponse> rsps = new ArrayList<UcsBladeResponse>(vos.size());
        for (UcsBladeVO vo : vos) {
            UcsBladeResponse rsp = new UcsBladeResponse();
            rsp.setObjectName("ucsblade");
            rsp.setId(vo.getUuid());
            rsp.setDn(vo.getDn());
            rsp.setHostId(hostIdToUuid(vo.getHostId()));
            rsp.setUcsManagerId(ucsManagerIdToUuid(vo.getUcsManagerId()));
            rsps.add(rsp);
        }
        
        ListResponse<UcsBladeResponse> response = new ListResponse<UcsBladeResponse>();
        response.setResponses(rsps);
        
        return response;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return this.params;
    }

    @Override
    public int getRunLevel() {
        return runLevel;
    }

    @Override
    public void setRunLevel(int level) {
        this.runLevel = level;
    }
}
