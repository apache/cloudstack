//
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
//

package com.cloud.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.LoadBalancerTO.DestinationTO;
import com.cloud.agent.api.to.LoadBalancerTO.StickinessPolicyTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;

public class HAProxyConfigurator implements LoadBalancerConfigurator {

    private static final Logger s_logger = Logger.getLogger(HAProxyConfigurator.class);
    private static final String blankLine = "\t ";
    private static String[] globalSection = {"global", "\tlog 127.0.0.1:3914   local0 warning", "\tmaxconn 4096", "\tmaxpipes 1024", "\tchroot /var/lib/haproxy",
        "\tuser haproxy", "\tgroup haproxy", "\tdaemon"};

    private static String[] defaultsSection = {"defaults", "\tlog     global", "\tmode    tcp", "\toption  dontlognull", "\tretries 3", "\toption redispatch",
        "\toption forwardfor", "\toption forceclose", "\ttimeout connect    5000", "\ttimeout client     50000", "\ttimeout server     50000"};

    private static String[] defaultListen = {"listen  vmops 0.0.0.0:9", "\toption transparent"};

    @Override
    public String[] generateConfiguration(final List<PortForwardingRuleTO> fwRules) {
        // Group the rules by publicip:publicport
        final Map<String, List<PortForwardingRuleTO>> pools = new HashMap<String, List<PortForwardingRuleTO>>();

        for (final PortForwardingRuleTO rule : fwRules) {
            final StringBuilder sb = new StringBuilder();
            final String poolName = sb.append(rule.getSrcIp().replace(".", "_")).append('-').append(rule.getSrcPortRange()[0]).toString();
            if (!rule.revoked()) {
                List<PortForwardingRuleTO> fwList = pools.get(poolName);
                if (fwList == null) {
                    fwList = new ArrayList<PortForwardingRuleTO>();
                    pools.put(poolName, fwList);
                }
                fwList.add(rule);
            }
        }

        final List<String> result = new ArrayList<String>();

        result.addAll(Arrays.asList(globalSection));
        result.add(blankLine);
        result.addAll(Arrays.asList(defaultsSection));
        result.add(blankLine);

        if (pools.isEmpty()) {
            // haproxy cannot handle empty listen / frontend or backend, so add
            // a dummy listener
            // on port 9
            result.addAll(Arrays.asList(defaultListen));
        }
        result.add(blankLine);

        for (final Map.Entry<String, List<PortForwardingRuleTO>> e : pools.entrySet()) {
            final List<String> poolRules = getRulesForPool(e.getKey(), e.getValue());
            result.addAll(poolRules);
        }

        return result.toArray(new String[result.size()]);
    }

    private List<String> getRulesForPool(final String poolName, final List<PortForwardingRuleTO> fwRules) {
        final PortForwardingRuleTO firstRule = fwRules.get(0);
        final String publicIP = firstRule.getSrcIp();
        final String publicPort = Integer.toString(firstRule.getSrcPortRange()[0]);
        // FIXEME: String algorithm = firstRule.getAlgorithm();

        final List<String> result = new ArrayList<String>();
        // add line like this: "listen  65_37_141_30-80 65.37.141.30:80"
        StringBuilder sb = new StringBuilder();
        sb.append("listen ").append(poolName).append(" ").append(publicIP).append(":").append(publicPort);
        result.add(sb.toString());
        sb = new StringBuilder();
        // FIXME sb.append("\t").append("balance ").append(algorithm);
        result.add(sb.toString());
        if (publicPort.equals(NetUtils.HTTP_PORT)
                // && global option httpclose set (or maybe not in this spot???)
                ) {
            sb = new StringBuilder();
            sb.append("\t").append("mode http");
            result.add(sb.toString());
            sb = new StringBuilder();
            sb.append("\t").append("option httpclose");
            result.add(sb.toString());
        }
        int i = 0;
        for (final PortForwardingRuleTO rule : fwRules) {
            // add line like this: "server  65_37_141_30-80_3 10.1.1.4:80 check"
            if (rule.revoked()) {
                continue;
            }
            sb = new StringBuilder();
            sb.append("\t")
            .append("server ")
            .append(poolName)
            .append("_")
            .append(Integer.toString(i++))
            .append(" ")
            .append(rule.getDstIp())
            .append(":")
            .append(rule.getDstPortRange()[0])
            .append(" check");
            result.add(sb.toString());
        }
        result.add(blankLine);
        return result;
    }

