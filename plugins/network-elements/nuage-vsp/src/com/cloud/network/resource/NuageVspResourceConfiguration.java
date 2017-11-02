//
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
//

package com.cloud.network.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.naming.ConfigurationException;

import net.nuage.vsp.acs.client.api.model.NuageVspUser;
import net.nuage.vsp.acs.client.api.model.VspHost;
import net.nuage.vsp.acs.client.common.NuageVspApiVersion;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.common.base.Preconditions;

import com.cloud.util.NuageVspUtil;

public class NuageVspResourceConfiguration {
    private static final String NAME = "name";
    private static final String GUID = "guid";
    private static final String ZONE_ID = "zoneid";
    private static final String HOST_NAME = "hostname";
    private static final String CMS_USER = "cmsuser";
    private static final String CMS_USER_PASSWORD = "cmsuserpass";
    private static final String PORT = "port";
    private static final String API_VERSION = "apiversion";
    private static final String API_RELATIVE_PATH = "apirelativepath";
    private static final String RETRY_COUNT = "retrycount";
    private static final String RETRY_INTERVAL = "retryinterval";
    private static final String NUAGE_VSP_CMS_ID = "nuagevspcmsid";

    private static final String CMS_USER_ENTEPRISE_NAME = "CSP";

    private String _name;
    private String _guid;
    private String _zoneId;
    private String _hostName;
    private String _cmsUser;
    private String _cmsUserPassword;
    private String _port;
    private String _apiVersion;
    private String _apiRelativePath;
    private String _retryCount;
    private String _retryInterval;
    private String _nuageVspCmsId;

    public String name() {
        return _name;
    }

    public String guid() {
        return this._guid;
    }

    public NuageVspResourceConfiguration guid(String guid) {
        this._guid = guid;
        return this;
    }

    public String zoneId() {
        return this._zoneId;
    }

    public NuageVspResourceConfiguration zoneId(String zoneId) {
        this._zoneId = zoneId;
        return this;
    }

    public String hostName() {
        return this._hostName;
    }

    public NuageVspResourceConfiguration hostName(String hostName) {
        this._hostName = hostName;
        this._name = "Nuage VSD - " + _hostName;
        return this;
    }

    public String cmsUser() {
        return this._cmsUser;
    }

    public NuageVspResourceConfiguration cmsUser(String cmsUser) {
        this._cmsUser = cmsUser;
        return this;
    }

    public String cmsUserPassword() {
        return this._cmsUserPassword;
    }

    public NuageVspResourceConfiguration cmsUserPassword(String cmsUserPassword) {
        this._cmsUserPassword = cmsUserPassword;
        return this;
    }

    public String port() {
        return this._port;
    }

    public NuageVspResourceConfiguration port(String port) {
        this._port = port;
        return this;
    }

    public String apiVersion() {
        return this._apiVersion;
    }

    public NuageVspResourceConfiguration apiVersion(String apiVersion) {
        this._apiVersion = apiVersion;
        return this;
    }

    public String apiRelativePath() {
        return this._apiRelativePath;
    }

    public NuageVspResourceConfiguration apiRelativePath(String apiRelativePath) {
        this._apiRelativePath = apiRelativePath;
        return this;
    }

    public String retryCount() {
        return this._retryCount;
    }

    public NuageVspResourceConfiguration retryCount(String retryCount) {
        this._retryCount = retryCount;
        return this;
    }

    public String retryInterval() {
        return this._retryInterval;
    }

    public NuageVspResourceConfiguration retryInterval(String retryInterval) {
        this._retryInterval = retryInterval;
        return this;
    }

    public String nuageVspCmsId() {
        return this._nuageVspCmsId;
    }

    public NuageVspResourceConfiguration nuageVspCmsId(String nuageVspCmsId) {
        this._nuageVspCmsId = nuageVspCmsId;
        return this;
    }

    public String getRootPath(){
        return "https://" + _hostName + ":" + _port + "/nuage";
    }

    public String getApiPath() {
            return "https://" + _hostName + ":" + _port + "/nuage/api/" + _apiVersion;
    }

    public NuageVspApiVersion getApiVersion() throws ConfigurationException {
        try {
            if(_apiVersion != null) {
                return NuageVspApiVersion.fromString(_apiVersion);
            }
            return null;
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Incorrect API version");
        }
    }

    public Map<String, String> build() {
        Map<String, String> map = new HashMap<>();
        putIfPresent(map, GUID, _guid);
        putIfPresent(map, ZONE_ID, _zoneId);
        putIfPresent(map, HOST_NAME, _hostName);
        putIfPresent(map, CMS_USER, _cmsUser);
        putIfPresent(map, CMS_USER_PASSWORD, _cmsUserPassword);
        putIfPresent(map, PORT, _port);
        putIfPresent(map, API_VERSION, _apiVersion);
        putIfPresent(map, API_RELATIVE_PATH, _apiRelativePath);
        putIfPresent(map, RETRY_COUNT, _retryCount);
        putIfPresent(map, RETRY_INTERVAL, _retryInterval);
        putIfPresent(map, NUAGE_VSP_CMS_ID, _nuageVspCmsId);
        return  map;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        Preconditions.checkNotNull(map);
        Preconditions.checkNotNull(key);

        if (value != null) {
            map.put(key, value);
        }
    }

