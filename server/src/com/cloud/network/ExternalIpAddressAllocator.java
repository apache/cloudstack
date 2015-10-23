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
package com.cloud.network;

import static com.cloud.utils.AutoCloseableUtil.closeAutoCloseable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.dc.dao.VlanDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = IpAddrAllocator.class)
public class ExternalIpAddressAllocator extends AdapterBase implements IpAddrAllocator {
    private static final Logger s_logger = Logger.getLogger(ExternalIpAddressAllocator.class);
    @Inject
    ConfigurationDao _configDao = null;
    @Inject
    IPAddressDao _ipAddressDao = null;
    @Inject
    VlanDao _vlanDao;
    private boolean _isExternalIpAllocatorEnabled = false;
    private String _externalIpAllocatorUrl = null;

    @Override
    public IpAddr getPrivateIpAddress(String macAddr, long dcId, long podId) {
        if (_externalIpAllocatorUrl == null || _externalIpAllocatorUrl.equalsIgnoreCase("")) {
            return new IpAddr();
        }
        String urlString = _externalIpAllocatorUrl + "?command=getIpAddr&mac=" + macAddr + "&dc=" + dcId + "&pod=" + podId;
        s_logger.debug("getIP:" + urlString);

        BufferedReader in = null;
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(30000);

            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                s_logger.debug(inputLine);
                String[] tokens = inputLine.split(",");
                if (tokens.length != 3) {
                    s_logger.debug("the return value should be: mac,netmask,gateway");
                    return new IpAddr();
                }
                return new IpAddr(tokens[0], tokens[1], tokens[2]);
            }

            return new IpAddr();
        } catch (MalformedURLException e) {
            throw new CloudRuntimeException("URL is malformed " + urlString, e);
        } catch (IOException e) {
            return new IpAddr();
        } finally {
            closeAutoCloseable(in, "closing buffered reader");
        }

    }

    @Override
    public IpAddr getPublicIpAddress(String macAddr, long dcId, long podId) {
        /*TODO: call API to get  ip address from external DHCP server*/
        return getPrivateIpAddress(macAddr, dcId, podId);
    }

    @Override
    public boolean releasePrivateIpAddress(String ip, long dcId, long podId) {
        /*TODO: call API to release the ip address from external DHCP server*/
        if (_externalIpAllocatorUrl == null || _externalIpAllocatorUrl.equalsIgnoreCase("")) {
            return false;
        }

        String urlString = _externalIpAllocatorUrl + "?command=releaseIpAddr&ip=" + ip + "&dc=" + dcId + "&pod=" + podId;

        s_logger.debug("releaseIP:" + urlString);
        BufferedReader in = null;
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(30000);

            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            return true;
        } catch (MalformedURLException e) {
            throw new CloudRuntimeException("URL is malformed " + urlString, e);
        } catch (IOException e) {
            return false;
        } finally {
            closeAutoCloseable(in, "buffered reader close");
        }
    }

    @Override
    public boolean releasePublicIpAddress(String ip, long dcId, long podId) {
        /*TODO: call API to release the ip address from external DHCP server*/
        return releasePrivateIpAddress(ip, dcId, podId);
    }

    @Override
    public boolean externalIpAddressAllocatorEnabled() {
        return _isExternalIpAllocatorEnabled;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _isExternalIpAllocatorEnabled = Boolean.parseBoolean(_configDao.getValue("direct.attach.network.externalIpAllocator.enabled"));
        _externalIpAllocatorUrl = _configDao.getValue("direct.attach.network.externalIpAllocator.url");
        _name = name;

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
