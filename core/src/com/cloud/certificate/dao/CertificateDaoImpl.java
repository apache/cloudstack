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
    
	@Override
	public boolean persistCustomCertToDb(String certPath){
		
		String certStr = null;
	    byte[] buffer = new byte[(int) new File(certPath).length()];
	    BufferedInputStream f = null;
	    try 
	    {
	        f = new BufferedInputStream(new FileInputStream(certPath));
	        f.read(buffer);
	    } catch (FileNotFoundException e) {
	    	s_logger.warn("Unable to read the certificate: "+e);
	    	return false;
		} catch (IOException e) {
	    	s_logger.warn("Unable to read the certificate: "+e);
	    	return false;		
	    } 
	    finally 
	    {
	        if (f != null) 
	        	try { f.close(); } catch (IOException ignored) { }
	    }
	    certStr = new String(buffer);

	    CertificateVO certRec = new CertificateVO(certStr);
	    this.persist(certRec);
	    
		return true;
	}
}
