package com.cloud.utils.db;

import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
public class GenericSearchBuilderTest {

    GenericSearchBuilder genericSearchBuilderSpy = Mockito.spy(new GenericSearchBuilder<>());

    @Test
    public void addAndConditionsTestNoParametersCallNothingAndReturnThis() {
        GenericSearchBuilder result = genericSearchBuilderSpy.addAndConditions();
        Assert.assertEquals(genericSearchBuilderSpy, result);
        Mockito.verify(genericSearchBuilderSpy, Mockito.never()).and(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void addAndConditionsTestArrayIsEmptyCallNothingAndReturnThis() {
        Pair[] pairs = new Pair[]{};

        GenericSearchBuilder result = genericSearchBuilderSpy.addAndConditions(pairs);
        Assert.assertEquals(genericSearchBuilderSpy, result);
        Mockito.verify(genericSearchBuilderSpy, Mockito.never()).and(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void addAndConditionsTestVarargsWithNullCallAndOnlyForNonNullValuesAndReturnThis() {
        Mockito.doReturn(null).when(genericSearchBuilderSpy).and(Mockito.anyString(), Mockito.any());

        GenericSearchBuilder result = genericSearchBuilderSpy.addAndConditions(null, new Pair<>("test", SearchCriteria.Op.EQ), null);
        Assert.assertEquals(genericSearchBuilderSpy, result);
        Mockito.verify(genericSearchBuilderSpy).and(Mockito.anyString(), Mockito.any());
    }
}
