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

package com.cloud.event;

import com.cloud.exception.InvalidParameterValueException;

public class Resource {
    private String _typeURI; //MANDATORY
    private String _id; //MANDATORY
    private String _name; //OPTIONAL
    private String _domain; //OPTIONAL
    private Credential _credential; //OPTIONAL //subtype
    private String _addresses; //OPTIONAL //subtype
    private String _host; //OPTIONAL //subtype
    private String _geolocation; //OPTIONAL //subtype
    private String _geolocationId; //OPTIONAL //subtype
    private String _attachements; //OPTIONAL //subtype
    private String _userid;

    public static class Credential {
       private String _type;
       private String _token;
       private String _authority;
       private String _assertions;

       protected Credential(String type, String token, String authority, String assertions) {
           _type = type;
           _token = _token;
           _authority = authority;
           _assertions = assertions;
       }

        public void checkMandatoryFields() {
            if (_token == null || _token.isEmpty()) {
                throw new InvalidParameterValueException("Resource CREDENTIAL token field is mandatory");
            }
        }

    }

    private static class Addresses {
        private String _url;
        private String _name;
        private String _port;

        protected Addresses(String url, String name, String port) {
            _url = url;
            _name = name;
            _port = port;
        }
    }

    private static class Host {
        private String _id;
        private String _address;
        private String _agent;
        private String _platform;

        protected Host(String id, String address, String agent, String platform) {
            _id = id;
            _address = address;
            _agent = agent;
            _platform = platform;
        }
    }

    private static class Geolocation {
        private String _id;
        private String _latitude;
        private String _longtitude;
        private String _elevation;
        private String _accurance;
        private String _city;
        private String _state;
        private String _regionICANN;
        private String _annotations;

        protected Geolocation(String id, String latittude, String longtitude, String elevation, String accurance,
                              String city, String state, String regionICANN, String annotations) {
            _id = id;
            _latitude = latittude;
            _longtitude = longtitude;
            _elevation = elevation;
            _accurance = accurance;
            _city = city;
            _state = state;
            _regionICANN = regionICANN;
            _annotations = annotations;
        }
    }

    private static class Attachements {
        private String _contentType;
        private String _content;
        private String _name;

        protected Attachements(String contentType, String content, String name) {
            _contentType = contentType;
            _content = content;
            _name = name;
        }

        public void checkMandatoryFields() {
            if ((_content == null || _content.isEmpty()) ||
                    (_contentType == null || _contentType.isEmpty()) ) {
                throw new InvalidParameterValueException("Resource ATTACHEMENTS contentType and content fields " +
                        "are mandatory");
            }
        }

    }

    private static class Endpoint {
        private String _url;
        private String _name;
        private String _port;

        protected Endpoint(String url, String name, String port) {
            _url = url;
            _name = name;
            _port = port;
        }

        public void checkMandatoryFields() {
            if (_url == null || _url.isEmpty()) {
                throw new InvalidParameterValueException("Resource ENDPOINT url field is mandatory");
            }
        }

    }

    public Resource(String typeURI) {
        _typeURI = typeURI;
        _credential = new Credential("","","","");
       }

    public Resource(String typeURI, String id, String name, String host, Credential credential,
                    String addresses, String userid) {
        _typeURI = typeURI;
        _id = id;
        _name = name;
        _host = host;
        _credential = credential;
        _addresses = addresses;
        _userid = userid;
    }

    public void checkMandatoryFields() {
        if (_id == null || _id.isEmpty()) {
            throw new InvalidParameterValueException("Resource " + _typeURI + " id field is mandatory");
        }
    }

}
