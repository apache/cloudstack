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

/**
 * AmazonS3MessageReceiverInOut.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.1  Built on : Oct 19, 2009 (10:59:00 EDT)
 */
package com.amazon.s3;

/**
*  AmazonS3MessageReceiverInOut message receiver
*/

public class AmazonS3MessageReceiverInOut extends org.apache.axis2.receivers.AbstractInOutMessageReceiver {

    public void invokeBusinessLogic(org.apache.axis2.context.MessageContext msgContext, org.apache.axis2.context.MessageContext newMsgContext)
        throws org.apache.axis2.AxisFault {

        try {

            // get the implementation class for the Web Service
            Object obj = getTheImplementationObject(msgContext);

            AmazonS3SkeletonInterface skel = (AmazonS3SkeletonInterface)obj;
            //Out Envelop
            org.apache.axiom.soap.SOAPEnvelope envelope = null;
            //Find the axisOperation that has been set by the Dispatch phase.
            org.apache.axis2.description.AxisOperation op = msgContext.getOperationContext().getAxisOperation();
            if (op == null) {
                throw new org.apache.axis2.AxisFault(
                    "Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
            }

            java.lang.String methodName;
            if ((op.getName() != null) && ((methodName = org.apache.axis2.util.JavaUtils.xmlNameToJavaIdentifier(op.getName().getLocalPart())) != null)) {

                if ("getBucketLoggingStatus".equals(methodName)) {

                    com.amazon.s3.GetBucketLoggingStatusResponse getBucketLoggingStatusResponse33 = null;
                    com.amazon.s3.GetBucketLoggingStatus wrappedParam =
                        (com.amazon.s3.GetBucketLoggingStatus)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.GetBucketLoggingStatus.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    getBucketLoggingStatusResponse33 =

                    skel.getBucketLoggingStatus(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), getBucketLoggingStatusResponse33, false);
                } else

                if ("copyObject".equals(methodName)) {

                    com.amazon.s3.CopyObjectResponse copyObjectResponse35 = null;
                    com.amazon.s3.CopyObject wrappedParam =
                        (com.amazon.s3.CopyObject)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.CopyObject.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    copyObjectResponse35 =

                    skel.copyObject(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), copyObjectResponse35, false);
                } else

                if ("getBucketAccessControlPolicy".equals(methodName)) {

                    com.amazon.s3.GetBucketAccessControlPolicyResponse getBucketAccessControlPolicyResponse37 = null;
                    com.amazon.s3.GetBucketAccessControlPolicy wrappedParam =
                        (com.amazon.s3.GetBucketAccessControlPolicy)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.s3.GetBucketAccessControlPolicy.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    getBucketAccessControlPolicyResponse37 =

                    skel.getBucketAccessControlPolicy(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), getBucketAccessControlPolicyResponse37, false);
                } else

                if ("listBucket".equals(methodName)) {

                    com.amazon.s3.ListBucketResponse listBucketResponse39 = null;
                    com.amazon.s3.ListBucket wrappedParam =
                        (com.amazon.s3.ListBucket)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.ListBucket.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    listBucketResponse39 =

                    skel.listBucket(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), listBucketResponse39, false);
                } else

