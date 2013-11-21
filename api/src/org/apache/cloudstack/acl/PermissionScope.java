package org.apache.cloudstack.acl;

public enum PermissionScope {
    RESOURCE(0),
    ACCOUNT(1),
    DOMAIN(2),
    REGION(3);

    private int _scale;

    private PermissionScope(int scale) {
        _scale = scale;
    }

    public int getScale() {
        return _scale;
    }

    public boolean greaterThan(PermissionScope s) {
        if (_scale > s.getScale())
            return true;
        else
            return false;
    }
}
