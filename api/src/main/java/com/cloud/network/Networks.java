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

import java.net.URI;
import java.net.URISyntaxException;

import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Network includes all of the enums used within networking.
 *
 */
public class Networks {

    public enum RouterPrivateIpStrategy {
        None, DcGlobal, // global to data center
        HostLocal;

        public static final String DummyPrivateIp = "169.254.1.1";
    }

    /**
     * Different ways to assign ip address to this network.
     */
    public enum Mode {
        None, Static, Dhcp, ExternalDhcp;
    };

    public enum AddressFormat {
        Ip4, Ip6, DualStack
    }

    /**
     * Different types of broadcast domains.
     */
    public enum BroadcastDomainType {
        Native(null, null) {
            @Override
            public <T> URI toUri(T value) {
                try {
                    if (value.toString().contains("://"))
                        return new URI(value.toString());
                    else
                        // strange requirement but this is how the code expects it
                        return new URI("vlan://" + value.toString());
                } catch (URISyntaxException e) {
                    throw new CloudRuntimeException("Unable to convert to broadcast URI: " + value);
                }
            }
        },
        Vlan("vlan", Integer.class) {
            @Override
            public <T> URI toUri(T value) {
                try {
                    if (value.toString().contains("://"))
                        return new URI(value.toString());
                    else
                        return new URI("vlan://" + value.toString());
                } catch (URISyntaxException e) {
                    throw new CloudRuntimeException("Unable to convert to broadcast URI: " + value);
                }
            }
            @Override
            public String getValueFrom(URI uri) {
                return uri.getAuthority();
            }
        },
        Vswitch("vs", String.class), LinkLocal(null, null), Vnet("vnet", Long.class), Storage("storage", Integer.class), Lswitch("lswitch", String.class) {
            @Override
            public <T> URI toUri(T value) {
                try {
                    return new URI("lswitch", value.toString(), null, null);
                } catch (URISyntaxException e) {
                    throw new CloudRuntimeException("Unable to convert to broadcast URI: " + value);
                }
            }

            /**
             * gets scheme specific part instead of host
             */
            @Override
            public String getValueFrom(URI uri) {
                return uri.getSchemeSpecificPart();
            }
        },
        Mido("mido", String.class), Pvlan("pvlan", String.class),
        Vxlan("vxlan", Long.class) {
            @Override
            public <T> URI toUri(T value) {
                try {
                    if (value.toString().contains("://"))
                        return new URI(value.toString());
                    else
                        return new URI("vxlan://" + value.toString());
                } catch (URISyntaxException e) {
                    throw new CloudRuntimeException("Unable to convert to broadcast URI: " + value);
                }
            }
        },
        Vcs("vcs", Integer.class) {
            @Override
            public <T> URI toUri(T value) {
                try {
                    if (value.toString().contains("://"))
                        return new URI(value.toString());
                    else
                        return new URI("vcs://" + value.toString());
                } catch (URISyntaxException e) {
                    throw new CloudRuntimeException("Unable to convert to broadcast URI: " + value);
                }
            }
        },
        UnDecided(null, null),
        OpenDaylight("opendaylight", String.class);

        private final String scheme;
        private final Class<?> type;

        private BroadcastDomainType(String scheme, Class<?> type) {
            this.scheme = scheme;
            this.type = type;
        }

        /**
         * @return scheme to be used in broadcast uri. Null indicates that this
         *         type does not have broadcast tags.
         */
        public String scheme() {
            return scheme;
        }

        /**
         * @return type of the value in the broadcast uri. Null indicates that
         *         this type does not have broadcast tags.
         */
        public Class<?> type() {
            return type;
        }

        /**
         * The default implementation of toUri returns an uri with the scheme and value as host
         *
         * @param value will be put in the host field
         * @return the resulting URI
         */
        public <T> URI toUri(T value) {
            try {
                return new URI(scheme + "://" + value.toString());
            } catch (URISyntaxException e) {
                throw new CloudRuntimeException("Unable to convert to broadcast URI: " + value);
            }
        }

        /**
         * get the enum value from this uri
         *
         * @param uri to get the scheme value from
         * @return the scheme as BroadcastDomainType
         */
        public static BroadcastDomainType getSchemeValue(URI uri) {
            return toEnumValue(uri.getScheme());
        }

        /**
         * gets the type from a string encoded uri
         *
         * @param str the uri string
         * @return the scheme as BroadcastDomainType
         * @throws URISyntaxException when the string can not be converted to URI
         */
        public static BroadcastDomainType getTypeOf(String str) throws URISyntaxException {
            if (com.cloud.dc.Vlan.UNTAGGED.equalsIgnoreCase(str)) {
                return Native;
            }
            return getSchemeValue(new URI(str));
        }

