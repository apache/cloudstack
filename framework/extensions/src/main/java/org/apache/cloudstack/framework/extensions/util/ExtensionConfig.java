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

package org.apache.cloudstack.framework.extensions.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtensionConfig {

    public String apiVersion;
    public String kind;
    public Metadata metadata;
    public Source source;
    public Spec spec;

    String archiveUrl;

    // -----------------------------
    // Nested compact model
    // -----------------------------

    public static class Metadata {
        public String name;
        public String displayName;
        public String description;
        public String version;
        public String maintainer;
        public String homepage;
    }

    public static class Source {
        public String type;
        public String url;
        public String refs;
    }

    public static class Spec {
        public String type;
        public Compatibility compatibility;
        public Entrypoint entrypoint;
        public Orchestrator orchestrator;
        private Map<String, String> details;
        public boolean enabled;
        public List<CustomAction> customActions;

        public Map<String, String> getDetails() {
            return details;
        }

        public void setDetails(Map<String, String> details) {
            this.details = details;
        }
    }

    public static class Compatibility {
        public CloudStack cloudstack;
    }

    public static class CloudStack {
        public String minVersion;
    }

    public static class Entrypoint {
        public String language;
        public String path;
        public String targetDir;
    }

    public static class Orchestrator {
        public boolean requiresPrepareVm;
    }

    public static class CustomAction {
        public String name;
        public String displayName;
        public String description;
        public String resourcetype;
        public boolean enabled;
        public int timeout;
        public List<String> allowedroletypes;
        public List<Parameter> parameters;

        public Map<Integer, Map<String, String>> getParametersAsMap() {
            Map<Integer, Map<String, String>> paramMap = new HashMap<>();
            int index = 0;
            for (Parameter param : parameters) {
                Map<String, String> singleParamMap = new HashMap<>();
                singleParamMap.put(ApiConstants.NAME, param.name);
                singleParamMap.put(ApiConstants.TYPE, param.type);
                singleParamMap.put(ApiConstants.VALIDATION_FORMAT, param.validationformat);
                singleParamMap.put(ApiConstants.REQUIRED, Boolean.toString(param.required));
                paramMap.put(index++, singleParamMap);
            }
            return paramMap;
        }
    }

    public static class Parameter {
        public String name;
        public String type;
        public String validationformat;
        public boolean required;
    }

    public String getArchiveUrl() {
        String type = source != null ? source.type : null;
        if ("git".equalsIgnoreCase(type) && source.url != null && source.url.contains("github.com")) {
            // ToDo: improve
            String ref = source.refs != null ? source.refs : "main";
            return source.url.replace("github.com", "codeload.github.com") + "/zip/refs/heads/" + ref;
        }
        return source == null ? null : source.url;
    }

    public Spec getSpec() {
        return spec;
    }
}
