package com.mercadosolidario.repository;

import com.mercadosolidario.model.StaffTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffTaskRepository extends JpaRepository<StaffTask, Long> {

    @Query("SELECT t FROM StaffTask t JOIN FETCH t.assignee JOIN FETCH t.createdBy "
            + "WHERE t.assignee.id = :userId OR t.createdBy.id = :userId ORDER BY t.createdAt DESC")
    List<StaffTask> findVisibleTo(@Param("userId") Long userId);
}
