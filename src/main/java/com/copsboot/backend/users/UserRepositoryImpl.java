package com.copsboot.backend.users;

import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import com.copsboot.backend.entities.UniqueIdGenerator;

public class UserRepositoryImpl implements UserRepositoryCustom {
    private final UniqueIdGenerator<UUID> generator;

    public UserRepositoryImpl(UniqueIdGenerator<UUID> generator) {
        this.generator = generator;
    }

    @Override
    public UserId nextId() {
        return new UserId(generator.getNextUniqueId());
    }

}