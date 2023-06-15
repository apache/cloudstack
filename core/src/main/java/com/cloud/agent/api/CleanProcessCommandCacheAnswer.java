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

package com.cloud.agent.api;

import com.cloud.exception.CloudException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CleanProcessCommandCacheAnswer extends Answer {
    int numberOfFiles = 0;
    List<String> fileNames;

    public CleanProcessCommandCacheAnswer(CleanProcessedCacheCommand cmd, String details) {
        super(cmd, new CloudException(details));
    }

    public CleanProcessCommandCacheAnswer(CleanProcessedCacheCommand cmd, String details, boolean b) {
        super(cmd, true, details);
        String[] lines = details.split("\n");
        for (String line: lines) {
            parse(line);
        }
    }

    void parse(String line) {
        if (line.startsWith("numberOfFiles:")) {
            String[] s = line.split(" ");
            numberOfFiles = Integer.parseInt(s[1]);
        } else if (line.startsWith("files:")) {
            String[] s = line.split(" ") ;
            fileNames = Arrays.stream(s, 1, s.length).collect(Collectors.toList());
        }
    }
}
