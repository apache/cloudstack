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

acs-api-commands
================

Collection of API commands.xml of [Apache CloudStack](http://cloudstack.apache.org/) versions. used to generate
[Apache CloudStack Release Notes](http://docs.cloudstack.apache.org/en/latest/releasenotes/index.html).

[How To Generate CloudStack API Documentation](https://cwiki.apache.org/confluence/display/CLOUDSTACK/How+To+Generate+CloudStack+API+Documentation)

Install NonOSS Dependencies
---------------------------

```bash
$ cd /tmp
$ git clone https://github.com/rhtyd/cloudstack-nonoss.git nonoss
$ cd nonoss && bash -x install-non-oss.sh
```

Build API doc
-------------

```bash
$ cd /path/to/cloudstack
$ git fetch <upstream>
$ git checkout main
$ git checkout <release_commit>
$ mvn -Pdeveloper -Dnoredist clean install -DskipTests=true
$ mvn -Pdeveloper -Dnoredist clean install -pl :cloud-apidoc
```

Generate Diff
-------------

```bash
$ cd /path/to/cloudstack
$ export COMMANDS=/path/to/tools/apidoc-versions-diff
$ export OLD_RELEASE=4.13
$ export NEW_RELEASE=4.14
$ cp tools/apidoc/target/commands.xml $COMMANDS/${NEW_RELEASE}_commands.xml
$ mkdir $COMMANDS/diff-${OLD_RELEASE//.}-${NEW_RELEASE//.}
$ java -cp $HOME/.m2/repository/com/thoughtworks/xstream/xstream/1.4.11.1/xstream-1.4.11.1.jar:$HOME/.m2/repository/com/google/code/gson/gson/1.7.2/gson-1.7.2.jar:server/target/classes com.cloud.api.doc.ApiXmlDocReader -old $COMMANDS/${OLD_RELEASE}_commands.xml -new $COMMANDS/${NEW_RELEASE}_commands.xml -d $COMMANDS/diff-${OLD_RELEASE//.}-${NEW_RELEASE//.}
```

Note
----

- For easier automation (i.e. select the "OLD_RELEASE" by simply choosing a previous branch like "4.12" or 4.13")
- and
- taking into consideration that the minor release never has a new/removed API/command (and very rarely changed/updated an existing API call)
- and
- taking into the consideration that we have never so far documented API changes between minor versions (i.e. 4.11.0 and 4.11.1),
- ...
- the naming scheme, used as of 4.11, is now in X.Y form (i.e. 4.11 or 4.12) instead of minor version (4.11.0 or 4.12.0),
while the older generated documentation was moved to folder "before-4.11".

In order to generate .rst version based on the diffs generated here, as well as for the generating a list of PRs/changes/fixed issues in the new release, please see https://github.com/swill/generate_acs_rn
