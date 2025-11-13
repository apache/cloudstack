<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 -->

### Configure virtualbox

1. Open virtualbox and navigate to its preferences/settings window.

1. Click onto the network tab and then onto the host only network tab.

1. Configure your adapters as follows:

    ##### vboxnet0
    - IPv4 IP address of 192.168.22.1
    - Subnet of 255.255.255.0
    - DHCP server disabled

    ##### vboxnet1
    - IPv4 IP address of 192.168.23.1
    - Subnet of 255.255.255.0
    - DHCP server disabled

    ##### vboxnet2
    - IPv4 IP address of 192.168.24.1
    - Subnet of 255.255.255.0
    - DHCP server disabled

### Start the vagrant boxes

```bash
vagrant up
```

*** Common issues: ***

- 'Cannot forward the specified ports on this VM': There could be MySQL or some other
  service running on the host OS causing vagrant to fail setting up local port forwarding.

### Start Cloudstack

1. Clone the Cloudstack Repository:

	```
	git clone https://github.com/apache/cloudstack.git
	```

	*** Note: ***

	Personally I prefer to use the 4.3 codebase rather than main. If you wish to do the same:

	```
	git reset --hard 0810029
	```

1. Download vhd-util:

	```bash
	cd /path/to/cloudstack/repo
	wget https://download.cloudstack.org/tools/vhd-util -P scripts/vm/hypervisor/xenserver/
	chmod +x scripts/vm/hypervisor/xenserver/vhd-util
	```

1. Compile Cloudstack:

	```bash
	cd /path/to/cloudstack/repo
	mvn -P developer,systemvm clean install -DskipTests=true
	```

1. Deploy Cloudstack Database:

	```bash
	cd /path/to/cloudstack/repo
	mvn -P developer -pl developer,tools/devcloud4 -Ddeploydb
	```

1. Start Cloudstack:

	```bash
	cd /path/to/cloudstack/repo
	mvn -pl :cloud-client-ui jetty:run
	```

1. Install Marvin:

	```
	cd /path/to/cloudstack/repo
	pip install tools/marvin/dist/Marvin-0.1.0.tar.gz
	```

1. Deploy:

    ```
    python -m marvin.deployDataCenter -i marvin.cfg
    ```
