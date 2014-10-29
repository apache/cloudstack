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

package com.cloud.utils.cisco.n1kv.vsm;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class VsmOkResponse extends VsmResponse {

    VsmOkResponse(String response) {
        super(response);
        initialize();
    }

    @Override
    protected void parse(Element root) {
        NodeList list = root.getElementsByTagName("nf:rpc-error");
        if (list.getLength() == 0) {
            // No rpc-error tag; means response was ok.
            assert (root.getElementsByTagName("nf:ok").getLength() > 0);
            _responseOk = true;
        } else {
            parseError(list.item(0));
            _responseOk = false;
        }
    }
}
