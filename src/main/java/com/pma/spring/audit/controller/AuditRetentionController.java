package com.pma.spring.audit.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.audit.dto.AuditRetentionPolicyRequest;
import com.pma.spring.audit.entity.AuditRetentionPolicy;
import com.pma.spring.audit.service.AuditRetentionService;

@RestController
@RequestMapping("/audit/retention")
public class AuditRetentionController {

    @Autowired
    private AuditRetentionService auditRetentionService;

    @GetMapping("/policies")
    public ResponseEntity<List<AuditRetentionPolicy>> policies() {
        return ResponseEntity.ok(auditRetentionService.listPolicies());
    }

    @PostMapping("/policies")
    public ResponseEntity<AuditRetentionPolicy> setPolicy(@RequestBody AuditRetentionPolicyRequest request) {
        return ResponseEntity
                .ok(auditRetentionService.setPolicy(request.getEntityType(), request.getRetentionDays()));
    }

    @PostMapping("/purge")
    public ResponseEntity<Integer> purge(@RequestParam("entityType") String entityType) {
        return ResponseEntity.ok(Integer.valueOf(auditRetentionService.purgeExpired(entityType)));
    }
}
