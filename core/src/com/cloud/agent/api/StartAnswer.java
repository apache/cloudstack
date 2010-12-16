/**
 * 
 */
package com.cloud.agent.api;

public class StartAnswer extends Answer {
    protected StartAnswer() {
    }
    
    public StartAnswer(StartCommand cmd, String msg) {
        super(cmd, false, msg);
    }
    
    public StartAnswer(StartCommand cmd, Exception e) {
        super(cmd, false, e.getMessage());
    }
    
    public StartAnswer(StartCommand cmd) {
        super(cmd, true, null);
    }
}
