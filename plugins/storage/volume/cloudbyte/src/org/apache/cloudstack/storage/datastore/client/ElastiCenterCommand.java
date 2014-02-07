package org.apache.cloudstack.storage.datastore.client;

import javax.ws.rs.core.MultivaluedMap;

public interface ElastiCenterCommand {

	/*
	 * Returns the command string to be sent to the ElastiCenter
	 */
	public String getCommandName();
	
	/*
	 * Utility method to allow the client to validate the 
	 * input parameters before sending to the ElastiCenter.
	 * 
	 * This command will be executed by the ElastiCenterClient only 
	 * this method returns true.
	 */
	public boolean validate();
	
	/*
	 *  Returns the query parameters that have to be passed to 
	 *  execute the command.
	 *  
	 *  Returns null if there are query parameters associated with 
	 *  the command 
	 */
	public MultivaluedMap<String, String> getCommandParameters();
	
	/*
	 *  Adds new key-value pair to the query paramters lists.
	 */
	public void putCommandParameter(String key, String value);
	
	/*
	 *  Return an instance of the Response Object Type.
	 *  
	 *  Return null if no response is expected. 
	 */
	public Object getResponseObject();
}
