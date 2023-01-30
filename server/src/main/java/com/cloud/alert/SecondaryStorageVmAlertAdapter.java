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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.storage.secondary.SecStorageVmAlertEventArgs;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.dao.SecondaryStorageVmDao;

@Component
public class SecondaryStorageVmAlertAdapter extends AdapterBase implements AlertAdapter {

    private static final Logger s_logger = Logger.getLogger(SecondaryStorageVmAlertAdapter.class);

    @Inject
    private AlertManager _alertMgr;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private SecondaryStorageVmDao _ssvmDao;

    public void onSSVMAlert(Object sender, SecStorageVmAlertEventArgs args) {
        if (s_logger.isDebugEnabled())
            s_logger.debug("received secondary storage vm alert");

        DataCenterVO dc = _dcDao.findById(args.getZoneId());
        SecondaryStorageVmVO secStorageVm = args.getSecStorageVm();
        if (secStorageVm == null && args.getSecStorageVmId() != 0)
            secStorageVm = _ssvmDao.findById(args.getSecStorageVmId());

        if (secStorageVm == null && args.getType() != SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE) {
            throw new CloudRuntimeException("Invalid alert arguments, secStorageVm must be set");
        }

        String secStorageVmHostName = "";
        String secStorageVmPublicIpAddress = "";
        String secStorageVmPrivateIpAddress = "N/A";
        Long secStorageVmPodIdToDeployIn = null;

        if (secStorageVm != null) {
            secStorageVmHostName = secStorageVm.getHostName();
            secStorageVmPublicIpAddress = secStorageVm.getPublicIpAddress();
            secStorageVmPrivateIpAddress = secStorageVm.getPrivateIpAddress() == null ? "N/A" : secStorageVm.getPrivateIpAddress();
            secStorageVmPodIdToDeployIn = secStorageVm.getPodIdToDeployIn();
        }
        String message = "";
        String zoneSecStorageVmPrivateAndPublicIp = String.format("zone [%s], secStorageVm [%s], public IP [%s], private IP [%s].", dc.getName(), secStorageVmHostName,
                secStorageVmPublicIpAddress, secStorageVmPrivateIpAddress);
        String errorDetails = " Error details: " + args.getMessage();
        String zone = String.format("(zone %s)", dc.getName());

        switch (args.getType()) {
            case SecStorageVmAlertEventArgs.SSVM_CREATED:
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("New secondary storage vm created in " + zoneSecStorageVmPrivateAndPublicIp);
                }
                break;

            case SecStorageVmAlertEventArgs.SSVM_UP:
                message = "Secondary Storage Vm is up in " + zoneSecStorageVmPrivateAndPublicIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_SSVM, args.getZoneId(), secStorageVmPodIdToDeployIn, message, "Secondary Storage Vm up " + zone);
                break;

            case SecStorageVmAlertEventArgs.SSVM_DOWN:
                message = "Secondary Storage Vm is down in " + zoneSecStorageVmPrivateAndPublicIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_SSVM, args.getZoneId(), secStorageVmPodIdToDeployIn, message, "Secondary Storage Vm down " + zone);
                break;

            case SecStorageVmAlertEventArgs.SSVM_REBOOTED:
                message = "Secondary Storage Vm rebooted in " + zoneSecStorageVmPrivateAndPublicIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_SSVM, args.getZoneId(), secStorageVmPodIdToDeployIn, message, "Secondary Storage Vm rebooted " + zone);
                break;

            case SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE:
                message = String.format("Secondary Storage Vm creation failure in zone [%s].", dc.getName());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_SSVM, args.getZoneId(), null, message + errorDetails,
                        "Secondary Storage Vm creation failure " + zone);
                break;

            case SecStorageVmAlertEventArgs.SSVM_START_FAILURE:
                message = "Secondary Storage Vm startup failure in " + zoneSecStorageVmPrivateAndPublicIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(message);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_SSVM, args.getZoneId(), secStorageVmPodIdToDeployIn, message + errorDetails,
                        "Secondary Storage Vm startup failure " + zone);
                break;

            case SecStorageVmAlertEventArgs.SSVM_FIREWALL_ALERT:
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Secondary Storage Vm firewall alert, " + zoneSecStorageVmPrivateAndPublicIp);
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_SSVM, args.getZoneId(), secStorageVmPodIdToDeployIn, "Failed to open secondary storage vm firewall port. "
                                + zoneSecStorageVmPrivateAndPublicIp, "Secondary Storage Vm alert " + zone);
                break;

            case SecStorageVmAlertEventArgs.SSVM_STORAGE_ALERT:
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Secondary Storage Vm storage alert, " + zoneSecStorageVmPrivateAndPublicIp + ", message: " + args.getMessage());
                }

                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_STORAGE_MISC, args.getZoneId(), secStorageVmPodIdToDeployIn,
                        "Secondary Storage Vm storage issue. zone: " + dc.getName() + ", message: " + args.getMessage(), "Secondary Storage Vm alert " + zone);
                break;
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        if (s_logger.isInfoEnabled())
            s_logger.info("Start configuring secondary storage vm alert manager : " + name);

        try {
            SubscriptionMgr.getInstance().subscribe(SecondaryStorageVmManager.ALERT_SUBJECT, this, "onSSVMAlert");
        } catch (SecurityException e) {
            throw new ConfigurationException("Unable to register secondary storage vm event subscription, exception: " + e);
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Unable to register secondary storage vm event subscription, exception: " + e);
        }

        return true;
    }
}
