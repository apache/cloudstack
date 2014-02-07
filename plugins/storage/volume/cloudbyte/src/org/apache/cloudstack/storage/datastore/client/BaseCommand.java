/**
 * 
 */
package org.apache.cloudstack.storage.datastore.client;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author cloudbyte
 *
 */
public abstract class BaseCommand implements ElastiCenterCommand {

	private String commandName = null;
	private MultivaluedMap<String, String> commandParameters = null;
	private Object responseObject = null;

	/*
	 *  Enforce the Commands to be initialized with command name
	 *  and optional response object
	 */
	protected BaseCommand( String cmdName, Object responseObj ) {
		commandName = cmdName;
		responseObject = responseObj;
	}

	@Override
	public String getCommandName() {
		return commandName;
	}

	@Override
	public boolean validate() {
		// TODO This method can be extended to do some generic 
		//  validations. 
		return true;
	}


	@Override
	public MultivaluedMap<String, String> getCommandParameters() {
		return commandParameters;
	}

	@Override
	public void putCommandParameter(String key, String value) {
		if ( null == commandParameters ) {
			commandParameters = new MultivaluedMapImpl();
		}
		commandParameters.add(key, value);
	}
	
	@Override
	public Object getResponseObject() {
		return responseObject;
	}

}
