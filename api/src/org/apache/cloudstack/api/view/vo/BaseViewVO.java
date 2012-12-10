package org.apache.cloudstack.api.view.vo;

import javax.persistence.Column;

/**
 * Base class for all VO objects for db views.
 * @author minc
 *
 */
public abstract class BaseViewVO {

    @Column(name="id", updatable=false, nullable = false)
    protected long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BaseViewVO other = (BaseViewVO) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
