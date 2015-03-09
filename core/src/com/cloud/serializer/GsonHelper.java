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

import org.apache.log4j.Logger;

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
import com.cloud.utils.Pair;

public class GsonHelper {
    private static final Logger s_logger = Logger.getLogger(GsonHelper.class);

    protected static final Gson s_gson;
    protected static final Gson s_gogger;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        s_gson = setDefaultGsonConfig(gsonBuilder);
        GsonBuilder loggerBuilder = new GsonBuilder();
        loggerBuilder.disableHtmlEscaping();
        loggerBuilder.setExclusionStrategies(new LoggingExclusionStrategy(s_logger));
        s_gogger = setDefaultGsonConfig(loggerBuilder);
        s_logger.info("Default Builder inited.");
    }

    static Gson setDefaultGsonConfig(GsonBuilder builder) {
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
        return s_logger;
    }
}
