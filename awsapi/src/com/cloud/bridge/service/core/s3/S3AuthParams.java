package com.cloud.bridge.service.core.s3;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.cloud.bridge.util.HeaderParam;

/**
 * We need to be able to pass in specific values into the S3 REST authentication algorithm
 * where these values can be obtained from either HTTP headers directly or from the body
 * of a POST request.
 */
public class S3AuthParams {

	private List<HeaderParam> headerList = new ArrayList<HeaderParam>();
	
	public S3AuthParams() {
	}
	
	public HeaderParam[] getHeaders() {
		return headerList.toArray(new HeaderParam[0]);
	}
	
	public void addHeader(HeaderParam param) {
		headerList.add( param );
	}
	
	public String getHeader( String header ) 
	{
		// ToDO - make this look up faster
		ListIterator it = headerList.listIterator();
		while( it.hasNext()) 
		{
			HeaderParam temp = (HeaderParam)it.next();
			if (header.equalsIgnoreCase( temp.getName())) {
				return temp.getValue();
			}
		}
		return null;
	}
}
