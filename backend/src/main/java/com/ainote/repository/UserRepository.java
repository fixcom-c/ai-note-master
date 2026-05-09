package com.ainote.repository;

import com.ainote.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndIsDeletedFalse(String username);
    boolean existsByIdAndUsernameAndIsDeletedFalse(Long id, String username);
    boolean existsByUsername(String username);
}
