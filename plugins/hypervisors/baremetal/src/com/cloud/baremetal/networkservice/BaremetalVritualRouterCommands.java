package com.cloud.baremetal.networkservice;

/**
 * Created by frank on 7/23/14.
 */
public class BaremetalVritualRouterCommands {
    public static String PREPARE_PXE_URL = "/baremetal/pxe/prepare";

    public abstract static class AgentCommand {
    }

    public abstract static class AgentResponse {
        private boolean success;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public static class PreparePxeCmd extends AgentCommand {
        private String guestMac;
        private String kickStartUrl;
        private String initrdUrl;
        private String kernelUrl;

        public String getGuestMac() {
            return guestMac;
        }

        public void setGuestMac(String guestMac) {
            this.guestMac = guestMac;
        }

        public String getKickStartUrl() {
            return kickStartUrl;
        }

        public void setKickStartUrl(String kickStartUrl) {
            this.kickStartUrl = kickStartUrl;
        }

        public String getInitrdUrl() {
            return initrdUrl;
        }

        public void setInitrdUrl(String initrdUrl) {
            this.initrdUrl = initrdUrl;
        }

        public String getKernelUrl() {
            return kernelUrl;
        }

        public void setKernelUrl(String kernelUrl) {
            this.kernelUrl = kernelUrl;
        }
    }

    public static class PreparePxeRsp extends AgentResponse {
    }
}
