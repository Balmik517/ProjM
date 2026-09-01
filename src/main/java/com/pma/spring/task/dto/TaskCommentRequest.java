package com.pma.spring.task.dto;

/**
 * Request payload for adding a comment to a task.
 */
public class TaskCommentRequest {

    private int authorId;
    private String comment;

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
