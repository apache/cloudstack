/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.feign.model;

import org.apache.cloudstack.storage.utils.Constants.ProtocolType;

public class OntapStorage {
    public static String _username;
    public static String _password;
    public static String _managementLIF;
    public static String _svmName;
    public static ProtocolType _protocolType;
    public static Boolean _isDisaggregated;

    public OntapStorage(String username, String password, String managementLIF, String svmName, ProtocolType protocolType, Boolean isDisaggregated) {
        _username = username;
        _password = password;
        _managementLIF = managementLIF;
        _svmName = svmName;
        _protocolType = protocolType;
        _isDisaggregated = isDisaggregated;
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(String username) {
        _username = username;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public String getManagementLIF() {
        return _managementLIF;
    }

    public void setManagementLIF(String managementLIF) {
        _managementLIF = managementLIF;
    }

    public String getSvmName() {
        return _svmName;
    }

    public void setSvmName(String svmName) {
        _svmName = svmName;
    }

    public ProtocolType getProtocol() {
        return _protocolType;
    }

    public void setProtocol(ProtocolType protocolType) {
        _protocolType = protocolType;
    }

    public Boolean getIsDisaggregated() {
        return _isDisaggregated;
    }

    public void setIsDisaggregated(Boolean isDisaggregated) {
        _isDisaggregated = isDisaggregated;
    }
}
