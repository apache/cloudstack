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

// Load this script after all scripts have executed to populate data
(function(cloudStack) {

    var loadListViewPreFilters = function(data, prefix) {
        $.each(Object.keys(data), function(idx, key)  {
            if (key == "listView") {
                // Load config flags
                if (cloudStackOptions.hiddenFields[prefix]) {
                    var oldPreFilter = data.listView.preFilter;
                    data.listView.preFilter = function() {
                        // Hide config specified fields only for users.
                        var hiddenFields = isUser() ? cloudStackOptions.hiddenFields[prefix] : [];
                        if (oldPreFilter) {
                            return hiddenFields.concat(oldPreFilter());
                        }
                        return hiddenFields;
                    }
                }
            } else if (data[key] && $.type(data[key]) == "object") {
                loadListViewPreFilters(data[key], (prefix != null && prefix.length > 0) ? prefix + "." + key : key);
            }
        });
    }

    loadListViewPreFilters(cloudStack.sections, "");

})(cloudStack);
