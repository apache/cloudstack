package com.cloud.api;

public class SerializationContext {
    private static ThreadLocal<SerializationContext> s_currentContext = new ThreadLocal<SerializationContext>();

    private boolean _doUuidTranslation = false;
    
    public SerializationContext() {
    }
    
    public static SerializationContext current() {
    	SerializationContext context = s_currentContext.get();
        if(context == null) {
        	context = new SerializationContext();
        	s_currentContext.set(context);
        }
        return context;
    }
    
    public boolean getUuidTranslation() {
    	return _doUuidTranslation;
    }
    
    public void setUuidTranslation(boolean value) {
    	_doUuidTranslation = value;
    }
}
