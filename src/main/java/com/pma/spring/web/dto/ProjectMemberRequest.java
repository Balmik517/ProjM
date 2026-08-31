package com.pma.spring.web.dto;

/**
 * Request payload for adding or updating a project membership.
 */
public class ProjectMemberRequest {

    private int userId;
    private String role;
    private int actorId;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getActorId() {
        return actorId;
    }

    public void setActorId(int actorId) {
        this.actorId = actorId;
    }
}
