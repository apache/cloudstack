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

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceService;
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

    private Map<Long, String> cookies = new HashMap<Long, String>();

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
        return "UcsManager";
    }

    @Override
    @DB
    public AddUcsManagerResponse addUcsManager(AddUcsManagerCmd cmd) {
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
        AddUcsManagerResponse rsp = new AddUcsManagerResponse();
        rsp.setId(String.valueOf(vo.getId()));
        rsp.setName(vo.getName());
        rsp.setUrl(vo.getUrl());
        rsp.setZoneId(String.valueOf(vo.getZoneId()));
        return rsp;
    }

    private String getUcsManagerIp() {
        SearchCriteriaService<UcsManagerVO, UcsManagerVO> serv = SearchCriteria2.create(UcsManagerVO.class);
        List<UcsManagerVO> vos = serv.list();
        if (vos.isEmpty()) {
            throw new CloudRuntimeException("Cannot find any UCS manager, you must add it first");
        }
        return vos.get(0).getUrl();
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
    public ListResponse<ListUcsProfileResponse> listUcsProfiles(ListUcsProfileCmd cmd) {
        List<UcsProfile> profiles = getUcsProfiles(cmd.getUcsManagerId());
        ListResponse<ListUcsProfileResponse> response = new ListResponse<ListUcsProfileResponse>();
        List<ListUcsProfileResponse> rs = new ArrayList<ListUcsProfileResponse>();
        for (UcsProfile p : profiles) {
            ListUcsProfileResponse r = new ListUcsProfileResponse();
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

    private String buildProfileNameForHost(HostVO vo) {
        return String.format("z%sp%sc%sh%s", vo.getDataCenterId(), vo.getPodId(), vo.getClusterId(), vo.getId());
    }

    private boolean isBladeAssociated(Long ucsMgrId, String dn) {
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
    }

    @Override
    public ListResponse<ListUcsManagerResponse> listUcsManager(ListUcsManagerCmd cmd) {
        SearchCriteriaService<UcsManagerVO, UcsManagerVO> serv = SearchCriteria2.create(UcsManagerVO.class);
        serv.addAnd(serv.getEntity().getZoneId(), Op.EQ, cmd.getZoneId());
        List<UcsManagerVO> vos = serv.list();

        List<ListUcsManagerResponse> rsps = new ArrayList<ListUcsManagerResponse>(vos.size());
        for (UcsManagerVO vo : vos) {
            ListUcsManagerResponse rsp = new ListUcsManagerResponse();
            rsp.setObjectName("ucsmanager");
            rsp.setId(String.valueOf(vo.getId()));
            rsp.setName(vo.getName());
            rsp.setZoneId(String.valueOf(vo.getZoneId()));
            rsps.add(rsp);
        }
        ListResponse<ListUcsManagerResponse> response = new ListResponse<ListUcsManagerResponse>();
        response.setResponses(rsps);
        return response;
    }
}
