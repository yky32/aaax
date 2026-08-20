package com.aaax.audit;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String action, String actor, String detail) {
        repository.save(new AuditEvent(action, actor, detail));
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditEvent> recent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
    }
}
