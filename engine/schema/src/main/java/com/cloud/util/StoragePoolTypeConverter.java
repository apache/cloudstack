package com.cloud.util;

import com.cloud.storage.Storage.StoragePoolType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Converts {@link StoragePoolType} to and from {@link String} using {@link StoragePoolType#name()}.
 *
 * @author mprokopchuk
 */
@Converter
public class StoragePoolTypeConverter implements AttributeConverter<StoragePoolType, String> {
    @Override
    public String convertToDatabaseColumn(StoragePoolType attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public StoragePoolType convertToEntityAttribute(String dbData) {
        return dbData != null ? StoragePoolType.valueOf(dbData) : null;
    }
}
