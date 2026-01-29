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
package org.apache.cloudstack.veeam;

import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(MockitoJUnitRunner.class)
public class VeeamControlServiceImplTest {

    @Test
    public void test_parseImageTransfer() {
        String data = "{\"active\":false,\"direction\":\"upload\",\"format\":\"cow\",\"inactivity_timeout\":3600,\"phase\":\"cancelled\",\"shallow\":false,\"transferred\":0,\"link\":[],\"disk\":{\"id\":\"dba4d72d-01de-4267-aa8e-305996b53599\"},\"image\":{},\"backup\":{\"creation_date\":0}}";
        Mapper mapper = new Mapper();
        try {
            ImageTransfer request = mapper.jsonMapper().readValue(data, ImageTransfer.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
