/**
 * 
 */
package com.cloud.agent.api;

public class Start2Answer extends Answer {
    protected Start2Answer() {
    }
    
    public Start2Answer(Start2Command cmd, String msg) {
        super(cmd, false, msg);
    }
    
    public Start2Answer(Start2Command cmd, Exception e) {
        super(cmd, false, e.getMessage());
    }
    
    public Start2Answer(Start2Command cmd) {
        super(cmd, true, null);
    }
}
