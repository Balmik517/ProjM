package com.pma.spring.web.util;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

/**
 * Intentionally risky native-access and JAXB patterns for analysis.
 */
public class NativeAndJaxbRiskBridge {

    private static final Logger logger = Logger.getLogger(NativeAndJaxbRiskBridge.class);

    // JNI declaration kept intentionally for native access analysis.
    public native int encryptNative(byte[] data, String key);

    public void loadLegacyNativeLibraries() {
        try {
            // Platform-specific and unsafe from portability perspective.
            System.loadLibrary("legacycrypto");
        } catch (UnsatisfiedLinkError e) {
            logger.warn("Native library loadLibrary failed: " + e.getMessage());
        }

        try {
            // Hard-coded absolute path style native loading.
            System.load("C:/legacy/libs/legacycrypto.dll");
        } catch (UnsatisfiedLinkError e) {
            logger.warn("Native library load failed: " + e.getMessage());
        }
    }

    public int executeNativeCommand(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            return p.waitFor();
        } catch (Exception e) {
            logger.error("Command execution failed", e);
            return -1;
        }
    }

    public LegacyPayload unmarshalXmlUnsafe(String xml) {
        try {
            JAXBContext context = JAXBContext.newInstance(LegacyPayload.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            // Intentionally unmarshalling untrusted XML directly.
            return (LegacyPayload) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception e) {
            throw new RuntimeException("JAXB unmarshal failed", e);
        }
    }

    @XmlRootElement(name = "payload")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LegacyPayload {

        @XmlElement
        private String name;

        @XmlElement
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
