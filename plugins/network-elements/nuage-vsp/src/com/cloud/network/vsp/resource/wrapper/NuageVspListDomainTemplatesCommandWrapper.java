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

import com.cloud.agent.api.manager.ListVspDomainTemplatesAnswer;
import com.cloud.agent.api.manager.ListVspDomainTemplatesCommand;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import net.nuage.vsp.acs.client.api.NuageVspManagerClient;
import net.nuage.vsp.acs.client.api.model.VspDomainTemplate;
import net.nuage.vsp.acs.client.exception.NuageVspException;

import javax.naming.ConfigurationException;
import java.util.List;


@ResourceWrapper(handles =  ListVspDomainTemplatesCommand.class)
public class NuageVspListDomainTemplatesCommandWrapper extends CommandWrapper<ListVspDomainTemplatesCommand, ListVspDomainTemplatesAnswer, NuageVspResource> {

    @Override
    public ListVspDomainTemplatesAnswer execute(ListVspDomainTemplatesCommand command, NuageVspResource serverResource) {
        NuageVspManagerClient client = null;
        try {
            client = serverResource.getNuageVspManagerClient();
            List<VspDomainTemplate> domainTemplates = client.getDomainTemplates(command.getDomain(), command.getName());

            return new ListVspDomainTemplatesAnswer(command, domainTemplates);
        } catch (ConfigurationException | NuageVspException e) {
            return new ListVspDomainTemplatesAnswer(command, e);
        }

    }
}
