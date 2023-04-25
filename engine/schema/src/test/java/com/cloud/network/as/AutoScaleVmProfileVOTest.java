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
package com.cloud.network.as;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Test;

public class AutoScaleVmProfileVOTest {

    @Test
    public void testCounterParamsForUpdate() {
        AutoScaleVmProfileVO profile = new AutoScaleVmProfileVO();

        Map<String, HashMap<String, String>> counterParamList = new HashMap<>();
        counterParamList.put("0", new HashMap<>() {{ put("name", "snmpcommunity"); put("value", "public"); }});
        counterParamList.put("1", new HashMap<>() {{ put("name", "snmpport"); put("value", "161"); }});

        profile.setCounterParamsForUpdate(counterParamList);
        Assert.assertEquals("snmpcommunity=public&snmpport=161", profile.getCounterParamsString());

        List<Pair<String, String>>  counterParams = profile.getCounterParams();
        Assert.assertEquals(2, counterParams.size());
        Assert.assertEquals("snmpcommunity", counterParams.get(0).first());
        Assert.assertEquals("public", counterParams.get(0).second());
        Assert.assertEquals("snmpport", counterParams.get(1).first());
        Assert.assertEquals("161", counterParams.get(1).second());
    }

    @Test
    public void tstSetOtherDeployParamsForUpdate() {
        AutoScaleVmProfileVO profile = new AutoScaleVmProfileVO();

        Map<String, HashMap<String, String>> otherDeployParamsMap = new HashMap<>();
        otherDeployParamsMap.put("0", new HashMap<>() {{ put("name", "serviceofferingid"); put("value", "a7fb50f6-01d9-11ed-8bc1-77f8f0228926"); }});
        otherDeployParamsMap.put("1", new HashMap<>() {{ put("name", "rootdisksize"); put("value", "10"); }});

        profile.setOtherDeployParamsForUpdate(otherDeployParamsMap);

        List<Pair<String, String>> otherDeployParamsList = profile.getOtherDeployParamsList();
        Assert.assertEquals(2, otherDeployParamsList.size());
        Assert.assertEquals("serviceofferingid", otherDeployParamsList.get(0).first());
        Assert.assertEquals("a7fb50f6-01d9-11ed-8bc1-77f8f0228926", otherDeployParamsList.get(0).second());
        Assert.assertEquals("rootdisksize", otherDeployParamsList.get(1).first());
        Assert.assertEquals("10", otherDeployParamsList.get(1).second());
    }
}
