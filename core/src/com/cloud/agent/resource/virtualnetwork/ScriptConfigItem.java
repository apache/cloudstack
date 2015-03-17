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

package com.cloud.agent.resource.virtualnetwork;

public class ScriptConfigItem extends ConfigItem {
    private String script;
    private String args;

    public ScriptConfigItem(String script, String args) {
        this.script = script;
        this.args = args;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    @Override
    public String getAggregateCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("<script>\n");
        sb.append("/opt/cloud/bin/");
        sb.append(script);
        sb.append(' ');
        sb.append(args);
        sb.append("\n</script>\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptConfigItem, executing ");
        sb.append(script);
        sb.append(' ');
        sb.append(args);
        return sb.toString();
    }

}
