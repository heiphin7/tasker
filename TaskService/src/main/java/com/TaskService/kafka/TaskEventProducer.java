package com.TaskService.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // Топики для задач
    private static final String TASK_CREATED_TOPIC = "task-created";
    private static final String TASK_UPDATED_TOPIC = "task-updated";
    private static final String TASK_STATUS_CHANGED_TOPIC = "task-status-changed";
    private static final String TASK_ASSIGNED_TOPIC = "task-assigned";
    private static final String TASK_ASSIGNED_TO_TEAM_TOPIC = "task-assigned-to-team";
    private static final String TASK_DELETED_TOPIC = "task-deleted";

    // Топики для подзадач
    private static final String SUBTASK_CREATED_TOPIC = "subtask-created";
    private static final String SUBTASK_UPDATED_TOPIC = "subtask-updated";
    private static final String SUBTASK_COMPLETED_TOPIC = "subtask-completed";
    private static final String SUBTASK_ASSIGNED_TOPIC = "subtask-assigned";
    private static final String SUBTASK_DELETED_TOPIC = "subtask-deleted";

    // Топики для комментариев
    private static final String COMMENT_CREATED_TOPIC = "comment-created";
    private static final String COMMENT_UPDATED_TOPIC = "comment-updated";
    private static final String COMMENT_DELETED_TOPIC = "comment-deleted";

    // Топики для вложений
    private static final String ATTACHMENT_UPLOADED_TOPIC = "attachment-uploaded";
    private static final String ATTACHMENT_DELETED_TOPIC = "attachment-deleted";

    // Топик для напоминаний
    private static final String REMINDER_TOPIC = "reminder";

    /*

    // Методы для отправки событий о задачах
    public void sendTaskCreatedEvent(TaskDto taskDto) {
        log.info("Отправка события о создании задачи с ID: {}", taskDto.getId());
        kafkaTemplate.send(TASK_CREATED_TOPIC, taskDto.getId().toString(), taskDto);
    }

    public void sendTaskUpdatedEvent(TaskDto taskDto) {
        log.info("Отправка события об обновлении задачи с ID: {}", taskDto.getId());
        kafkaTemplate.send(TASK_UPDATED_TOPIC, taskDto.getId().toString(), taskDto);
    }

    public void sendTaskStatusChangedEvent(TaskDto taskDto) {
        log.info("Отправка события об изменении статуса задачи с ID: {} на {}", taskDto.getId(), taskDto.getStatus());
        kafkaTemplate.send(TASK_STATUS_CHANGED_TOPIC, taskDto.getId().toString(), taskDto);
    }

    public void sendTaskAssignedEvent(TaskDto taskDto) {
        log.info("Отправка события о назначении задачи с ID: {} исполнителю с ID: {}", taskDto.getId(), taskDto.getAssigneeId());
        kafkaTemplate.send(TASK_ASSIGNED_TOPIC, taskDto.getId().toString(), taskDto);
    }

    public void sendTaskAssignedToTeamEvent(TaskDto taskDto) {
        log.info("Отправка события о назначении задачи с ID: {} команде с ID: {}", taskDto.getId(), taskDto.getTeamId());
        kafkaTemplate.send(TASK_ASSIGNED_TO_TEAM_TOPIC, taskDto.getId().toString(), taskDto);
    }

    public void sendTaskDeletedEvent(Long taskId) {
        log.info("Отправка события об удалении задачи с ID: {}", taskId);
        kafkaTemplate.send(TASK_DELETED_TOPIC, taskId.toString(), taskId);
    }

    // Методы для отправки событий о подзадачах
    public void sendSubtaskCreatedEvent(SubtaskDto subtaskDto) {
        log.info("Отправка события о создании подзадачи с ID: {}", subtaskDto.getId());
        kafkaTemplate.send(SUBTASK_CREATED_TOPIC, subtaskDto.getId().toString(), subtaskDto);
    }

    public void sendSubtaskUpdatedEvent(SubtaskDto subtaskDto) {
        log.info("Отправка события об обновлении подзадачи с ID: {}", subtaskDto.getId());
        kafkaTemplate.send(SUBTASK_UPDATED_TOPIC, subtaskDto.getId().toString(), subtaskDto);
    }

    public void sendSubtaskCompletedEvent(SubtaskDto subtaskDto) {
        log.info("Отправка события о завершении подзадачи с ID: {}", subtaskDto.getId());
        kafkaTemplate.send(SUBTASK_COMPLETED_TOPIC, subtaskDto.getId().toString(), subtaskDto);
    }

    public void sendSubtaskAssignedEvent(SubtaskDto subtaskDto) {
        log.info("Отправка события о назначении подзадачи с ID: {} исполнителю с ID: {}", subtaskDto.getId(), subtaskDto.getAssigneeId());
        kafkaTemplate.send(SUBTASK_ASSIGNED_TOPIC, subtaskDto.getId().toString(), subtaskDto);
    }

    public void sendSubtaskDeletedEvent(Long subtaskId) {
        log.info("Отправка события об удалении подзадачи с ID: {}", subtaskId);
        kafkaTemplate.send(SUBTASK_DELETED_TOPIC, subtaskId.toString(), subtaskId);
    }

    // Методы для отправки событий о комментариях
    public void sendCommentCreatedEvent(TaskCommentDto commentDto) {
        log.info("Отправка события о создании комментария с ID: {}", commentDto.getId());
        kafkaTemplate.send(COMMENT_CREATED_TOPIC, commentDto.getId().toString(), commentDto);
    }

    public void sendCommentUpdatedEvent(TaskCommentDto commentDto) {
        log.info("Отправка события об обновлении комментария с ID: {}", commentDto.getId());
        kafkaTemplate.send(COMMENT_UPDATED_TOPIC, commentDto.getId().toString(), commentDto);
    }

    public void sendCommentDeletedEvent(Long commentId) {
        log.info("Отправка события об удалении комментария с ID: {}", commentId);
        kafkaTemplate.send(COMMENT_DELETED_TOPIC, commentId.toString(), commentId);
    }

    // Методы для отправки событий о вложениях
    public void sendAttachmentUploadedEvent(TaskAttachmentDto attachmentDto) {
        log.info("Отправка события о загрузке вложения с ID: {}", attachmentDto.getId());
        kafkaTemplate.send(ATTACHMENT_UPLOADED_TOPIC, attachmentDto.getId().toString(), attachmentDto);
    }

    public void sendAttachmentDeletedEvent(Long attachmentId) {
        log.info("Отправка события об удалении вложения с ID: {}", attachmentId);
        kafkaTemplate.send(ATTACHMENT_DELETED_TOPIC, attachmentId.toString(), attachmentId);
    }

    // Метод для отправки напоминаний
    public void sendReminderEvent(ReminderDto reminderDto) {
        log.info("Отправка напоминания с ID: {} для задачи с ID: {} пользователю с ID: {}", 
                reminderDto.getId(), reminderDto.getTaskId(), reminderDto.getUserId());
        kafkaTemplate.send(REMINDER_TOPIC, reminderDto.getId().toString(), reminderDto);
    }
     */
} 