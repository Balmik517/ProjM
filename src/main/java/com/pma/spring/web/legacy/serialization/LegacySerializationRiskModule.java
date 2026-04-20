package com.pma.spring.web.legacy.serialization;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Legacy serialization module with intentionally unsafe deserialization patterns.
 */
@Component
public class LegacySerializationRiskModule {

    private static final Logger logger = Logger.getLogger(LegacySerializationRiskModule.class);

    public byte[] javaSerialize(Object value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(value);
            oos.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("serialize failed", e);
        }
    }

    public Object javaDeserialize(byte[] bytes) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object obj = ois.readObject();
            ois.close();
            return obj;
        } catch (Exception e) {
            logger.error("java deserialization failed", e);
            throw new RuntimeException(e);
        }
    }

    public Object xmlDecode(String xml) {
        try {
            XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Object value = decoder.readObject();
            decoder.close();
            return value;
        } catch (Exception e) {
            logger.error("xml decoder failed", e);
            throw new RuntimeException(e);
        }
    }
}
