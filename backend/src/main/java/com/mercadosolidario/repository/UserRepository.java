package com.mercadosolidario.repository;

import com.mercadosolidario.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean findByActiveTrue();

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.active = true AND r.name IN :roleNames")
    List<User> findActiveByRoleNames(@Param("roleNames") Collection<String> roleNames);
}
