package com.cloud.agent.api;

public class ScaleVmAnswer extends Answer {

    protected ScaleVmAnswer() {
    }
    
    public ScaleVmAnswer(ScaleVmCommand cmd, boolean result, String detail) {
        super(cmd, result, detail);
    }

}