    public static NuageVspResourceConfiguration fromConfiguration(Map<String, ?> configuration) {
        return new NuageVspResourceConfiguration()
                .guid((String)configuration.get(GUID))
                .zoneId((String)configuration.get(ZONE_ID))
                .hostName((String)configuration.get(HOST_NAME))
                .cmsUser((String)configuration.get(CMS_USER))
                .cmsUserPassword((String)configuration.get(CMS_USER_PASSWORD))
                .port((String)configuration.get(PORT))
                .apiVersion((String)configuration.get(API_VERSION))
                .apiRelativePath((String)configuration.get(API_RELATIVE_PATH))
                .retryCount((String)configuration.get(RETRY_COUNT))
                .retryInterval((String)configuration.get(RETRY_INTERVAL))
                .nuageVspCmsId((String)configuration.get(NUAGE_VSP_CMS_ID));
    }

    private void verifyNotNull(String name, String value) throws ConfigurationException {
        if (value == null) {
            throw new ConfigurationException("Unable to find " + name);
        }
    }

    private void verifyNotEmpty(String name, String value) throws ConfigurationException {
        if (StringUtils.isEmpty(value)) {
            throw new ConfigurationException("Unable to find " + name);
        }
    }

    private int verifyInRange(String name, String value, int min, int max) throws ConfigurationException {
        verifyNotEmpty(name, value);

        int parsedValue;
        try {
            parsedValue = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(name + " has to be between " + min + " and " + max);
        }
        if ((parsedValue < min) || (parsedValue > max)) {
            throw new ConfigurationException(name + " has to be between " + min + " and " + max);
        }
        return parsedValue;
    }

    public void validate() throws ConfigurationException {
        verifyNotNull("name", _name);
        verifyNotNull("guid", _guid);
        verifyNotNull("zone", _zoneId);
        verifyNotNull("hostname", _hostName);
        verifyNotNull("CMS username", _cmsUser);
        verifyNotNull("CMS password", _cmsUserPassword);
        verifyNotEmpty("API version", _apiVersion);

        try {
            NuageVspApiVersion.fromString(_apiVersion);
        } catch(IllegalArgumentException e) {
            throw new ConfigurationException("Incorrect API version");
        }

        verifyNotEmpty("number of retries", _retryCount);
        verifyNotEmpty("retry interval", _retryInterval);
    }

    public int parseRetryCount() throws ConfigurationException {
        return verifyInRange("Number of retries", _retryCount, 1, 10);
    }

    public int parseRetryInterval() throws ConfigurationException {
        return verifyInRange("Retry interval", _retryInterval, 1, 10000);
    }

    public VspHost buildVspHost() throws ConfigurationException {
        return new VspHost.Builder()
                .cmsUser(new NuageVspUser(CMS_USER_ENTEPRISE_NAME, _cmsUser,  NuageVspUtil.decodePassword(_cmsUserPassword)))
                .apiVersion(getApiVersion())
                .restRelativePath(getApiPath())
                .rootPath(getRootPath())
                .nuageVspCmsId(_nuageVspCmsId)
                .noofRetry(parseRetryCount())
                .retryInterval(parseRetryInterval())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof NuageVspResourceConfiguration)) {
            return false;
        }

        NuageVspResourceConfiguration that = (NuageVspResourceConfiguration) o;

        return super.equals(that)
                && Objects.equals(_name, that._name)
                && Objects.equals(_guid, that._guid)
                && Objects.equals(_zoneId, that._zoneId)
                && Objects.equals(_hostName, that._hostName)
                && Objects.equals(_cmsUser, that._cmsUser)
                && Objects.equals(_cmsUserPassword, that._cmsUserPassword)
                && Objects.equals(_port, that._port)
                && Objects.equals(_apiVersion, that._apiVersion)
                && Objects.equals(_apiRelativePath, that._apiRelativePath)
                && Objects.equals(_retryCount, that._retryCount)
                && Objects.equals(_retryInterval, that._retryInterval)
                && Objects.equals(_nuageVspCmsId, that._nuageVspCmsId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(_name)
                .append(_guid)
                .append(_zoneId)
                .append(_hostName)
                .append(_cmsUser)
                .append(_cmsUserPassword)
                .append(_port)
                .append(_apiVersion)
                .append(_apiRelativePath)
                .append(_retryCount)
                .append(_retryInterval)
                .append(_nuageVspCmsId)
                .toHashCode();
    }

    @Override public String toString() {
        return new ToStringBuilder(this)
                .append("_name", _name)
                .append("_guid", _guid)
                .append("_zoneId", _zoneId)
                .append("_hostName", _hostName)
                .append("_cmsUser", _cmsUser)
                .append("_cmsUserPassword", _cmsUserPassword)
                .append("_port", _port)
                .append("_apiVersion", _apiVersion)
                .append("_apiRelativePath", _apiRelativePath)
                .append("_retryCount", _retryCount)
                .append("_retryInterval", _retryInterval)
                .append("_nuageVspCmsId", _nuageVspCmsId)
                .toString();
    }
}
