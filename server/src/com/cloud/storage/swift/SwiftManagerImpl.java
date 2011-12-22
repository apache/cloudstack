/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/**
 * 
 * @author Anthony Xu
 * 
 */

package com.cloud.storage.swift;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteObjectFromSwiftCommand;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.api.commands.AddSwiftCmd;
import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.ListSwiftCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.SwiftVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateSwiftVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.SwiftDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateSwiftDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.exception.CloudRuntimeException;



@Local(value = { SwiftManager.class })
public class SwiftManagerImpl implements SwiftManager {
    private static final Logger s_logger = Logger.getLogger(SwiftManagerImpl.class);



    private String _name;
    @Inject
    private SwiftDao _swiftDao;
    @Inject
    VMTemplateSwiftDao _vmTmpltSwiftlDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VMTemplateZoneDao _vmTmpltZoneDao;
    @Inject
    private VMTemplateHostDao _vmTmpltHostDao;
    @Inject
    private HostDao _hostDao;

    @Override
    public SwiftTO getSwiftTO(Long swiftId) {
        return _swiftDao.getSwiftTO(swiftId);
    }

    @Override
    public SwiftTO getSwiftTO() {
        return _swiftDao.getSwiftTO(null);
    }

    @Override
    public boolean isSwiftEnabled() {
        Boolean swiftEnable = Boolean.valueOf(_configDao.getValue(Config.SwiftEnable.key()));
        if (swiftEnable) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isTemplateInstalled(Long templateId) {

        SearchCriteriaService<VMTemplateSwiftVO, VMTemplateSwiftVO> sc = SearchCriteria2.create(VMTemplateSwiftVO.class);
        sc.addAnd(sc.getEntity().getTemplateId(), Op.EQ, templateId);
        return !sc.list().isEmpty();
    }

    @Override
    public SwiftVO addSwift(AddSwiftCmd cmd) throws DiscoveryException {
        if (!isSwiftEnabled()) {
            throw new DiscoveryException("Swift is not enabled");
        }
        SwiftVO swift = new SwiftVO(cmd.getUrl(), cmd.getAccount(), cmd.getUsername(), cmd.getKey());
        swift = _swiftDao.persist(swift);
        return swift;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start Swift Manager");
        }

        return true;
    }

    @Override
    public void deleteIso(DeleteIsoCmd cmd) {
        String msg;
        SwiftTO swift = getSwiftTO();
        if (swift == null) {
            msg = "There is no Swift in this setup";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        VMTemplateSwiftVO tmpltSwiftRef = _vmTmpltSwiftlDao.findBySwiftTemplate(swift.getId(), cmd.getId());
        if ( tmpltSwiftRef == null ) {
           msg = "Delete ISO failed due to  cannot find ISO " + cmd.getId() + " in Swift ";
           s_logger.warn(msg);
           throw new CloudRuntimeException(msg);
        }
        Answer answer = _agentMgr.sendToSSVM(null, new DeleteObjectFromSwiftCommand(swift, "T-" + cmd.getId(), null));
        if (answer == null || !answer.getResult()) {
            msg = "Failed to delete " + tmpltSwiftRef + " due to " + ((answer == null) ? "answer is null" : answer.getDetails());
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        } else {
            _vmTmpltSwiftlDao.remove(tmpltSwiftRef.getId());
            s_logger.debug("Deleted template " + cmd.getId() + " in Swift");
        }
    }

    @Override
    public void deleteTemplate(DeleteTemplateCmd cmd) {
        String msg;
        SwiftTO swift = getSwiftTO();
        if (swift == null) {
            msg = "There is no Swift in this setup";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        VMTemplateSwiftVO tmpltSwiftRef = _vmTmpltSwiftlDao.findBySwiftTemplate(swift.getId(), cmd.getId());
        if (tmpltSwiftRef == null) {
            msg = "Delete Template failed due to cannot find Template" + cmd.getId() + " in Swift ";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        Answer answer = _agentMgr.sendToSSVM(null, new DeleteObjectFromSwiftCommand(swift, "T-" + cmd.getId(), null));
        if (answer == null || !answer.getResult()) {
            msg = "Failed to delete " + tmpltSwiftRef + " due to " + ((answer == null) ? "answer is null" : answer.getDetails());
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        } else {
            _vmTmpltSwiftlDao.remove(tmpltSwiftRef.getId());
            s_logger.debug("Deleted template " + cmd.getId() + " in Swift");
        }
    }

    @Override
    public void propagateTemplateOnAllZones(Long tmpltId) {
        String msg;
        SwiftTO swift = getSwiftTO();
        if (swift == null) {
            msg = "There is no Swift in this setup";
            s_logger.trace(msg);
            return;
        }
        VMTemplateSwiftVO tmpltSwiftRef = _vmTmpltSwiftlDao.findOneByTemplateId(tmpltId);
        if (tmpltSwiftRef != null) {
            List<DataCenterVO> dcs = _dcDao.listAll();
            for (DataCenterVO dc : dcs) {
                VMTemplateZoneVO tmpltZoneVO = new VMTemplateZoneVO(dc.getId(), tmpltId, new Date());
                try {
                    _vmTmpltZoneDao.persist(tmpltZoneVO);
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public void propagateSwiftTmplteOnZone(Long zoneId) {
        String msg;
        SwiftTO swift = getSwiftTO();
        if (swift == null) {
            msg = "There is no Swift in this setup";
            s_logger.trace(msg);
            return;
        }
        List<Long> tmpltIds = new ArrayList<Long>();
        List<VMTemplateSwiftVO> tmpltSwiftRefs = _vmTmpltSwiftlDao.listAll();
        if (tmpltSwiftRefs == null) {
            return;
        }
        for (VMTemplateSwiftVO tmpltSwiftRef : tmpltSwiftRefs) {
            Long tmpltId = tmpltSwiftRef.getTemplateId();
            if (!tmpltIds.contains(tmpltId)) {
                tmpltIds.add(tmpltId);
                VMTemplateZoneVO tmpltZoneVO = new VMTemplateZoneVO(zoneId, tmpltId, new Date());
                try {
                    _vmTmpltZoneDao.persist(tmpltZoneVO);
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public Long chooseZoneForTmpltExtract(Long tmpltId) {
        SwiftTO swift = getSwiftTO();
        if (swift == null) {
            return null;
        }
        
        List<VMTemplateHostVO> tmpltHosts = _vmTmpltHostDao.listByOnlyTemplateId(tmpltId);
        if (tmpltHosts != null) {
            Collections.shuffle(tmpltHosts);
            for (VMTemplateHostVO tHost : tmpltHosts) {
                HostVO host = _hostDao.findById(tHost.getHostId());
                if (host != null) {
                    return host.getDataCenterId();
                }
                throw new CloudRuntimeException("can not find secondary storage host");
            }
        }
        List<DataCenterVO> dcs = _dcDao.listAll();
        Collections.shuffle(dcs);
        return dcs.get(0).getId();
    }

    @Override
    public List<SwiftVO> listSwift(ListSwiftCmd cmd) {
        if (cmd.getId() == null) {
            return _swiftDao.listAll();
        } else {
            List<SwiftVO> list = new ArrayList<SwiftVO>();
            SwiftVO swift = _swiftDao.findById(cmd.getId());
            list.add(swift);
            return list;
        }
    }


    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stop Swift Manager");
        }
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring Swift Manager : " + name);
        }

        _name = name;

        return true;
    }

    protected SwiftManagerImpl() {
    }
}