                if ("putObject".equals(methodName)) {

                    com.amazon.s3.PutObjectResponse putObjectResponse41 = null;
                    com.amazon.s3.PutObject wrappedParam =
                        (com.amazon.s3.PutObject)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.PutObject.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    putObjectResponse41 =

                    skel.putObject(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), putObjectResponse41, false);
                } else

                if ("createBucket".equals(methodName)) {

                    com.amazon.s3.CreateBucketResponse createBucketResponse43 = null;
                    com.amazon.s3.CreateBucket wrappedParam =
                        (com.amazon.s3.CreateBucket)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.CreateBucket.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    createBucketResponse43 =

                    skel.createBucket(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), createBucketResponse43, false);
                } else

                if ("listAllMyBuckets".equals(methodName)) {

                    com.amazon.s3.ListAllMyBucketsResponse listAllMyBucketsResponse45 = null;
                    com.amazon.s3.ListAllMyBuckets wrappedParam =
                        (com.amazon.s3.ListAllMyBuckets)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.ListAllMyBuckets.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    listAllMyBucketsResponse45 =

                    skel.listAllMyBuckets(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), listAllMyBucketsResponse45, false);
                } else

                if ("getObject".equals(methodName)) {

                    com.amazon.s3.GetObjectResponse getObjectResponse47 = null;
                    com.amazon.s3.GetObject wrappedParam =
                        (com.amazon.s3.GetObject)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.GetObject.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    getObjectResponse47 =

                    skel.getObject(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), getObjectResponse47, false);
                } else

                if ("deleteBucket".equals(methodName)) {

                    com.amazon.s3.DeleteBucketResponse deleteBucketResponse49 = null;
                    com.amazon.s3.DeleteBucket wrappedParam =
                        (com.amazon.s3.DeleteBucket)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.DeleteBucket.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteBucketResponse49 =

                    skel.deleteBucket(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteBucketResponse49, false);
                } else

                if ("setBucketLoggingStatus".equals(methodName)) {

                    com.amazon.s3.SetBucketLoggingStatusResponse setBucketLoggingStatusResponse51 = null;
                    com.amazon.s3.SetBucketLoggingStatus wrappedParam =
                        (com.amazon.s3.SetBucketLoggingStatus)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.SetBucketLoggingStatus.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    setBucketLoggingStatusResponse51 =

                    skel.setBucketLoggingStatus(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), setBucketLoggingStatusResponse51, false);
                } else

                if ("getObjectAccessControlPolicy".equals(methodName)) {

                    com.amazon.s3.GetObjectAccessControlPolicyResponse getObjectAccessControlPolicyResponse53 = null;
                    com.amazon.s3.GetObjectAccessControlPolicy wrappedParam =
                        (com.amazon.s3.GetObjectAccessControlPolicy)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.s3.GetObjectAccessControlPolicy.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    getObjectAccessControlPolicyResponse53 =

                    skel.getObjectAccessControlPolicy(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), getObjectAccessControlPolicyResponse53, false);
                } else

                if ("deleteObject".equals(methodName)) {

                    com.amazon.s3.DeleteObjectResponse deleteObjectResponse55 = null;
                    com.amazon.s3.DeleteObject wrappedParam =
                        (com.amazon.s3.DeleteObject)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.DeleteObject.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    deleteObjectResponse55 =

                    skel.deleteObject(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), deleteObjectResponse55, false);
                } else

                if ("setBucketAccessControlPolicy".equals(methodName)) {

                    com.amazon.s3.SetBucketAccessControlPolicyResponse setBucketAccessControlPolicyResponse57 = null;
                    com.amazon.s3.SetBucketAccessControlPolicy wrappedParam =
                        (com.amazon.s3.SetBucketAccessControlPolicy)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.s3.SetBucketAccessControlPolicy.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    setBucketAccessControlPolicyResponse57 =

                    skel.setBucketAccessControlPolicy(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), setBucketAccessControlPolicyResponse57, false);
                } else

                if ("setObjectAccessControlPolicy".equals(methodName)) {

                    com.amazon.s3.SetObjectAccessControlPolicyResponse setObjectAccessControlPolicyResponse59 = null;
                    com.amazon.s3.SetObjectAccessControlPolicy wrappedParam =
                        (com.amazon.s3.SetObjectAccessControlPolicy)fromOM(msgContext.getEnvelope().getBody().getFirstElement(),
                            com.amazon.s3.SetObjectAccessControlPolicy.class, getEnvelopeNamespaces(msgContext.getEnvelope()));

                    setObjectAccessControlPolicyResponse59 =

                    skel.setObjectAccessControlPolicy(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), setObjectAccessControlPolicyResponse59, false);
                } else

                if ("putObjectInline".equals(methodName)) {

                    com.amazon.s3.PutObjectInlineResponse putObjectInlineResponse61 = null;
                    com.amazon.s3.PutObjectInline wrappedParam =
                        (com.amazon.s3.PutObjectInline)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.PutObjectInline.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    putObjectInlineResponse61 =

                    skel.putObjectInline(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), putObjectInlineResponse61, false);
                } else

                if ("getObjectExtended".equals(methodName)) {

                    com.amazon.s3.GetObjectExtendedResponse getObjectExtendedResponse63 = null;
                    com.amazon.s3.GetObjectExtended wrappedParam =
                        (com.amazon.s3.GetObjectExtended)fromOM(msgContext.getEnvelope().getBody().getFirstElement(), com.amazon.s3.GetObjectExtended.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    getObjectExtendedResponse63 =

                    skel.getObjectExtended(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), getObjectExtendedResponse63, false);

                } else {
                    throw new java.lang.RuntimeException("method not found");
                }

                newMsgContext.setEnvelope(envelope);
            }
        } catch (java.lang.Exception e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    //
    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetBucketLoggingStatus param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetBucketLoggingStatus.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetBucketLoggingStatusResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetBucketLoggingStatusResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.CopyObject param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.CopyObject.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.CopyObjectResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.CopyObjectResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetBucketAccessControlPolicy param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetBucketAccessControlPolicy.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetBucketAccessControlPolicyResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetBucketAccessControlPolicyResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.ListBucket param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.ListBucket.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.ListBucketResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.ListBucketResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.PutObject param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.PutObject.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.PutObjectResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.PutObjectResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.CreateBucket param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.CreateBucket.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.CreateBucketResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.CreateBucketResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.ListAllMyBuckets param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.ListAllMyBuckets.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.ListAllMyBucketsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.ListAllMyBucketsResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetObject param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetObject.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetObjectResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetObjectResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.DeleteBucket param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.DeleteBucket.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.DeleteBucketResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.DeleteBucketResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.SetBucketLoggingStatus param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.SetBucketLoggingStatus.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.SetBucketLoggingStatusResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.SetBucketLoggingStatusResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetObjectAccessControlPolicy param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetObjectAccessControlPolicy.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetObjectAccessControlPolicyResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetObjectAccessControlPolicyResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.DeleteObject param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.DeleteObject.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.DeleteObjectResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.DeleteObjectResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.SetBucketAccessControlPolicy param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.SetBucketAccessControlPolicy.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.SetBucketAccessControlPolicyResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.SetBucketAccessControlPolicyResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.SetObjectAccessControlPolicy param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.SetObjectAccessControlPolicy.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.SetObjectAccessControlPolicyResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.SetObjectAccessControlPolicyResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.PutObjectInline param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.PutObjectInline.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.PutObjectInlineResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.PutObjectInlineResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetObjectExtended param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetObjectExtended.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.om.OMElement toOM(com.amazon.s3.GetObjectExtendedResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {

        try {
            return param.getOMElement(com.amazon.s3.GetObjectExtendedResponse.MY_QNAME, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }

    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.GetBucketLoggingStatusResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.GetBucketLoggingStatusResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.GetBucketLoggingStatusResponse wrapGetBucketLoggingStatus() {
        com.amazon.s3.GetBucketLoggingStatusResponse wrappedElement = new com.amazon.s3.GetBucketLoggingStatusResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.CopyObjectResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.CopyObjectResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.CopyObjectResponse wrapCopyObject() {
        com.amazon.s3.CopyObjectResponse wrappedElement = new com.amazon.s3.CopyObjectResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.GetBucketAccessControlPolicyResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.GetBucketAccessControlPolicyResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.GetBucketAccessControlPolicyResponse wrapGetBucketAccessControlPolicy() {
        com.amazon.s3.GetBucketAccessControlPolicyResponse wrappedElement = new com.amazon.s3.GetBucketAccessControlPolicyResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.ListBucketResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.ListBucketResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.ListBucketResponse wrapListBucket() {
        com.amazon.s3.ListBucketResponse wrappedElement = new com.amazon.s3.ListBucketResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.PutObjectResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.PutObjectResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.PutObjectResponse wrapPutObject() {
        com.amazon.s3.PutObjectResponse wrappedElement = new com.amazon.s3.PutObjectResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.CreateBucketResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.CreateBucketResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.CreateBucketResponse wrapCreateBucket() {
        com.amazon.s3.CreateBucketResponse wrappedElement = new com.amazon.s3.CreateBucketResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.ListAllMyBucketsResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.ListAllMyBucketsResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.ListAllMyBucketsResponse wrapListAllMyBuckets() {
        com.amazon.s3.ListAllMyBucketsResponse wrappedElement = new com.amazon.s3.ListAllMyBucketsResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.GetObjectResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.GetObjectResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.GetObjectResponse wrapGetObject() {
        com.amazon.s3.GetObjectResponse wrappedElement = new com.amazon.s3.GetObjectResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.DeleteBucketResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.DeleteBucketResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.DeleteBucketResponse wrapDeleteBucket() {
        com.amazon.s3.DeleteBucketResponse wrappedElement = new com.amazon.s3.DeleteBucketResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.SetBucketLoggingStatusResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.SetBucketLoggingStatusResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.SetBucketLoggingStatusResponse wrapSetBucketLoggingStatus() {
        com.amazon.s3.SetBucketLoggingStatusResponse wrappedElement = new com.amazon.s3.SetBucketLoggingStatusResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.GetObjectAccessControlPolicyResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.GetObjectAccessControlPolicyResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.GetObjectAccessControlPolicyResponse wrapGetObjectAccessControlPolicy() {
        com.amazon.s3.GetObjectAccessControlPolicyResponse wrappedElement = new com.amazon.s3.GetObjectAccessControlPolicyResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.DeleteObjectResponse param, boolean optimizeContent)
        throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.DeleteObjectResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.DeleteObjectResponse wrapDeleteObject() {
        com.amazon.s3.DeleteObjectResponse wrappedElement = new com.amazon.s3.DeleteObjectResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.SetBucketAccessControlPolicyResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.SetBucketAccessControlPolicyResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.SetBucketAccessControlPolicyResponse wrapSetBucketAccessControlPolicy() {
        com.amazon.s3.SetBucketAccessControlPolicyResponse wrappedElement = new com.amazon.s3.SetBucketAccessControlPolicyResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.SetObjectAccessControlPolicyResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.SetObjectAccessControlPolicyResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.SetObjectAccessControlPolicyResponse wrapSetObjectAccessControlPolicy() {
        com.amazon.s3.SetObjectAccessControlPolicyResponse wrappedElement = new com.amazon.s3.SetObjectAccessControlPolicyResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope
        toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.PutObjectInlineResponse param, boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.PutObjectInlineResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.PutObjectInlineResponse wrapPutObjectInline() {
        com.amazon.s3.PutObjectInlineResponse wrappedElement = new com.amazon.s3.PutObjectInlineResponse();
        return wrappedElement;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, com.amazon.s3.GetObjectExtendedResponse param,
        boolean optimizeContent) throws org.apache.axis2.AxisFault {
        try {
            org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

            emptyEnvelope.getBody().addChild(param.getOMElement(com.amazon.s3.GetObjectExtendedResponse.MY_QNAME, factory));

            return emptyEnvelope;
        } catch (org.apache.axis2.databinding.ADBException e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    private com.amazon.s3.GetObjectExtendedResponse wrapGetObjectExtended() {
        com.amazon.s3.GetObjectExtendedResponse wrappedElement = new com.amazon.s3.GetObjectExtendedResponse();
        return wrappedElement;
    }

    /**
    *  get the default envelope
    */
    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory) {
        return factory.getDefaultEnvelope();
    }

    private java.lang.Object fromOM(org.apache.axiom.om.OMElement param, java.lang.Class type, java.util.Map extraNamespaces) throws org.apache.axis2.AxisFault {

        try {

            if (com.amazon.s3.GetBucketLoggingStatus.class.equals(type)) {

                return com.amazon.s3.GetBucketLoggingStatus.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetBucketLoggingStatusResponse.class.equals(type)) {

                return com.amazon.s3.GetBucketLoggingStatusResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.CopyObject.class.equals(type)) {

                return com.amazon.s3.CopyObject.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.CopyObjectResponse.class.equals(type)) {

                return com.amazon.s3.CopyObjectResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetBucketAccessControlPolicy.class.equals(type)) {

                return com.amazon.s3.GetBucketAccessControlPolicy.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetBucketAccessControlPolicyResponse.class.equals(type)) {

                return com.amazon.s3.GetBucketAccessControlPolicyResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.ListBucket.class.equals(type)) {

                return com.amazon.s3.ListBucket.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.ListBucketResponse.class.equals(type)) {

                return com.amazon.s3.ListBucketResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.PutObject.class.equals(type)) {

                return com.amazon.s3.PutObject.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.PutObjectResponse.class.equals(type)) {

                return com.amazon.s3.PutObjectResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.CreateBucket.class.equals(type)) {

                return com.amazon.s3.CreateBucket.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.CreateBucketResponse.class.equals(type)) {

                return com.amazon.s3.CreateBucketResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.ListAllMyBuckets.class.equals(type)) {

                return com.amazon.s3.ListAllMyBuckets.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.ListAllMyBucketsResponse.class.equals(type)) {

                return com.amazon.s3.ListAllMyBucketsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetObject.class.equals(type)) {

                return com.amazon.s3.GetObject.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetObjectResponse.class.equals(type)) {

                return com.amazon.s3.GetObjectResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.DeleteBucket.class.equals(type)) {

                return com.amazon.s3.DeleteBucket.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.DeleteBucketResponse.class.equals(type)) {

                return com.amazon.s3.DeleteBucketResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.SetBucketLoggingStatus.class.equals(type)) {

                return com.amazon.s3.SetBucketLoggingStatus.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.SetBucketLoggingStatusResponse.class.equals(type)) {

                return com.amazon.s3.SetBucketLoggingStatusResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetObjectAccessControlPolicy.class.equals(type)) {

                return com.amazon.s3.GetObjectAccessControlPolicy.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetObjectAccessControlPolicyResponse.class.equals(type)) {

                return com.amazon.s3.GetObjectAccessControlPolicyResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.DeleteObject.class.equals(type)) {

                return com.amazon.s3.DeleteObject.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.DeleteObjectResponse.class.equals(type)) {

                return com.amazon.s3.DeleteObjectResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.SetBucketAccessControlPolicy.class.equals(type)) {

                return com.amazon.s3.SetBucketAccessControlPolicy.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.SetBucketAccessControlPolicyResponse.class.equals(type)) {

                return com.amazon.s3.SetBucketAccessControlPolicyResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.SetObjectAccessControlPolicy.class.equals(type)) {

                return com.amazon.s3.SetObjectAccessControlPolicy.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.SetObjectAccessControlPolicyResponse.class.equals(type)) {

                return com.amazon.s3.SetObjectAccessControlPolicyResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.PutObjectInline.class.equals(type)) {

                return com.amazon.s3.PutObjectInline.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.PutObjectInlineResponse.class.equals(type)) {

                return com.amazon.s3.PutObjectInlineResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetObjectExtended.class.equals(type)) {

                return com.amazon.s3.GetObjectExtended.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (com.amazon.s3.GetObjectExtendedResponse.class.equals(type)) {

                return com.amazon.s3.GetObjectExtendedResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

        } catch (java.lang.Exception e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
        return null;
    }

    /**
    *  A utility method that copies the namepaces from the SOAPEnvelope
    */
    private java.util.Map getEnvelopeNamespaces(org.apache.axiom.soap.SOAPEnvelope env) {
        java.util.Map returnMap = new java.util.HashMap();
        java.util.Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
            org.apache.axiom.om.OMNamespace ns = (org.apache.axiom.om.OMNamespace)namespaceIterator.next();
            returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
        }
        return returnMap;
    }

    private org.apache.axis2.AxisFault createAxisFault(java.lang.Exception e) {
        org.apache.axis2.AxisFault f;
        Throwable cause = e.getCause();
        if (cause != null) {
            f = new org.apache.axis2.AxisFault(e.getMessage(), cause);
        } else {
            f = new org.apache.axis2.AxisFault(e.getMessage());
        }

        return f;
    }

}//end of class
