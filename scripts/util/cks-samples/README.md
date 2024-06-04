# CloudStack Kubernetes Service Cloud-init Overrides - Examples

Example files that can be used when creating the CloudStack Kubernetes Service ISO file to override the normal behavior of the Kubernetes cluster creation


## Usage

### The basic directory

In the basic directory, there is a scripts directory that can be used as a template to start the customization. It is basically the original cloud-init scripts, but they can then be modified as needed for the CKS cluster. To start, just copy the scripts directory into the util directory, peer with the create-kubernetes-binaries-iso.sh script.

### The cilium directory

In the cilium directory, there is a modified version of create-kubernetes-binaries-iso.sh which contains what would be needed to install Helm and install Cilium instead of Weave. Helm needs to be installed and in the path on the OS where the ISO build will be run. There is no need to customize anything under the cilium directory if you are only looking to run Cilium instead of Weave, but just like the basic directory, other aspects can be customized.


## References in CloudStack

The following cloud-init scripts will call the setup-kube-system scripts found here when the cluster is created:

* plugins/integrations/kubernetes-service/src/main/resources/conf/k8s-control-node.yml
* plugins/integrations/kubernetes-service/src/main/resources/conf/k8s-control-node-add.yml
* plugins/integrations/kubernetes-service/src/main/resources/conf/k8s-node.yml

## License

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

