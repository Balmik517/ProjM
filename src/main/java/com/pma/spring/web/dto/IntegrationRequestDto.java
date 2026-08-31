package com.pma.spring.web.dto;

/**
 * Request payload for queueing an outbound integration call.
 */
public class IntegrationRequestDto {

    private int projectId;
    private String integrationType;
    private String payload;

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
