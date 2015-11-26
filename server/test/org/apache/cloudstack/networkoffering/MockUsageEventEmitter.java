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

package org.apache.cloudstack.networkoffering;

import java.util.Map;

import com.cloud.event.UsageEventEmitter;

public class MockUsageEventEmitter implements UsageEventEmitter {

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            Long paramLong4, Long paramLong5, Long paramLong6,
            String paramString3, String paramString4) {
    }

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            Long paramLong4, Long paramLong5, Long paramLong6,
            String paramString3, String paramString4, boolean paramBoolean) {
    }

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            Long paramLong4, Long paramLong5, Long paramLong6, Long paramLong7,
            String paramString3, String paramString4) {
    }

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            String paramString3, String paramString4) {
    }

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            String paramString3, String paramString4, boolean paramBoolean) {
    }

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            boolean paramBoolean1, String paramString3, boolean paramBoolean2,
            String paramString4, String paramString5) {
    }

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            Long paramLong4, Long paramLong5, String paramString3,
            String paramString4, String paramString5, boolean paramBoolean) {
    }

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, long paramLong4,
            String paramString2, String paramString3) {
    }

    @Override
    public void publishUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            Long paramLong4, Long paramLong5, String paramString3,
            String paramString4, String paramString5,
            Map<String, String> paramMap, boolean paramBoolean) {
    }

    @Override
    public void saveUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            Long paramLong4, Long paramLong5, Long paramLong6) {

    }

    @Override
    public void saveUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            Long paramLong4, Long paramLong5, Long paramLong6, Long paramLong7) {
    }

    @Override
    public void saveUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2) {
    }

    @Override
    public void saveUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            boolean paramBoolean1, String paramString3, boolean paramBoolean2) {
    }

    @Override
    public void saveUsageEvent(String paramString1, long paramLong1,
            long paramLong2, long paramLong3, String paramString2,
            Long paramLong4, Long paramLong5, String paramString3) {
    }

    @Override
    public void saveUsageEvent(String paramString, long paramLong1,
            long paramLong2, long paramLong3, long paramLong4) {
    }

}
