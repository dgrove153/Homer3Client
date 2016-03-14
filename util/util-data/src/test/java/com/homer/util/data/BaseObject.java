package com.homer.util.data;

import com.homer.util.core.IBaseObject;
import org.joda.time.DateTime;

import javax.persistence.Column;

/**
 * Created by arigolub on 2/14/16.
 */
public class BaseObject implements IBaseObject {

    @Column
    private long id;
    @Column
    private DateTime createdDateUTC;
    @Column
    private DateTime updatedDateUTC;

    @Override
    public DateTime getCreatedDateUTC() {
        return createdDateUTC;
    }

    @Override
    public void setCreatedDateUTC(DateTime createdDateUTC) {
        this.createdDateUTC = createdDateUTC;
    }

    @Override
    public DateTime getUpdatedDateUTC() {
        return updatedDateUTC;
    }

    @Override
    public void setUpdatedDateUTC(DateTime updatedDateUTC) {
        this.updatedDateUTC = updatedDateUTC;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseObject that = (BaseObject) o;

        if (id != that.id) return false;
        if (!createdDateUTC.equals(that.createdDateUTC)) return false;
        return updatedDateUTC.equals(that.updatedDateUTC);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + createdDateUTC.hashCode();
        result = 31 * result + updatedDateUTC.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BaseObject{" +
                "id=" + id +
                ", createdDateUTC=" + createdDateUTC +
                ", updatedDateUTC=" + updatedDateUTC +
                '}';
    }
}
