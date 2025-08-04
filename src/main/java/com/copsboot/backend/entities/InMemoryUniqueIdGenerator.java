package com.copsboot.backend.entities;

import java.util.UUID;

public class InMemoryUniqueIdGenerator implements UniqueIdGenerator<UUID> {
    
    @Override
    public UUID getNextUniqueId() {
        return UUID.randomUUID();
    }
}
