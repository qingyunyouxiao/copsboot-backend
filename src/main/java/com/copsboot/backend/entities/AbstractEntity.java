package com.copsboot.backend.entities;

import java.util.Objects;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;
import com.google.common.base.MoreObjects;


@MappedSuperclass
public abstract class AbstractEntity<T extends EntityId> implements Entity<T> {
    
    @EmbeddedId
    private T id;

    protected AbstractEntity() {

    }

    protected AbstractEntity(T id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public T getId() {
        return id;
    }   

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (this == o) {
            result = true;
        } else if (o instanceof AbstractEntityId) {
            AbstractEntityId other = (AbstractEntityId) o;
            result = Objects.equals(id, other.id);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .toString();
    }

}