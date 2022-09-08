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
package com.cloud.network.as;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AutoScaleCounter {
    public static class AutoScaleCounterType {
        private String _name;

        public static final AutoScaleCounterType Snmp = new AutoScaleCounterType(Counter.Source.SNMP.name());
        public static final AutoScaleCounterType Cpu = new AutoScaleCounterType(Counter.Source.CPU.name());
        public static final AutoScaleCounterType Memory = new AutoScaleCounterType(Counter.Source.MEMORY.name());
        public static final AutoScaleCounterType Netscaler = new AutoScaleCounterType(Counter.Source.NETSCALER.name());
        public static final AutoScaleCounterType VirtualRouter = new AutoScaleCounterType(Counter.Source.VIRTUALROUTER.name());

        public AutoScaleCounterType(String name) {
            _name = name;
        }

        public String getName() {
            return _name;
        }
    }

    public class AutoScaleCounterParam {
        @SerializedName("paramname")
        private String _paramName;

        @SerializedName("required")
        private Boolean _required;

        @SerializedName("isflag")
        private Boolean _isFlag;

        @SerializedName("description")
        private String _description;

        public AutoScaleCounterParam(String name, Boolean required, String description, Boolean flag) {
            this._paramName = name;
            this._required = required;
            this._description = description;
            this._isFlag = flag;
        }

        public String getParamName() {
            return _paramName;
        }

        public void setParamName(String paramName) {
            this._paramName = paramName;
        }

        public Boolean getIsflag() {
            return _isFlag;
        }

        public void setIsflag(Boolean isFlag) {
            this._isFlag = isFlag;
        }

        public Boolean getRequired() {
            return _required;
        }

        public void setRequired(Boolean required) {
            this._required = required;
        }

        public String getDescription() {
            return _description;
        }

        public void setDescription(String description) {
            this._description = description;
        }
    }

    @SerializedName("methodname")
    private String _counterName;

    @SerializedName("paramlist")
    private List<AutoScaleCounterParam> _paramList;

    public AutoScaleCounter(AutoScaleCounterType methodType) {
        this._counterName = methodType.getName();
        this._paramList = new ArrayList<AutoScaleCounterParam>(1);
    }

    public void addParam(String name, Boolean required, String description, Boolean isFlag) {
        AutoScaleCounterParam param = new AutoScaleCounterParam(name, required, description, isFlag);
        _paramList.add(param);
        return;
    }

    public String getName() {
        return _counterName;
    }

    public List<AutoScaleCounterParam> getParamList() {
        return _paramList;
    }

    public void setParamList(List<AutoScaleCounterParam> paramList) {
        this._paramList = paramList;
    }
}
