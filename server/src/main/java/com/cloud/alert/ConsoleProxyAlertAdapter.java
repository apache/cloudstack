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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.alert.AlertService;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.consoleproxy.ConsoleProxyAlertEventArgs;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.dao.ConsoleProxyDao;

@Component
public class ConsoleProxyAlertAdapter extends AdapterBase implements AlertAdapter {

    private static final Logger s_logger = Logger.getLogger(ConsoleProxyAlertAdapter.class);

    @Inject
    private AlertManager _alertMgr;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private ConsoleProxyDao _consoleProxyDao;

    public void onProxyAlert(Object sender, ConsoleProxyAlertEventArgs args) {
        if (s_logger.isDebugEnabled())
            s_logger.debug("received console proxy alert");

        DataCenterVO dc = _dcDao.findById(args.getZoneId());
        ConsoleProxyVO proxy = args.getProxy();
        //FIXME - Proxy can be null in case of creation failure. Have a better fix than checking for != 0
        if (proxy == null && args.getProxyId() != 0)
            proxy = _consoleProxyDao.findById(args.getProxyId());

        if (proxy == null && args.getType() != ConsoleProxyAlertEventArgs.PROXY_CREATE_FAILURE) {
            throw new CloudRuntimeException("Invalid alert arguments, proxy must be set");
        }

        String proxyHostName = "";
        String proxyPublicIpAddress = "";
        String proxyPrivateIpAddress = "N/A";
        Long proxyPodIdToDeployIn = null;

        if (proxy != null) {
            proxyHostName = proxy.getHostName();
            proxyPublicIpAddress = proxy.getPublicIpAddress();
            proxyPrivateIpAddress = proxy.getPrivateIpAddress() == null ? "N/A" : proxy.getPrivateIpAddress();
            proxyPodIdToDeployIn = proxy.getPodIdToDeployIn();
        }

        String message = "";
        String zoneProxyPublicAndPrivateIp = String.format("zone [%s], proxy [%s], public IP [%s], private IP [%s].", dc.getName(), proxyHostName, proxyPublicIpAddress,
                proxyPrivateIpAddress);
        String zone = String.format("(zone %s)", dc.getName());
        String errorDetails = " Error details: " + args.getMessage();


        switch (args.getType()) {
            case ConsoleProxyAlertEventArgs.PROXY_CREATED:
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("New console proxy created, " + zoneProxyPublicAndPrivateIp);
                }
                break;

            case ConsoleProxyAlertEventArgs.PROXY_UP:
                message = "Console proxy up in " + zoneProxyPublicAndPrivateIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_CONSOLE_PROXY, args.getZoneId(), proxyPodIdToDeployIn, message, "Console proxy up " + zone);
                break;

            case ConsoleProxyAlertEventArgs.PROXY_DOWN:
                message = "Console proxy is down in " + zoneProxyPublicAndPrivateIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_CONSOLE_PROXY, args.getZoneId(), proxyPodIdToDeployIn, message, "Console proxy down " + zone);
                break;

            case ConsoleProxyAlertEventArgs.PROXY_REBOOTED:
                message = "Console proxy is rebooted in " + zoneProxyPublicAndPrivateIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_CONSOLE_PROXY, args.getZoneId(), proxyPodIdToDeployIn, message, "Console proxy rebooted " + zone);
                break;

            case ConsoleProxyAlertEventArgs.PROXY_CREATE_FAILURE:
                message = String.format("Console proxy creation failure. Zone [%s].", dc.getName());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_CONSOLE_PROXY, args.getZoneId(), null, message + errorDetails, "Console proxy creation failure " + zone);
                break;

            case ConsoleProxyAlertEventArgs.PROXY_START_FAILURE:
                message = "Console proxy startup failure in " + zoneProxyPublicAndPrivateIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_CONSOLE_PROXY, args.getZoneId(), proxyPodIdToDeployIn, message + errorDetails,
                        "Console proxy startup failure " + zone);
                break;

            case ConsoleProxyAlertEventArgs.PROXY_FIREWALL_ALERT:
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Console proxy firewall alert, " + zoneProxyPublicAndPrivateIp);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_CONSOLE_PROXY, args.getZoneId(), proxyPodIdToDeployIn, "Failed to open console proxy firewall port. " +
                        zoneProxyPublicAndPrivateIp, "Console proxy alert " + zone);
                break;

            case ConsoleProxyAlertEventArgs.PROXY_STORAGE_ALERT:
                message = zoneProxyPublicAndPrivateIp + ", message: " + args.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Console proxy storage alert, " + message);
                }
                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_STORAGE_MISC, args.getZoneId(), proxyPodIdToDeployIn, "Console proxy storage issue. " + message,
                        "Console proxy alert " + zone);
                break;
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

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
