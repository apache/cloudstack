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

// Define custom options configurable by admins for UI
cloudStackOptions = {
    aboutText: "label.app.name", // This is the text shown in the 'About' box
    aboutTitle: "label.about.app", // This is the Application 'Title' shown in the 'About' box
    docTitle: "label.app.name", // This is the Application 'Title' shown on browser tab.

    helpURL: "http://docs.cloudstack.apache.org/", // This is the URL that opens when users click Help
    keyboardOptions: {
        "us": "label.standard.us.keyboard",
        "uk": "label.uk.keyboard",
        "fr": "label.french.azerty.keyboard",
        "jp": "label.japanese.keyboard",
        "sc": "label.simplified.chinese.keyboard"
    },
    hiddenFields: { // Fields to be hidden only for users in the tables below
        "metrics.instances": [], // Options - "name", "state", "ipaddress", "zonename", "cpuused", "memused", "network", "disk"
        "metrics.volumes": [] // Options - "name", "state", "vmname", "sizegb", "physicalsize", "utilization", "storagetype", "storage"
    }
};
