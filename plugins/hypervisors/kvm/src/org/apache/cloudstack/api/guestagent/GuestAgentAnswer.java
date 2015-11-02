package org.apache.cloudstack.api.guestagent;

import com.google.gson.annotations.SerializedName;

public class GuestAgentAnswer {

    public static class GuestAgentIntegerAnswer extends GuestAgentAnswer {
        @SerializedName("return")
        int answer;

        public int getAnswer() {
            return answer;
        }
    }

    public static class GuestAgentStringAnswer extends GuestAgentAnswer {
        @SerializedName("return")
        String answer;

        public String getAnswer() {
            return answer;
        }
    }
}
