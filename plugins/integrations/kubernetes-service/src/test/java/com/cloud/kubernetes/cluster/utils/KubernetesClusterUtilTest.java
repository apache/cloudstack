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

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterUtilTest {

    private void executeThrowAndTestVersionMatch() {
        Pair<Boolean, String> resultPair = null;
        Pair<Boolean, String> result = KubernetesClusterUtil.clusterNodeVersionMatches(resultPair, "1.24.0");
        Assert.assertFalse(result.first());
    }

    private void executeAndTestVersionMatch(boolean status, String response, boolean expectedResult) {
        Pair<Boolean, String> resultPair = new Pair<>(status, response);
        Pair<Boolean, String> result = KubernetesClusterUtil.clusterNodeVersionMatches(resultPair, "1.24.0");
        Assert.assertEquals(expectedResult, result.first());
    }

    @Test
    public void testClusterNodeVersionMatches() {
        String v1233WorkerNodeOutput = "v1.23.3";
        String v1240WorkerNodeOutput = "v1.24.0";

        executeAndTestVersionMatch(true, v1240WorkerNodeOutput, true);

        executeAndTestVersionMatch(true, v1233WorkerNodeOutput, false);

        executeAndTestVersionMatch(false, v1240WorkerNodeOutput, false);

        executeAndTestVersionMatch(false, v1233WorkerNodeOutput, false);

        executeThrowAndTestVersionMatch();
    }

    @Test
    public void testGenerateBootstrapToken() {
        String token = KubernetesClusterUtil.generateBootstrapToken();
        Assert.assertTrue(token.matches("[a-z0-9]{6}\\.[a-z0-9]{16}"));
        Assert.assertNotEquals(token, KubernetesClusterUtil.generateBootstrapToken());
    }

    @Test
    public void testGenerateCertificateKey() {
        String key = KubernetesClusterUtil.generateCertificateKey();
        Assert.assertTrue(key.matches("[0-9a-f]{64}"));
        Assert.assertNotEquals(key, KubernetesClusterUtil.generateCertificateKey());
    }

    @Test
    public void testGetCaCertHash() throws Exception {
        byte[] der = Base64.getDecoder().decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAET4KHpN8Rsx35juCq20Wi0WJyBRRF" +
                "Yl0p4YCFF6xxCY02Gelg7EYwHdPA/dbs+HrNwBBb/wuUNnEZiw1OQJXIRg==");
        PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
        Assert.assertEquals("sha256:9826a516d7c2f8974dd0a09a64048c5cba6ff67780b01568b13cd3acf92a409f",
                KubernetesClusterUtil.getCaCertHash(key));
    }

    @Test
    public void testParseJoinCommand() {
        String hash = "sha256:9826a516d7c2f8974dd0a09a64048c5cba6ff67780b01568b13cd3acf92a409f";
        Pair<String, String> result = KubernetesClusterUtil.parseJoinCommand(
                "kubeadm join 10.1.1.10:6443 --token abcdef.0123456789abcdef --discovery-token-ca-cert-hash " + hash + " \n");
        Assert.assertEquals("abcdef.0123456789abcdef", result.first());
        Assert.assertEquals(hash, result.second());
    }

    @Test
    public void testParseJoinEndpointIp() {
        String joinArgs = " --token abcdef.0123456789abcdef --discovery-token-ca-cert-hash sha256:9826a516d7c2f8974dd0a09a64048c5cba6ff67780b01568b13cd3acf92a409f \n";
        Assert.assertEquals("203.0.113.5", KubernetesClusterUtil.parseJoinEndpointIp("kubeadm join 203.0.113.5:6443" + joinArgs));
        Assert.assertNull(KubernetesClusterUtil.parseJoinEndpointIp(null));
        Assert.assertNull(KubernetesClusterUtil.parseJoinEndpointIp("kubeadm join api.example.com:6443" + joinArgs));
        Assert.assertNull(KubernetesClusterUtil.parseJoinEndpointIp("kubeadm join 999.1.1.1:6443" + joinArgs));
        Assert.assertNull(KubernetesClusterUtil.parseJoinEndpointIp("kubeadm join 10.1.1.10:6443;id" + joinArgs));
    }

    @Test
    public void testParseCertificateCheckIp() {
        Assert.assertEquals(Boolean.TRUE, KubernetesClusterUtil.parseCertificateCheckIp("IP 10.1.1.10 does match certificate\n", "10.1.1.10"));
        Assert.assertEquals(Boolean.FALSE, KubernetesClusterUtil.parseCertificateCheckIp("IP 10.1.1.10 does NOT match certificate\n", "10.1.1.10"));
        Assert.assertNull(KubernetesClusterUtil.parseCertificateCheckIp("IP 10.1.1.100 does match certificate\n", "10.1.1.10"));
        Assert.assertNull(KubernetesClusterUtil.parseCertificateCheckIp("Could not read certificate from <stdin>\nUnable to load certificate\n", "10.1.1.10"));
        Assert.assertNull(KubernetesClusterUtil.parseCertificateCheckIp(null, "10.1.1.10"));
    }

    @Test
    public void testParseJoinCommandInvalid() {
        Assert.assertNull(KubernetesClusterUtil.parseJoinCommand(null));
        Assert.assertNull(KubernetesClusterUtil.parseJoinCommand("failed to load admin kubeconfig"));
        Assert.assertNull(KubernetesClusterUtil.parseJoinCommand("kubeadm join 10.1.1.10:6443 --token abcdef.0123456789abcdef"));
        Assert.assertNull(KubernetesClusterUtil.parseJoinCommand(
                "kubeadm join 10.1.1.10:6443 --token $(reboot) --discovery-token-ca-cert-hash sha256:9826a516d7c2f8974dd0a09a64048c5cba6ff67780b01568b13cd3acf92a409f"));
        Assert.assertNull(KubernetesClusterUtil.parseJoinCommand(
                "kubeadm join 10.1.1.10:6443 --token abcdef.0123456789abcdef;id --discovery-token-ca-cert-hash sha256:9826a516d7c2f8974dd0a09a64048c5cba6ff67780b01568b13cd3acf92a409f"));
    }

    @Test
    public void testGetJoinTokenTtl() {
        Assert.assertEquals("3600s", KubernetesClusterUtil.getJoinTokenTtl(3600));
        Assert.assertEquals("1s", KubernetesClusterUtil.getJoinTokenTtl(0));
    }
}
