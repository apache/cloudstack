/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.hypervisor.xen.discoverer;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostEnvironment;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase;
import com.cloud.hypervisor.xen.resource.XenServer56FP1PremiumResource;
import com.cloud.hypervisor.xen.resource.XenServer56Resource;
import com.cloud.maint.Version;
import com.cloud.resource.Discoverer;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostPatch;
import com.xensource.xenapi.PoolPatch;
import com.xensource.xenapi.Types.XenAPIException;

@Local(value=Discoverer.class)
public class XenServerDiscoverer extends XcpServerDiscoverer {
    private static final Logger s_logger = Logger.getLogger(XenServerDiscoverer.class);
    private String _minProductVersion;
    private String _minXapiVersion;
    private String _minXenVersion;
    private String _maxProductVersion;
    private String _maxXapiVersion;
    private String _maxXenVersion;
    private List<Pair<String, String>> _requiredPatches;

    private boolean _setupMultipath;
    
    protected XenServerDiscoverer() {
    }
    
    
    @Override
    protected CitrixResourceBase createServerResource(long dcId, Long podId, Host.Record record) {
        String prodBrand = record.softwareVersion.get("product_brand").trim();
        String prodVersion = record.softwareVersion.get("product_version").trim();
               
        if(prodBrand.equals("XenServer")){
            if(prodVersion.equals("5.6.0")) {
             	return new XenServer56Resource();
            } else if(prodVersion.equals("5.6.100")) { 
                return new XenServer56FP1PremiumResource();
            }
        }    
        String msg = "Only support XenServer 5.6 and XenServer 5.6 FP1, but this one is " + prodBrand + " " + prodVersion;
        _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
        s_logger.debug(msg);
        throw new RuntimeException(msg);
    }
    
    
    protected boolean checkServer(Connection conn, long dcId, Long podId, Host host, Host.Record record) {
        String prodVersion = record.softwareVersion.get("product_version");
        String xapiVersion = record.softwareVersion.get("xapi");
        String xenVersion = record.softwareVersion.get("xen");
        
        String prodBrand = record.softwareVersion.get("product_brand");
        
        if(!prodBrand.equals("XenServer")) {
            String msg = "Do not support product brand " + prodBrand;
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
            s_logger.debug(msg);
            throw new RuntimeException(msg);
        }
        
        if (Version.compare(_minProductVersion, prodVersion) > 0) {
            String msg = "Unable to add host " + record.address + " because the product version " + prodVersion + " is lower than the minimum " + _minProductVersion;
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
            s_logger.debug(msg);
            return false;
        }
        
        if (_maxProductVersion != null && Version.compare(prodVersion, _maxProductVersion) > 0) {
            String msg = "Unable to add host " + record.address + " because the product version " + prodVersion + " is higher than the maximum " + _maxProductVersion;
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
            s_logger.debug(msg);
            return false;
        }
        
        if (Version.compare(_minXenVersion, xenVersion) > 0) {
            String msg = "Unable to add host " + record.address + " because the xen version " + xenVersion + " is lower than the minimum " + _minXenVersion;
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
            s_logger.debug(msg);
            return false;
        }
        
        if (_maxXenVersion != null && Version.compare(xenVersion, _maxXenVersion) > 0) {
            String msg = "Unable to add host " + record.address + " because the xen version " + xenVersion + " is higher than the maximum " + _maxXenVersion;
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
            s_logger.debug(msg);
            return false;
        }
        
        if (Version.compare(_minXapiVersion, xapiVersion) > 0) {
            String msg = "Unable to add host " + record.address + " because the xapi version " + xapiVersion + " is lower than the minimum " + _minXapiVersion;
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
            s_logger.debug(msg);
            return false;
        }
        
        if (_maxXapiVersion != null && Version.compare(xapiVersion, _maxXapiVersion) > 0) {
            String msg = "Unable to add host " + record.address + " because the xapi version " + xapiVersion + " is higher than the maximum " + _maxXapiVersion;
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
            s_logger.debug(msg);
            return false;
        }
        

        
        if(Version.compare(prodVersion, _minProductVersion) >= 0)
            return true;
        
        
        Set<Ternary<String, String, Boolean>> required = new HashSet<Ternary<String, String, Boolean>>(_requiredPatches.size());
        for (Pair<String, String> req : _requiredPatches) {
            required.add(new Ternary<String, String, Boolean>(req.first(), req.second(), false));
        }
        
        try {
            Set<HostPatch> patches = host.getPatches(conn);
            if (patches != null) {
                for (HostPatch patch : patches) {
                    HostPatch.Record hpr = patch.getRecord(conn);
                    PoolPatch.Record ppr = hpr.poolPatch.getRecord(conn);
                    
                    for (Ternary<String, String, Boolean> req: required) {
                        if (hpr.nameLabel != null && hpr.nameLabel.contains(req.first()) &&
                            hpr.version != null && hpr.version.contains(req.second())) {
                            req.third(true);
                            break;
                        }
                        
                        if (ppr.nameLabel != null && ppr.nameLabel.contains(req.first()) &&
                            ppr.version != null && ppr.version.contains(req.second())) {
                            req.third(true);
                            break;
                        }
                    }
                }
            }
            for (Ternary<String, String, Boolean> req : required) {
                if (!req.third()) {
                    String msg = "Unable to add host " + record.address + " because unable to find the following patch: " + req.first() + " version " + req.second();
                    s_logger.warn(msg);
                    _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
                    return false;
                }
            }
        } catch (XenAPIException e) {
            s_logger.warn("Unable to add " + record.address, e);
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + record.address , "Error is " + e.getMessage());
            return false;
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to add " + record.address, e);
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + record.address, "Error is " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    @Override
    protected void serverConfig() {
	    _minXenVersion = _params.get(Config.XenMinVersion.key());
	    if (_minXenVersion == null) {
	        _minXenVersion = "3.3.1";
	    }
	    
	    _minProductVersion = _params.get(Config.XenProductMinVersion.key());
	    if (_minProductVersion == null) {
	        _minProductVersion = "5.5.0";
	    }
	    
	    _minXapiVersion = _params.get(Config.XenXapiMinVersion.key());
	    if (_minXapiVersion == null) {
	        _minXapiVersion = "1.3";
	    }
   
        _maxXenVersion = _params.get(Config.XenMaxVersion.key());
        _maxProductVersion = _params.get(Config.XenProductMaxVersion.key());
        _maxXapiVersion = _params.get(Config.XenXapiMaxVersion.key());
        
        String value = _params.get(Config.XenSetupMultipath.key());
        _setupMultipath = Boolean.parseBoolean(value);
    }
    
    @Override
    public void processConnect(HostVO agent, StartupCommand cmd) throws ConnectionException {
    	super.processConnect(agent, cmd);

        if (!(cmd instanceof StartupRoutingCommand )) {
            return;
        }
    	
        long agentId = agent.getId();
        
        StartupRoutingCommand startup = (StartupRoutingCommand)cmd;
        if (startup.getHypervisorType() != HypervisorType.XenServer) {
            s_logger.debug("Not XenServer so moving on.");
            return;
        }
        
        HostVO host = _hostDao.findById(agentId);
        if (host.isSetup()) {
            return;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Setting up host " + agentId);
        }
        HostEnvironment env = new HostEnvironment();
        
        SetupCommand setup = new SetupCommand(env);
        if (_setupMultipath) {
            setup.setMultipathOn();
        }
        try {
            SetupAnswer answer = (SetupAnswer)_agentMgr.send(agentId, setup);
            if (answer != null && answer.getResult()) {
                host.setSetup(true);
                host.setLastPinged((System.currentTimeMillis()>>10) - 5 * 60 );
                _hostDao.update(host.getId(), host);
                if ( answer.needReconnect() ) {
                    throw new ConnectionException(false, "Reinitialize agent after setup.");
                }
                return;
            } else {
                s_logger.warn("Unable to setup agent " + agentId + " due to " + ((answer != null)?answer.getDetails():"return null"));
            }
        } catch (AgentUnavailableException e) {
            s_logger.warn("Unable to setup agent " + agentId + " because it became unavailable.", e);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Unable to setup agent " + agentId + " because it timed out", e);
        }
        throw new ConnectionException(true, "Reinitialize agent after setup.");
    }
}
