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
package com.cloud.kubernetes.cluster.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.exception.InvalidParameterValueException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KubeadmConfigBuilderTest {

    private static List<JsonObject> parse(String config) {
        List<JsonObject> documents = new ArrayList<>();
        for (String document : config.split("\n---\n")) {
            documents.add(JsonParser.parseString(document).getAsJsonObject());
        }
        return documents;
    }

    @Test
    public void buildStackedWithoutSettings() {
        List<JsonObject> docs = parse(KubeadmConfigBuilder.build("1.30.2", "10.1.1.10", "10.1.1.10", 6443, Collections.emptyList(),
                "abcdef.0123456789abcdef", "3600s", "key", false, Collections.emptyMap(), false, null));
        Assert.assertEquals(2, docs.size());
        JsonObject cluster = docs.get(0);
        Assert.assertEquals("ClusterConfiguration", cluster.get("kind").getAsString());
        Assert.assertEquals("kubeadm.k8s.io/v1beta3", cluster.get("apiVersion").getAsString());
        Assert.assertEquals("1.30.2", cluster.get("kubernetesVersion").getAsString());
        Assert.assertFalse(cluster.has("controlPlaneEndpoint"));
        Assert.assertFalse(cluster.has("etcd"));
        Assert.assertEquals(1, cluster.getAsJsonObject("apiServer").getAsJsonArray("certSANs").size());
        Assert.assertEquals("10.1.1.10", cluster.getAsJsonObject("apiServer").getAsJsonArray("certSANs").get(0).getAsString());
        Assert.assertFalse(cluster.getAsJsonObject("apiServer").has("extraArgs"));
        JsonObject init = docs.get(1);
        Assert.assertEquals("InitConfiguration", init.get("kind").getAsString());
        Assert.assertEquals("abcdef.0123456789abcdef", init.getAsJsonArray("bootstrapTokens").get(0).getAsJsonObject().get("token").getAsString());
        Assert.assertEquals("3600s", init.getAsJsonArray("bootstrapTokens").get(0).getAsJsonObject().get("ttl").getAsString());
        Assert.assertFalse(init.has("localAPIEndpoint"));
    }

    @Test
    public void buildExternalEtcd() {
        List<JsonObject> docs = parse(KubeadmConfigBuilder.build("1.30.2", "10.1.1.10", "10.1.1.10", 6443, List.of("http://10.1.1.20:2379"),
                "abcdef.0123456789abcdef", "0", "key", false, Collections.emptyMap(), false, null));
        JsonObject cluster = docs.get(0);
        Assert.assertEquals("10.1.1.10:6443", cluster.get("controlPlaneEndpoint").getAsString());
        Assert.assertEquals("http://10.1.1.20:2379", cluster.getAsJsonObject("etcd").getAsJsonObject("external").getAsJsonArray("endpoints").get(0).getAsString());
        JsonObject init = docs.get(1);
        Assert.assertEquals("10.1.1.10", init.getAsJsonObject("localAPIEndpoint").get("advertiseAddress").getAsString());
        Assert.assertEquals(6443, init.getAsJsonObject("localAPIEndpoint").get("bindPort").getAsInt());
        Assert.assertEquals("key", init.get("certificateKey").getAsString());
    }

    @Test
    public void buildExternalEtcdBehindPublicIp() {
        List<JsonObject> docs = parse(KubeadmConfigBuilder.build("1.30.2", "10.1.1.10", "203.0.113.5", 6443, List.of("http://10.1.1.20:2379"),
                "abcdef.0123456789abcdef", "0", "key", false, Collections.emptyMap(), false, null));
        JsonObject cluster = docs.get(0);
        Assert.assertEquals("203.0.113.5:6443", cluster.get("controlPlaneEndpoint").getAsString());
        JsonArray certSans = cluster.getAsJsonObject("apiServer").getAsJsonArray("certSANs");
        Assert.assertEquals(2, certSans.size());
        Assert.assertEquals("10.1.1.10", certSans.get(0).getAsString());
        Assert.assertEquals("203.0.113.5", certSans.get(1).getAsString());
        Assert.assertEquals("10.1.1.10", docs.get(1).getAsJsonObject("localAPIEndpoint").get("advertiseAddress").getAsString());
    }

    @Test
    public void buildStackedWithSettingsBehindPublicIp() {
        List<JsonObject> docs = parse(KubeadmConfigBuilder.build("1.30.2", "10.1.1.10", "203.0.113.5", 6443, Collections.emptyList(),
                "abcdef.0123456789abcdef", "0", "key", true, Map.of("oidc-client-id", "kubernetes"), false, null));
        JsonArray certSans = docs.get(0).getAsJsonObject("apiServer").getAsJsonArray("certSANs");
        Assert.assertEquals(2, certSans.size());
        Assert.assertEquals("10.1.1.10", certSans.get(0).getAsString());
        Assert.assertEquals("203.0.113.5", certSans.get(1).getAsString());
        Assert.assertFalse(docs.get(1).has("localAPIEndpoint"));
    }

    @Test
    public void buildWithSettings() {
        String kubelet = KubeadmConfigBuilder.normalizeKubeletConfig("{\"systemReserved\": {\"cpu\": \"500m\"}, \"evictionHard\": {\"memory.available\": \"10%\"}}");
        List<JsonObject> docs = parse(KubeadmConfigBuilder.build("1.30.2", "10.1.1.10", "10.1.1.10", 6443, Collections.emptyList(),
                "abcdef.0123456789abcdef", "0", "key", true,
                Map.of("oidc-issuer-url", "https://idp.example.com/realms/k8s", "audit-log-maxage", "7"), true, kubelet));
        Assert.assertEquals(3, docs.size());
        JsonObject cluster = docs.get(0);
        Assert.assertEquals("10.1.1.10:6443", cluster.get("controlPlaneEndpoint").getAsString());
        JsonObject extraArgs = cluster.getAsJsonObject("apiServer").getAsJsonObject("extraArgs");
        Assert.assertEquals("https://idp.example.com/realms/k8s", extraArgs.get("oidc-issuer-url").getAsString());
        Assert.assertEquals(KubeadmConfigBuilder.AUDIT_POLICY_PATH, extraArgs.get("audit-policy-file").getAsString());
        Assert.assertEquals("7", extraArgs.get("audit-log-maxage").getAsString());
        Assert.assertEquals("100", extraArgs.get("audit-log-maxsize").getAsString());
        Assert.assertEquals(2, cluster.getAsJsonObject("apiServer").getAsJsonArray("extraVolumes").size());
        JsonObject kubeletDoc = docs.get(2);
        Assert.assertEquals("KubeletConfiguration", kubeletDoc.get("kind").getAsString());
        Assert.assertEquals("500m", kubeletDoc.getAsJsonObject("systemReserved").get("cpu").getAsString());
        Assert.assertEquals("10%", kubeletDoc.getAsJsonObject("evictionHard").get("memory.available").getAsString());
    }

    @Test
    public void buildV1beta4ArgsAsList() {
        List<JsonObject> docs = parse(KubeadmConfigBuilder.build("1.31.0", "10.1.1.10", "10.1.1.10", 6443, Collections.emptyList(),
                "abcdef.0123456789abcdef", "0", "key", false, Map.of("oidc-client-id", "kubernetes"), false, null));
        Assert.assertEquals("kubeadm.k8s.io/v1beta4", docs.get(0).get("apiVersion").getAsString());
        Assert.assertEquals("kubeadm.k8s.io/v1beta4", docs.get(1).get("apiVersion").getAsString());
        JsonObject arg = docs.get(0).getAsJsonObject("apiServer").getAsJsonArray("extraArgs").get(0).getAsJsonObject();
        Assert.assertEquals("oidc-client-id", arg.get("name").getAsString());
        Assert.assertEquals("kubernetes", arg.get("value").getAsString());
    }

    @Test
    public void validApiServerArgs() {
        KubeadmConfigBuilder.validateApiServerArgs(Map.of("oidc-username-claim", "email", "oidc-username-prefix", "",
                "oidc-groups-prefix", "oidc:", "oidc-required-claim", "hd=example.com"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void apiServerArgWithDashes() {
        KubeadmConfigBuilder.validateApiServerArgs(Map.of("--oidc-client-id", "kubernetes"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void apiServerArgWithSpace() {
        KubeadmConfigBuilder.validateApiServerArgs(Map.of("oidc-client-id", "kubernetes --anonymous-auth=true"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void apiServerArgWithTemplate() {
        KubeadmConfigBuilder.validateApiServerArgs(Map.of("oidc-client-id", "{{ds.meta_data}}"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void apiServerArgManaged() {
        KubeadmConfigBuilder.validateApiServerArgs(Map.of("audit-log-path", "/tmp/audit.log"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void apiServerArgRequestHeader() {
        KubeadmConfigBuilder.validateApiServerArgs(Map.of("requestheader-allowed-names", "foo"));
    }

    @Test
    public void buildKeepsNodeRestriction() {
        List<JsonObject> docs = parse(KubeadmConfigBuilder.build("1.30.2", "10.1.1.10", "10.1.1.10", 6443, Collections.emptyList(),
                "abcdef.0123456789abcdef", "0", "key", false, Map.of("enable-admission-plugins", "PodNodeSelector"), false, null));
        Assert.assertEquals("PodNodeSelector,NodeRestriction",
                docs.get(0).getAsJsonObject("apiServer").getAsJsonObject("extraArgs").get("enable-admission-plugins").getAsString());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void apiServerArgNeededToJoin() {
        KubeadmConfigBuilder.validateApiServerArgs(Map.of("anonymous-auth", "false"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void apiServerArgSecurePort() {
        KubeadmConfigBuilder.validateApiServerArgs(Map.of("secure-port", "7443"));
    }

    @Test
    public void apiServerArgsToJsonKeepsEquals() {
        Assert.assertEquals("{\"a\":\"x=y\",\"b\":\"1\"}", KubeadmConfigBuilder.apiServerArgsToJson(Map.of("b", "1", "a", "x=y")));
    }

    @Test
    public void apiServerArgsFromJson() {
        Assert.assertTrue(KubeadmConfigBuilder.apiServerArgsFromJson(null).isEmpty());
        Assert.assertEquals(Map.of("a", "x=y"), KubeadmConfigBuilder.apiServerArgsFromJson("{\"a\":\"x=y\"}"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void kubeletConfigCgroupDriver() {
        KubeadmConfigBuilder.normalizeKubeletConfig("{\"cgroupDriver\": \"cgroupfs\"}");
    }

    @Test
    public void kubeletConfigBlank() {
        Assert.assertNull(KubeadmConfigBuilder.normalizeKubeletConfig(" "));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void kubeletConfigNotJson() {
        KubeadmConfigBuilder.normalizeKubeletConfig("systemReserved: {cpu: 500m}");
    }

    @Test
    public void kubeletConfigNotStrictJson() {
        for (String config : new String[] {"{maxPods: 200}", "{'maxPods': '200'}", "{\"maxPods\": 200} {}"}) {
            try {
                KubeadmConfigBuilder.normalizeKubeletConfig(config);
                Assert.fail("accepted " + config);
            } catch (InvalidParameterValueException e) {
                Assert.assertTrue(e.getMessage().startsWith("Kubelet configuration must be a JSON object"));
            }
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void kubeletConfigNullValue() {
        KubeadmConfigBuilder.normalizeKubeletConfig("{\"evictionHard\": {\"memory.available\": null}}");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void kubeletConfigNotObject() {
        KubeadmConfigBuilder.normalizeKubeletConfig("[\"a\"]");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void kubeletConfigKind() {
        KubeadmConfigBuilder.normalizeKubeletConfig("{\"kind\": \"KubeletConfiguration\"}");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void kubeletConfigUnsafeValue() {
        KubeadmConfigBuilder.normalizeKubeletConfig("{\"systemReserved\": {\"cpu\": \"{% raw %}\"}}");
    }

    @Test
    public void kubeletConfigNormalized() {
        Assert.assertEquals("{\"maxPods\":200,\"serializeImagePulls\":false}",
                KubeadmConfigBuilder.normalizeKubeletConfig("{ \"maxPods\": 200,\n \"serializeImagePulls\": false }"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void auditPolicySupplementaryCharacter() {
        KubeadmConfigBuilder.validateAuditPolicy("# \uD83D\uDE00\nrules: []\n");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void auditPolicyBlank() {
        KubeadmConfigBuilder.validateAuditPolicy(" ");
    }

    @Test
    public void auditPolicyWriteFile() throws IOException {
        Assert.assertEquals("", KubeadmConfigBuilder.auditPolicyWriteFile(null));
        String policy = "apiVersion: audit.k8s.io/v1\nkind: Policy\nrules:\n- level: Metadata\n";
        String entry = KubeadmConfigBuilder.auditPolicyWriteFile(policy);
        Assert.assertTrue(entry.startsWith("- path: " + KubeadmConfigBuilder.AUDIT_POLICY_PATH + "\n"));
        Assert.assertTrue(entry.contains("    encoding: gz+b64\n"));
        String content = entry.substring(entry.indexOf("content: ") + "content: ".length());
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(content)))) {
            Assert.assertEquals(policy, new String(gzip.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
