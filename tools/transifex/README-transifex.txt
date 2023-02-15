# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

sync-transifex-ui.sh is a script to automate the synchronisation between
Apache CloudStack L10N resource files and Transifex CloudStack project.

Requirements to use this script:
* A GNU/Linux or Unix machine
* Transifex client installed
http://support.transifex.com/customer/portal/topics/440187-transifex-client/articles
On Debian/Ubuntu: apt-get install transifex-client

Common usage is:

1/ Init and configure the transifex client CLI
(Already made on git CloudStack repo)

  ./sync-transifex-ui.sh init-transifex https://www.transifex.com/projects/p/CloudStack_UI/

2/ Upload to Transifex the last version of the source language (en)
which generally have the new keys/values to translate.

 ./sync-transifex-ui.sh upload-source-language CloudStack_UI.410_messagesjson

3/ Download the latest L10N resource files from Transifex to resource
files directory in CloudStack tree to update the L10N resource files
with the translatons from traductors.

 ./sync-transifex-ui.sh download-l10n-languages CloudStack_UI.410_messagesjson

=====
The sync-transifex-ui provide too the ability to :

* Download from Transifex the source language resource files. Be carrefully, 
with this, you can remove some transaction on Transifex if some keys has
been removed inside the source language resource files.

 ./sync-transifex-ui.sh download-source-language CloudStack_UI.410_messagesjson

* Upload the L10N resource files on Transifex. 

 ./sync-transifex-ui.sh upload-l10n-languages CloudStack_UI.410_messagesjson

=====
Note:
If you want add a new L10N language, we need edit the sync-transifex-ui.sh script
to add his language code in LIST_LANG variable, before run the download-l10n-languages
command.

======
See: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Update+L10N+files+from+Transifex+to+git+repo
