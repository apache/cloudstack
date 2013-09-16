// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.service.exception;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;

public class EC2ServiceException extends RuntimeException {
	private static final long serialVersionUID = 8857313467757867680L;
	
	// ServerError & ClientError are correct as of schema version 2010-08-31
	
	public static enum ServerError {
        InsufficientAddressCapacity("Server.InsufficientAddressCapacity", 500),
        InsufficientInstanceCapacity("Server.InsufficientInstanceCapacity", 500),
        InsufficientReservedInstanceCapacity("Server.InsufficientReservedInstanceCapacity", 500),
        InternalError("Server.InternalError", 500),
        Unavailable("Server.Unavailable", 501);
		
		private String errorString;
		private int httpErrorCode;
		
		private ServerError(String errorString, int errorCode) { 
			this.errorString = errorString;
			this.httpErrorCode = errorCode;
		}
		
		public String getErrorString() { return errorString; }
		public int getHttpErrorCode() {return httpErrorCode; }
	}
	
	public static enum ClientError {
        AddressLimitExceeded("Client.AddressLimitExceeded", 400),
        AttachmentLimitExceeded("Client.AttachmentLimitExceeded", 400),
        AuthFailure("Client.AuthFailure", 400),
        Blocked("Client.Blocked", 400),
        DependencyViolation("Client.DependencyViolation", 400),
        FilterLimitExceeded("Client.FilterLimitExceeded", 400),
        IdempotentParameterMismatch("Client.IdempotentParameterMismatch", 400),
        IncorrectState("Client.IncorrectState", 400),
        IncorrectInstanceState("Client.IncorrectInstanceState", 400),
        InstanceLimitExceeded("Client.InstanceLimitExceeded", 400),
        InsufficientInstanceCapacity("Client.InsufficientInstanceCapacity", 400),
        InsufficientReservedInstancesCapacity("Client.InsufficientReservedInstancesCapacity", 400),
        InvalidAMIAttributeItemValue("Client.InvalidAMIAttributeItemValue", 400),
        InvalidAMIID_Malformed("Client.InvalidAMIID.Malformed", 400),
        InvalidAMIID_NotFound("Client.InvalidAMIID.NotFound", 400),
        InvalidAMIID_Unavailable("Client.InvalidAMIID.Unavailable", 400),
        InvalidAttachment_NotFound("Client.InvalidAttachment.NotFound", 400),
        InvalidDevice_InUse("Client.InvalidDevice.InUse", 400),
        InvalidFilter("Client.InvalidFilter", 400),
        InvalidGroup_Duplicate("Client.InvalidGroup.Duplicate", 400),
        InvalidGroup_InUse("Client.InvalidGroup.InUse", 400),
        InvalidGroup_NotFound("Client.InvalidGroup.NotFound", 400),
        InvalidGroup_Reserved("Client.InvalidGroup.Reserved", 400),
        InvalidInstanceID_Malformed("Client.InvalidInstanceID.Malformed", 400),
        InvalidInstanceID_NotFound("Client.InvalidInstanceID.NotFound", 400),
        InvalidIPAddress_InUse("Client.InvalidIPAddress.InUse", 400),
        InvalidKeyPair_Duplicate("Client.InvalidKeyPair.Duplicate", 400),
        InvalidKeyPair_Format("Client.InvalidKeyPair.Format", 400),
        InvalidKeyPair_NotFound("Client.InvalidKeyPair.NotFound", 400),
        InvalidManifest("Client.InvalidManifest", 400),
        InvalidParameterCombination("Client.InvalidParameterCombination", 400),
        InvalidParameterValue("Client.InvalidParameterValue", 400),
        InvalidPermission_Duplicate("Client.InvalidPermission.Duplicate", 400),
        InvalidPermission_Malformed("Client.InvalidPermission.Malformed", 400),
        InvalidReservationID_Malformed("Client.InvalidReservationID.Malformed", 400),
        InvalidReservationID_NotFound("Client.InvalidReservationID.NotFound", 400),
        InvalidSecurity_RequestHasExpired("Client.InvalidSecurity.RequestHasExpired", 400),	
        InvalidSnapshotID_Malformed("Client.InvalidSnapshotID.Malformed", 400),
        InvalidSnapshot_NotFound("Client.InvalidSnapshot.NotFound", 400),
        InvalidUserID_Malformed("Client.InvalidUserID.Malformed", 400),
        InvalidReservedInstancesId("Client.InvalidReservedInstancesId", 400),
        InvalidReservedInstancesOfferingId("Client.InvalidReservedInstancesOfferingId", 400),
        InvalidVolumeID_Duplicate("Client.InvalidVolumeID.Duplicate", 400),
        InvalidVolumeID_Malformed("Client.InvalidVolumeID.Malformed", 400),
        InvalidVolume_NotFound("Client.InvalidVolume.NotFound", 400),
        InvalidVolumeID_ZoneMismatch("Client.InvalidVolumeID.ZoneMismatch", 400),
        InvalidZone_NotFound("Client.InvalidZone.NotFound", 400),
        MissingParamter("Client.MissingParamter", 400),
        NonEBSInstance("Client.NonEBSInstance", 400),
        PendingVerification("Client.PendingVerification", 400),
        PendingSnapshotLimitExceeded("Client.PendingSnapshotLimitExceeded", 400),
        SignatureDoesNotMatch("Client.SignatureDoesNotMatch", 400),
        ReservedInstancesLimitExceeded("Client.ReservedInstancesLimitExceeded", 400),
        ResourceLimitExceeded("Client.ResourceLimitExceeded", 400),
        SnapshotLimitExceeded("Client.SnapshotLimitExceeded", 400),
        UnknownParameter("Client.UnknownParameter", 400),
        Unsupported("Client.UnsupportedOperation", 400),
        VolumeLimitExceeded("Client.VolumeLimitExceeded", 400);

		private String errorString;
		private int httpErrorCode;
		
		private ClientError(String errorString, int errorCode) { 
			this.errorString = errorString;
			this.httpErrorCode = errorCode;
		}
		
		public String getErrorString() { return errorString; }
		public int getHttpErrorCode() {return httpErrorCode; }
	}

	private int httpErrorCode = 0;
	
	public EC2ServiceException() {
	}
	
	public EC2ServiceException(String message) {
		super(message);
	}
	
	public EC2ServiceException(Throwable e) {
		super(e);
	}

	public EC2ServiceException(String message, Throwable e) {
		super(message, e);
	}

	public EC2ServiceException(String message, int errorCode) {
		super(message, new AxisFault(message, new QName("Error")));
		this.httpErrorCode = errorCode;
	}
	
	public EC2ServiceException(String message, Throwable e, int errorCode) {
		super(message, e);
		this.httpErrorCode = errorCode;
	}
	
	public EC2ServiceException(ServerError errorCode, String message) {
		super(message, new AxisFault(message, new QName(errorCode.getErrorString())));
		this.httpErrorCode = errorCode.getHttpErrorCode(); 
	}
	
	public EC2ServiceException(ClientError errorCode, String message) {
		super(message, new AxisFault(message, new QName(errorCode.getErrorString())));
		this.httpErrorCode = errorCode.getHttpErrorCode(); 
	}
	
	public int getErrorCode() {
		return this.httpErrorCode;
	}
}
