// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai;

/**
 * Usage types for AI Services (SPEC.md section 9).
 *
 * "The plugin pulls /v1/usage hourly and writes usage events for existing
 * billing integrations." These ids sit above CloudStack's own range so a
 * provider's billing pipeline can tell them apart without ambiguity;
 * CloudStack's built-in types stop well below 100.
 */
public final class AiServicesUsageTypes {

    private AiServicesUsageTypes() {
    }

    public static final int INPUT_TOKENS = 100;
    public static final int OUTPUT_TOKENS = 101;
    public static final int GPU_HOURS = 102;
    /** v2: the premium charged on top of the base price for a LoRA adapter. */
    public static final int ADAPTER_PREMIUM = 103;
    /** v2: batch tokens, billed at a discount to the online rate. */
    public static final int BATCH_TOKENS = 104;

    public static String describe(int usageType) {
        switch (usageType) {
            case INPUT_TOKENS:
                return "AI input tokens";
            case OUTPUT_TOKENS:
                return "AI output tokens";
            case GPU_HOURS:
                return "AI GPU hours";
            case ADAPTER_PREMIUM:
                return "AI adapter premium";
            case BATCH_TOKENS:
                return "AI batch tokens";
            default:
                throw new IllegalArgumentException("not an AI Services usage type: " + usageType);
        }
    }

    public static int[] all() {
        return new int[] {
                INPUT_TOKENS, OUTPUT_TOKENS, GPU_HOURS, ADAPTER_PREMIUM, BATCH_TOKENS,
        };
    }
}
