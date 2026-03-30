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

export function buildServiceCapabilityParams (params, values, selectedServiceProviderMap, registeredServicePackages) {
  const supportedServices = Object.keys(selectedServiceProviderMap)
  params.supportedservices = supportedServices.join(',')
  for (const k in supportedServices) {
    params[`serviceProviderList[${k}].service`] = supportedServices[k]
    params[`serviceProviderList[${k}].provider`] = selectedServiceProviderMap[supportedServices[k]]
  }
  let serviceCapabilityIndex = 0
  if (supportedServices.includes('Connectivity')) {
    if (values.supportsstrechedl2subnet === true) {
      params[`serviceCapabilityList[${serviceCapabilityIndex}].service`] = 'Connectivity'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilitytype`] = 'RegionLevelVpc'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilityvalue`] = true
      serviceCapabilityIndex++
    }
    if (values.supportspublicaccess === true) {
      params[`serviceCapabilityList[${serviceCapabilityIndex}].service`] = 'Connectivity'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilitytype`] = 'DistributedRouter'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilityvalue`] = true
      serviceCapabilityIndex++
    }
    delete params.supportsstrechedl2subnet
    delete params.supportspublicaccess
  }
  // SourceNat capabilities
  if (supportedServices.includes('SourceNat')) {
    if (values.redundantroutercapability === true) {
      params[`serviceCapabilityList[${serviceCapabilityIndex}].service`] = 'SourceNat'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilitytype`] = 'RedundantRouter'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilityvalue`] = true
      serviceCapabilityIndex++
    }
    params[`servicecapabilitylist[${serviceCapabilityIndex}].service`] = 'SourceNat'
    params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilitytype`] = 'SupportedSourceNatTypes'
    params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilityvalue`] = values.sourcenattype
    serviceCapabilityIndex++
    delete params.redundantroutercapability
    delete params.sourcenattype
  } else if (values.redundantroutercapability === true) {
    params[`serviceCapabilityList[${serviceCapabilityIndex}].service`] = 'Gateway'
    params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilitytype`] = 'RedundantRouter'
    params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilityvalue`] = true
    serviceCapabilityIndex++
  }
  // StaticNat capabilities
  if (supportedServices.includes('SourceNat')) {
    if (values.elasticip === true) {
      params[`servicecapabilitylist[${serviceCapabilityIndex}].service`] = 'StaticNat'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilitytype`] = 'ElasticIp'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilityvalue`] = true
      serviceCapabilityIndex++
    }
    if (values.elasticip === true || values.associatepublicip === true) {
      params[`servicecapabilitylist[${serviceCapabilityIndex}].service`] = 'StaticNat'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilitytype`] = 'associatePublicIP'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilityvalue`] = values.associatepublicip
      serviceCapabilityIndex++
    }
    delete params.elasticip
    delete params.associatepublicip
  }
  // Lb capabilities
  if (supportedServices.includes('Lb')) {
    if ('vmautoscalingcapability' in values) {
      params[`servicecapabilitylist[${serviceCapabilityIndex}].service`] = 'lb'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilitytype`] = 'VmAutoScaling'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilityvalue`] = values.vmautoscalingcapability
      serviceCapabilityIndex++
    }
    if (values.elasticlb === true) {
      params[`servicecapabilitylist[${serviceCapabilityIndex}].service`] = 'lb'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilitytype`] = 'ElasticLb'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilityvalue`] = true
      serviceCapabilityIndex++
    }
    if (values.inlinemode === true && ((selectedServiceProviderMap.Lb === 'F5BigIp') || (selectedServiceProviderMap.Lb === 'Netscaler'))) {
      params[`servicecapabilitylist[${serviceCapabilityIndex}].service`] = 'lb'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilitytype`] = 'InlineMode'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilityvalue`] = values.inlinemode
      serviceCapabilityIndex++
    }
    params[`servicecapabilitylist[${serviceCapabilityIndex}].service`] = 'lb'
    params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilitytype`] = 'SupportedLbIsolation'
    params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilityvalue`] = values.isolation || 'dedicated'
    serviceCapabilityIndex++
    if (selectedServiceProviderMap.Lb === 'InternalLbVm') {
      params[`servicecapabilitylist[${serviceCapabilityIndex}].service`] = 'lb'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilitytype`] = 'lbSchemes'
      params[`servicecapabilitylist[${serviceCapabilityIndex}].capabilityvalue`] = 'internal'
      serviceCapabilityIndex++
    }
    if ('netscalerservicepackages' in values &&
      registeredServicePackages.length > values.netscalerservicepackages &&
      'netscalerservicepackagesdescription' in values) {
      params['details[0].servicepackageuuid'] = registeredServicePackages[values.netscalerservicepackages].id
      params['details[1].servicepackagedescription'] = values.netscalerservicepackagesdescription
    }
  }
}

/**
 * Build the VPC service capability params for Add/Clone VPC Offering forms.
 * Handles: RegionLevelVpc, DistributedRouter, RedundantRouter (SourceNat/Gateway)
 */
export function buildVpcServiceCapabilityParams (params, values, selectedServiceProviderMap, isVpcVirtualRouterForAtLeastOneService) {
  const supportedServices = Object.keys(selectedServiceProviderMap)
  let serviceCapabilityIndex = 0
  if (supportedServices.includes('Connectivity')) {
    if (values.regionlevelvpc === true) {
      params[`serviceCapabilityList[${serviceCapabilityIndex}].service`] = 'Connectivity'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilitytype`] = 'RegionLevelVpc'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilityvalue`] = true
      serviceCapabilityIndex++
    }
    if (values.distributedrouter === true) {
      params[`serviceCapabilityList[${serviceCapabilityIndex}].service`] = 'Connectivity'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilitytype`] = 'DistributedRouter'
      params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilityvalue`] = true
      serviceCapabilityIndex++
    }
  }
  if (supportedServices.includes('SourceNat') && values.redundantrouter === true) {
    params[`serviceCapabilityList[${serviceCapabilityIndex}].service`] = 'SourceNat'
    params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilitytype`] = 'RedundantRouter'
    params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilityvalue`] = true
    serviceCapabilityIndex++
  } else if (values.redundantrouter === true) {
    params[`serviceCapabilityList[${serviceCapabilityIndex}].service`] = 'Gateway'
    params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilitytype`] = 'RedundantRouter'
    params[`serviceCapabilityList[${serviceCapabilityIndex}].capabilityvalue`] = true
    serviceCapabilityIndex++
  }
  if (values.serviceofferingid && isVpcVirtualRouterForAtLeastOneService) {
    params.serviceofferingid = values.serviceofferingid
  }
}
