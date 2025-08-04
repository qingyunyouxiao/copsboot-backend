package com.copsboot.backend.entities;

import java.io.Serializable;

public interface EntityId<T> extends Serializable {
    T getId();
    String asString();

}
