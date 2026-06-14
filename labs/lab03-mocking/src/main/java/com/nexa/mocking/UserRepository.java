package com.nexa.mocking;

public interface UserRepository {

    User findById(Long id);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    void deleteById(Long id);
}
