/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.mom.kafka;

import java.io.FileInputStream;

import java.util.Map;
import java.util.UUID;
import java.util.Properties;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.framework.events.EventTopic;

import com.cloud.utils.component.ManagerBase;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.cloud.utils.PropertiesUtil;

@Local(value = EventBus.class)
public class KafkaEventBus extends ManagerBase implements EventBus {

    public static final String DEFAULT_TOPIC = "cloudstack";
    public static final String DEFAULT_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

    private String _topic = null;
    private Producer<String,String> _producer;
    private static final Logger s_logger = Logger.getLogger(KafkaEventBus.class);

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        final Properties props = new Properties();

        try (final FileInputStream is = new FileInputStream(PropertiesUtil.findConfigFile("kafka.producer.properties"));) {
            props.load(is);

            _topic = (String)props.remove("topic");
            if (_topic == null) {
                _topic = DEFAULT_TOPIC;
            }

            if (!props.containsKey("key.serializer")) {
                props.put("key.serializer", DEFAULT_SERIALIZER);
            }

            if (!props.containsKey("value.serializer")) {
                props.put("value.serializer", DEFAULT_SERIALIZER);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Could not read kafka properties");
        }

        _producer = new KafkaProducer<String,String>(props);
        _name = name;

        return true;
    }

    @Override
    public void setName(String name) {
        _name = name;
    }

    @Override
    public UUID subscribe(EventTopic topic, EventSubscriber subscriber) throws EventBusException {
        /* NOOP */
        return UUID.randomUUID();
    }

    @Override
    public void unsubscribe(UUID subscriberId, EventSubscriber subscriber) throws EventBusException {
        /* NOOP */
    }

    @Override
    public void publish(Event event) throws EventBusException {
        ProducerRecord<String, String> record = new ProducerRecord<String,String>(_topic, event.getResourceUUID(), event.getDescription());
        _producer.send(record);
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
