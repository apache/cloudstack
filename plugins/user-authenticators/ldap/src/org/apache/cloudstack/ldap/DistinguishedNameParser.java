package org.apache.cloudstack.ldap;

public class DistinguishedNameParser {

    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String NAME_SEPARATOR = ",";

    public static String parseLeafName(final String distinguishedName) {
        if (distinguishedName.contains(NAME_SEPARATOR)) {
            final String[] parts = distinguishedName.split(NAME_SEPARATOR);
            return parseValue(parts[0]);
        } else {
            return parseValue(distinguishedName);
        }
    }

    private static String parseValue(final String distinguishedName) {
        if (distinguishedName.contains(KEY_VALUE_SEPARATOR)) {
            final String[] parts = distinguishedName.split(KEY_VALUE_SEPARATOR);
            if (parts.length > 1) {
                return parts[1];
            }
        }
        throw new IllegalArgumentException("Malformed distinguished name: " + distinguishedName);
    }

}
