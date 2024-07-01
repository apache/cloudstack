# CloudStack Kubernetes Service Cloud-init Overrides - Examples

Example files that can be used when creating the CloudStack Kubernetes Service ISO file to override the normal behavior of the Kubernetes cluster creation


## Usage

### The basic template - create-scripts.sh

If you want to override the CKS installation starting with the basic CKS installation scripts, just run:

```
bash create-scripts.sh ${CLOUDSTACK_REPO_HOME}/plugins/integrations/kubernetes-service/src/main/resources/conf ${CLOUDSTACK_REPO_HOME}/scripts/util/scripts
```

You can make any adjustments to the scripts under ${CLOUDSTACK_REPO_HOME}/scripts/util/scripts as necessary. Then you can run create-kubernetes-binaries-iso.sh in the parent directory as you would normally to create your ISO.

### Cilium - create-scripts-cilium.sh

If you want a CKS installation that installs both Cilium (instead of Weave) and Helm, you can run the Cilium create script like so:

```
bash create-scripts-cilium.sh ${CLOUDSTACK_REPO_HOME}/plugins/integrations/kubernetes-service/src/main/resources/conf ${CLOUDSTACK_REPO_HOME}/scripts/util/cks-samples/scripts
```

You can make any adjustments to the scripts under ${CLOUDSTACK_REPO_HOME}/scripts/util/cks-samples/scripts as necessary, although by default, the scripts should install what is necessary to install Cilium. There are helm-overrides for Cilium under the helm-overrides directory that can be modified as well.

Once any adjustments are made (not required), you can run create-kubernetes-binaries-iso-cilium.sh in a similar fashion to how create-kubernetes-binaries-iso.sh would be run, replacing the Weave URL with the Cilium version and Helm version (see examples in the usage).


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

