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
package com.cloud.bridge.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.bridge.service.exception.UnsupportedException;
import com.cloud.bridge.util.OrderedPair;
import com.cloud.bridge.util.Triple;

/**
 * A model of stored ACLs to remember the ACL permissions per canonicalUserID per grantee
 * Hold the AWS S3 grantee and permission constants.
 *
 * This class implements two forms of getCannedAccessControls mappings, as static methods,
 *
 * (a) an OrderedPair which provides a maplet across
 *         < permission, grantee >
 * when given an aclRequestString and a target (i.e. bucket or object),
 *
 * (b) a Triplet
 *         < permission1, permission2, symbol >
 * when given an aclRequestString, a target (i.e. bucket or object) and the ID of the owner.
 */
@Entity
@Table(name = "acl")
public class SAclVO implements SAcl {
    private static final long serialVersionUID = 7900837117165018850L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "Target")
    private String target;

    @Column(name = "TargetID")
    private long targetId;

    @Column(name = "GranteeType")
    private int granteeType;

    @Column(name = "GranteeCanonicalID")
    private String granteeCanonicalId;

    @Column(name = "Permission")
    private int permission;

    @Column(name = "GrantOrder")
    private int grantOrder;

    @Column(name = "CreateTime")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date createTime;

    @Column(name = "LastModifiedTime")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastModifiedTime;

    public SAclVO() {
    }

    public Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public int getGranteeType() {
        return granteeType;
    }

    public void setGranteeType(int granteeType) {
        this.granteeType = granteeType;
    }

    public String getGranteeCanonicalId() {
        return granteeCanonicalId;
    }

    public void setGranteeCanonicalId(String granteeCanonicalId) {
        this.granteeCanonicalId = granteeCanonicalId;
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    public int getGrantOrder() {
        return grantOrder;
    }

    public void setGrantOrder(int grantOrder) {
        this.grantOrder = grantOrder;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    /** Return an OrderedPair
     *              < permission, grantee >
     * comprising
     * a permission - which is one of SAcl.PERMISSION_PASS, SAcl.PERMISSION_NONE, SAcl.PERMISSION_READ,
     *     SAcl.PERMISSION_WRITE, SAcl.PERMISSION_READ_ACL, SAcl.PERMISSION_WRITE_ACL, SAcl.PERMISSION_FULL
     * a grantee - which is one of GRANTEE_ALLUSERS, GRANTEE_AUTHENTICATED, GRANTEE_USER
     *
     * Access controls that are specified via the "x-amz-acl:" headers in REST requests for buckets.
     * The ACL request string is treated as a request for a known cannedAccessPolicy
     * @param aclRequestString - The requested ACL from the set of AWS S3 canned ACLs
     * @param target - Either "SBucket" or otherwise assumed to be for a single object item
     */
    public static OrderedPair<Integer, Integer> getCannedAccessControls(String aclRequestString, String target) throws UnsupportedException {
        if (aclRequestString.equalsIgnoreCase("public-read"))
            // All users granted READ access.
            return new OrderedPair<Integer, Integer>(PERMISSION_READ, GRANTEE_ALLUSERS);
        else if (aclRequestString.equalsIgnoreCase("public-read-write"))
            // All users granted READ and WRITE access
            return new OrderedPair<Integer, Integer>((PERMISSION_READ | PERMISSION_WRITE), GRANTEE_ALLUSERS);
        else if (aclRequestString.equalsIgnoreCase("authenticated-read"))
            // Authenticated users have READ access
            return new OrderedPair<Integer, Integer>(PERMISSION_READ, GRANTEE_AUTHENTICATED);
        else if (aclRequestString.equalsIgnoreCase("private"))
            // Only Owner gets FULL_CONTROL
            return new OrderedPair<Integer, Integer>(PERMISSION_FULL, GRANTEE_USER);
        else if (aclRequestString.equalsIgnoreCase("bucket-owner-read")) {
            // Object Owner gets FULL_CONTROL, Bucket Owner gets READ
            if (target.equalsIgnoreCase("SBucket"))
                return new OrderedPair<Integer, Integer>(PERMISSION_READ, GRANTEE_USER);
            else
                return new OrderedPair<Integer, Integer>(PERMISSION_FULL, GRANTEE_USER);
        } else if (aclRequestString.equalsIgnoreCase("bucket-owner-full-control")) {
            // Object Owner gets FULL_CONTROL, Bucket Owner gets FULL_CONTROL
            // This is equivalent to private when used with PUT Bucket
            return new OrderedPair<Integer, Integer>(PERMISSION_FULL, GRANTEE_USER);
        } else
            throw new UnsupportedException("Unknown Canned Access Policy: " + aclRequestString + " is not supported");
    }

    /** Return a Triple
     *         < permission1, permission2, symbol >
     *  comprising
     * two permissions - which is one of SAcl.PERMISSION_PASS, SAcl.PERMISSION_NONE, SAcl.PERMISSION_READ,
     *     SAcl.PERMISSION_WRITE, SAcl.PERMISSION_READ_ACL, SAcl.PERMISSION_WRITE_ACL, SAcl.PERMISSION_FULL
     * permission1 applies to objects, permission2 applies to buckets.
     * a symbol to indicate whether the principal is anonymous (i.e. string "A") or authenticated user (i.e.
     *     string "*") - otherwise null indicates a single ACL for all users.
     *
     * Access controls that are specified via the "x-amz-acl:" headers in REST requests for buckets.
     * The ACL request string is treated as a request for a known cannedAccessPolicy
     * @param aclRequestString - The requested ACL from the set of AWS S3 canned ACLs
     * @param target - Either "SBucket" or otherwise assumed to be for a single object item
     * @param ownerID - An ID for the owner, if used in place of symbols "A" or "*"
     */
    public static Triple<Integer, Integer, String> getCannedAccessControls(String aclRequestString, String target, String ownerID) throws UnsupportedException {
        if (aclRequestString.equalsIgnoreCase("public-read"))
            // Owner gets FULL_CONTROL and the anonymous principal (the 'A' symbol here) is granted READ access.
            return new Triple<Integer, Integer, String>(PERMISSION_FULL, PERMISSION_READ, "A");
        else if (aclRequestString.equalsIgnoreCase("public-read-write"))
            // Owner gets FULL_CONTROL and the anonymous principal (the 'A' symbol here) is granted READ and WRITE access
            return new Triple<Integer, Integer, String>(PERMISSION_FULL, (PERMISSION_READ | PERMISSION_WRITE), "A");
        else if (aclRequestString.equalsIgnoreCase("authenticated-read"))
            // Owner gets FULL_CONTROL and ANY principal authenticated as a registered S3 user (the '*' symbol here) is granted READ access
            return new Triple<Integer, Integer, String>(PERMISSION_FULL, PERMISSION_READ, "*");
        else if (aclRequestString.equalsIgnoreCase("private"))
            // This is termed the "private" or default ACL, "Owner gets FULL_CONTROL"
            return new Triple<Integer, Integer, String>(PERMISSION_FULL, PERMISSION_FULL, null);
        else if (aclRequestString.equalsIgnoreCase("bucket-owner-read")) {
            // Object Owner gets FULL_CONTROL, Bucket Owner gets READ
            // This is equivalent to private when used with PUT Bucket
            if (target.equalsIgnoreCase("SBucket"))
                return new Triple<Integer, Integer, String>(PERMISSION_FULL, PERMISSION_FULL, null);
            else
                return new Triple<Integer, Integer, String>(PERMISSION_FULL, PERMISSION_READ, ownerID);
        } else if (aclRequestString.equalsIgnoreCase("bucket-owner-full-control")) {
            // Object Owner gets FULL_CONTROL, Bucket Owner gets FULL_CONTROL
            // This is equivalent to private when used with PUT Bucket
            if (target.equalsIgnoreCase("SBucket"))
                return new Triple<Integer, Integer, String>(PERMISSION_FULL, PERMISSION_FULL, null);
            else
                return new Triple<Integer, Integer, String>(PERMISSION_FULL, PERMISSION_FULL, ownerID);
        } else
            throw new UnsupportedException("Unknown Canned Access Policy: " + aclRequestString + " is not supported");
    }

}
