package com.UserService.repository;

import com.UserService.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"teams"})
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.notifications = u.notifications + 1 WHERE u.id IN (SELECT m.id FROM Team t JOIN t.members m WHERE t.id = :teamId)")
    void incrementNotificationsForTeam(@Param("teamId") Long teamId);


} 