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
package com.cloud.agent.api.to;

public class DeployAsIsInfoTO {

    private boolean deployAsIs;
    private String templatePath;
    private String deploymentConfiguration;

    public DeployAsIsInfoTO() {
    }

    public boolean isDeployAsIs() {
        return deployAsIs;
    }

    public void setDeployAsIs(boolean deployAsIs) {
        this.deployAsIs = deployAsIs;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templateInSecondaryPath) {
        this.templatePath = templateInSecondaryPath;
    }

    public String getDeploymentConfiguration() {
        return deploymentConfiguration;
    }

    public void setDeploymentConfiguration(String deploymentConfiguration) {
        this.deploymentConfiguration = deploymentConfiguration;
    }
}
