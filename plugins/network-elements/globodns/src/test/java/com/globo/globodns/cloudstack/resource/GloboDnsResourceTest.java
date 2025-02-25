/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.globo.globodns.cloudstack.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.cloud.agent.api.Answer;
import com.globo.globodns.client.GloboDns;
import com.globo.globodns.client.api.DomainAPI;
import com.globo.globodns.client.api.ExportAPI;
import com.globo.globodns.client.api.RecordAPI;
import com.globo.globodns.client.model.Domain;
import com.globo.globodns.client.model.Record;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateDomainCommand;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateRecordAndReverseCommand;
import com.globo.globodns.cloudstack.commands.RemoveDomainCommand;
import com.globo.globodns.cloudstack.commands.RemoveRecordCommand;

@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class GloboDnsResourceTest {

    private GloboDnsResource _globoDnsResource;

    private GloboDns _globoDnsApi;
    private DomainAPI _domainApi;
    private RecordAPI _recordApi;
    private ExportAPI _exportApi;

    private static final Long TEMPLATE_ID = 1l;

    private static long sequenceId = 10l;

    @Before
    public void setUp() throws Exception {

        String name = "GloboDNS";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("zoneId", "1");
        params.put("guid", "globodns");
        params.put("name", name);
        params.put("url", "http://example.com");
        params.put("username", "username");
        params.put("password", "password");

        _globoDnsResource = new GloboDnsResource();
        _globoDnsResource.configure(name, params);

        _globoDnsApi = spy(_globoDnsResource._globoDns);
        _globoDnsResource._globoDns = _globoDnsApi;

        _domainApi = mock(DomainAPI.class);
        when(_globoDnsApi.getDomainAPI()).thenReturn(_domainApi);

        _recordApi = mock(RecordAPI.class);
        when(_globoDnsApi.getRecordAPI()).thenReturn(_recordApi);

        _exportApi = mock(ExportAPI.class);
        when(_globoDnsApi.getExportAPI()).thenReturn(_exportApi);
    }

    @After
    public void tearDown() throws Exception {
    }

    ///////////////////////
    // Auxiliary Methods //
    ///////////////////////

    private Domain generateFakeDomain(String domainName, boolean reverse) {
        Domain domain = new Domain();
        domain.getDomainAttributes().setId(sequenceId++);
        domain.getDomainAttributes().setName(domainName);
        List<Domain> domainList = new ArrayList<Domain>();
        domainList.add(domain);
        if (reverse) {
            when(_domainApi.listReverseByQuery(eq(domainName))).thenReturn(domainList);
        } else {
            when(_domainApi.listByQuery(eq(domainName))).thenReturn(domainList);
        }
        return domain;
    }

    private Record generateFakeRecord(Domain domain, String recordName, String recordContent, boolean reverse) {
        Record record = new Record();
        if (reverse) {
            record.getTypePTRRecordAttributes().setName(recordName);
            record.getTypePTRRecordAttributes().setContent(recordContent);
            record.getTypePTRRecordAttributes().setDomainId(domain.getId());
            record.getTypePTRRecordAttributes().setId(sequenceId++);
        } else {
            record.getTypeARecordAttributes().setName(recordName);
            record.getTypeARecordAttributes().setContent(recordContent);
            record.getTypeARecordAttributes().setDomainId(domain.getId());
            record.getTypeARecordAttributes().setId(sequenceId++);
        }
        List<Record> recordList = new ArrayList<Record>();
        recordList.add(record);
        when(_recordApi.listByQuery(eq(domain.getId()), eq(recordName))).thenReturn(recordList);
        return record;
    }

    /////////////////////////
    // Create Domain tests //
    /////////////////////////

    @Test
    public void testCreateDomainWithSuccessWhenDomainDoesntExistAndOverrideIsTrue() throws Exception {
        String domainName = "domain.name.com";

        Domain domain = new Domain();
        domain.getDomainAttributes().setId(sequenceId++);
        domain.getDomainAttributes().setName(domainName);

        when(_domainApi.createDomain(eq(domain.getName()), eq(TEMPLATE_ID), eq("M"))).thenReturn(domain);

        Answer answer = _globoDnsResource.execute(new CreateOrUpdateDomainCommand(domainName, TEMPLATE_ID));
        assertNotNull(answer);
        assertEquals(true, answer.getResult());
        verify(_exportApi, times(1)).scheduleExport();
    }

    @Test
    @SuppressWarnings("unused")
    public void testCreateDomainWillSucceedWhenDomainAlreadyExistsAndOverrideIsFalse() throws Exception {
        String domainName = "domain.name.com";

        Domain domain = generateFakeDomain(domainName, false);

        Answer answer = _globoDnsResource.execute(new CreateOrUpdateDomainCommand(domainName, TEMPLATE_ID));
        assertNotNull(answer);
        assertEquals(true, answer.getResult());
    }

    /////////////////////////
    // Create Record tests //
    /////////////////////////

    @Test
    @SuppressWarnings("unused")
    public void testCreateRecordAndReverseWithSuccessWhenDomainExistsAndRecordDoesntExistAndOverrideIsTrue() throws Exception {
        String recordName = "recordname";
        String recordIp = "40.30.20.10";
        String domainName = "domain.name.com";
        String reverseDomainName = "20.30.40.in-addr.arpa";
        String reverseRecordName = "10";
        String reverseRecordContent = recordName + "." + domainName;

        Domain domain = generateFakeDomain(domainName, false);
        Record record = generateFakeRecord(domain, recordName, recordIp, false);
        Domain reverseDomain = generateFakeDomain(reverseDomainName, true);
        Record reverseRecord = generateFakeRecord(reverseDomain, reverseRecordName, reverseRecordContent, true);

        when(_recordApi.createRecord(eq(domain.getId()), eq(recordName), eq(recordIp), eq("A"))).thenReturn(record);
        when(_recordApi.createRecord(eq(reverseDomain.getId()), eq(reverseRecordName), eq(reverseRecordContent), eq("PTR"))).thenReturn(record);

        Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, recordIp, domainName, TEMPLATE_ID, true));
        assertNotNull(answer);
        assertEquals(true, answer.getResult());
        verify(_exportApi, times(1)).scheduleExport();
    }

    @Test
    @SuppressWarnings("unused")
    public void testCreateRecordAndReverseWillFailWhenRecordAlreadyExistsAndOverrideIsFalse() throws Exception {
        String recordName = "recordname";
        String newIp = "40.30.20.10";
        String oldIp = "50.40.30.20";
        String domainName = "domain.name.com";

        Domain domain = generateFakeDomain(domainName, false);
        Record record = generateFakeRecord(domain, recordName, oldIp, false);

        Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, newIp, domainName, TEMPLATE_ID, false));
        assertNotNull(answer);
        assertEquals(false, answer.getResult());
    }

    @Test
    @SuppressWarnings("unused")
    public void testCreateRecordAndReverseWillFailWhenReverseRecordAlreadyExistsAndOverrideIsFalse() throws Exception {
        String recordName = "recordname";
        String recordIp = "40.30.20.10";
        String domainName = "domain.name.com";
        String reverseDomainName = "20.30.40.in-addr.arpa";
        String reverseRecordName = "10";

        Domain domain = generateFakeDomain(domainName, false);
        Record record = generateFakeRecord(domain, recordName, recordIp, false);
        Domain reverseDomain = generateFakeDomain(reverseDomainName, true);
        Record reverseRecord = generateFakeRecord(reverseDomain, reverseRecordName, "X", true);

        Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, recordIp, domainName, TEMPLATE_ID, false));
        assertNotNull(answer);
        assertEquals(false, answer.getResult());
    }

    @Test
    public void testCreateRecordAndReverseWhenDomainDoesNotExist() throws Exception {
        String recordName = "recordname";
        String recordIp = "40.30.20.10";
        String domainName = "domain.name.com";
        String reverseDomainName = "20.30.40.in-addr.arpa";
        String reverseRecordName = "10";
        String reverseRecordContent = recordName + "." + domainName;

        Domain domain = new Domain();
        domain.getDomainAttributes().setId(sequenceId++);
        Domain reverseDomain = new Domain();
        reverseDomain.getDomainAttributes().setId(sequenceId++);

        Record record = new Record();

        when(_domainApi.listByQuery(domainName)).thenReturn(new ArrayList<Domain>());
        when(_domainApi.createDomain(eq(domainName), eq(TEMPLATE_ID), eq("M"))).thenReturn(domain);
        when(_recordApi.createRecord(eq(domain.getId()), eq(recordName), eq(recordIp), eq("A"))).thenReturn(record);
        when(_domainApi.createReverseDomain(eq(reverseDomainName), eq(TEMPLATE_ID), eq("M"))).thenReturn(reverseDomain);
        when(_recordApi.createRecord(eq(reverseDomain.getId()), eq(reverseRecordName), eq(reverseRecordContent), eq("PTR"))).thenReturn(record);

        Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, recordIp, domainName, TEMPLATE_ID, true));
        assertNotNull(answer);
        assertEquals(true, answer.getResult());
        verify(_exportApi, times(1)).scheduleExport();
    }

    @Test
    public void testCreateRecordAndReverseWhenDomainDoesNotExistAndOverrideIsFalse() throws Exception {
        String recordName = "recordname";
        String recordIp = "40.30.20.10";
        String domainName = "domain.name.com";
        String reverseDomainName = "20.30.40.in-addr.arpa";
        String reverseRecordName = "10";
        String reverseRecordContent = recordName + "." + domainName;

        Domain domain = new Domain();
        domain.getDomainAttributes().setId(sequenceId++);
        Domain reverseDomain = new Domain();
        reverseDomain.getDomainAttributes().setId(sequenceId++);

        Record record = new Record();

        when(_domainApi.listByQuery(domainName)).thenReturn(new ArrayList<Domain>());
        when(_domainApi.createDomain(eq(domainName), eq(TEMPLATE_ID), eq("M"))).thenReturn(domain);
        when(_recordApi.createRecord(eq(domain.getId()), eq(recordName), eq(recordIp), eq("A"))).thenReturn(record);
        when(_domainApi.createReverseDomain(eq(reverseDomainName), eq(TEMPLATE_ID), eq("M"))).thenReturn(reverseDomain);
        when(_recordApi.createRecord(eq(reverseDomain.getId()), eq(reverseRecordName), eq(reverseRecordContent), eq("PTR"))).thenReturn(record);

        Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, recordIp, domainName, TEMPLATE_ID, false));
        assertNotNull(answer);
        assertEquals(true, answer.getResult());
        verify(_exportApi, times(1)).scheduleExport();
    }

    /////////////////////////
    // Update Record tests //
    /////////////////////////

    @Test
    public void testUpdateRecordAndReverseWhenDomainExistsAndOverrideIsTrue() throws Exception {
        String recordName = "recordname";
        String oldRecordIp = "40.30.20.10";
        String newRecordIp = "50.40.30.20";
        String domainName = "domain.name.com";
        String reverseDomainName = "30.40.50.in-addr.arpa";
        String reverseRecordName = "20";
        String reverseRecordContent = recordName + "." + domainName;

        Domain domain = generateFakeDomain(domainName, false);
        Record record = generateFakeRecord(domain, recordName, oldRecordIp, false);
        Domain reverseDomain = generateFakeDomain(reverseDomainName, true);
        Record reverseRecord = generateFakeRecord(reverseDomain, reverseRecordName, "X", true);

        Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, newRecordIp, domainName, TEMPLATE_ID, true));

        // ensure calls in sequence to ensure this call are the only ones.
        InOrder inOrder = inOrder(_recordApi);
        inOrder.verify(_recordApi, times(1)).updateRecord(eq(record.getId()), eq(domain.getId()), eq(recordName), eq(newRecordIp));
        inOrder.verify(_recordApi, times(1)).updateRecord(eq(reverseRecord.getId()), eq(reverseDomain.getId()), eq(reverseRecordName), eq(reverseRecordContent));

        assertNotNull(answer);
        assertEquals(true, answer.getResult());
        verify(_exportApi, times(1)).scheduleExport();
    }

    /////////////////////////
    // Remove Record tests //
    /////////////////////////

    @Test
    public void testRemoveRecordWhenRecordExists() throws Exception {
        String recordName = "recordname";
        String recordIp = "40.30.20.10";
        String domainName = "domain.name.com";
        String reverseDomainName = "20.30.40.in-addr.arpa";
        String reverseRecordName = "10";
        String reverseRecordContent = recordName + "." + domainName;

        Domain domain = generateFakeDomain(domainName, false);
        Record record = generateFakeRecord(domain, recordName, recordIp, false);
        Domain reverseDomain = generateFakeDomain(reverseDomainName, true);
        Record reverseRecord = generateFakeRecord(reverseDomain, reverseRecordName, reverseRecordContent, true);

        Answer answer = _globoDnsResource.execute(new RemoveRecordCommand(recordName, recordIp, domainName, true));

        assertNotNull(answer);
        assertEquals(true, answer.getResult());
        verify(_recordApi, times(1)).removeRecord(eq(record.getId()));
        verify(_recordApi, times(1)).removeRecord(eq(reverseRecord.getId()));
        verify(_exportApi, times(1)).scheduleExport();
    }

    @Test
    public void testRemoveRecordWithSuccessAndReverseRecordNotRemovedWhenReverseRecordExistsWithDifferentValueAndOverrideIsFalse() throws Exception {
        String recordName = "recordname";
        String recordIp = "40.30.20.10";
        String domainName = "domain.name.com";
        String reverseDomainName = "20.30.40.in-addr.arpa";
        String reverseRecordName = "10";

        Domain domain = generateFakeDomain(domainName, false);
        Record record = generateFakeRecord(domain, recordName, recordIp, false);
        Domain reverseDomain = generateFakeDomain(reverseDomainName, true);
        Record reverseRecord = generateFakeRecord(reverseDomain, reverseRecordName, "X", true);

        Answer answer = _globoDnsResource.execute(new RemoveRecordCommand(recordName, recordIp, domainName, false));

        assertEquals(true, answer.getResult());
        verify(_recordApi, times(1)).removeRecord(eq(record.getId()));
        verify(_recordApi, never()).removeRecord(eq(reverseRecord.getId()));
        verify(_exportApi, times(1)).scheduleExport();
    }

    @Test
    public void testRemoveReverseRecordButNotRemoveRecordWhenRecordExistsWithDifferentValueAndOverrideIsFalse() throws Exception {
        String recordName = "recordname";
        String recordIp = "40.30.20.10";
        String domainName = "domain.name.com";
        String reverseDomainName = "20.30.40.in-addr.arpa";
        String reverseRecordName = "10";
        String reverseRecordContent = recordName + "." + domainName;

        Domain domain = generateFakeDomain(domainName, false);
        Record record = generateFakeRecord(domain, recordName, "X", false);
        Domain reverseDomain = generateFakeDomain(reverseDomainName, true);
        Record reverseRecord = generateFakeRecord(reverseDomain, reverseRecordName, reverseRecordContent, true);

        Answer answer = _globoDnsResource.execute(new RemoveRecordCommand(recordName, recordIp, domainName, false));

        assertEquals(true, answer.getResult());
        verify(_recordApi, never()).removeRecord(eq(record.getId()));
        verify(_recordApi, times(1)).removeRecord(eq(reverseRecord.getId()));
        verify(_exportApi, times(1)).scheduleExport();
    }

    /////////////////////////
    // Remove Domain tests //
    /////////////////////////

    @Test
    public void testRemoveDomainWithSuccessButDomainKeptWhenDomainExistsAndThereAreRecordsAndOverrideIsFalse() throws Exception {
        String recordName = "recordname";
        String recordIp = "40.30.20.10";
        String domainName = "domain.name.com";

        Domain domain = generateFakeDomain(domainName, false);
        Record record = generateFakeRecord(domain, recordName, "X", false);
        when(_recordApi.listAll(domain.getId())).thenReturn(Arrays.asList(record));

        Answer answer = _globoDnsResource.execute(new RemoveRecordCommand(recordName, recordIp, domainName, false));

        assertEquals(true, answer.getResult());
        verify(_domainApi, never()).removeDomain(any(Long.class));
        verify(_exportApi, never()).scheduleExport();
    }

    @Test
    public void testRemoveDomainWithSuccessWhenDomainExistsAndThereAreOnlyNSRecordsAndOverrideIsFalse() throws Exception {
        String domainName = "domain.name.com";

        Domain domain = generateFakeDomain(domainName, false);
        List<Record> recordList = new ArrayList<Record>();
        for (int i = 0; i < 10; i++) {
            Record record = new Record();
            record.getTypeNSRecordAttributes().setDomainId(domain.getId());
            record.getTypeNSRecordAttributes().setId(sequenceId++);
            record.getTypeNSRecordAttributes().setType("NS");
            recordList.add(record);
        }
        when(_recordApi.listAll(domain.getId())).thenReturn(recordList);

        Answer answer = _globoDnsResource.execute(new RemoveDomainCommand(domainName, false));

        assertEquals(true, answer.getResult());
        verify(_domainApi, times(1)).removeDomain(eq(domain.getId()));
        verify(_exportApi, times(1)).scheduleExport();
    }

    @Test
    public void testRemoveDomainWithSuccessWhenDomainExistsAndThereAreRecordsAndOverrideIsTrue() throws Exception {
        String domainName = "domain.name.com";

        Domain domain = generateFakeDomain(domainName, false);
        List<Record> recordList = new ArrayList<Record>();
        for (int i = 0; i < 10; i++) {
            Record record = new Record();
            record.getTypeNSRecordAttributes().setDomainId(domain.getId());
            record.getTypeNSRecordAttributes().setId(sequenceId++);
            record.getTypeNSRecordAttributes().setType(new String[] {"A", "NS", "PTR"}[i % 3]);
            recordList.add(record);
        }
        when(_recordApi.listAll(domain.getId())).thenReturn(recordList);

        Answer answer = _globoDnsResource.execute(new RemoveDomainCommand(domainName, true));

        assertEquals(true, answer.getResult());
        verify(_domainApi, times(1)).removeDomain(eq(domain.getId()));
        verify(_exportApi, times(1)).scheduleExport();
    }

}
