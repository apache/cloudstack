package org.apache.cloudstack.vnf;

public class VnfTransport {
    public static class VnfResponse { public final int status; public final String body;
        public VnfResponse(int s, String b){ status=s; body=b; } }

    public VnfResponse forward(long networkId, String vnfIp, int port,
                               VnfTemplateRenderer.RenderedRequest req, String idemKey) {
        // TODO: POST https://<vr>:8443/v1/forward with mTLS + JWT; return status/body
        return new VnfResponse(200, "{}");
    }
}
