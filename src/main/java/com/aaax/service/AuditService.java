package com.aaax.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aaax.entity.po.AuditEvent;
import com.aaax.repository.AuditEventRepository;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String action, String actor, String detail) {
        repository.save(new AuditEvent(null, action, actor, detail));
    }

    @Transactional
    public void record(String eventId, String action, String actor, String detail) {
        repository.save(new AuditEvent(eventId, action, actor, detail));
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditEvent> recent(int limit) {
        return repository.findAllByOrderByCreateDtDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
    }
}
