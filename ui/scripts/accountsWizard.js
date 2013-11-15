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

        informationWithinLdap: {
            username: {
                label: 'label.username',
                validation: {
                    required: true
                },
                docID: 'helpAccountUsername'
            },
            password: {
                label: 'label.password',
                validation: {
                    required: true
                },
                isPassword: true,
                id: 'password',
                docID: 'helpAccountPassword'
            },
            'password-confirm': {
                label: 'label.confirm.password',
                validation: {
                    required: true,
                    equalTo: '#password'
                },
                isPassword: true,
                docID: 'helpAccountConfirmPassword'
            },
            email: {
                label: 'label.email',
                validation: {
                    required: true,
                    email: true
                },
                docID: 'helpAccountEmail'
            },
            firstname: {
                label: 'label.first.name',
                validation: {
                    required: true
                },
                docID: 'helpAccountFirstName'
            },
            lastname: {
                label: 'label.last.name',
                validation: {
                    required: true
                },
                docID: 'helpAccountLastName'
            }
        },

        informationNotInLdap: {
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

                                if (this.level === 0)
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
                    required: false
                }
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
            },
            ldapGroupName: {
                label: 'label.ldap.group.name',
                docID: 'helpLdapGroupName',
                validation: {
                    required: false
                }
            }

        },

        action: function(args) {
            var array1 = [];
            var ldapStatus = isLdapEnabled();
            if (args.username) {
                array1.push("&username=" + args.username);
            }

            if (!ldapStatus) {
                var password = args.data.password;
                if (md5Hashed) {
                    password = $.md5(password);
                }
                array1.push("&email=" + args.data.email);
                array1.push("&firstname=" + args.data.firstname);
                array1.push("&lastname=" + args.data.lastname);

                password = args.data.password;
                if (md5Hashed) {
                    password = $.md5(password);
                }
                array1.push("&password=" + password);
            }

            array1.push("&domainid=" + args.data.domainid);

            var account = args.data.account;
            if (account === null || account.length === 0) {
                account = args.username;
            }
            array1.push("&account=" + account);

            var accountType = args.data.accounttype;
            if (args.data.accounttype == "1" && args.data.domainid != rootDomainId) { //if account type is admin, but domain is not Root domain
                accountType = "2"; // Change accounttype from root-domain("1") to domain-admin("2")
            }
            array1.push("&accounttype=" + accountType);

            if (args.data.timezone !== null && args.data.timezone.length > 0) {
                array1.push("&timezone=" + args.data.timezone);
            }

            if (args.data.networkdomain !== null && args.data.networkdomain.length > 0) {
                array1.push("&networkdomain=" + args.data.networkdomain);
            }
            if (args.groupname && args.groupname !== null && args.groupname.length > 0) {
                array1.push("&group=" + args.groupname);
            }

            if (ldapStatus) {
                if (args.groupname) {
                    $.ajax({
                        url: createURL('importLdapUsers' + array1.join("")),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            var count = json.ldapuserresponse.count;
                            args.response.success({
                                data: count
                            });
                        },
                        error: function(XMLHttpResponse) {
                            args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                        }
                    });
                } else if (args.username) {
                    $.ajax({
                        url: createURL('ldapCreateAccount' + array1.join("")),
                        dataType: "json",
                        async: false,
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
            } else {
                $.ajax({
                    url: createURL('createAccount' + array1.join("")),
                    dataType: "json",
                    async: false,
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
        /*
                action: function(args) {
                    var array1 = [];

                    var username = args.data.username;

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

                    for (var i = 0; i < username.length; i++) {
                        $.ajax({
                            url: createURL("ldapCreateAccount&username=" + username[i] + array1.join("")),
                            dataType: "json",
                            async: false,
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
                */
    };
}(cloudStack, jQuery));