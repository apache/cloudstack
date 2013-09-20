package net.juniper.contrail.model;

import java.util.Comparator;
import java.util.TreeSet;

public abstract class ModelObjectBase implements ModelObject {
    public static class UuidComparator implements Comparator<ModelObject> {
        @Override
        public int compare(ModelObject lhs, ModelObject rhs) {
            if (lhs == null) {
                if (rhs == null) {
                    return 0;
                }
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.compareTo(rhs);
        }
    }
    private TreeSet<ModelReference> _ancestors;
    
    private TreeSet<ModelObject> _successors;

    ModelObjectBase() {
        _ancestors = new TreeSet<ModelReference>();
        _successors = new TreeSet<ModelObject>(new UuidComparator());
    }
    
    @Override
    public void addSuccessor(ModelObject child) {
        _successors.add(child);
        ModelObjectBase base = (ModelObjectBase) child;
        base._ancestors.add(new ModelReference(this));
    }
    
    @Override
    public TreeSet<ModelReference> ancestors() {
        return _ancestors;
    }
    
    private void clearAncestorReference(ModelObjectBase child) {
        ModelReference ref = null;
        for (ModelReference objref : child._ancestors) {
            if (objref.get() == this) {
                ref = objref;
                break;
            }
        }
        if (ref != null) {
            child._ancestors.remove(ref);
        }
    }
    
    @Override
    public void clearSuccessors() {
        for (ModelObject successor : _successors) {
            clearAncestorReference((ModelObjectBase) successor);
        }
        _successors.clear();
    }

    @Override
    public boolean equals(Object rhs) {
        ModelObject other;
        try {
            other = (ModelObject) rhs;
        } catch (ClassCastException ex) {
            return false;
        }
        return compareTo(other) == 0;
    }
    
    @Override
    protected void finalize() {
        clearSuccessors();
    }

    public boolean hasDescendents() {
        return !successors().isEmpty();
    }
    
    @Override
    public void removeSuccessor(ModelObject child) {
        clearAncestorReference((ModelObjectBase) child);
        _successors.remove(child);
    }

    @Override
    public TreeSet<ModelObject> successors() {
        return _successors;
    }
}
