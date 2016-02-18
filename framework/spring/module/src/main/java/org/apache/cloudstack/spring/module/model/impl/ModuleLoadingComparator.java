package org.apache.cloudstack.spring.module.model.impl;

import java.util.Comparator;

public class ModuleLoadingComparator implements Comparator<String>{

    @Override
    public int compare(String o1, String o2) {
        if (o1.equals("server-naming")) {
            return -1;
        }
        if (o2.equals("server-naming")) {
            return 1;
        }
        else {
            return (o1.compareTo(o2));
        }
    }

}
