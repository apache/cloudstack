package com.cloud.bridge.service;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloud.bridge.persist.CloudStackDao;
import com.cloud.bridge.persist.dao.UserCredentialsDao;
import com.cloud.bridge.util.ConfigurationHelper;

public class EC2MainServlet extends HttpServlet{

	private static final long serialVersionUID = 2201599478145974479L;
	
	public static final String EC2_REST_SERVLET_PATH="/rest/AmazonEC2/";
	public static final String EC2_SOAP_SERVLET_PATH="/services/AmazonEC2/";
	public static final String ENABLE_EC2_API="enable.ec2.api";
	private static boolean isEC2APIEnabled = false;
	
	/**
	 * We build the path to where the keystore holding the WS-Security X509 certificates
	 * are stored.
	 */
	public void init( ServletConfig config ) throws ServletException {
		ConfigurationHelper.preConfigureConfigPathFromServletContext(config.getServletContext());
		UserCredentialsDao.preCheckTableExistence();
		// check if API is enabled
		
		CloudStackDao csDao = new CloudStackDao();
		String value = csDao.getConfigValue(ENABLE_EC2_API);
		if(value != null){
		    isEC2APIEnabled = Boolean.valueOf(value);
		}
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
	    doGetOrPost(req, resp);
    }
	
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
	    doGetOrPost(req, resp);
    }

    protected void doGetOrPost(HttpServletRequest request, HttpServletResponse response) {
        String action = request.getParameter( "Action" );
        
        if(!isEC2APIEnabled){
           throw new RuntimeException("EC2 API is disabled.");
        }
        
    	if(action != null){
    		//We presume it's a Query/Rest call
    		try {
				RequestDispatcher dispatcher = request.getRequestDispatcher(EC2_REST_SERVLET_PATH);
				dispatcher.forward(request, response);
			} catch (ServletException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
    	}
    	else {
    		try {
				request.getRequestDispatcher(EC2_SOAP_SERVLET_PATH).forward(request, response);
			} catch (ServletException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
    	}
    	
    }
}
