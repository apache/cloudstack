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

===========================================================

Allows spinning up the systemvm appliance from ../../appliance inside
vagrant, and then running tests against it with nose.

To use, install vagrant, rvm, ruby, bundler, python and pip.
Then run ./test.sh.

To write tests, create files underneath ../../../test/systemvm
named test_xxx.py. These tests are standard python unit tests with
some logic to SSH into the SystemVM. See
../../../test/systemvm/README.md for more info.

