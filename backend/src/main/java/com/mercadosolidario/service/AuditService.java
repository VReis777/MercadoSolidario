package com.mercadosolidario.service;

import com.mercadosolidario.model.AuditLog;
import com.mercadosolidario.model.User;
import com.mercadosolidario.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String entityType, Long entityId, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDetails(details);

            // Obter usuário autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                // Se o principal for um User, usá-lo diretamente
                if (authentication.getPrincipal() instanceof User) {
                    auditLog.setUser((User) authentication.getPrincipal());
                }
            }

            auditLogRepository.save(auditLog);
            log.debug("Audit log criado: action={}, entityType={}, entityId={}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Erro ao criar audit log: action={}, entityType={}, entityId={}", action, entityType, entityId, e);
            // Não lançar exceção para não interromper a operação principal
        }
    }

    @Transactional
    public void logFamilyCreated(Long familyId, String responsibleName) {
        Map<String, Object> details = new HashMap<>();
        details.put("responsibleName", responsibleName);
        log("FAMILY_CREATED", "Family", familyId, mapToJson(details));
    }

    @Transactional
    public void logFamilyUpdated(Long familyId, String responsibleName) {
        Map<String, Object> details = new HashMap<>();
        details.put("responsibleName", responsibleName);
        log("FAMILY_UPDATED", "Family", familyId, mapToJson(details));
    }

    @Transactional
    public void logFamilyStatusChanged(Long familyId, String oldStatus, String newStatus) {
        Map<String, Object> details = new HashMap<>();
        details.put("oldStatus", oldStatus);
        details.put("newStatus", newStatus);
        log("FAMILY_STATUS_CHANGED", "Family", familyId, mapToJson(details));
    }

    private String mapToJson(Map<String, Object> map) {
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.error("Erro ao converter mapa para JSON", e);
            return "{}";
        }
    }
}
