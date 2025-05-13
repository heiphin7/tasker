package com.TaskService.repository;

import com.TaskService.model.TeamTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamTaskRepository extends JpaRepository<TeamTask, Long> {
    List<TeamTask> findAllByTeamId(Long teamId);
}
