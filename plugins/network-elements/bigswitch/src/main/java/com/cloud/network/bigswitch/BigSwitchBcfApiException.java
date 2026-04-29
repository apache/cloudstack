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

package com.cloud.network.bigswitch;

public class BigSwitchBcfApiException extends Exception {

    private static final long serialVersionUID = -5864952230870945604L;
    private boolean topologySyncRequested = false;

    public BigSwitchBcfApiException() {
    }

    public BigSwitchBcfApiException(String message) {
        super(message);
    }

    public BigSwitchBcfApiException(Throwable cause) {
        super(cause);
    }

    public BigSwitchBcfApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public BigSwitchBcfApiException(String message, boolean syncRequest) {
        super(message);
        this.set_topologySyncRequested(syncRequest);
    }

    public boolean is_topologySyncRequested() {
        return topologySyncRequested;
    }

    public void set_topologySyncRequested(boolean requested) {
        this.topologySyncRequested = requested;
    }

}
