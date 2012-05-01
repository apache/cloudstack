package com.cloud.bridge.util;

public class HeaderParam {

	protected String name;
	protected String value;
	
<<<<<<< HEAD
=======
	
>>>>>>> 6472e7b... Now really adding the renamed files!
	public HeaderParam() {
		name  = null;
		value = null;
	}
	
<<<<<<< HEAD
=======
	public HeaderParam (String name, String value) {
         this.name = name;
         this.name = value;     
	}
	
>>>>>>> 6472e7b... Now really adding the renamed files!
	public void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setValue( String value ) {
		this.value = value;
	}
	
	public String getValue() {
		return this.value;
	}
}
