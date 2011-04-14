/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.certificate.dao;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.certificate.CertificateVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;

@Local(value={CertificateDao.class}) @DB(txn=false)
public class CertificateDaoImpl extends GenericDaoBase<CertificateVO, Long>  implements CertificateDao {
	
    private static final Logger s_logger = Logger.getLogger(CertificateDaoImpl.class);
    
    public CertificateDaoImpl(){
    	
    }
    
	@Override
	public Long persistCustomCertToDb(String certStr, CertificateVO cert, Long managementServerId){		
	    BufferedInputStream f = null;
		try 
	    {
        	cert.setCertificate(certStr);
        	cert.setUpdated("Y");
        	update(cert.getId(),cert);
        	return cert.getId();
	    }  catch (Exception e){
	    	s_logger.warn("Unable to read the certificate: "+e);
	    	return new Long(0);
	    }
	    finally 
	    {
	        if (f != null) 
	        	try { f.close(); } catch (IOException ignored) { }
	    }
	}
}
