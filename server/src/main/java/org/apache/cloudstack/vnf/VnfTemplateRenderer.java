package org.apache.cloudstack.vnf;

import java.util.Map;

public class VnfTemplateRenderer {
    public RenderedRequest render(Dictionary dict, String key, Map<String,Object> inputs, Map<String,String> injectedHeaders) {
        // TODO: SnakeYAML load -> map; replace ${...}; build method/path/body/headers; return RenderedRequest
        return new RenderedRequest("POST", "/api/v2/firewall/rule", Map.of(), injectedHeaders);
    }

    public static class Dictionary { public Map<String,Object> root; }
    public static class RenderedRequest {
        public final String method, path; public final Object body; public final Map<String,String> headers;
        public RenderedRequest(String m, String p, Object b, Map<String,String> h){ method=m; path=p; body=b; headers=h; }
    }
}
