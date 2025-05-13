package com.TaskService.service.impl;

import com.TaskService.dto.CreateSubtaskDto;
import com.TaskService.dto.CreateTeamTaskDto;
import com.TaskService.model.Subtask;
import com.TaskService.model.TeamTask;
import com.TaskService.repository.SubtaskRepository;
import com.TaskService.repository.TeamTaskRepository;
import com.TaskService.service.TeamTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamTaskServiceImpl implements TeamTaskService {

    private final TeamTaskRepository teamTaskRepository;
    private final SubtaskRepository subtaskRepository;

    /* IMPORTANT:
    * Здесь мы не проверяем такие вещи как:
    * Принадлежит ли задача пользвателю или же
    * состоит ли пользователь в команде и так далее, так как это
    * проверяется выше, отдельным запросок в FrontendService!!!
    * */

    @Override
    public void createTeamTask(Long userId, Long teamId, CreateTeamTaskDto dto) {
        // Перед тем как создавать сначал проверяем есть ли команда и состоит ли в ней пользователь
        TeamTask teamTask = new TeamTask();
        teamTask.setTitle(dto.getTitle());
        teamTask.setDescription(dto.getDescription());
        teamTask.setTeamId(dto.getTeamId());
        teamTask.setCreatedAt(LocalDateTime.now());
        teamTask.setCreatorId(userId);
        teamTask.setAssigneeId(dto.getAssigneeId());
        teamTask.setDueDate(dto.getDueDate());

        teamTaskRepository.save(teamTask);
    }

    @Override
    public void delete(Long userId, Long teamId, Long teamTaskId) throws IllegalArgumentException {

        TeamTask teamTask = teamTaskRepository.findById(teamTaskId).orElseThrow(
                () -> new IllegalArgumentException("Указанной задачи не сущетствует!")
        );
        teamTaskRepository.delete(teamTask);
    }

    @Override
    public void update(Long userId, Long teamId, TeamTask updatedTask) {
        TeamTask teamTask = teamTaskRepository.findById(updatedTask.getId()).orElseThrow(
                () -> new IllegalArgumentException("Такой задачи не существует!")
        );

        // Если они совпадают по ID, то тогда при использовании save она автоматический обновляется
        teamTaskRepository.save(updatedTask);
    }

    @Override
    public void deleteTeamTask(Long teamId, Long taskId) throws IllegalArgumentException{
        TeamTask teamTaskFromDb = teamTaskRepository.findById(taskId).orElseThrow(
                () -> new IllegalArgumentException("Задачи с id=" + taskId + " не существует!")
        );

        log.info("TeamId in DB: " + teamTaskFromDb.getTeamId() + " TeamId in request: " + teamId);

        if (!teamTaskFromDb.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("Вы не состоите в данной команде!");
        }

        teamTaskRepository.delete(teamTaskFromDb);
    }

    @Override
    public void updateTeamTask(TeamTask teamTask) {
        teamTaskRepository.save(teamTask);
    }

    @Override
    public List<TeamTask> getAllTeamTasks(Long teamId) {
        return teamTaskRepository.findAllByTeamId(teamId);
    }

    @Override
    public void addTeamSubTask(CreateSubtaskDto subtask, Long userId, Long teamId, Long taskId) {
        TeamTask parentTask = teamTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Задача (taskId=" + taskId + ") не найдена."));

        if (!parentTask.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("Задача не принадлежит команде teamId=" + teamId);
        }

        Subtask subtaskToSave = new Subtask();
        subtaskToSave.setTitle(subtask.getTitle());
        subtaskToSave.setDescription(subtask.getDescription());
        subtaskToSave.setCompleted(false);
        subtaskToSave.setParentTask(parentTask);

        subtaskRepository.save(subtaskToSave);
    }


    @Override
    public void deleteTeamSubTask(Long subtaskId, Long userId, Long teamId, Long taskId) {
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("Подзадача (id=" + subtaskId + ") не найдена."));

        TeamTask parentTask = subtask.getParentTask();

        // Проверка, что родительская задача действительно существует в базе данных
        if (parentTask.getId() == null || teamTaskRepository.findById(parentTask.getId()).isEmpty()) {
            throw new IllegalArgumentException("Родительская задача не найдена.");
        }

        // Проверяем соответствие идентификаторов задачи и команды
        if (!parentTask.getId().equals(taskId)) {
            throw new IllegalArgumentException("Эта подзадача не принадлежит задаче с taskId=" + taskId);
        }

        if (!parentTask.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("Родительская задача не принадлежит команде с teamId=" + teamId);
        }

        subtask.setParentTask(null);
        parentTask.getSubtasks().remove(subtask);
        teamTaskRepository.save(parentTask);

        log.info("Подзадача с id={} успешно удалена из задачи с id={}", subtaskId, taskId);
    }

    @Override
    public void editTeamSubtask(CreateSubtaskDto updatedSubtask, Long userId, Long teamId, Long taskId) {
        Subtask subtaskFromDb = subtaskRepository.findById(updatedSubtask.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Субзадача c id=" + updatedSubtask.getId() + " не найдена."));

        TeamTask parent = teamTaskRepository.findById(taskId).orElseThrow(
                () -> new IllegalArgumentException("Родительская задача не найдена!")
        );
        if (!parent.getTeamId().equals(teamId)) {
            throw new IllegalArgumentException("Задача с id=" + taskId
                    + " не принадлежит команде (teamId=" + teamId + ")");
        }


        subtaskFromDb.setTitle(updatedSubtask.getTitle());
        subtaskFromDb.setDescription(updatedSubtask.getDescription());
        subtaskFromDb.setCompleted(updatedSubtask.isCompleted());

        subtaskRepository.save(subtaskFromDb);
    }



}
