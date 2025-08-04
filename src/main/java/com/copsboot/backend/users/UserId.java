package com.copsboot.backend.users;

import com.copsboot.backend.entities.AbstractEntityId;
import java.util.UUID;

public class UserId extends AbstractEntityId<UUID> {
   
    protected UserId(){
    
    }

    public UserId (UUID id) {
        super(id);
    }
}

