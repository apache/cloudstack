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
package com.cloud.agent.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.OVFInformationTO;
import com.cloud.agent.api.to.deployasis.OVFPropertyTO;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.google.gson.Gson;
import com.cloud.agent.api.to.deployasis.OVFNetworkTO;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DownloadAnswerTest {
    Gson gson = GsonHelper.getGson();

    VMTemplateStorageResourceAssoc.Status status = VMTemplateStorageResourceAssoc.Status.DOWNLOADED;
    DownloadAnswer answer = new DownloadAnswer("nothing wrong", status);

    @Test
    public void redeserialise ()
    {
        String json = gson.toJson(answer);
        DownloadAnswer received = gson.fromJson(json, DownloadAnswer.class);
        Assert.assertEquals(received,answer);
    }
    @Test
    public void properties ()
    {
        List<OVFPropertyTO> properties = new ArrayList<>();
        properties.add(new OVFPropertyTO());
        List<OVFNetworkTO> networks = new ArrayList<>();
        networks.add(new OVFNetworkTO());

        OVFInformationTO informationTO = new OVFInformationTO();
        informationTO.setProperties(properties);
        informationTO.setNetworks(networks);
        answer.setOvfInformationTO(informationTO);

        String json = gson.toJson(answer);
        Answer received = gson.fromJson(json, Answer.class);
        Assert.assertEquals(received,answer);
    }
}
