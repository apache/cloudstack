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
package org.apache.cloudstack.storage.datastore.adapter;

public class ProviderVolumeNamer {

    private static final String SNAPSHOT_PREFIX = "snap";
    private static final String VOLUME_PREFIX = "vol";
    private static final String TEMPLATE_PREFIX = "tpl";
    private static final String ENV_PREFIX = System.getProperty("adaptive.storage.provider.envIdentifier");

    public static String generateObjectName(ProviderAdapterContext context, ProviderAdapterDataObject obj) {
        ProviderAdapterDataObject.Type objType = obj.getType();
        String prefix = null;
        if (objType == ProviderAdapterDataObject.Type.SNAPSHOT) {
            prefix = SNAPSHOT_PREFIX;
        } else if (objType == ProviderAdapterDataObject.Type.VOLUME) {
            prefix = VOLUME_PREFIX;
        } else if (objType == ProviderAdapterDataObject.Type.TEMPLATE) {
            prefix = TEMPLATE_PREFIX;
        } else {
            throw new RuntimeException("Unknown ManagedDataObject type provided: " + obj.getType());
        }

        if (ENV_PREFIX != null) {
            prefix = ENV_PREFIX + "-" + prefix;
        }

        return prefix + "-" + obj.getDataStoreId() + "-" + context.getDomainId() + "-" + context.getAccountId() + "-" + obj.getId();
    }


   public static String generateObjectComment(ProviderAdapterContext context, ProviderAdapterDataObject obj) {
        return "CSInfo [Account=" + context.getAccountName()
            + "; Domain=" + context.getDomainName()
            + "; DomainUUID=" + context.getDomainUuid()
            + "; Account=" + context.getAccountName()
            + "; AccountUUID=" + context.getAccountUuid()
            + "; ObjectEndUserName=" + obj.getName()
            + "; ObjectUUID=" + obj.getUuid() + "]";
    }

}
