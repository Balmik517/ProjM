package com.pma.spring.web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.legacy.auth.LegacyAuthModule;
import com.pma.spring.web.legacy.crypto.InsecureCryptoModule;
import com.pma.spring.web.legacy.nativeaccess.LegacyNativeAccessModule;
import com.pma.spring.web.legacy.runtime.LegacyClassLoaderLeakModule;
import com.pma.spring.web.legacy.serialization.LegacySerializationRiskModule;
import com.pma.spring.web.legacy.soap.LegacySoapGateway;

@RestController
@RequestMapping("/api/legacy/risk")
public class LegacyRiskController {

    @Autowired
    private LegacyAuthModule legacyAuthModule;

    @Autowired
    private InsecureCryptoModule insecureCryptoModule;

    @Autowired
    private LegacySerializationRiskModule serializationRiskModule;

    @Autowired
    private LegacySoapGateway legacySoapGateway;

    @Autowired
    private LegacyNativeAccessModule legacyNativeAccessModule;

    @Autowired
    private LegacyClassLoaderLeakModule leakModule;

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> auth(@RequestBody Map<String, String> body) {
        String user = body.get("user");
        String password = body.get("password");

        boolean ok = legacyAuthModule.authenticate(user, password);
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("authenticated", ok);
        if (ok) {
            res.put("token", legacyAuthModule.generateToken(user));
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/crypto")
    public ResponseEntity<Map<String, String>> crypto(@RequestParam(defaultValue = "legacy") String text) {
        Map<String, String> res = new HashMap<String, String>();
        res.put("md5", insecureCryptoModule.md5(text));
        res.put("sha1", insecureCryptoModule.sha1(text));
        res.put("des", insecureCryptoModule.desEncryptEcb(text));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/deserialize")
    public ResponseEntity<Map<String, Object>> deserialize(@RequestBody Map<String, String> body) {
        String xml = body.get("xml");
        Object value = serializationRiskModule.xmlDecode(xml);
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("decodedType", value == null ? "null" : value.getClass().getName());
        res.put("decoded", value);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/soap")
    public ResponseEntity<String> soap() {
        return ResponseEntity.ok(legacySoapGateway.createDispatchClient());
    }

    @GetMapping("/native")
    public ResponseEntity<Map<String, Object>> nativeAccess(@RequestParam(defaultValue = "cmd /c echo legacy") String cmd) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("nativeLoad", legacyNativeAccessModule.loadNative());
        res.put("execExit", legacyNativeAccessModule.runOsCommand(cmd));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/leaks")
    public ResponseEntity<Map<String, Integer>> leaks() {
        Map<String, Integer> res = new HashMap<String, Integer>();
        res.put("classLoaders", leakModule.leakedLoaderCount());
        res.put("timers", leakModule.leakedTimerCount());
        return ResponseEntity.ok(res);
    }
}
