package com.copsboot.backend.users;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, UserId>, UserRepositoryCustom {

    Optional<User> findByEmailIgnoreCase(String email);
}