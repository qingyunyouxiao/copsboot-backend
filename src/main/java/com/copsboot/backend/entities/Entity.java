package com.copsboot.backend.entities;

public interface Entity<T extends EntityId> {
    T getId();
}