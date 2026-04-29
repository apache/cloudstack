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

package org.apache.cloudstack.network.opendaylight.api;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.httpclient.HttpMethodBase;

public class NeutronRestFactory {

    private Map<String, NeutronRestApi> flyweight = new Hashtable<String, NeutronRestApi>();

    private static NeutronRestFactory instance;

    static {
        instance = new NeutronRestFactory();
    }

    private NeutronRestFactory() {
    }

    public static NeutronRestFactory getInstance() {
        return instance;
    }

    public NeutronRestApi getNeutronApi(final Class<? extends HttpMethodBase> clazz) {
        if (!flyweight.containsKey(clazz.getName())) {
            NeutronRestApi api = new NeutronRestApi(clazz);
            addNeutronApi(api);
        }
        return flyweight.get(clazz.getName());
    }

    public void addNeutronApi(final NeutronRestApi api) {
        flyweight.put(api.getHttpClazz().getName(), api);
    }
}
