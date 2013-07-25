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

(function(cloudStack, $) {
    cloudStack.accountsWizard = {

        manuallyInputtedAccountInformation: {
            domainid: {
                label: 'label.domain',
                docID: 'helpAccountDomain',
                validation: {
                    required: true
                },
                select: function(args) {
                    var data = {};

                    if (args.context.users) { // In accounts section
                        data.listAll = true;
                    } else if (args.context.domains) { // In domain section (use specific domain)
                        data.id = args.context.domains[0].id;
                    }

                    $.ajax({
                        url: createURL("listDomains"),
                        data: data,
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            var items = [];
                            domainObjs = json.listdomainsresponse.domain;
                            $(domainObjs).each(function() {
                                items.push({
                                    id: this.id,
                                    description: this.path
                                });

                                if (this.level == 0)
                                    rootDomainId = this.id;
                            });
                            args.response.success({
                                data: items
                            });
                        }
                    });
                }
            },
            account: {
                label: 'label.account',
                docID: 'helpAccountAccount',
                validation: {
                    required: true
                },
            },
            accounttype: {
                label: 'label.type',
                docID: 'helpAccountType',
                validation: {
                    required: true
                },
                select: function(args) {
                    var items = [];
                    items.push({
                        id: 0,
                        description: "User"
                    }); //regular-user
                    items.push({
                        id: 1,
                        description: "Admin"
                    }); //root-admin
                    args.response.success({
                        data: items
                    });
                }
            },
            timezone: {
                label: 'label.timezone',
                docID: 'helpAccountTimezone',
                select: function(args) {
                    var items = [];
                    items.push({
                        id: "",
                        description: ""
                    });
                    for (var p in timezoneMap)
                        items.push({
                            id: p,
                            description: timezoneMap[p]
                        });
                    args.response.success({
                        data: items
                    });
                }
            },
            networkdomain: {
                label: 'label.network.domain',
                docID: 'helpAccountNetworkDomain',
                validation: {
                    required: false
                }
            }
        },

        action: function(args) {
            var array1 = [];

            array1.push("&username=" + args.data.username);
            array1.push("&domainid=" + args.data.domainid);

            if (args.data.account != null && args.data.account.length != 0) {
                array1.push("&account=" + args.data.account);
            }

            if (args.data.accounttype == "1" && args.data.domainid != rootDomainId) {
                args.data.accounttype = "2";
            }
            array1.push("&accountType=" + args.data.accounttype);

            if (args.data.timezone != null && args.data.timezone.length != 0) {
                array1.push("&timezone=" + args.data.timezone);
            }

            if (args.data.networkdomain != null && args.data.networkdomain != 0) {
                array1.push("&networkDomain=" + args.data.networkdomain);
            }

            console.log(array1.join(""));
            console.log(args.data);

		$.ajax({
			url: createURL("ldapCreateAccount" + array1.join("")),
			dataType: "json",
			success: function(json) {
				var item = json.createaccountresponse.account;
				args.response.success({
					data: item
				});
			},
			error: function(XMLHttpResponse) {
				args.response.error(parseXMLHttpResponse(XMLHttpResponse));
			}
		});
        }
    }
}(cloudStack, jQuery));
