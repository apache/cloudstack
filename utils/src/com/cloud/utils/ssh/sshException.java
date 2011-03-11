package com.cloud.utils.ssh;

import com.cloud.utils.SerialVersionUID;

public class sshException extends Exception {
	 private static final long serialVersionUID = SerialVersionUID.sshException;
	 public sshException(String msg) {
		 super(msg);
	 }
}
