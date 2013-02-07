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
package com.cloud.alert;

import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.consoleproxy.ConsoleProxyAlertEventArgs;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.dao.ConsoleProxyDao;

@Component
@Local(value=AlertAdapter.class)
public class ConsoleProxyAlertAdapter extends AdapterBase implements AlertAdapter {

    private static final Logger s_logger = Logger.getLogger(ConsoleProxyAlertAdapter.class);

    @Inject private AlertManager _alertMgr;
    @Inject private DataCenterDao _dcDao;
    @Inject private ConsoleProxyDao _consoleProxyDao;

    public void onProxyAlert(Object sender, ConsoleProxyAlertEventArgs args) {
        if(s_logger.isDebugEnabled())
            s_logger.debug("received console proxy alert");

        DataCenterVO dc = _dcDao.findById(args.getZoneId());
        ConsoleProxyVO proxy = args.getProxy();
        if(proxy == null)
            proxy = _consoleProxyDao.findById(args.getProxyId());

        switch(args.getType()) {
        case ConsoleProxyAlertEventArgs.PROXY_CREATED :
            if(s_logger.isDebugEnabled())
                s_logger.debug("New console proxy created, zone: " + dc.getName() + ", proxy: " + 
                        proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() + ", private IP: " + 
                        proxy.getPrivateIpAddress());
            break;

        case ConsoleProxyAlertEventArgs.PROXY_UP :
            if(s_logger.isDebugEnabled())
                s_logger.debug("Console proxy is up, zone: " + dc.getName() + ", proxy: " + 
                        proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() + ", private IP: " + 
                        proxy.getPrivateIpAddress());

            _alertMgr.sendAlert(
                    AlertManager.ALERT_TYPE_CONSOLE_PROXY,
                    args.getZoneId(),
                    proxy.getPodIdToDeployIn(),
                    "Console proxy up in zone: " + dc.getName() + ", proxy: " + proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() 
                    + ", private IP: " + (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()),
                    "Console proxy up (zone " + dc.getName() + ")" 	
                    );
            break;

        case ConsoleProxyAlertEventArgs.PROXY_DOWN :
            if(s_logger.isDebugEnabled())
                s_logger.debug("Console proxy is down, zone: " + dc.getName() + ", proxy: " + 
                        proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() + ", private IP: " + 
                        (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()));

            _alertMgr.sendAlert(
                    AlertManager.ALERT_TYPE_CONSOLE_PROXY,
                    args.getZoneId(),
                    proxy.getPodIdToDeployIn(),
                    "Console proxy down in zone: " + dc.getName() + ", proxy: " + proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() 
                    + ", private IP: " + (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()),
                    "Console proxy down (zone " + dc.getName() + ")" 	
                    );
            break;

        case ConsoleProxyAlertEventArgs.PROXY_REBOOTED :
            if(s_logger.isDebugEnabled())
                s_logger.debug("Console proxy is rebooted, zone: " + dc.getName() + ", proxy: " + 
                        proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() + ", private IP: " + 
                        (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()));

            _alertMgr.sendAlert(
                    AlertManager.ALERT_TYPE_CONSOLE_PROXY,
                    args.getZoneId(),
                    proxy.getPodIdToDeployIn(),
                    "Console proxy rebooted in zone: " + dc.getName() + ", proxy: " + proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() 
                    + ", private IP: " + (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()),
                    "Console proxy rebooted (zone " + dc.getName() + ")" 	
                    );
            break;

        case ConsoleProxyAlertEventArgs.PROXY_CREATE_FAILURE :
            if(s_logger.isDebugEnabled())
                s_logger.debug("Console proxy creation failure, zone: " + dc.getName() + ", proxy: " + 
                        proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() + ", private IP: " + 
                        (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()));

            _alertMgr.sendAlert(
                    AlertManager.ALERT_TYPE_CONSOLE_PROXY,
                    args.getZoneId(),
                    proxy.getPodIdToDeployIn(),
                    "Console proxy creation failure. zone: " + dc.getName() + ", proxy: " + proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() 
                    + ", private IP: " + (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()) 
                    + ", error details: " + args.getMessage(),
                    "Console proxy creation failure (zone " + dc.getName() + ")"
                    );
            break;

        case ConsoleProxyAlertEventArgs.PROXY_START_FAILURE :
            if(s_logger.isDebugEnabled())
                s_logger.debug("Console proxy startup failure, zone: " + dc.getName() + ", proxy: " + 
                        proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() + ", private IP: " + 
                        (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()));

            _alertMgr.sendAlert(
                    AlertManager.ALERT_TYPE_CONSOLE_PROXY,
                    args.getZoneId(),
                    proxy.getPodIdToDeployIn(),
                    "Console proxy startup failure. zone: " + dc.getName() + ", proxy: " + proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() 
                    + ", private IP: " + (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()) 
                    + ", error details: " + args.getMessage(),
                    "Console proxy startup failure (zone " + dc.getName() + ")" 	
                    );
            break;

        case ConsoleProxyAlertEventArgs.PROXY_FIREWALL_ALERT :
            if(s_logger.isDebugEnabled())
                s_logger.debug("Console proxy firewall alert, zone: " + dc.getName() + ", proxy: " + 
                        proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() + ", private IP: " + 
                        (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()));

            _alertMgr.sendAlert(
                    AlertManager.ALERT_TYPE_CONSOLE_PROXY,
                    args.getZoneId(),
                    proxy.getPodIdToDeployIn(),
                    "Failed to open console proxy firewall port. zone: " + dc.getName() + ", proxy: " + proxy.getHostName() 
                    + ", public IP: " + proxy.getPublicIpAddress() 
                    + ", private IP: " + (proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress()),
                    "Console proxy alert (zone " + dc.getName() + ")"	
                    );
            break;

        case ConsoleProxyAlertEventArgs.PROXY_STORAGE_ALERT :
            if(s_logger.isDebugEnabled())
                s_logger.debug("Console proxy storage alert, zone: " + dc.getName() + ", proxy: " + 
                        proxy.getHostName() + ", public IP: " + proxy.getPublicIpAddress() + ", private IP: " + 
                        proxy.getPrivateIpAddress() + ", message: " + args.getMessage());

            _alertMgr.sendAlert(
                    AlertManager.ALERT_TYPE_STORAGE_MISC,
                    args.getZoneId(),
                    proxy.getPodIdToDeployIn(),
                    "Console proxy storage issue. zone: " + dc.getName() + ", message: " + args.getMessage(),
                    "Console proxy alert (zone " + dc.getName() + ")"
                    );
            break;
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {

        if (s_logger.isInfoEnabled())
            s_logger.info("Start configuring console proxy alert manager : " + name);

        try {
            SubscriptionMgr.getInstance().subscribe(ConsoleProxyManager.ALERT_SUBJECT, this, "onProxyAlert");
        } catch (SecurityException e) {
            throw new ConfigurationException("Unable to register console proxy event subscription, exception: " + e);
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Unable to register console proxy event subscription, exception: " + e);
        }

        return true;
    }
}
