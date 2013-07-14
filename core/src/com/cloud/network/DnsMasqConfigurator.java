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
package com.cloud.network;

import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.to.DhcpTO;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;



 public class DnsMasqConfigurator  {

         private static final Logger s_logger = Logger.getLogger(DnsMasqConfigurator.class);
         private static String[] Dnsmasq_config = {"# Never forward plain names (without a dot or domain part) \ndomain-needed\n",
                                           "# Never forward addresses in the non-routed address spaces. \nbogus-priv\n",
                                           "# Uncomment this to filter useless windows-originated DNS requests # which can trigger dial-on-demand links needlessly. \n # Note that (amongst other things) this blocks all SRV requests, # so don't use it if you use eg Kerberos, SIP, XMMP or Google-talk.# This option only affects forwarding, SRV records originating for # dnsmasq (via srv-host= lines) are not suppressed by it. \nfilterwin2k\n",
                                           "# Change this line if you want dns to get its upstream servers from# somewhere other that /etc/resolv.conf \nresolv-file=/etc/dnsmasq-resolv.conf\n",
                                           "# Add local-only domains here, queries in these domains are answered\n # from /etc/hosts or DHCP only.\n local=/cs1cloud.internal/",
                                           "# If you want dnsmasq to listen for DHCP and DNS requests only on\n #specified interfaces (and the loopback) give the name of the\n# interface (eg eth0) here.\n# Repeat the line for more than one interface.\ninterface=eth0\n",
                                           "# Or you can specify which interface _not_ to listen on\nexcept-interface=eth1\nexcept-interface=eth2\nexcept-interface=lo\n",
                                           "# Or which to listen on by address (remember to include 127.0.0.1 if\n# you use this.)\n#listen-address=?\n",
                                           "# If you want dnsmasq to provide only DNS service on an interface,\n# configure it as shown above, and then use the following line to\n#disable DHCP and TFTP on it.\nno-dhcp-interface=eth1\nno-dhcp-interface=eth2\n",
                                           "# On systems which support it, dnsmasq binds the wildcard address,\n" +
                                                   "# even when it is listening on only some interfaces. It then discards\n" +
                                                   "# requests that it shouldn't reply to. This has the advantage of\n" +
                                                   "# working even when interfaces come and go and change address. If you\n" +
                                                   "# want dnsmasq to really bind only the interfaces it is listening on,\n" +
                                                   "# uncomment this option. About the only time you may need this is when\n" +
                                                   "# running another nameserver on the same machine.\n" +
                                                   "bind-interfaces\n",
                                           "# Set this (and domain: see below) if you want to have a domain\n" +
                                                   "# automatically added to simple names in a hosts-file.\n" +
                                                   "expand-hosts\n",
                                           "# Set the domain for dnsmasq. this is optional, but if it is set, it\n" +
                                                   "# does the following things.\n" +
                                                   "# 1) Allows DHCP hosts to have fully qualified domain names, as long\n" +
                                                   "#     as the domain part matches this setting.\n" +
                                                   "# 2) Sets the \"domain\" DHCP option thereby potentially setting the\n" +
                                                   "#    domain of all systems configured by DHCP\n" +
                                                   "# 3) Provides the domain part for \"expand-hosts\"\n",
                                                   "domain=cs1cloud.internal\n",
                                           "# Set a different domain for a particular subnet\n",
                                                   "domain=cs1cloud.internal\n",
                                           "# Same idea, but range rather then subnet\n",
                                                   "domain=cs1cloud.internal\n",
                                           "# Uncomment this to enable the integrated DHCP server, you need\n" +
                                                   "# to supply the range of addresses available for lease and optionally\n" +
                                                   "# a lease time. If you have more than one network, you will need to\n" +
                                                   "# repeat this for each network on which you want to supply DHCP\n" +
                                                   "# service.\n",
                                                   "dhcp-range=set:net1,ipaddress,static\n",
                                           "dhcp-hostsfile=/etc/dhcphosts.txt\n",
                                           "log-facility=/var/log/dnsmasq.log\n",
                                           "conf-dir=/etc/dnsmasq.d\n",
                                           "dhcp-option=tag:net1,3,ipaddress\n",
                                           "dhcp-option=tag:net1,1,netmask\n",
                                           "dhcp-option=6,router_ip,external_dns\n",
                                           "dhcp-optsfile=/etc/dhcpopts.txt\n",

         };

     public String[] generateConfiguration(DnsMasqConfigCommand dnsMasqconfigcmd) {
         List<DhcpTO> dhcpTOs = dnsMasqconfigcmd.getIps();
         List <String> dnsMasqconf = Arrays.asList(Dnsmasq_config);
         String range="";
         String gateway="";
         String netmask="";
         String domain= dnsMasqconfigcmd.getDomain();
         String dnsServers="";
         String dns_external="";
         if (dnsMasqconfigcmd.getDns1()!= null) {
             dns_external = dnsMasqconfigcmd.getDns1()+",";
         }
         if (dnsMasqconfigcmd.getDns2() != null) {
             dns_external = dns_external+dnsMasqconfigcmd.getDns2()+",";
         }
         dns_external = dns_external + "*";
         dns_external = dns_external.replace(",*","");
         int i=0;
         for (; i< dhcpTOs.size(); i++) {
              range=range + "dhcp-range=set:range"+i+","+ dhcpTOs.get(i).getStartIpOfSubnet()+",static\n";
              gateway=gateway +"dhcp-option=tag:range"+i+",3,"+ dhcpTOs.get(i).getGateway()+"\n";
              netmask=netmask +"dhcp-option=tag:range"+i+",1,"+ dhcpTOs.get(i).getNetmask()+"\n";
              if (!dnsMasqconfigcmd.isDnsProvided()) {
                  dnsServers = dnsServers+"dhcp-option=tag:range"+i+",6,"+dns_external+"\n";
              }
              else {
                  dnsServers=dnsServers+"dhcp-option=tag:range"+i+",6,"+ dhcpTOs.get(i).getRouterIp()+","+dns_external+"\n";
              }

         }
         String domain_suffix= dnsMasqconfigcmd.getDomainSuffix();

         if (domain != null) {
             if (domain_suffix != null) {

                 dnsMasqconf.get(5).replace(" local=/cs1cloud.internal/"," local=/"+domain+"/");
                 dnsMasqconf.set(12, "domain="+domain_suffix+domain+"\n");
                 dnsMasqconf.set(14, "domain="+domain_suffix+domain+"\n");
                 dnsMasqconf.set(16,"domain="+domain_suffix+domain+"\n");
             } else {
                 dnsMasqconf.get(5).replace(" local=/cs1cloud.internal/"," local=/"+domain+"/");
                 dnsMasqconf.set(12, "domain="+domain+"\n");
                 dnsMasqconf.set(14, "domain="+domain+"\n");
                 dnsMasqconf.set(16,"domain="+domain+"\n");
             }
         }
         ///if no domain is specified. this happens when dns service is not provided by the virtualrouter.
         else {
             dnsMasqconf.get(5).replace(" local=/cs1cloud.internal/"," local=/"+domain+"/");
             dnsMasqconf.set(12, "domain="+"cloudnine.internal\n");
             dnsMasqconf.set(14, "domain="+"cloudnine.internal\n");
             dnsMasqconf.set(16,"domain="+"cloudnine.internal\n");
         }

         dnsMasqconf.set(18, range);
         dnsMasqconf.set(22, gateway);
         dnsMasqconf.set(23, netmask);
         dnsMasqconf.set(24,dnsServers);
         return dnsMasqconf.toArray( new  String[dnsMasqconf.size()]);
     }

 }
