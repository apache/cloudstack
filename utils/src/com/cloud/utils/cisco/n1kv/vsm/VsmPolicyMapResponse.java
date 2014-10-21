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

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VsmPolicyMapResponse extends VsmResponse {
    private static final Logger s_logger = Logger.getLogger(VsmPolicyMapResponse.class);
    private static final String s_policyMapDetails = "__XML__OPT_Cmd_show_policy-map___readonly__";

    private PolicyMap _policyMap = new PolicyMap();

    VsmPolicyMapResponse(String response) {
        super(response);
        initialize();
    }

    public PolicyMap getPolicyMap() {
        return _policyMap;
    }

    @Override
    protected void parse(Element root) {
        NodeList list = root.getElementsByTagName("nf:rpc-error");
        if (list.getLength() == 0) {
            // No rpc-error tag; means response was ok.
            NodeList dataList = root.getElementsByTagName("nf:data");
            if (dataList.getLength() > 0) {
                parseData(dataList.item(0));
                _responseOk = true;
            }
        } else {
            super.parseError(list.item(0));
            _responseOk = false;
        }
    }

    protected void parseData(Node data) {
        try {
            NodeList list = ((Element)data).getElementsByTagName(s_policyMapDetails);
            if (list.getLength() > 0) {
                NodeList readOnlyList = ((Element)list.item(0)).getElementsByTagName("__readonly__");
                Element readOnly = (Element)readOnlyList.item(0);

                for (Node node = readOnly.getFirstChild(); node != null; node = node.getNextSibling()) {
                    String currentNode = node.getNodeName();
                    String value = node.getTextContent();
                    if ("pmap-name-out".equalsIgnoreCase(currentNode)) {
                        _policyMap.policyMapName = value;
                    } else if ("cir".equalsIgnoreCase(currentNode)) {
                        _policyMap.committedRate = Integer.parseInt(value.trim());
                    } else if ("bc".equalsIgnoreCase(currentNode)) {
                        _policyMap.burstRate = Integer.parseInt(value.trim());
                    } else if ("pir".equalsIgnoreCase(currentNode)) {
                        _policyMap.peakRate = Integer.parseInt(value.trim());
                    }
                }
            }
        } catch (DOMException e) {
            s_logger.error("Error parsing the response : " + e.toString());
        }
    }
}
