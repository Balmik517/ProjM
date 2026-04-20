package com.pma.spring.web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.service.LegacyIntegrationBridge;

@RestController
@RequestMapping("/api/legacy/integration")
public class LegacyIntegrationController {

    @Autowired
    private LegacyIntegrationBridge legacyIntegrationBridge;

    @PostMapping("/xml")
    public ResponseEntity<Map<String, Object>> xmlRoundTrip(@RequestBody Map<String, Object> body) {
        String xml = legacyIntegrationBridge.toLegacyXml(body);
        Map<String, Object> result = legacyIntegrationBridge.fromLegacyXml(xml);
        Map<String, Object> response = new HashMap<>();
        response.put("xml", xml);
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rule")
    public ResponseEntity<Map<String, Object>> evaluate(@RequestBody Map<String, Object> body) {
        String expression = String.valueOf(body.get("expression"));
        Object out = legacyIntegrationBridge.evaluateRule(expression, body);
        Map<String, Object> response = new HashMap<>();
        response.put("output", out);
        response.put("envelope", legacyIntegrationBridge.buildJmsEnvelope("RULE_EVAL", String.valueOf(body.get("owner"))));
        return ResponseEntity.ok(response);
    }
}
