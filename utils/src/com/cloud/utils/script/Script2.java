package com.cloud.utils.script;


import java.util.HashMap;
import org.apache.log4j.Logger;


public class Script2 extends Script {
	HashMap<String, ParamType> _params = new HashMap<String, ParamType>();
	
	public static enum ParamType {
		NORMAL,
		PASSWORD,
	}
	
    public Script2(String command, Logger logger) {
        this(command, 0, logger);
    }
    
    public Script2(String command, long timeout, Logger logger) {
    	super(command, timeout, logger);
    }
    
    public void add(String param, ParamType type) {
    	_params.put(param, type);
    	super.add(param);
    }
    
    @Override
    public void add(String param) {
    	add(param, ParamType.NORMAL);
    }
    
    private ParamType getType(String cmd) {
    	return _params.get(cmd);
    }
    
    @Override
    protected String buildCommandLine(String[] command) {
    	StringBuilder builder = new StringBuilder();
    	for (int i = 0; i < command.length; i++) {
    		String cmd = command[i];
    		ParamType type = getType(cmd);
    		if (type == ParamType.PASSWORD) {
    			builder.append("******").append(" ");
    		} else {
    			builder.append(command[i]).append(" ");
    		}
    	}
    	
    	return builder.toString();
    }
}