        /**
         * converts a String to a BroadcastDomainType
         *
         * @param scheme convert a string representation to a BroacastDomainType
         * @return the value of this
         */
        public static BroadcastDomainType toEnumValue(String scheme) {
            // scheme might be null and some of the enumvalue.scheme are as well, so
            if (scheme == null) {
                return UnDecided;
            }
            for (BroadcastDomainType type : values()) {
                if (scheme.equalsIgnoreCase(type.scheme())) {
                    return type;
                }
            }
            return UnDecided;
        }

        /**
         * The default implementation of getValueFrom returns the host part of the uri
         *
         * @param uri to get the value from
         * @return the host part as String
         */
        public String getValueFrom(URI uri) {
            return uri.getHost();
        }

        /**
         * get the BroadcastDomain value from an arbitrary URI
         * TODO what when the uri is useless
         *
         * @param uri the uri
         * @return depending on the scheme/BroadcastDomainType
         */
        public static String getValue(URI uri) {
            return getSchemeValue(uri).getValueFrom(uri);
        }

        /**
         * get the BroadcastDomain value from an arbitrary String
         * TODO what when the uriString is useless
         *
         * @param uriString the string encoded uri
         * @return depending on the scheme/BroadcastDomainType
         * @throws URISyntaxException the string is not even an uri
         */
        public static String getValue(String uriString) throws URISyntaxException {
            return getValue(new URI(uriString));
        }

        /**
         * encode a string into a BroadcastUri
         * @param candidate the input string
         * @return an URI containing an appropriate (possibly given) scheme and the value
         *
         */
        public static URI fromString(String candidate) {
            try {
                Long.parseLong(candidate);
                return Vlan.toUri(candidate);
            } catch (NumberFormatException nfe) {
                if (com.cloud.dc.Vlan.UNTAGGED.equalsIgnoreCase(candidate)) {
                    return Native.toUri(candidate);
                }
                try {
                    URI uri = new URI(candidate);
                    BroadcastDomainType tiep = getSchemeValue(uri);
                    if (tiep.scheme != null && tiep.scheme.equals(uri.getScheme())) {
                        return uri;
                    } else {
                        throw new CloudRuntimeException("string '" + candidate + "' has an unknown BroadcastDomainType.");
                    }
                } catch (URISyntaxException e) {
                    throw new CloudRuntimeException("string is not a broadcast URI: " + candidate);
                }
            }
        }
    };

    /**
     * Different types of network traffic in the data center.
     */
    public enum TrafficType {
        None, Public, Guest, Storage, Management, Control, Vpn;

        public static boolean isSystemNetwork(TrafficType trafficType) {
            if (Storage.equals(trafficType) || Management.equals(trafficType) || Control.equals(trafficType)) {
                return true;
            }
            return false;
        }

        public static TrafficType getTrafficType(String type) {
            if ("Public".equals(type)) {
                return Public;
            } else if ("Guest".equals(type)) {
                return Guest;
            } else if ("Storage".equals(type)) {
                return Storage;
            } else if ("Management".equals(type)) {
                return Management;
            } else if ("Control".equals(type)) {
                return Control;
            } else if ("Vpn".equals(type)) {
                return Vpn;
            } else {
                return None;
            }
        }
    };

    public enum IsolationType {
        None(null, null), Ec2("ec2", String.class), Vlan("vlan", Integer.class) {
            @Override
            public <T> URI toUri(T value) {
                try {
                    if (value.toString().contains(":"))
                        return new URI(value.toString());
                    else
                        return new URI("vlan", value.toString(), null, null);
                } catch (URISyntaxException e) {
                    throw new CloudRuntimeException("Unable to convert to isolation URI: " + value);
                }
            }
        },
        Vswitch("vs", String.class), Undecided(null, null), Vnet("vnet", Long.class);

        private final String scheme;
        private final Class<?> type;

        private IsolationType(String scheme, Class<?> type) {
            this.scheme = scheme;
            this.type = type;
        }

        public String scheme() {
            return scheme;
        }

        public Class<?> type() {
            return type;
        }

        public <T> URI toUri(T value) {
            try {
                return new URI(scheme + "://" + value.toString());
            } catch (URISyntaxException e) {
                throw new CloudRuntimeException("Unable to convert to isolation type URI: " + value);
            }
        }
    }

    public enum BroadcastScheme {
        Vlan("vlan"), VSwitch("vswitch");

        private final String scheme;

        private BroadcastScheme(String scheme) {
            this.scheme = scheme;
        }

        @Override
        public String toString() {
            return scheme;
        }
    }
}
