package org.apache.cloudstack.storage.datastore.adapter.primera;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraHostDescriptor {
    private String IPAddr = null;
    private String os = null;
    public String getIPAddr() {
        return IPAddr;
    }
    public void setIPAddr(String iPAddr) {
        IPAddr = iPAddr;
    }
    public String getOs() {
        return os;
    }
    public void setOs(String os) {
        this.os = os;
    }

}
