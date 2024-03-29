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
package org.apache.cloudstack.api;

import java.io.Serializable;

// This interface is a contract that getId() will give the internal
// ID of an entity which extends this interface
// Any class having an internal ID in db table/schema should extend this
// For example, all ControlledEntity, OwnedBy would have an internal ID

public interface InternalIdentity extends Serializable {
    long getId();

    /*
     Helper method to add conditions in joins where some column name is equal to a string value
     */
    default Object setString(String str) {
        return null;
    }

    /*
    Helper method to add conditions in joins where some column name is equal to a long value
     */
    default Object setLong(Long l) {
        return null;
    }
}
