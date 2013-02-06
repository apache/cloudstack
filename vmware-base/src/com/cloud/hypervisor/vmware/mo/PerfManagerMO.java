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
package com.cloud.hypervisor.vmware.mo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PerfCompositeMetric;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfInterval;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfProviderSummary;
import com.vmware.vim25.PerfQuerySpec;

import edu.emory.mathcs.backport.java.util.Arrays;

public class PerfManagerMO extends BaseMO {
    public PerfManagerMO(VmwareContext context, ManagedObjectReference mor) {
        super(context, mor);
    }

    public PerfManagerMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public void createPerfInterval(PerfInterval interval) throws Exception {
        _context.getService().createPerfInterval(_mor, interval);
    }

    /**
     * Converts Calendar object into XMLGregorianCalendar
     *
     * @param calendar Object to be converted
     * @return XMLGregorianCalendar
     */
    private XMLGregorianCalendar calendarToXMLGregorianCalendar(Calendar calendar) throws DatatypeConfigurationException {

        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar xgc = dtf.newXMLGregorianCalendar();
        xgc.setYear(calendar.get(Calendar.YEAR));
        xgc.setMonth(calendar.get(Calendar.MONTH) + 1);
        xgc.setDay(calendar.get(Calendar.DAY_OF_MONTH));
        xgc.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        xgc.setMinute(calendar.get(Calendar.MINUTE));
        xgc.setSecond(calendar.get(Calendar.SECOND));
        xgc.setMillisecond(calendar.get(Calendar.MILLISECOND));

        // Calendar ZONE_OFFSET and DST_OFFSET fields are in milliseconds.
        int offsetInMinutes = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000);
        xgc.setTimezone(offsetInMinutes);
        return xgc;
    }

    public List<PerfMetricId> queryAvailablePerfMetric(ManagedObjectReference morEntity, Calendar beginTime,
        Calendar endTime, Integer intervalId) throws Exception {

        return _context.getService().queryAvailablePerfMetric(_mor, morEntity, calendarToXMLGregorianCalendar(beginTime),
                calendarToXMLGregorianCalendar(endTime), intervalId);
    }

    public PerfCompositeMetric queryPerfComposite(PerfQuerySpec spec) throws Exception {
        return _context.getService().queryPerfComposite(_mor, spec);
    }

    public List<PerfCounterInfo> queryPerfCounter(int[] counterId) throws Exception {
        List<Integer> counterArr = new ArrayList<Integer>();
        if ( counterId != null){
            for (int i = 0; i < counterId.length; i++ ){
                counterArr.add(counterId[i]);
            }
        }
        return _context.getService().queryPerfCounter(_mor, counterArr);
    }

    public List<PerfCounterInfo> queryPerfCounterByLevel(int level) throws Exception {
        return _context.getService().queryPerfCounterByLevel(_mor, level);
    }

    public PerfProviderSummary queryPerfProviderSummary(ManagedObjectReference morEntity) throws Exception {
        return _context.getService().queryPerfProviderSummary(_mor, morEntity);
    }

    public List<PerfEntityMetricBase> queryPerf(PerfQuerySpec[] specs) throws Exception {
        return _context.getService().queryPerf(_mor, Arrays.asList(specs));
    }

    public void removePerfInterval(int samplePeriod) throws Exception {
        _context.getService().removePerfInterval(_mor, samplePeriod);
    }

    public void updatePerfInterval(PerfInterval interval) throws Exception {
        _context.getService().updatePerfInterval(_mor, interval);
    }

    public List<PerfCounterInfo> getCounterInfo() throws Exception {
        return (List<PerfCounterInfo>)_context.getVimClient().getDynamicProperty(_mor, "perfCounter");
    }

    public List<PerfInterval> getIntervalInfo() throws Exception {
        return (List<PerfInterval>)_context.getVimClient().getDynamicProperty(_mor, "historicalInterval");
    }
}
