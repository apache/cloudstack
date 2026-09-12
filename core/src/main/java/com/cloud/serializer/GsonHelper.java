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

package com.cloud.serializer;

import java.util.List;

import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NetworkTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.transport.compat.DiskTOAdaptor;
import com.cloud.agent.transport.compat.MigrateCommandAdaptor;
import com.cloud.agent.transport.compat.NetworkTOAdaptor;
import com.cloud.agent.transport.compat.VirtualMachineTOAdaptor;
import com.cloud.hypervisor.Hypervisor;
import org.apache.cloudstack.transport.HypervisorTypeAdaptor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.SecStorageFirewallCfgCommand.PortConfig;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.transport.ArrayTypeAdaptor;
import com.cloud.agent.transport.InterfaceTypeAdaptor;
import com.cloud.agent.transport.LoggingExclusionStrategy;
import com.cloud.agent.transport.Request.NwGroupsCommandTypeAdaptor;
import com.cloud.agent.transport.Request.PortConfigListTypeAdaptor;
import com.cloud.agent.transport.StoragePoolTypeAdaptor;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;

public class GsonHelper {
    protected static Logger LOGGER = LogManager.getLogger(GsonHelper.class);

    protected static final Gson s_gson;
    protected static final Gson s_gogger;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        s_gson = setDefaultGsonConfig(gsonBuilder);
        GsonBuilder LOGGERBuilder = new GsonBuilder();
        LOGGERBuilder.disableHtmlEscaping();
        LOGGERBuilder.setExclusionStrategies(new LoggingExclusionStrategy(LOGGER));
        LOGGERBuilder.serializeSpecialFloatingPointValues();
        // maybe add LOGGERBuilder.serializeNulls(); as well?
        s_gogger = setDefaultGsonConfig(LOGGERBuilder);
        LOGGER.info("Default Builder inited.");
    }

    public static Gson setDefaultGsonConfig(GsonBuilder builder) {
        builder.setVersion(1.5);
        InterfaceTypeAdaptor<DataStoreTO> dsAdaptor = new InterfaceTypeAdaptor<DataStoreTO>();
        builder.registerTypeAdapter(DataStoreTO.class, dsAdaptor);
        InterfaceTypeAdaptor<DataTO> dtAdaptor = new InterfaceTypeAdaptor<DataTO>();
        builder.registerTypeAdapter(DataTO.class, dtAdaptor);
        ArrayTypeAdaptor<Command> cmdAdaptor = new ArrayTypeAdaptor<Command>();
        builder.registerTypeAdapter(Command[].class, cmdAdaptor);
        ArrayTypeAdaptor<Answer> ansAdaptor = new ArrayTypeAdaptor<Answer>();
        builder.registerTypeAdapter(Answer[].class, ansAdaptor);
        builder.registerTypeAdapter(new TypeToken<List<PortConfig>>() {
        }.getType(), new PortConfigListTypeAdaptor());
        builder.registerTypeAdapter(new TypeToken<Pair<Long, Long>>() {
        }.getType(), new NwGroupsCommandTypeAdaptor());
        builder.registerTypeAdapter(Storage.StoragePoolType.class, new StoragePoolTypeAdaptor());
        builder.registerTypeAdapter(Hypervisor.HypervisorType.class, new HypervisorTypeAdaptor());

        // added for compatibility purposes, remove after all Agents migrate to the new version
        //
        // Each compat adaptor below needs a "base" Gson to run its own reflective (pre-rename)
        // serialization through, so that nested TOs are renamed too and the exclusion strategy set
        // on `builder` (e.g. log redaction) is honoured consistently at every nesting level. That base
        // Gson is built incrementally off the same builder, snapshotted (via builder.create()) just
        // before each adaptor's own type is registered on it, so it carries every sibling adaptor it
        // can nest without ever routing back into itself and recursing forever.
        DiskTOAdaptor diskAdaptor = new DiskTOAdaptor();
        NetworkTOAdaptor netAdaptor = new NetworkTOAdaptor();
        VirtualMachineTOAdaptor vmAdaptor = new VirtualMachineTOAdaptor();
        MigrateCommandAdaptor migrateAdaptor = new MigrateCommandAdaptor();

        // DiskTO and NetworkTO don't nest any other compat TO, so the plain config built so far is
        // already the correct base Gson for them.
        Gson leafDelegateGson = builder.create();
        diskAdaptor.initGson(leafDelegateGson);
        netAdaptor.initGson(leafDelegateGson);

        // VirtualMachineTO nests DiskTO[] and NicTO[] (NicTO extends NetworkTO), so its base Gson needs
        // Disk/Network adapters too. registerTypeHierarchyAdapter is used for NetworkTO so that the
        // NicTO[]-declared "nics" field is matched via its supertype.
        builder.registerTypeAdapter(DiskTO.class, diskAdaptor);
        builder.registerTypeHierarchyAdapter(NetworkTO.class, netAdaptor);
        Gson vmDelegateGson = builder.create();
        vmAdaptor.initGson(vmDelegateGson);

        // MigrateCommand nests a VirtualMachineTO, so its base Gson needs the VirtualMachineTO adapter
        // (which already renames the nested disks/nics above).
        builder.registerTypeAdapter(VirtualMachineTO.class, vmAdaptor);
        Gson migrateDelegateGson = builder.create();
        migrateAdaptor.initGson(migrateDelegateGson);

        builder.registerTypeAdapter(MigrateCommand.class, migrateAdaptor);

        Gson gson = builder.create();
        dsAdaptor.initGson(gson);
        dtAdaptor.initGson(gson);
        cmdAdaptor.initGson(gson);
        ansAdaptor.initGson(gson);
        return gson;
    }

    public final static Gson getGson() {
        return s_gson;
    }

    public final static Gson getGsonLogger() {
        return s_gogger;
    }

    public final static Logger getLogger() {
        return LOGGER;
    }
}
