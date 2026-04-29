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
package org.apache.cloudstack.vm;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface ImportVmTask extends Identity, InternalIdentity {
    enum Step {
        Prepare, CloningInstance, ConvertingInstance, Importing, Completed
    }

    enum TaskState {
        Running, Completed, Failed;

        public static TaskState getValue(String state) {
            for (TaskState s : TaskState.values()) {
                if (s.name().equalsIgnoreCase(state)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Invalid task state: " + state);
        }
    }
}
