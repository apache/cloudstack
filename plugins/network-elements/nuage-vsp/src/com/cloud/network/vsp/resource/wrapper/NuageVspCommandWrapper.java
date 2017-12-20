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

package com.cloud.network.vsp.resource.wrapper;

import javax.naming.ConfigurationException;

import net.nuage.vsp.acs.client.exception.NuageVspException;

import net.nuage.vsp.acs.client.exception.NuageVspUnsupportedRequestException;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.resource.CommandWrapper;

public abstract class NuageVspCommandWrapper<T extends Command> extends CommandWrapper<T, Answer, NuageVspResource> {

    private static final Logger s_logger = Logger.getLogger(NuageVspResource.class);

    @Override
    public final Answer execute(final T command, final NuageVspResource nuageVspResource) {
        try {
            boolean success = executeNuageVspCommand(command, nuageVspResource);
            String detail = fillDetail(new StringBuilder(), command).append(" on ").append(nuageVspResource.getName()).toString();
            return new Answer(command, success, detail);
        } catch (NuageVspUnsupportedRequestException e) {
            s_logger.error("Failure during " + command + " on " + nuageVspResource.getName(), e);
            return new UnsupportedAnswer(command, e.getMessage()); //New Exception so there is no stacktrace showed in the UI when changing ACL lists.
        } catch (Exception e) {
            s_logger.error("Failure during " + command + " on " + nuageVspResource.getName(), e);
            return new Answer(command, e);
        }
    }

    public abstract boolean executeNuageVspCommand(final T command, final NuageVspResource nuageVspResource) throws ConfigurationException, NuageVspException;

    public abstract StringBuilder fillDetail(final StringBuilder stringBuilder, final T command);
}