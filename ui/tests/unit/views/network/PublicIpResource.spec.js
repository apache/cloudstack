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

import PublicIpResource from '@/views/network/PublicIpResource'
import ResourceView from '@/components/view/ResourceView'

jest.mock('@api', () => ({ getAPI: jest.fn() }), { virtual: true })
jest.mock('@/api', () => ({ getAPI: jest.fn() }))
jest.mock('@/views/AutogenView.vue', () => ({}))
jest.mock('@/components/view/ActionButton', () => ({}))
jest.mock('@/components/view/DetailsTab', () => ({}))
jest.mock('@/components/view/InfoCard', () => ({}))
jest.mock('@/layouts/ResourceLayout', () => ({}))

const hasService = name => services => services.filter(x => x.name === name).length > 0

const routeTabs = () => [
  { name: 'details' },
  { name: 'firewall', networkServiceFilter: hasService('Firewall') },
  { name: 'portforwarding', networkServiceFilter: hasService('PortForwarding') },
  { name: 'loadbalancing', networkServiceFilter: hasService('Lb') },
  { name: 'vpn', show: () => true },
  { name: 'events' },
  { name: 'comments' }
]

const services = (...names) => names.map(name => ({ name }))

// A VPC tier never offers the Firewall service, the VPC does
const vpcTier = (conserveMode = true) => ({
  id: 'tier-1',
  networkofferingconservemode: conserveMode,
  service: services('Dhcp', 'Dns', 'Lb', 'NetworkACL', 'PortForwarding', 'SourceNat', 'StaticNat', 'UserData', 'Vpn')
})

const vpc = (withFirewall, conserveMode = true) => ({
  id: 'vpc-1',
  vpcofferingconservemode: conserveMode,
  service: withFirewall
    ? services('Dhcp', 'Dns', 'Firewall', 'Lb', 'NetworkACL', 'PortForwarding', 'SourceNat', 'StaticNat', 'UserData', 'Vpn')
    : services('Dhcp', 'Dns', 'Lb', 'NetworkACL', 'PortForwarding', 'SourceNat', 'StaticNat', 'UserData', 'Vpn')
})

const createContext = ({ resource, vpcResponse = null, network = null, portForwardingRules = 0, loadBalancerRules = 0 }) => ({
  resource,
  tabs: [],
  defaultTabs: [{ name: 'details' }, { name: 'events' }, { name: 'comments' }],
  $route: { meta: { tabs: routeTabs() } },
  fetchVpc: jest.fn(() => Promise.resolve(vpcResponse)),
  fetchNetwork: jest.fn(() => Promise.resolve(network)),
  fetchPortFWRule: jest.fn(() => Promise.resolve(portForwardingRules)),
  fetchLoadBalancerRule: jest.fn(() => Promise.resolve(loadBalancerRules)),
  addFirewallTab: PublicIpResource.methods.addFirewallTab,
  hasVpcFirewallCapability: PublicIpResource.methods.hasVpcFirewallCapability
})

// Tabs that ResourceView renders for the IP, given the network the IP is associated with
const visibleTabs = (context, network) => context.tabs
  .filter(tab => ResourceView.methods.showTab.call({
    networkService: network,
    resource: context.resource,
    $route: context.$route,
    $store: { getters: { userInfo: {} } }
  }, tab))
  .map(tab => tab.name)

const vpcIp = (overrides = {}) => ({
  id: 'ip-1',
  state: 'Allocated',
  vpcid: 'vpc-1',
  issourcenat: false,
  isstaticnat: false,
  ...overrides
})

describe('PublicIpResource firewall tab for VPC public IPs', () => {
  it('keeps the firewall tab after a port forwarding rule associates the IP with a tier', async () => {
    const tier = vpcTier()
    const context = createContext({
      resource: vpcIp({ associatednetworkid: tier.id }),
      vpcResponse: vpc(true),
      network: tier,
      portForwardingRules: 1
    })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, tier)).toEqual(
      ['details', 'firewall', 'portforwarding', 'loadbalancing', 'vpn', 'events', 'comments'])
  })

  it('keeps the firewall tab for an IP with port forwarding rules on a tier without conserve mode', async () => {
    const tier = vpcTier(false)
    const context = createContext({
      resource: vpcIp({ associatednetworkid: tier.id }),
      vpcResponse: vpc(true, false),
      network: tier,
      portForwardingRules: 1
    })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, tier)).toEqual(['details', 'firewall', 'portforwarding', 'vpn', 'events', 'comments'])
  })

  it('shows the firewall tab for a source NAT IP of a VPC with conserve mode', async () => {
    const tier = vpcTier()
    const context = createContext({
      resource: vpcIp({ issourcenat: true, associatednetworkid: tier.id }),
      vpcResponse: vpc(true),
      network: tier
    })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, tier)).toContain('firewall')
  })

  it('shows the firewall tab for a source NAT IP of a VPC without conserve mode', async () => {
    const tier = vpcTier(false)
    const context = createContext({
      resource: vpcIp({ issourcenat: true, associatednetworkid: tier.id }),
      vpcResponse: vpc(true, false),
      network: tier
    })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, tier)).toEqual(['details', 'events', 'comments', 'vpn', 'firewall'])
  })

  it('shows the firewall tab for a static NAT IP', async () => {
    const tier = vpcTier()
    const context = createContext({
      resource: vpcIp({ isstaticnat: true, associatednetworkid: tier.id }),
      vpcResponse: vpc(true),
      network: tier
    })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, tier)).toContain('firewall')
  })

  it('shows the firewall tab for an IP that is not associated with a tier yet', async () => {
    const context = createContext({
      resource: vpcIp(),
      vpcResponse: vpc(true)
    })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, null)).toContain('firewall')
  })

  it('does not show the firewall tab when the VPC has no Firewall service', async () => {
    const tier = vpcTier()
    const context = createContext({
      resource: vpcIp({ associatednetworkid: tier.id }),
      vpcResponse: vpc(false),
      network: tier,
      portForwardingRules: 1
    })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, tier)).not.toContain('firewall')
  })
})

describe('PublicIpResource firewall tab for isolated network public IPs', () => {
  const isolatedIp = { id: 'ip-2', state: 'Allocated', issourcenat: true, isstaticnat: false, associatednetworkid: 'net-1' }

  it('still filters the firewall tab by the services of the network', async () => {
    const network = { id: 'net-1', service: services('Dhcp', 'Dns', 'PortForwarding', 'SourceNat') }
    const context = createContext({ resource: isolatedIp })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, network)).not.toContain('firewall')
  })

  it('shows the firewall tab when the network has the Firewall service', async () => {
    const network = { id: 'net-1', service: services('Dhcp', 'Dns', 'Firewall', 'PortForwarding', 'SourceNat') }
    const context = createContext({ resource: isolatedIp })

    await PublicIpResource.methods.filterTabs.call(context)

    expect(visibleTabs(context, network)).toContain('firewall')
  })
})
