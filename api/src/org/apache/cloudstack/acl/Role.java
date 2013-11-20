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
package org.apache.cloudstack.acl;

//metadata - consists of default dynamic roles in CS + any custom roles added by user
public interface Role {

    public static final short ROOT_ADMIN = 0;
    public static final short DOMAIN_ADMIN = 1;
    public static final short DOMAIN_USER = 2;
    public static final short OWNER = 3;
    public static final short PARENT_DOMAIN_ADMIN = 4;
    public static final short PARENT_DOMAIN_USER = 5;
    public static final short CHILD_DOMAIN_ADMIN = 6;
    public static final short CHILD_DOMAIN_USER = 7;

    public long getId();

    public short getRoleType();
}
