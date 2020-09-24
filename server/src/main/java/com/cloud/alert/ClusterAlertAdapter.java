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

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterNodeJoinEventArgs;
import com.cloud.cluster.ClusterNodeLeftEventArgs;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.events.EventArgs;
import com.cloud.utils.events.SubscriptionMgr;

@Component
public class ClusterAlertAdapter extends AdapterBase implements AlertAdapter {

    private static final Logger s_logger = Logger.getLogger(ClusterAlertAdapter.class);

    @Inject
    private AlertManager _alertMgr;
    @Inject
    private ManagementServerHostDao _mshostDao;

    public void onClusterAlert(Object sender, EventArgs args) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Receive cluster alert, EventArgs: " + args.getClass().getName());
        }

        if (args instanceof ClusterNodeJoinEventArgs) {
            onClusterNodeJoined(sender, (ClusterNodeJoinEventArgs)args);
        } else if (args instanceof ClusterNodeLeftEventArgs) {
            onClusterNodeLeft(sender, (ClusterNodeLeftEventArgs)args);
        } else {
            s_logger.error("Unrecognized cluster alert event");
        }
    }

    private void onClusterNodeJoined(Object sender, ClusterNodeJoinEventArgs args) {
        if (s_logger.isDebugEnabled()) {
            for (ManagementServerHostVO mshost : args.getJoinedNodes()) {
                s_logger.debug("Handle cluster node join alert, joined node: " + mshost.getServiceIP() + ", msid: " + mshost.getMsid());
            }
        }

        for (ManagementServerHostVO mshost : args.getJoinedNodes()) {
            if (mshost.getId() == args.getSelf().longValue()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Management server node " + mshost.getServiceIP() + " is up, send alert");
                }

                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_MANAGMENT_NODE, 0, new Long(0), "Management server node " + mshost.getServiceIP() + " is up", "");
                break;
            }
        }
    }

    private void onClusterNodeLeft(Object sender, ClusterNodeLeftEventArgs args) {

        if (s_logger.isDebugEnabled()) {
            for (ManagementServerHostVO mshost : args.getLeftNodes()) {
                s_logger.debug("Handle cluster node left alert, leaving node: " + mshost.getServiceIP() + ", msid: " + mshost.getMsid());
            }
        }

        for (ManagementServerHostVO mshost : args.getLeftNodes()) {
            if (mshost.getId() != args.getSelf().longValue()) {
                if (_mshostDao.increaseAlertCount(mshost.getId()) > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Detected management server node " + mshost.getServiceIP() + " is down, send alert");
                    }
                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_MANAGMENT_NODE, 0, new Long(0), "Management server node " + mshost.getServiceIP() + " is down",
                        "");
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Detected management server node " + mshost.getServiceIP() + " is down, but alert has already been set");
                    }
                }
            }
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring cluster alert manager : " + name);
        }

        try {
            SubscriptionMgr.getInstance().subscribe(ClusterManager.ALERT_SUBJECT, this, "onClusterAlert");
        } catch (SecurityException e) {
            throw new ConfigurationException("Unable to register cluster event subscription");
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Unable to register cluster event subscription");
        }

        return true;
    }
}
