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
package org.apache.cloudstack.region;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public interface Region  {
    
    public int getId();

	public String getName();

	public void setName(String name);

	public String getEndPoint();
	

    public boolean checkIfServiceEnabled(Service service);

    /**
     * A region level service, is a service that constitute services across one or more zones in the region or a service
     * made available to all the zones in the region.
     */
    public static class Service {

        private String name;
        private static List<Service> regionServices = new ArrayList<Service>();

        public static final Service Gslb = new Service("Gslb");

        public Service(String name ) {
            this.name = name;
            regionServices.add(this);
        }

        public String getName() {
            return name;
        }

    }

    /**
     * A provider provides the region level service in a zone.
     */
    public static class Provider {

        private static List<Provider> supportedProviders = new ArrayList<Provider>();
        private String name;
        private Service service;

        public static final Provider Netscaler = new Provider("Netscaler", Service.Gslb);

        public Provider(String name, Service service) {
            this.name = name;
            this.service = service;
            supportedProviders.add(this);
        }

        public String getName() {
            return name;
        }
    }
}
