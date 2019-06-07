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

package org.apache.cloudstack.cadf;

import com.cloud.exception.InvalidParameterValueException;
import com.google.gson.annotations.Expose;

public class Resource {
    @Expose
    private String typeURI; //MANDATORY
    @Expose
    private String id; //MANDATORY
    @Expose
    private String name; //OPTIONAL
    @Expose
    private String domain; //OPTIONAL
    @Expose
    private Credential credential; //OPTIONAL //subtype
    @Expose
    private String addresses; //OPTIONAL //subtype
    @Expose
    protected Host host; //OPTIONAL //subtype
    @Expose
    private String geolocation; //OPTIONAL //subtype
    @Expose
    private String geolocationId; //OPTIONAL //subtype
    @Expose
    private String attachements; //OPTIONAL //subtype
    @Expose
    protected String userid;
    @Expose
    protected String csAccountName;

    public static class Credential {
        @Expose
        private String type;
        @Expose
        private String token;
        @Expose
        private String authority;
        @Expose
        private String assertions;

        protected Credential(String type, String token, String authority, String assertions) {
           this.type = type;
           this.token = token;
           this.authority = authority;
           this.assertions = assertions;
        }

        public void checkMandatoryFields() {
            if (token == null || token.isEmpty()) {
                throw new InvalidParameterValueException("Resource CREDENTIAL token field is mandatory");
            }
        }

    }

    private static class Addresses {
        @Expose
        private String url;
        @Expose
        private String name;
        @Expose
        private String port;

        protected Addresses(String url, String name, String port) {
            this.url = url;
            this.name = name;
            this.port = port;
        }
    }

    public static class Host {
        @Expose
        private String id;
        @Expose
        private String address;
        @Expose
        private String agent;
        @Expose
        private String platform;

        protected Host() {
            //
        }

        protected Host(String id, String address, String agent, String platform) {
            this();
            this.id = id;
            this.address = address;
            this.agent = agent;
            this.platform = platform;
        }
    }

    private static class Geolocation {
        @Expose
        private String id;
        @Expose
        private String latitude;
        @Expose
        private String longtitude;
        @Expose
        private String elevation;
        @Expose
        private String accurance;
        @Expose
        private String city;
        @Expose
        private String state;
        @Expose
        private String regionICANN;
        @Expose
        private String annotations;

        protected Geolocation(String id, String latittude, String longtitude, String elevation, String accurance,
                              String city, String state, String regionICANN, String annotations) {
            this.id = id;
            this.latitude = latittude;
            this.longtitude = longtitude;
            this.elevation = elevation;
            this.accurance = accurance;
            this.city = city;
            this.state = state;
            this.regionICANN = regionICANN;
            this.annotations = annotations;
        }
    }

    private static class Attachements {
        @Expose
        private String contentType;
        @Expose
        private String content;
        @Expose
        private String name;

        protected Attachements(String contentType, String content, String name) {
            this.contentType = contentType;
            this.content = content;
            this.name = name;
        }

        public void checkMandatoryFields() {
            if ((content == null || content.isEmpty()) ||
                    (contentType == null || contentType.isEmpty()) ) {
                throw new InvalidParameterValueException("Resource ATTACHEMENTS contentType and content fields " +
                        "are mandatory");
            }
        }

    }

    private static class Endpoint {
        @Expose
        private String url;
        @Expose
        private String name;
        @Expose
        private String port;

        protected Endpoint(String url, String name, String port) {
            this.url = url;
            this.name = name;
            this.port = port;
        }

        public void checkMandatoryFields() {
            if (url == null || url.isEmpty()) {
                throw new InvalidParameterValueException("Resource ENDPOINT url field is mandatory");
            }
        }

    }

    public Resource(String typeURI) {
        this.typeURI = typeURI;
        this.credential = new Credential("","","","");
    }

    public Resource(String typeURI, String id) {
        this.typeURI = typeURI;
        this.id = id;
        this.credential = new Credential("","","","");
       }

    public Resource(String typeURI, String id, String userid) {
        this.typeURI = typeURI;
        this.id = id;
        this.userid= userid;
        this.credential = new Credential("","","","");
    }

    public Resource(String typeURI, String id, String name, String host, Credential credential,
                    String addresses, String userid) {
        this.typeURI = typeURI;
        this.id = id;
        this.name = name;
        //_host = host;
        this.credential = credential;
        this.addresses = addresses;
        this.userid = userid;
    }

    public void checkMandatoryFields() {
        if (id == null || id.isEmpty()) {
            throw new InvalidParameterValueException("Resource " + typeURI + " id field is mandatory");
        }
    }

}