package com.copsboot.backend.entities;

public interface UniqueIdGenerator<T> {

    T getNextUniqueId();
}
