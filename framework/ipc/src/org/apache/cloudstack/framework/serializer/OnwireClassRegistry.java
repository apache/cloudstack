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
package org.apache.cloudstack.framework.serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.log4j.Logger;


//
// Finding classes in a given package code is taken and modified from
// Credit: http://internna.blogspot.com/2007/11/java-5-retrieving-all-classes-from.html
//
public class OnwireClassRegistry {
    private static final Logger s_logger = Logger.getLogger(OnwireClassRegistry.class);

    private List<String> packages = new ArrayList<String>();
    private final Map<String, Class<?>> registry = new HashMap<String, Class<?>>();

    public OnwireClassRegistry() {
        registry.put("Object", Object.class);
    }

    public OnwireClassRegistry(String packageName) {
        addPackage(packageName);
    }

    public OnwireClassRegistry(List<String> packages) {
        packages.addAll(packages);
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    public void addPackage(String packageName) {
        packages.add(packageName);
    }

    public void scan() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (String pkg : packages) {
            classes.addAll(getClasses(pkg));
        }

        for (Class<?> clz : classes) {
            OnwireName onwire = clz.getAnnotation(OnwireName.class);
            if (onwire != null) {
                assert (onwire.name() != null);

                registry.put(onwire.name(), clz);
            }
        }
    }

    public Class<?> getOnwireClass(String onwireName) {
        return registry.get(onwireName);
    }

    static Set<Class<?>> getClasses(String packageName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return getClasses(loader, packageName);
    }

    //
    // Following helper methods can be put in a separated helper class,
    // will do that later
    //
    static Set<Class<?>> getClasses(ClassLoader loader, String packageName) {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        String path = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = loader.getResources(path);
            if (resources != null) {
                while (resources.hasMoreElements()) {
                    String filePath = resources.nextElement().getFile();
                    if (filePath != null) {
                        // WINDOWS HACK
                        if (filePath.indexOf("%20") > 0)
                            filePath = filePath.replaceAll("%20", " ");
                        if ((filePath.indexOf("!") > 0) && (filePath.indexOf(".jar") > 0)) {
                            String jarPath = filePath.substring(0, filePath.indexOf("!")).substring(filePath.indexOf(":") + 1);
                            // WINDOWS HACK
                            if (jarPath.indexOf(":") >= 0)
                                jarPath = jarPath.substring(1);
                            classes.addAll(getFromJARFile(jarPath, path));
                        } else {
                            classes.addAll(getFromDirectory(new File(filePath), packageName));
                        }
                    }
                }
            }
        } catch (IOException e) {
            s_logger.debug("Encountered IOException", e);
        } catch (ClassNotFoundException e) {
            s_logger.info("[ignored] class not found", e);
        }
        return classes;
    }

    static Set<Class<?>> getFromDirectory(File directory, String packageName) throws ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        if (directory.exists()) {
            for (String file : directory.list()) {
                if (file.endsWith(".class")) {
                    String name = packageName + '.' + stripFilenameExtension(file);
                    try {
                        Class<?> clazz = Class.forName(name);
                        classes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        s_logger.info("[ignored] class not found in directory " + directory, e);
                    } catch (Exception e) {
                        s_logger.debug("Encountered unexpect exception! ", e);
                    }
                } else {
                    File f = new File(directory.getPath() + "/" + file);
                    if (f.isDirectory()) {
                        classes.addAll(getFromDirectory(f, packageName + "." + file));
                    }
                }
            }
        }
        return classes;
    }

    static Set<Class<?>> getFromJARFile(String jar, String packageName) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        try (JarInputStream jarFile = new JarInputStream(new FileInputStream(jar));) {
            JarEntry jarEntry;
            do {
                jarEntry = jarFile.getNextJarEntry();
                if (jarEntry != null) {
                    String className = jarEntry.getName();
                    if (className.endsWith(".class")) {
                        className = stripFilenameExtension(className);
                        if (className.startsWith(packageName)) {
                            try {
                                Class<?> clz = Class.forName(className.replace('/', '.'));
                                classes.add(clz);
                            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                                s_logger.warn("Unable to load class from jar file", e);
                            }
                        }
                    }
                }
            } while (jarEntry != null);
            return classes;
        }
    }

    static String stripFilenameExtension(String file) {
        return file.substring(0, file.lastIndexOf('.'));
    }
}