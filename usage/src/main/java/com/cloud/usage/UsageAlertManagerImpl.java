// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.usage;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.alert.AlertManager;
import com.cloud.alert.AlertVO;
import com.cloud.alert.dao.AlertDao;
import com.cloud.utils.component.ManagerBase;
import java.util.HashSet;
import java.util.Set;
import org.apache.cloudstack.utils.mailing.MailAddress;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;

@Component
public class UsageAlertManagerImpl extends ManagerBase implements AlertManager {
    private static final Logger s_logger = Logger.getLogger(UsageAlertManagerImpl.class.getName());

    private String senderAddress;
    protected SMTPMailSender mailSender;
    protected String[] recipients;

    @Inject
    private AlertDao _alertDao;
    @Inject
    private ConfigurationDao _configDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration("management-server", params);

        senderAddress = configs.get("alert.email.sender");
        String emailAddressList = configs.get("alert.email.addresses");
        recipients = null;
        if (emailAddressList != null) {
            recipients = emailAddressList.split(",");
        }

        String namespace = "alert.smtp";

        mailSender = new SMTPMailSender(configs, namespace);

        return true;
    }

    @Override
    public void clearAlert(AlertType alertType, long dataCenterId, long podId) {
        try {
            clearAlert(alertType.getType(), dataCenterId, podId);
        } catch (Exception ex) {
            s_logger.error("Problem clearing email alert", ex);
        }
    }

    @Override
    public void sendAlert(AlertType alertType, long dataCenterId, Long podId, String subject, String content) {
        AlertVO alert = null;
        if ((alertType != AlertManager.AlertType.ALERT_TYPE_HOST) && (alertType != AlertManager.AlertType.ALERT_TYPE_USERVM)
                && (alertType != AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER) && (alertType != AlertManager.AlertType.ALERT_TYPE_CONSOLE_PROXY)
                && (alertType != AlertManager.AlertType.ALERT_TYPE_SSVM) && (alertType != AlertManager.AlertType.ALERT_TYPE_STORAGE_MISC)
                && (alertType != AlertManager.AlertType.ALERT_TYPE_MANAGMENT_NODE)) {
            alert = _alertDao.getLastAlert(alertType.getType(), dataCenterId, podId);
        }
        if (alert == null) {
            AlertVO newAlert = new AlertVO();
            newAlert.setType(alertType.getType());
            newAlert.setSubject(subject);
            newAlert.setPodId(podId);
            newAlert.setDataCenterId(dataCenterId);
            newAlert.setSentCount(1);
            newAlert.setLastSent(new Date());
            newAlert.setName(alertType.getName());
            _alertDao.persist(newAlert);
        } else {
            s_logger.debug(String.format("Have already sent: [%s] emails for alert type [%s] -- skipping send email.", alert.getSentCount(), alertType));
            return;
        }

        SMTPMailProperties mailProps = new SMTPMailProperties();
        mailProps.setSender(new MailAddress(senderAddress));
        mailProps.setSubject(subject);
        mailProps.setContent(content);
        mailProps.setContentType("text/plain");

        Set<MailAddress> addresses = new HashSet<>();
        for (String recipient : recipients) {
            addresses.add(new MailAddress(recipient));
        }

        mailProps.setRecipients(addresses);
        mailSender.sendMail(mailProps);
    }

    public void clearAlert(short alertType, long dataCenterId, Long podId) {
        if (alertType != -1) {
            AlertVO alert = _alertDao.getLastAlert(alertType, dataCenterId, podId);
            if (alert != null) {
                AlertVO updatedAlert = _alertDao.createForUpdate();
                updatedAlert.setResolved(new Date());
                _alertDao.update(alert.getId(), updatedAlert);
            }
        }
    }

    @Override
    public void recalculateCapacity() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean generateAlert(AlertType alertType, long dataCenterId, Long podId, String msg) {
        try {
            sendAlert(alertType, dataCenterId, podId, msg, msg);
            return true;
        } catch (Exception ex) {
            s_logger.warn("Failed to generate an alert of type=" + alertType + "; msg=" + msg, ex);
            return false;
        }
    }
}
