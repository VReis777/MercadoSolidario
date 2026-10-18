package com.mercadosolidario.repository;

import com.mercadosolidario.model.PublicServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PublicServiceRequestRepository extends JpaRepository<PublicServiceRequest, Long>,
        JpaSpecificationExecutor<PublicServiceRequest> {
    Optional<PublicServiceRequest> findByProtocolHash(String protocolHash);
}
