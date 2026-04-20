package com.pma.spring.web.legacy.soap;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Legacy SOAP gateway with hardcoded endpoints and JAX-WS APIs.
 */
@Component
public class LegacySoapGateway {

    private static final Logger logger = Logger.getLogger(LegacySoapGateway.class);

    public String createDispatchClient() {
        try {
            URL wsdlUrl = new URL("http://localhost:9999/legacy/Service?wsdl");
            QName serviceQName = new QName("http://legacy.example.com/", "LegacyService");
            QName portQName = new QName("http://legacy.example.com/", "LegacyServicePort");

            Service service = Service.create(wsdlUrl, serviceQName);
            Dispatch<String> dispatch = service.createDispatch(portQName, String.class, Service.Mode.MESSAGE);

            return "dispatch-created:" + dispatch.getClass().getName();
        } catch (Exception e) {
            logger.warn("SOAP client creation failed: " + e.getMessage());
            return "soap-init-failed";
        }
    }
}
