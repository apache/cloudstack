package com.cloud.bridge.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.cloud.bridge.persist.PersistContext;
import com.cloud.bridge.persist.dao.CloudStackConfigurationDao;
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
		try{
    	    ConfigurationHelper.preConfigureConfigPathFromServletContext(config.getServletContext());
    		UserCredentialsDao.preCheckTableExistence();
    		// check if API is enabled
    		CloudStackConfigurationDao csDao = new CloudStackConfigurationDao();
    		String value = csDao.getConfigValue(ENABLE_EC2_API);
    		if(value != null){
    		    isEC2APIEnabled = Boolean.valueOf(value);
    		}
    		
		}finally {
		    PersistContext.commitTransaction(true);
            PersistContext.closeSession(true);
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
            //response.sendError(404, "EC2 API is disabled.");
            response.setStatus(404);
            faultResponse(response, "404" , "EC2 API is disabled.");
            return;
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
    
    private void faultResponse(HttpServletResponse response, String errorCode, String errorMessage) {
        try {
            OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream());
            response.setContentType("text/xml; charset=UTF-8");
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.write("<Response><Errors><Error><Code>");
            out.write(errorCode);
            out.write("</Code><Message>");
            out.write(errorMessage);
            out.write("</Message></Error></Errors><RequestID>");
            out.write(UUID.randomUUID().toString());
            out.write("</RequestID></Response>");
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }    
}