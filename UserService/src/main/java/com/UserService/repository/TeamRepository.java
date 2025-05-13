package com.UserService.repository;

import com.UserService.model.Team;
import com.UserService.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    @EntityGraph(attributePaths = {"members"})
    List<Team> findByMembersContaining(User member);
} 