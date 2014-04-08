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

package com.cloud.api.dispatch;

import java.util.Map;

import org.apache.cloudstack.api.BaseCmd;

/**
 * This class wraps all the data that any worker could need. If we don't wrap it this
 * way and we pass the parameters one by one, in the end we could end up having all the
 * N workers receiving plenty of parameters and changing the signature, each time one
 * of them changes. This way, if a certain worker needs something else, you just need
 * to change it in this wrapper class and the worker itself.
 */
@SuppressWarnings("rawtypes")
public class DispatchTask {

    protected BaseCmd cmd;

    protected Map params;

    public DispatchTask(final BaseCmd cmd, final Map params) {
        this.cmd = cmd;
        this.params = params;
    }

    public BaseCmd getCmd() {
        return cmd;
    }

    public void setCmd(final BaseCmd cmd) {
        this.cmd = cmd;
    }

    public Map getParams() {
        return params;
    }

    public void setParams(final Map params) {
        this.params = params;
    }
}
