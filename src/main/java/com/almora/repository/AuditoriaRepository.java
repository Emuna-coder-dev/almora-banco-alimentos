package com.almora.repository;

import com.almora.model.auditoria.RegistroAuditoria;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditoriaRepository extends MongoRepository<RegistroAuditoria, String> {
}
