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
package com.cloud.api.doc;

import com.cloud.alert.AlertManager;
import com.cloud.serializer.Param;
import com.cloud.utils.IteratorUtil;
import com.cloud.utils.ReflectUtil;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.xstream.XStream;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ApiXmlDocWriter {
    protected static Logger LOGGER = LogManager.getLogger(ApiXmlDocWriter.class);

    private static String s_dirName = "";
    private static Map<String, Class<?>> s_apiNameCmdClassMap = new HashMap<String, Class<?>>();
    private static LinkedHashMap<Object, String> s_allApiCommands = new LinkedHashMap<Object, String>();
    private static TreeMap<Object, String> s_allApiCommandsSorted = new TreeMap<Object, String>();
    private static final List<String> AsyncResponses = setAsyncResponses();

    private static List<String> setAsyncResponses() {
        List<String> asyncResponses = new ArrayList<String>();
        asyncResponses.add(TemplateResponse.class.getName());
        asyncResponses.add(VolumeResponse.class.getName());
        //asyncResponses.add(LoadBalancerResponse.class.getName());
        asyncResponses.add(HostResponse.class.getName());
        asyncResponses.add(IPAddressResponse.class.getName());
        asyncResponses.add(StoragePoolResponse.class.getName());
        asyncResponses.add(UserVmResponse.class.getName());
        asyncResponses.add(SecurityGroupResponse.class.getName());
        //asyncResponses.add(ExternalLoadBalancerResponse.class.getName());
        asyncResponses.add(SnapshotResponse.class.getName());

        return asyncResponses;
    }

    public static void main(String[] args) {
        Set<Class<?>> cmdClasses = ReflectUtil.getClassesWithAnnotation(APICommand.class, new String[] {"org.apache.cloudstack.api", "com.cloud.api",
                "com.cloud.api.commands", "com.globo.globodns.cloudstack.api", "org.apache.cloudstack.network.opendaylight.api",
                "org.apache.cloudstack.api.command.admin.zone", "org.apache.cloudstack.network.contrail.api.command"});

        for (Class<?> cmdClass : cmdClasses) {
            if(cmdClass.getAnnotation(APICommand.class)==null){
               System.out.println("Warning, API Cmd class " + cmdClass.getName() + " has no APICommand annotation ");
               continue;
            }
            String apiName = cmdClass.getAnnotation(APICommand.class).name();
            if (s_apiNameCmdClassMap.containsKey(apiName)) {
                // handle API cmd separation into admin cmd and user cmd with the common api name
                Class<?> curCmd = s_apiNameCmdClassMap.get(apiName);
                if (curCmd.isAssignableFrom(cmdClass)) {
                    // api_cmd map always keep the admin cmd class to get full response and parameters
                    s_apiNameCmdClassMap.put(apiName, cmdClass);
                } else if (cmdClass.isAssignableFrom(curCmd)) {
                    // just skip this one without warning
                    continue;
                } else {
                    System.out.println("Warning, API Cmd class " + cmdClass.getName() + " has non-unique apiname " + apiName);
                    continue;
                }
            } else {
                s_apiNameCmdClassMap.put(apiName, cmdClass);
            }
        }
        System.out.printf("Scanned and found %d APIs\n", s_apiNameCmdClassMap.size());
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if (arg.equals("-d")) {
                s_dirName = iter.next();
            }
        }

        for (Map.Entry<String, Class<?>> entry: s_apiNameCmdClassMap.entrySet()) {
            Class<?> cls = entry.getValue();
            s_allApiCommands.put(entry.getKey(), cls.getName());
        }

        s_allApiCommandsSorted.putAll(s_allApiCommands);

        try {
            // Create object writer
            XStream xs = new XStream();
            xs.alias("command", Command.class);
            xs.alias("arg", Argument.class);
            String xmlDocDir = s_dirName + "/xmldoc";
            String rootAdminDirName = xmlDocDir + "/apis";
            (new File(rootAdminDirName)).mkdirs();

            ObjectOutputStream out = xs.createObjectOutputStream(new FileWriter(s_dirName + "/commands.xml"), "commands");
            ObjectOutputStream rootAdmin = xs.createObjectOutputStream(new FileWriter(rootAdminDirName + "/" + "apiSummary.xml"), "commands");
            ObjectOutputStream rootAdminSorted = xs.createObjectOutputStream(new FileWriter(rootAdminDirName + "/" + "apiSummarySorted.xml"), "commands");

            Iterator<?> it = s_allApiCommands.keySet().iterator();
            while (it.hasNext()) {
                String key = (String)it.next();
                // Write admin commands
                writeCommand(out, key);
                writeCommand(rootAdmin, key);
                // Write single commands to separate xml files
                ObjectOutputStream singleRootAdminCommandOs = xs.createObjectOutputStream(new FileWriter(rootAdminDirName + "/" + key + ".xml"), "command");
                writeCommand(singleRootAdminCommandOs, key);
                singleRootAdminCommandOs.close();
            }

            // Write sorted commands
            it = s_allApiCommandsSorted.keySet().iterator();
            while (it.hasNext()) {
                String key = (String)it.next();
                writeCommand(rootAdminSorted, key);
            }

            out.close();
            rootAdmin.close();
            rootAdminSorted.close();

            // write alerttypes to xml
            writeAlertTypes(xmlDocDir);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(2);
        }
    }

    private static void writeCommand(ObjectOutputStream out, String command) throws ClassNotFoundException, IOException {
        Class<?> clas = Class.forName(s_allApiCommands.get(command));
        ArrayList<Argument> request = new ArrayList<Argument>();
        ArrayList<Argument> response = new ArrayList<Argument>();

        // Create a new command, set name/description/usage
        Command apiCommand = new Command();
        apiCommand.setName(command);

        APICommand impl = clas.getAnnotation(APICommand.class);
        if (impl == null) {
            impl = clas.getSuperclass().getAnnotation(APICommand.class);
        }

        if (impl == null) {
            throw new IllegalStateException(String.format("An %1$s annotation is required for class %2$s.", APICommand.class.getCanonicalName(), clas.getCanonicalName()));
        }

        if (impl.includeInApiDoc()) {
            String commandDescription = impl.description();
            if (commandDescription != null && !commandDescription.isEmpty()) {
                apiCommand.setDescription(commandDescription);
            } else {
                System.out.println("Command " + apiCommand.getName() + " misses description");
            }

            String commandUsage = impl.usage();
            if (commandUsage != null && !commandUsage.isEmpty()) {
                apiCommand.setUsage(commandUsage);
            }

            //Set version when the API is added
            if (!impl.since().isEmpty()) {
                apiCommand.setSinceVersion(impl.since());
            }

            boolean isAsync = ReflectUtil.isCmdClassAsync(clas, new Class<?>[] {BaseAsyncCmd.class, BaseAsyncCreateCmd.class});
            apiCommand.setAsync(isAsync);

            boolean isDeprecated = clas.getAnnotation(Deprecated.class) != null;
            apiCommand.setDeprecated(isDeprecated);

            Set<Field> fields = ReflectUtil.getAllFieldsForClass(clas, new Class<?>[] {BaseCmd.class, BaseAsyncCmd.class, BaseAsyncCreateCmd.class});

            request = setRequestFields(fields);

            // Get response parameters
            Class<?> responseClas = impl.responseObject();
            Field[] responseFields = responseClas.getDeclaredFields();
            response = setResponseFields(responseFields, responseClas);

            apiCommand.setRequest(request);
            apiCommand.setResponse(response);

            out.writeObject(apiCommand);
        } else {
            LOGGER.debug("Command " + command + " is not exposed in api doc");
        }
    }

    private static ArrayList<Argument> setRequestFields(Set<Field> fields) {
        ArrayList<Argument> arguments = new ArrayList<Argument>();
        Set<Argument> requiredArguments = new HashSet<Argument>();
        Set<Argument> optionalArguments = new HashSet<Argument>();
        Argument id = null;
        for (Field f : fields) {
            Parameter parameterAnnotation = f.getAnnotation(Parameter.class);
            if (parameterAnnotation != null && parameterAnnotation.expose() && parameterAnnotation.includeInApiDoc()) {
                Argument reqArg = new Argument(parameterAnnotation.name());
                reqArg.setRequired(parameterAnnotation.required());
                if (!parameterAnnotation.description().isEmpty()) {
                    reqArg.setDescription(parameterAnnotation.description());
                }

                if (parameterAnnotation.type() == BaseCmd.CommandType.LIST || parameterAnnotation.type() == BaseCmd.CommandType.MAP) {
                    reqArg.setType(parameterAnnotation.type().toString().toLowerCase());
                }

                reqArg.setDataType(parameterAnnotation.type().toString().toLowerCase());

                if (!parameterAnnotation.since().isEmpty()) {
                    reqArg.setSinceVersion(parameterAnnotation.since());
                }

                if (reqArg.isRequired()) {
                    if (parameterAnnotation.name().equals("id")) {
                        id = reqArg;
                    } else {
                        requiredArguments.add(reqArg);
                    }
                } else {
                    optionalArguments.add(reqArg);
                }
            }
        }

        // sort required and optional arguments here
        if (id != null) {
            arguments.add(id);
        }
        arguments.addAll(IteratorUtil.asSortedList(requiredArguments));
        arguments.addAll(IteratorUtil.asSortedList(optionalArguments));

        return arguments;
    }

    private static ArrayList<Argument> setResponseFields(Field[] responseFields, Class<?> responseClas) {
        ArrayList<Argument> arguments = new ArrayList<Argument>();
        ArrayList<Argument> sortedChildlessArguments = new ArrayList<Argument>();
        ArrayList<Argument> sortedArguments = new ArrayList<Argument>();

        Argument id = null;

        for (Field responseField : responseFields) {
            SerializedName nameAnnotation = responseField.getAnnotation(SerializedName.class);
            if (nameAnnotation != null) {
                Param paramAnnotation = responseField.getAnnotation(Param.class);
                Argument respArg = new Argument(nameAnnotation.value());

                boolean hasChildren = false;
                if (paramAnnotation != null && paramAnnotation.includeInApiDoc()) {
                    String description = paramAnnotation.description();
                    Class fieldClass = paramAnnotation.responseObject();
                    if (description != null && !description.isEmpty()) {
                        respArg.setDescription(description);
                    }

                    respArg.setDataType(responseField.getType().getSimpleName().toLowerCase());

                    if (!paramAnnotation.since().isEmpty()) {
                        respArg.setSinceVersion(paramAnnotation.since());
                    }

                    if (fieldClass != null) {
                        Class<?> superClass = fieldClass.getSuperclass();
                        if (superClass != null) {
                            String superName = superClass.getName();
                            if (superName.equals(BaseResponse.class.getName())) {
                                ArrayList<Argument> fieldArguments = new ArrayList<Argument>();
                                Field[] fields = fieldClass.getDeclaredFields();
                                fieldArguments = setResponseFields(fields, fieldClass);
                                respArg.setArguments(fieldArguments);
                                hasChildren = true;
                            }
                        }
                    }
                }

                if (paramAnnotation != null && paramAnnotation.includeInApiDoc()) {
                    if (nameAnnotation.value().equals("id")) {
                        id = respArg;
                    } else {
                        if (hasChildren) {
                            respArg.setName(nameAnnotation.value() + "(*)");
                            sortedArguments.add(respArg);
                        } else {
                            sortedChildlessArguments.add(respArg);
                        }
                    }
                }
            }
        }

        Collections.sort(sortedArguments);
        Collections.sort(sortedChildlessArguments);

        if (id != null) {
            arguments.add(id);
        }
        arguments.addAll(sortedChildlessArguments);
        arguments.addAll(sortedArguments);

        if (responseClas.getName().equalsIgnoreCase(AsyncJobResponse.class.getName())) {
            Argument jobIdArg = new Argument("jobid", "the ID of the async job");
            arguments.add(jobIdArg);
        } else if (AsyncResponses.contains(responseClas.getName())) {
            Argument jobIdArg = new Argument("jobid", "the ID of the latest async job acting on this object");
            Argument jobStatusArg = new Argument("jobstatus", "the current status of the latest async job acting on this object");
            arguments.add(jobIdArg);
            arguments.add(jobStatusArg);
        }

        return arguments;
    }

    private static void zipDir(String zipFileName, String dir) throws Exception {
        File dirObj = new File(dir);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        addDir(dirObj, out);
        out.close();
    }

    static void addDir(File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];
        String pathToDir = s_dirName;

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                addDir(files[i], out);
                continue;
            }
            try(FileInputStream in = new FileInputStream(files[i].getPath());) {
                out.putNextEntry(new ZipEntry(files[i].getPath().substring(pathToDir.length())));
                int len;
                while ((len = in.read(tmpBuf)) > 0) {
                    out.write(tmpBuf, 0, len);
                }
                out.closeEntry();
            }catch(IOException ex)
            {
                LOGGER.error("addDir:Exception:"+ ex.getMessage(),ex);
            }
        }
    }

    private static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    deleteDir(new File(dir, children[i]));
                }
            }
        }
        dir.delete();
    }

    private static void writeAlertTypes(String dirName) {
        XStream xs = new XStream();
        xs.alias("alert", Alert.class);
        try(ObjectOutputStream out = xs.createObjectOutputStream(new FileWriter(dirName + "/alert_types.xml"), "alerts");) {
            for (Field f : AlertManager.class.getFields()) {
                if (f.getClass().isAssignableFrom(Number.class)) {
                    String name = f.getName().substring(11);
                    Alert alert = new Alert(name, f.getInt(null));
                    out.writeObject(alert);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create output stream to write an alert types ", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to read alert fields ", e);
        }
    }

    private static class LinkedProperties extends Properties {
        private final LinkedList<Object> keys = new LinkedList<Object>();

        @Override
        public Enumeration<Object> keys() {
            return Collections.<Object> enumeration(keys);
        }

        @Override
        public Object put(Object key, Object value) {
            // System.out.println("Adding key" + key);
            keys.add(key);
            return super.put(key, value);
        }
    }
}
