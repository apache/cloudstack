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
package org.apache.cloudstack.utils.net.cidr;

public class CIDRError extends Error {

    private static final long serialVersionUID = -432126076052875403L;

    public CIDRError(String msg) {
        super(msg);
    }

    public CIDRError(String msg, Error cause) {
        super(msg, cause);
    }

    public CIDRError(String msg, Exception cause) {
        super(msg, cause);
    }

    public CIDRError() {
    }
}
