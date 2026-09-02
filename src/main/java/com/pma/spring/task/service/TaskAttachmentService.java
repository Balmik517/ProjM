package com.pma.spring.task.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.task.entity.TaskAttachment;
import com.pma.spring.task.repository.TaskAttachmentRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Tracks attachment metadata for a task.
 *
 * Unlike {@link TaskCommentService}, this component does not notify anyone -
 * it exists mainly to give the task domain a second table so a future
 * decomposition candidate has more than one piece of internal cohesion to
 * evaluate, not just a single-table slice.
 */
@Service
public class TaskAttachmentService {

    private static final Logger logger = Logger.getLogger(TaskAttachmentService.class);

    private static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    /** Cross-package read: attachments can only be added to a task that exists. */
    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Transactional
    public TaskAttachment attach(int taskId, String fileName, long fileSize, int uploadedBy) {
        Optional<?> task = projectTaskRepository.findById(Integer.valueOf(taskId));
        if (!task.isPresent()) {
            throw new RuntimeException("Task not found for id " + taskId);
        }
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new RuntimeException("Attachment exceeds maximum size for task " + taskId);
        }

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setFileName(LegacyUtils.truncate(StringUtils.defaultString(fileName), 200));
        attachment.setFileSize(fileSize);
        attachment.setUploadedBy(uploadedBy);
        attachment.setUploadedAt(new Date());

        TaskAttachment saved = taskAttachmentRepository.save(attachment);
        LegacyUtils.recordDomainTouch("task", "ATTACHMENT_ADDED");
        logger.info("Attachment " + fileName + " added to task " + taskId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TaskAttachment> findForTask(int taskId) {
        return taskAttachmentRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public long countForTask(int taskId) {
        return taskAttachmentRepository.countByTaskId(taskId);
    }
}