    /*
    cookie <name> [ rewrite | insert | prefix ] [ indirect ] [ nocache ]
                [ postonly ] [ domain <domain> ]*
    Enable cookie-based persistence in a backend.
    May be used in sections :   defaults | frontend | listen | backend
                                   yes   |    no    |   yes  |   yes
    Arguments :
      <name>    is the name of the cookie which will be monitored, modified or
                inserted in order to bring persistence. This cookie is sent to
                the client via a "Set-Cookie" header in the response, and is
                brought back by the client in a "Cookie" header in all requests.
                Special care should be taken to choose a name which does not
                conflict with any likely application cookie. Also, if the same
                backends are subject to be used by the same clients (eg:
                HTTP/HTTPS), care should be taken to use different cookie names
                between all backends if persistence between them is not desired.

      rewrite   This keyword indicates that the cookie will be provided by the
                server and that haproxy will have to modify its value to set the
                server's identifier in it. This mode is handy when the management
                of complex combinations of "Set-cookie" and "Cache-control"
                headers is left to the application. The application can then
                decide whether or not it is appropriate to emit a persistence
                cookie. Since all responses should be monitored, this mode only
                works in HTTP close mode. Unless the application behaviour is
                very complex and/or broken, it is advised not to start with this
                mode for new deployments. This keyword is incompatible with
                "insert" and "prefix".

      insert    This keyword indicates that the persistence cookie will have to
                be inserted by haproxy in the responses. If the server emits a
                cookie with the same name, it will be replaced anyway. For this
                reason, this mode can be used to upgrade existing configurations
                running in the "rewrite" mode. The cookie will only be a session
                cookie and will not be stored on the client's disk. Due to
                caching effects, it is generally wise to add the "indirect" and
                "nocache" or "postonly" keywords (see below). The "insert"
                keyword is not compatible with "rewrite" and "prefix".

      prefix    This keyword indicates that instead of relying on a dedicated
                cookie for the persistence, an existing one will be completed.
                This may be needed in some specific environments where the client
                does not support more than one single cookie and the application
                already needs it. In this case, whenever the server sets a cookie
                named <name>, it will be prefixed with the server's identifier
                and a delimiter. The prefix will be removed from all client
                requests so that the server still finds the cookie it emitted.
                Since all requests and responses are subject to being modified,
                this mode requires the HTTP close mode. The "prefix" keyword is
                not compatible with "rewrite" and "insert".

      indirect  When this option is specified in insert mode, cookies will only
                be added when the server was not reached after a direct access,
                which means that only when a server is elected after applying a
                load-balancing algorithm, or after a redispatch, then the cookie
                will be inserted. If the client has all the required information
                to connect to the same server next time, no further cookie will
                be inserted. In all cases, when the "indirect" option is used in
                insert mode, the cookie is always removed from the requests
                transmitted to the server. The persistence mechanism then becomes
                totally transparent from the application point of view.

      nocache   This option is recommended in conjunction with the insert mode
                when there is a cache between the client and HAProxy, as it
                ensures that a cacheable response will be tagged non-cacheable if
                a cookie needs to be inserted. This is important because if all
                persistence cookies are added on a cacheable home page for
                instance, then all customers will then fetch the page from an
                outer cache and will all share the same persistence cookie,
                leading to one server receiving much more traffic than others.
                See also the "insert" and "postonly" options.

      postonly  This option ensures that cookie insertion will only be performed
                on responses to POST requests. It is an alternative to the
                "nocache" option, because POST responses are not cacheable, so
                this ensures that the persistence cookie will never get cached.
                Since most sites do not need any sort of persistence before the
                first POST which generally is a login request, this is a very
                efficient method to optimize caching without risking to find a
                persistence cookie in the cache.
                See also the "insert" and "nocache" options.

      domain    This option allows to specify the domain at which a cookie is
                inserted. It requires exactly one parameter: a valid domain
                name. If the domain begins with a dot, the browser is allowed to
                use it for any host ending with that name. It is also possible to
                specify several domain names by invoking this option multiple
                times. Some browsers might have small limits on the number of
                domains, so be careful when doing that. For the record, sending
                10 domains to MSIE 6 or Firefox 2 works as expected.

    There can be only one persistence cookie per HTTP backend, and it can be
    declared in a defaults section. The value of the cookie will be the value
    indicated after the "cookie" keyword in a "server" statement. If no cookie
    is declared for a given server, the cookie is not set.

    Examples :
          cookie JSESSIONID prefix
          cookie SRV insert indirect nocache
          cookie SRV insert postonly indirect


    appsession <cookie> len <length> timeout <holdtime>
             [request-learn] [prefix] [mode <path-parameters|query-string>]
    Define session stickiness on an existing application cookie.
    May be used in sections :   defaults | frontend | listen | backend
                                   no    |    no    |   yes  |   yes
    Arguments :
      <cookie>   this is the name of the cookie used by the application and which
                 HAProxy will have to learn for each new session.

      <length>   this is the max number of characters that will be memorized and
                 checked in each cookie value.

      <holdtime> this is the time after which the cookie will be removed from
                 memory if unused. If no unit is specified, this time is in
                 milliseconds.

      request-learn
                 If this option is specified, then haproxy will be able to learn
                 the cookie found in the request in case the server does not
                 specify any in response. This is typically what happens with
                 PHPSESSID cookies, or when haproxy's session expires before
                 the application's session and the correct server is selected.
                 It is recommended to specify this option to improve reliability.

      prefix     When this option is specified, haproxy will match on the cookie
                 prefix (or URL parameter prefix). The appsession value is the
                 data following this prefix.

                 Example :
                 appsession ASPSESSIONID len 64 timeout 3h prefix

                 This will match the cookie ASPSESSIONIDXXXX=XXXXX,
                 the appsession value will be XXXX=XXXXX.

      mode       This option allows to change the URL parser mode.
                 2 modes are currently supported :
                 - path-parameters :
                   The parser looks for the appsession in the path parameters
                   part (each parameter is separated by a semi-colon), which is
                   convenient for JSESSIONID for example.
                   This is the default mode if the option is not set.
                 - query-string :
                   In this mode, the parser will look for the appsession in the
                   query string.

    When an application cookie is defined in a backend, HAProxy will check when
    the server sets such a cookie, and will store its value in a table, and
    associate it with the server's identifier. Up to <length> characters from
    the value will be retained. On each connection, haproxy will look for this
    cookie both in the "Cookie:" headers, and as a URL parameter (depending on
    the mode used). If a known value is found, the client will be directed to the
    server associated with this value. Otherwise, the load balancing algorithm is
    applied. Cookies are automatically removed from memory when they have been
    unused for a duration longer than <holdtime>.

    The definition of an application cookie is limited to one per backend.
    Example :
          appsession JSESSIONID len 52 timeout 3h
     */
    private String getLbSubRuleForStickiness(final LoadBalancerTO lbTO) {
        int i = 0;

        if (lbTO.getStickinessPolicies() == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();

        for (final StickinessPolicyTO stickinessPolicy : lbTO.getStickinessPolicies()) {
            if (stickinessPolicy == null) {
                continue;
            }
            final List<Pair<String, String>> paramsList = stickinessPolicy.getParams();
            i++;

            /*
             * cookie <name> [ rewrite | insert | prefix ] [ indirect ] [ nocache ]
              [ postonly ] [ domain <domain> ]*

             */
            if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                /* Default Values */
                String cookieName = null; // optional
                String mode = "insert "; // optional
                Boolean indirect = false; // optional
                Boolean nocache = false; // optional
                Boolean postonly = false; // optional
                StringBuilder domainSb = null; // optional

                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("cookie-name".equalsIgnoreCase(key)) {
                        cookieName = value;
                    }
                    if ("mode".equalsIgnoreCase(key)) {
                        mode = value;
                    }
                    if ("domain".equalsIgnoreCase(key)) {
                        if (domainSb == null) {
                            domainSb = new StringBuilder();
                        }
                        domainSb = domainSb.append("domain ");
                        domainSb.append(value).append(" ");
                    }
                    if ("indirect".equalsIgnoreCase(key)) {
                        indirect = true;
                    }
                    if ("nocache".equalsIgnoreCase(key)) {
                        nocache = true;
                    }
                    if ("postonly".equalsIgnoreCase(key)) {
                        postonly = true;
                    }
                }
                if (cookieName == null) {// re-check all haproxy mandatory params
                    final StringBuilder tempSb = new StringBuilder();
                    String srcip = lbTO.getSrcIp();
                    if (srcip == null) {
                        srcip = "TESTCOOKIE";
                    }
                    tempSb.append("lbcooki_").append(srcip.hashCode()).append("_").append(lbTO.getSrcPort());
                    cookieName = tempSb.toString();
                }
                sb.append("\t").append("cookie ").append(cookieName).append(" ").append(mode).append(" ");
                if (indirect) {
                    sb.append("indirect ");
                }
                if (nocache) {
                    sb.append("nocache ");
                }
                if (postonly) {
                    sb.append("postonly ");
                }
                if (domainSb != null) {
                    sb.append(domainSb).append(" ");
                }
            } else if (StickinessMethodType.SourceBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                /* Default Values */
                String tablesize = "200k"; // optional
                String expire = "30m"; // optional

                /* overwrite default values with the stick parameters */
                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("tablesize".equalsIgnoreCase(key)) {
                        tablesize = value;
                    }
                    if ("expire".equalsIgnoreCase(key)) {
                        expire = value;
                    }
                }
                sb.append("\t").append("stick-table type ip size ").append(tablesize).append(" expire ").append(expire);
                sb.append("\n\t").append("stick on src");
            } else if (StickinessMethodType.AppCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                /*
                 * FORMAT : appsession <cookie> len <length> timeout <holdtime>
                 * [request-learn] [prefix] [mode
                 * <path-parameters|query-string>]
                 */
                /* example: appsession JSESSIONID len 52 timeout 3h */
                String cookieName = null; // optional
                String length = "52"; // optional
                String holdtime = "3h"; // optional
                String mode = null; // optional
                Boolean requestlearn = false; // optional
                Boolean prefix = false; // optional

                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("cookie-name".equalsIgnoreCase(key)) {
                        cookieName = value;
                    }
                    if ("length".equalsIgnoreCase(key)) {
                        length = value;
                    }
                    if ("holdtime".equalsIgnoreCase(key)) {
                        holdtime = value;
                    }
                    if ("mode".equalsIgnoreCase(key)) {
                        mode = value;
                    }
                    if ("request-learn".equalsIgnoreCase(key)) {
                        requestlearn = true;
                    }
                    if ("prefix".equalsIgnoreCase(key)) {
                        prefix = true;
                    }
                }
                if (cookieName == null) {// re-check all haproxy mandatory params
                    final StringBuilder tempSb = new StringBuilder();
                    String srcip = lbTO.getSrcIp();
                    if (srcip == null) {
                        srcip = "TESTCOOKIE";
                    }
                    tempSb.append("appcookie_").append(srcip.hashCode()).append("_").append(lbTO.getSrcPort());
                    cookieName = tempSb.toString();
                }
                sb.append("\t").append("appsession ").append(cookieName).append(" len ").append(length).append(" timeout ").append(holdtime).append(" ");
                if (prefix) {
                    sb.append("prefix ");
                }
                if (requestlearn) {
                    sb.append("request-learn").append(" ");
                }
                if (mode != null) {
                    sb.append("mode ").append(mode).append(" ");
                }
            } else {
                /*
                 * Error is silently swallowed.
                 * Not supposed to reach here, validation of methods are
                 * done at the higher layer
                 */
                s_logger.warn("Haproxy stickiness policy for lb rule: " + lbTO.getSrcIp() + ":" + lbTO.getSrcPort() + ": Not Applied, cause:invalid method ");
                return null;
            }
        }
        if (i == 0) {
            return null;
        }
        return sb.toString();
    }

    private List<String> getRulesForPool(final LoadBalancerTO lbTO, final boolean keepAliveEnabled) {
        StringBuilder sb = new StringBuilder();
        final String poolName = sb.append(lbTO.getSrcIp().replace(".", "_")).append('-').append(lbTO.getSrcPort()).toString();
        final String publicIP = lbTO.getSrcIp();
        final String publicPort = Integer.toString(lbTO.getSrcPort());
        final String algorithm = lbTO.getAlgorithm();

        final List<String> result = new ArrayList<String>();
        // add line like this: "listen  65_37_141_30-80 65.37.141.30:80"
        sb = new StringBuilder();
        sb.append("listen ").append(poolName).append(" ").append(publicIP).append(":").append(publicPort);
        result.add(sb.toString());
        sb = new StringBuilder();
        sb.append("\t").append("balance ").append(algorithm);
        result.add(sb.toString());

        int i = 0;
        Boolean destsAvailable = false;
        final String stickinessSubRule = getLbSubRuleForStickiness(lbTO);
        final List<String> dstSubRule = new ArrayList<String>();
        final List<String> dstWithCookieSubRule = new ArrayList<String>();
        for (final DestinationTO dest : lbTO.getDestinations()) {
            // add line like this: "server  65_37_141_30-80_3 10.1.1.4:80 check"
            if (dest.isRevoked()) {
                continue;
            }
            sb = new StringBuilder();
            sb.append("\t")
            .append("server ")
            .append(poolName)
            .append("_")
            .append(Integer.toString(i++))
            .append(" ")
            .append(dest.getDestIp())
            .append(":")
            .append(dest.getDestPort())
            .append(" check");
            if(lbTO.getLbProtocol() != null && lbTO.getLbProtocol().equals("tcp-proxy")) {
                sb.append(" send-proxy");
            }
            dstSubRule.add(sb.toString());
            if (stickinessSubRule != null) {
                sb.append(" cookie ").append(dest.getDestIp().replace(".", "_")).append('-').append(dest.getDestPort()).toString();
                dstWithCookieSubRule.add(sb.toString());
            }
            destsAvailable = true;
        }

        Boolean httpbasedStickiness = false;
        /* attach stickiness sub rule only if the destinations are available */
        if (stickinessSubRule != null && destsAvailable == true) {
            for (final StickinessPolicyTO stickinessPolicy : lbTO.getStickinessPolicies()) {
                if (stickinessPolicy == null) {
                    continue;
                }
                if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName()) ||
                        StickinessMethodType.AppCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                    httpbasedStickiness = true;
                }
            }
            if (httpbasedStickiness) {
                result.addAll(dstWithCookieSubRule);
            } else {
                result.addAll(dstSubRule);
            }
            result.add(stickinessSubRule);
        } else {
            result.addAll(dstSubRule);
        }
        if (stickinessSubRule != null && !destsAvailable) {
            s_logger.warn("Haproxy stickiness policy for lb rule: " + lbTO.getSrcIp() + ":" + lbTO.getSrcPort() + ": Not Applied, cause:  backends are unavailable");
        }
        if (publicPort.equals(NetUtils.HTTP_PORT) && !keepAliveEnabled || httpbasedStickiness) {
            sb = new StringBuilder();
            sb.append("\t").append("mode http");
            result.add(sb.toString());
            sb = new StringBuilder();
            sb.append("\t").append("option httpclose");
            result.add(sb.toString());
        }

        result.add(blankLine);
        return result;
    }

    private String generateStatsRule(final LoadBalancerConfigCommand lbCmd, final String ruleName, final String statsIp) {
        final StringBuilder rule = new StringBuilder("\nlisten ").append(ruleName).append(" ").append(statsIp).append(":").append(lbCmd.lbStatsPort);
        // TODO DH: write test for this in both cases
        if (!lbCmd.keepAliveEnabled) {
            s_logger.info("Haproxy mode http enabled");
            rule.append("\n\tmode http\n\toption httpclose");
        }
        rule.append("\n\tstats enable\n\tstats uri     ")
        .append(lbCmd.lbStatsUri)
        .append("\n\tstats realm   Haproxy\\ Statistics\n\tstats auth    ")
        .append(lbCmd.lbStatsAuth);
        rule.append("\n");
        final String result = rule.toString();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Haproxystats rule: " + result);
        }
        return result;
    }

    @Override
    public String[] generateConfiguration(final LoadBalancerConfigCommand lbCmd) {
        final List<String> result = new ArrayList<String>();
        final List<String> gSection = Arrays.asList(globalSection);
        //        note that this is overwritten on the String in the static ArrayList<String>
        gSection.set(2, "\tmaxconn " + lbCmd.maxconn);
        // TODO DH: write test for this function
        final String pipesLine = "\tmaxpipes " + Long.toString(Long.parseLong(lbCmd.maxconn) / 4);
        gSection.set(3, pipesLine);
        if (s_logger.isDebugEnabled()) {
            for (final String s : gSection) {
                s_logger.debug("global section: " + s);
            }
        }
        result.addAll(gSection);
        // TODO decide under what circumstances these options are needed
        //        result.add("\tnokqueue");
        //        result.add("\tnopoll");

        result.add(blankLine);
        final List<String> dSection = Arrays.asList(defaultsSection);
        if (lbCmd.keepAliveEnabled) {
            dSection.set(7, "\tno option forceclose");
        }

        if (s_logger.isDebugEnabled()) {
            for (final String s : dSection) {
                s_logger.debug("default section: " + s);
            }
        }
        result.addAll(dSection);
        if (!lbCmd.lbStatsVisibility.equals("disabled")) {
            /* new rule : listen admin_page guestip/link-local:8081 */
            if (lbCmd.lbStatsVisibility.equals("global")) {
                result.add(generateStatsRule(lbCmd, "stats_on_public", lbCmd.lbStatsPublicIP));
            } else if (lbCmd.lbStatsVisibility.equals("guest-network")) {
                result.add(generateStatsRule(lbCmd, "stats_on_guest", lbCmd.lbStatsGuestIP));
            } else if (lbCmd.lbStatsVisibility.equals("link-local")) {
                result.add(generateStatsRule(lbCmd, "stats_on_private", lbCmd.lbStatsPrivateIP));
            } else if (lbCmd.lbStatsVisibility.equals("all")) {
                result.add(generateStatsRule(lbCmd, "stats_on_public", lbCmd.lbStatsPublicIP));
                result.add(generateStatsRule(lbCmd, "stats_on_guest", lbCmd.lbStatsGuestIP));
                result.add(generateStatsRule(lbCmd, "stats_on_private", lbCmd.lbStatsPrivateIP));
            } else {
                /*
                 * stats will be available on the default http serving port, no
                 * special stats port
                 */
                final StringBuilder subRule =
                        new StringBuilder("\tstats enable\n\tstats uri     ").append(lbCmd.lbStatsUri)
                        .append("\n\tstats realm   Haproxy\\ Statistics\n\tstats auth    ")
                        .append(lbCmd.lbStatsAuth);
                result.add(subRule.toString());
            }

        }
        result.add(blankLine);
        boolean has_listener = false;
        for (final LoadBalancerTO lbTO : lbCmd.getLoadBalancers()) {
            if (lbTO.isRevoked()) {
                continue;
            }
            final List<String> poolRules = getRulesForPool(lbTO, lbCmd.keepAliveEnabled);
            result.addAll(poolRules);
            has_listener = true;
        }
        result.add(blankLine);
        if (!has_listener) {
            // haproxy cannot handle empty listen / frontend or backend, so add
            // a dummy listener
            // on port 9
            result.addAll(Arrays.asList(defaultListen));
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public String[][] generateFwRules(final LoadBalancerConfigCommand lbCmd) {
        final String[][] result = new String[3][];
        final Set<String> toAdd = new HashSet<String>();
        final Set<String> toRemove = new HashSet<String>();
        final Set<String> toStats = new HashSet<String>();

        for (final LoadBalancerTO lbTO : lbCmd.getLoadBalancers()) {

            final StringBuilder sb = new StringBuilder();
            sb.append(lbTO.getSrcIp()).append(":");
            sb.append(lbTO.getSrcPort()).append(":");
            final String lbRuleEntry = sb.toString();
            if (!lbTO.isRevoked()) {
                toAdd.add(lbRuleEntry);
            } else {
                toRemove.add(lbRuleEntry);
            }
        }
        StringBuilder sb = new StringBuilder("");
        if (lbCmd.lbStatsVisibility.equals("guest-network")) {
            sb = new StringBuilder(lbCmd.lbStatsGuestIP).append(":").append(lbCmd.lbStatsPort).append(":").append(lbCmd.lbStatsSrcCidrs).append(":,");
        } else if (lbCmd.lbStatsVisibility.equals("link-local")) {
            sb = new StringBuilder(lbCmd.lbStatsPrivateIP).append(":").append(lbCmd.lbStatsPort).append(":").append(lbCmd.lbStatsSrcCidrs).append(":,");
        } else if (lbCmd.lbStatsVisibility.equals("global")) {
            sb = new StringBuilder(lbCmd.lbStatsPublicIP).append(":").append(lbCmd.lbStatsPort).append(":").append(lbCmd.lbStatsSrcCidrs).append(":,");
        } else if (lbCmd.lbStatsVisibility.equals("all")) {
            sb = new StringBuilder("0.0.0.0/0").append(":").append(lbCmd.lbStatsPort).append(":").append(lbCmd.lbStatsSrcCidrs).append(":,");
        }
        toStats.add(sb.toString());

        toRemove.removeAll(toAdd);
        result[ADD] = toAdd.toArray(new String[toAdd.size()]);
        result[REMOVE] = toRemove.toArray(new String[toRemove.size()]);
        result[STATS] = toStats.toArray(new String[toStats.size()]);

        return result;
    }
}
