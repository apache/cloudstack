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
package com.cloud.network.rules;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class LbStickinessMethod {
    public static class StickinessMethodType {
        private String _name;

        public static final StickinessMethodType LBCookieBased = new StickinessMethodType("LbCookie");
        public static final StickinessMethodType AppCookieBased = new StickinessMethodType("AppCookie");
        public static final StickinessMethodType SourceBased = new StickinessMethodType("SourceBased");

        public StickinessMethodType(String name) {
            _name = name;
        }

        public String getName() {
            return _name;
        }
    }

    public class LbStickinessMethodParam {
        @SerializedName("paramname")
        private String _paramName;

        @SerializedName("required")
        private Boolean _required;

        @SerializedName("isflag")
        private Boolean _isFlag;

        @SerializedName("description")
        private String _description;

        public LbStickinessMethodParam(String name, Boolean required, String description, Boolean flag) {
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
    private String _methodName;

    @SerializedName("paramlist")
    private List<LbStickinessMethodParam> _paramList;

    @SerializedName("description")
    private String _description;

    public LbStickinessMethod(StickinessMethodType methodType, String description) {
        this._methodName = methodType.getName();
        this._description = description;
        this._paramList = new ArrayList<LbStickinessMethodParam>(1);
    }

    public void addParam(String name, Boolean required, String description, Boolean isFlag) {
        /* FIXME : UI is breaking if the capability string length is larger , temporarily description is commented out */
        // LbStickinessMethodParam param = new LbStickinessMethodParam(name, required, description);
        LbStickinessMethodParam param = new LbStickinessMethodParam(name, required, " ", isFlag);
        _paramList.add(param);
        return;
    }

    public String getMethodName() {
        return _methodName;
    }

    public List<LbStickinessMethodParam> getParamList() {
        return _paramList;
    }

    public void setParamList(List<LbStickinessMethodParam> paramList) {
        this._paramList = paramList;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        /* FIXME : UI is breaking if the capability string length is larger , temporarily description is commented out */
        //this.description = description;
        this._description = " ";
    }
}
