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

import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.BindingType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.PortProfileType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;

public class VsmPortProfileResponse extends VsmResponse {
    private static final Logger s_logger = Logger.getLogger(VsmPortProfileResponse.class);
    private static final String s_portProfileDetails = "__XML__OPT_Cmd_show_port_profile___readonly__";

    private PortProfile _portProfile = new PortProfile();

    VsmPortProfileResponse(String response) {
        super(response);
        initialize();
    }

    public PortProfile getPortProfile() {
        return _portProfile;
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
            NodeList list = ((Element)data).getElementsByTagName(s_portProfileDetails);
            if (list.getLength() > 0) {
                NodeList readOnlyList = ((Element)list.item(0)).getElementsByTagName("__readonly__");
                Element readOnly = (Element)readOnlyList.item(0);

                for (Node node = readOnly.getFirstChild(); node != null; node = node.getNextSibling()) {
                    String currentNode = node.getNodeName();
                    String value = node.getTextContent();
                    if ("port_binding".equalsIgnoreCase(currentNode)) {
                        setPortBinding(value);
                    } else if ("profile_name".equalsIgnoreCase(currentNode)) {
                        // Set the port profile name.
                        _portProfile.profileName = value;
                    } else if ("profile_cfg".equalsIgnoreCase(currentNode)) {
                        setProfileConfiguration(value);
                    } else if ("type".equalsIgnoreCase(currentNode)) {
                        setPortType(value);
                    } else if ("status".equalsIgnoreCase(currentNode)) {
                        // Has the profile been enabled.
                        if (value.equalsIgnoreCase("1")) {
                            _portProfile.status = true;
                        }
                    } else if ("max_ports".equalsIgnoreCase(currentNode)) {
                        // Has the profile been enabled.
                        _portProfile.maxPorts = Integer.parseInt(value.trim());
                    }
                }
            }
        } catch (DOMException e) {
            s_logger.error("Error parsing the response : " + e.toString());
        }
    }

    private void setProfileConfiguration(String value) {
        StringTokenizer tokens = new StringTokenizer(value.trim());
        if (tokens.hasMoreTokens()) {
            String currentToken = tokens.nextToken();
            if ("switchport".equalsIgnoreCase(currentToken)) {
                parseProfileMode(tokens);
            } else if ("service-policy".equalsIgnoreCase(currentToken)) {
                String ioType = tokens.nextToken();
                if ("input".equalsIgnoreCase(ioType)) {
                    _portProfile.inputPolicyMap = tokens.nextToken();
                } else if ("output".equalsIgnoreCase(ioType)) {
                    _portProfile.outputPolicyMap = tokens.nextToken();
                }
            }
        }
    }

    private void parseProfileMode(StringTokenizer tokens) {
        if (tokens.hasMoreTokens()) {
            String firstToken = tokens.nextToken();
            if ("mode".equalsIgnoreCase(firstToken)) {
                setPortMode(tokens.nextToken());
            } else if ("access".equalsIgnoreCase(firstToken)) {
                if (tokens.hasMoreTokens()) {
                    String secondToken = tokens.nextToken();
                    assert ("vlan".equalsIgnoreCase(secondToken));
                    if (tokens.hasMoreTokens()) {
                        _portProfile.vlan = tokens.nextToken();
                    }
                }
            }
        }
    }

    private void setPortMode(String value) {
        // Set the mode for port profile.
        if ("access".equalsIgnoreCase(value)) {
            _portProfile.mode = SwitchPortMode.access;
        } else if ("trunk".equalsIgnoreCase(value)) {
            _portProfile.mode = SwitchPortMode.trunk;
        } else if ("privatevlanhost".equalsIgnoreCase(value)) {
            _portProfile.mode = SwitchPortMode.privatevlanhost;
        } else if ("privatevlanpromiscuous".equalsIgnoreCase(value)) {
            _portProfile.mode = SwitchPortMode.privatevlanpromiscuous;
        }
    }

    private void setPortBinding(String value) {
        // Set the binding type for the port profile.
        if ("static".equalsIgnoreCase(value)) {
            _portProfile.binding = BindingType.portbindingstatic;
        } else if ("dynamic".equalsIgnoreCase(value)) {
            _portProfile.binding = BindingType.portbindingdynamic;
        } else if ("ephermal".equalsIgnoreCase(value)) {
            _portProfile.binding = BindingType.portbindingephermal;
        }
    }

    private void setPortType(String value) {
        // Set the type field (vethernet/ethernet).
        if ("vethernet".equalsIgnoreCase(value)) {
            _portProfile.type = PortProfileType.vethernet;
        } else if ("ethernet".equalsIgnoreCase(value)) {
            _portProfile.type = PortProfileType.ethernet;
        }
    }
}
