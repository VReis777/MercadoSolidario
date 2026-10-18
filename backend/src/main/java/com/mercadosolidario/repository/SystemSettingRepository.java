package com.mercadosolidario.repository;

import com.mercadosolidario.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    @Query("SELECT s FROM SystemSetting s WHERE s.key = :key")
    Optional<SystemSetting> findByKey(@Param("key") String key);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SystemSetting s WHERE s.key = :key")
    boolean existsByKey(@Param("key") String key);
}
