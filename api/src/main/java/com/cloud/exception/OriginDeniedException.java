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
package com.cloud.exception;

import com.cloud.user.Account;
import com.cloud.utils.SerialVersionUID;
import com.cloud.utils.exception.CloudRuntimeException;

import java.net.InetAddress;

public class OriginDeniedException extends CloudRuntimeException {

    private static final long serialVersionUID = SerialVersionUID.OriginDeniedException;

    public OriginDeniedException(String message) {
        super(message);
    }

    public OriginDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    protected OriginDeniedException() {
        super();
    }

    InetAddress origin;
    Account account;

    public OriginDeniedException(String message, Account account, InetAddress origin) {
        super(message);
        this.origin = origin;
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    public InetAddress getOrigin() {
        return origin;
    }

    public void addDetails(Account account, InetAddress origin) {
        this.account = account;
        this.origin = origin;
    }
}
