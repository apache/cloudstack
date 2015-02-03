package com.cloud.hypervisor.kvm.storage;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cloud.storage.Storage.StoragePoolType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ TYPE })
public @interface StorageAdaptorInfo {
    StoragePoolType storagePoolType();
}
