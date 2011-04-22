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
package com.cloud.keystore;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.security.CertificateHelper;

@Local(value=KeystoreManager.class)
public class KeystoreManagerImpl implements KeystoreManager {
    private static final Logger s_logger = Logger.getLogger(KeystoreManagerImpl.class);

	private String _name;
	@Inject private KeystoreDao _ksDao;
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}
	
	@Override
	public boolean validateCertificate(String certificate, String key, String domainSuffix) {
		if(certificate == null || certificate.isEmpty() ||
			key == null || key.isEmpty() ||
			domainSuffix == null || domainSuffix.isEmpty()) {
			s_logger.error("Invalid parameter found in (certificate, key, domainSuffix) tuple for domain: " + domainSuffix);
			return false;
		}

		try {
			String ksPassword = "passwordForValidation";
			byte[] ksBits = CertificateHelper.buildAndSaveKeystore(domainSuffix, certificate, key, ksPassword);
			KeyStore ks = CertificateHelper.loadKeystore(ksBits, ksPassword);
			if(ks != null)
				return true;

			s_logger.error("Unabled to construct keystore for domain: " + domainSuffix);
		} catch(Exception e) {
			s_logger.error("Certificate validation failed due to exception for domain: " + domainSuffix, e);
		}
		return false;
	}
	
	@Override
	public void saveCertificate(String name, String certificate, String key, String domainSuffix) {
		if(name == null || name.isEmpty() ||
			certificate == null || certificate.isEmpty() ||
			key == null || key.isEmpty() ||
			domainSuffix == null || domainSuffix.isEmpty())
			throw new CloudRuntimeException("invalid parameter in saveCerticate");
		
		_ksDao.save(name, certificate, key, domainSuffix);
	}
	
	@Override
	public byte[] getKeystoreBits(String name, String aliasForCertificateInStore, String storePassword) {
		assert(name != null);
		assert(aliasForCertificateInStore != null);
		assert(storePassword != null);
		
		KeystoreVO ksVo = _ksDao.findByName(name);
		if(ksVo == null)
			throw new CloudRuntimeException("Unable to find keystore " + name);
	
		try {
			return CertificateHelper.buildAndSaveKeystore(aliasForCertificateInStore, ksVo.getCertificate(), getKeyContent(ksVo.getKey()), storePassword);
		} catch(KeyStoreException e) {
			s_logger.warn("Unable to build keystore for " + name + " due to KeyStoreException");
		} catch(CertificateException e) {
			s_logger.warn("Unable to build keystore for " + name + " due to CertificateException");
		} catch(NoSuchAlgorithmException e) {
			s_logger.warn("Unable to build keystore for " + name + " due to NoSuchAlgorithmException");
		} catch(InvalidKeySpecException e) {
			s_logger.warn("Unable to build keystore for " + name + " due to InvalidKeySpecException");
		} catch(IOException e) {
			s_logger.warn("Unable to build keystore for " + name + " due to IOException");
		}
		return null;
	}
	
	private static String getKeyContent(String key) {
    	Pattern regex = Pattern.compile("(^[\\-]+[^\\-]+[\\-]+[\\n]?)([^\\-]+)([\\-]+[^\\-]+[\\-]+$)");
    	Matcher m = regex.matcher(key);
    	if(m.find())
    		return m.group(2);
		
    	return key;
	}
}

