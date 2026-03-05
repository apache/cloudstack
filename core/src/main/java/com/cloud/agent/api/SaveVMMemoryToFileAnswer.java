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

/**
 * Answer for {@link SaveVMMemoryToFileCommand}.
 *
 * <p>Contains the result of saving VM memory to a file, including the actual
 * file path where memory was saved and the file size.</p>
 */
public class SaveVMMemoryToFileAnswer extends Answer {

    private String memoryFilePath;
    private long memoryFileSize;

    public SaveVMMemoryToFileAnswer() {
        // Default constructor for serialization
    }

    public SaveVMMemoryToFileAnswer(SaveVMMemoryToFileCommand cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    public SaveVMMemoryToFileAnswer(SaveVMMemoryToFileCommand cmd, boolean success, String details,
                                     String memoryFilePath, long memoryFileSize) {
        super(cmd, success, details);
        this.memoryFilePath = memoryFilePath;
        this.memoryFileSize = memoryFileSize;
    }

    public String getMemoryFilePath() {
        return memoryFilePath;
    }

    public void setMemoryFilePath(String memoryFilePath) {
        this.memoryFilePath = memoryFilePath;
    }

    public long getMemoryFileSize() {
        return memoryFileSize;
    }

    public void setMemoryFileSize(long memoryFileSize) {
        this.memoryFileSize = memoryFileSize;
    }
}
