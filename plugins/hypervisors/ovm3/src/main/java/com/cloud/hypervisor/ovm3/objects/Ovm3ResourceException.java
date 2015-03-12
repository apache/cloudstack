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

package com.cloud.hypervisor.ovm3.objects;

public class Ovm3ResourceException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final Throwable CAUSE = null;
    public Ovm3ResourceException() {
        super();
    }

    public Ovm3ResourceException(String message) {
        super(message);
    }

    public Ovm3ResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Throwable getCause() {
        return CAUSE;
    }
}
