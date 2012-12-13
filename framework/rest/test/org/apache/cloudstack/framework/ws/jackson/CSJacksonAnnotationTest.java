package org.apache.cloudstack.framework.ws.jackson;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule.Priority;

public class CSJacksonAnnotationTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
        ObjectMapper mapper = new ObjectMapper();
        JaxbAnnotationModule jaxbModule = new JaxbAnnotationModule();
        jaxbModule.setPriority(Priority.SECONDARY);
        mapper.registerModule(jaxbModule);
        mapper.registerModule(new CSJacksonAnnotationModule());

        StringWriter writer = new StringWriter();

        TestVO vo = new TestVO(1000, "name");
        vo.names = new ArrayList<String>();
        vo.names.add("name1");
        vo.names.add("name2");
        vo.values = new HashMap<String, Long>();
        vo.values.put("key1", 1000l);
        vo.values.put("key2", 2000l);
        vo.vo2.name = "testvoname2";
        vo.pods="abcde";

        try {
            mapper.writeValue(writer, vo);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print(writer.getBuffer().toString());

    }

    @XmlRootElement(name="xml-test2")
    public class Test2VO {
        public String name;
    }

    @XmlRootElement(name="abc")
    public class TestVO {
        public int id;

        public Map<String, Long> values;

        public String name;


        public List<String> names;

        public String pods;


        @XmlElement(name="test2")
        public Test2VO vo2 = new Test2VO();

        public TestVO(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Url(clazz=TestVO.class, method="getName")
        public String getName() {
            return name;
        }

        @Url(clazz=TestVO.class, method="getNames", type=List.class)
        public List<String> getNames() {
            return names;
        }

    }

}
