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
package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

import com.cloud.bridge.util.StringHelper;

public class EC2Filter {

    // -> a single filter can have several possible values to compare to
    protected String filterName;
    protected List<String> valueSet = new ArrayList<String>();

    public EC2Filter() {
        filterName = null;
    }

    public String getName() {
        return filterName;
    }

    public void setName(String param) {
        filterName = param;
    }

    /**
     * From Amazon:
     * "You can use wildcards with the filter values: * matches zero or more characters, and ? matches
     * exactly one character. You can escape special characters using a backslash before the character. For
     * example, a value of \*amazon\?\\ searches for the literal string *amazon?\. "
     */
    public void addValueEncoded(String param) {
        valueSet.add(StringHelper.toRegex(param));
    }

    public void addValue(String param) {
        valueSet.add(param);
    }

    public String[] getValueSet() {
        return valueSet.toArray(new String[0]);
    }
}
