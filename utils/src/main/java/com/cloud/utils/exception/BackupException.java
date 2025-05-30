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
package com.cloud.utils.exception;

public class BackupException extends RuntimeException {

    boolean isVmConsistent;

    public BackupException(String message, Throwable cause, boolean isVmConsistent) {
        super(message, cause);
        this.isVmConsistent = isVmConsistent;
    }

    public BackupException(String message, boolean isVmConsistent) {
        super(message);
        this.isVmConsistent = isVmConsistent;
    }

    /**
     * If false, the backup process met an error that could not be recovered from automatically and manual intervention is needed. If true, the backup process did not make the
     * VM inconsistent, but it might have already been inconsistent from another process.
     * */
    public boolean isVmConsistent() {
        return isVmConsistent;
    }
}
