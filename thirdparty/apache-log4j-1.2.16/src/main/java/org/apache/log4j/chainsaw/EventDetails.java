/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.chainsaw;

import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Represents the details of a logging event. It is intended to overcome the
 * problem that a LoggingEvent cannot be constructed with purely fake data.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 * @version 1.0
 */
class EventDetails {

    /** the time of the event **/
    private final long mTimeStamp;
    /** the priority of the event **/
    private final Priority mPriority;
    /** the category of the event **/
    private final String mCategoryName;
    /** the NDC for the event **/
    private final String mNDC;
    /** the thread for the event **/
    private final String mThreadName;
    /** the msg for the event **/
    private final String mMessage;
    /** the throwable details the event **/
    private final String[] mThrowableStrRep;
    /** the location details for the event **/
    private final String mLocationDetails;

    /**
     * Creates a new <code>EventDetails</code> instance.
     * @param aTimeStamp a <code>long</code> value
     * @param aPriority a <code>Priority</code> value
     * @param aCategoryName a <code>String</code> value
     * @param aNDC a <code>String</code> value
     * @param aThreadName a <code>String</code> value
     * @param aMessage a <code>String</code> value
     * @param aThrowableStrRep a <code>String[]</code> value
     * @param aLocationDetails a <code>String</code> value
     */
    EventDetails(long aTimeStamp,
                 Priority aPriority,
                 String aCategoryName,
                 String aNDC,
                 String aThreadName,
                 String aMessage,
                 String[] aThrowableStrRep,
                 String aLocationDetails)
    {
        mTimeStamp = aTimeStamp;
        mPriority = aPriority;
        mCategoryName = aCategoryName;
        mNDC = aNDC;
        mThreadName = aThreadName;
        mMessage = aMessage;
        mThrowableStrRep = aThrowableStrRep;
        mLocationDetails = aLocationDetails;
    }

    /**
     * Creates a new <code>EventDetails</code> instance.
     *
     * @param aEvent a <code>LoggingEvent</code> value
     */
    EventDetails(LoggingEvent aEvent) {

        this(aEvent.timeStamp,
             aEvent.getLevel(),
             aEvent.getLoggerName(),
             aEvent.getNDC(),
             aEvent.getThreadName(),
             aEvent.getRenderedMessage(),
             aEvent.getThrowableStrRep(),
             (aEvent.getLocationInformation() == null)
             ? null : aEvent.getLocationInformation().fullInfo);
    }

    /** @see #mTimeStamp **/
    long getTimeStamp() {
        return mTimeStamp;
    }

    /** @see #mPriority **/
    Priority getPriority() {
        return mPriority;
    }

    /** @see #mCategoryName **/
    String getCategoryName() {
        return mCategoryName;
    }

    /** @see #mNDC **/
    String getNDC() {
        return mNDC;
    }

    /** @see #mThreadName **/
    String getThreadName() {
        return mThreadName;
    }

    /** @see #mMessage **/
    String getMessage() {
        return mMessage;
    }

    /** @see #mLocationDetails **/
    String getLocationDetails(){
        return mLocationDetails;
    }

    /** @see #mThrowableStrRep **/
    String[] getThrowableStrRep() {
        return mThrowableStrRep;
    }
}
