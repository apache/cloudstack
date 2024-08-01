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

package org.apache.cloudstack.cluster;

import com.cloud.utils.Ternary;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.apache.cloudstack.cluster.ClusterDrsAlgorithm.getMetricValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDrsAlgorithmTest extends TestCase {

    @Test
    public void testGetMetricValue() {
        List<Ternary<Boolean, String, Double>> testData = List.of(
                new Ternary<>(true, "free", 0.4),
                new Ternary<>(false, "free", 40.0),
                new Ternary<>(true, "used", 0.3),
                new Ternary<>(false, "used", 30.0)
        );

        long used = 30;
        long free = 40;
        long total = 100;

        for (Ternary<Boolean, String, Double> data : testData) {
            boolean useRatio = data.first();
            String metricType = data.second();
            double expectedValue = data.third();

            try (MockedStatic<ClusterDrsAlgorithm> ignored = Mockito.mockStatic(ClusterDrsAlgorithm.class)) {
                when(ClusterDrsAlgorithm.getDrsMetricUseRatio(1L)).thenReturn(useRatio);
                when(ClusterDrsAlgorithm.getDrsMetricType(1L)).thenReturn(metricType);
                when(ClusterDrsAlgorithm.getMetricValue(anyLong(), anyLong(), anyLong(), anyLong(), any())).thenCallRealMethod();

                assertEquals(expectedValue, getMetricValue(1, used, free, total, null));
            }
        }
    }

    @Test
    public void testGetMetricValueWithSkipThreshold() {
        List<Ternary<Boolean, String, Double>> testData = List.of(
                new Ternary<>(true, "free", 0.15),
                new Ternary<>(false, "free", 15.0),
                new Ternary<>(true, "used", null),
                new Ternary<>(false, "used", null)
        );

        long used = 80;
        long free = 15;
        long total = 100;

        for (Ternary<Boolean, String, Double> data : testData) {
            boolean useRatio = data.first();
            String metricType = data.second();
            Double expectedValue = data.third();
            float skipThreshold = metricType.equals("free") ? 0.1f : 0.7f;

            try (MockedStatic<ClusterDrsAlgorithm> ignored = Mockito.mockStatic(ClusterDrsAlgorithm.class)) {
                when(ClusterDrsAlgorithm.getDrsMetricUseRatio(1L)).thenReturn(useRatio);
                when(ClusterDrsAlgorithm.getDrsMetricType(1L)).thenReturn(metricType);
                when(ClusterDrsAlgorithm.getMetricValue(anyLong(), anyLong(), anyLong(), anyLong(), anyFloat())).thenCallRealMethod();

                assertEquals(expectedValue, ClusterDrsAlgorithm.getMetricValue(1L, used, free, total, skipThreshold));
            }
        }
    }
}
