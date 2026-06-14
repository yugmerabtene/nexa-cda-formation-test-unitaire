package com.nexa.usermanager.repository;

import com.nexa.usermanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByNomContainingIgnoreCase(String nom);
    List<User> findByActif(boolean actif);
    Page<User> findByRole(User.Role role, Pageable pageable);
    long countByRole(User.Role role);
}
