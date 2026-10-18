package com.mercadosolidario.repository;

import com.mercadosolidario.model.PasswordSetupToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordSetupToken token where token.tokenHash = :tokenHash")
    Optional<PasswordSetupToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<PasswordSetupToken> findByUserId(Long userId);
}
