package com.UserService.service;

import com.UserService.dto.CreateTeamDTO;
import com.UserService.dto.UserDto;
import com.UserService.exception.ResourceNotFoundException;
import com.UserService.model.Team;
import com.UserService.model.User;
import com.UserService.repository.TeamRepository;
import com.UserService.repository.UserRepository;
import jakarta.validation.constraints.Null;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;


    @Transactional(readOnly = true)
    public List<Team> getAllUsersTeam(Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    Set<Team> teams = new HashSet<>(user.getTeams());  // Создаем копию, чтобы избежать модификаций
                    return new ArrayList<>(teams);
                })
                .orElseGet(ArrayList::new);
    }


    @Transactional
    public void createTeam(CreateTeamDTO dto, Long creatorId) throws NullPointerException {
        Team team = new Team();
        Set<User> members = new HashSet<>();
        User manager = userRepository.findById(creatorId).orElse(null);

        if (manager == null) {
            log.info("Неправильные ID у Manager-а" + creatorId);
            throw new NullPointerException("Неправильный ID у Mananger-а: ");
        }

        members.add(manager); // Создатель также является участником

        for (String username: dto.getMembersName()) {
            User member = userRepository.findByUsername(username).orElse(null);

            if (member == null) {
                throw new IllegalArgumentException("Пользователя с username=" + username + " не существует ");
            }

            members.add(member);
        }

        if (dto.getIsCorporate() != null) {
            team.setIsCorporate(dto.getIsCorporate());
        }

        List<User> membersList = new ArrayList<>(members);

        team.setName(dto.getTitle());
        team.setDescription(dto.getDescription());
        team.setCreatedAt(LocalDateTime.now());
        team.setMembers(membersList);
        team.setLeaderId(manager.getId());
        team.setLeaderName(manager.getFirstName());
        team.setMembersCount(members.size());

        teamRepository.save(team);
        log.info("Успешно создана команда: " + team);
    }

    public User findById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }


    // Проверка, состоит ли пользователь в команде
    @Transactional
    public void checkUserAndTeam(Long userId, Long teamId) throws IllegalArgumentException {
        // Тут мы сразу ставим всем нотификации о создании новой задачи
        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new IllegalArgumentException("Такой команды не сущесвует!")
        );

        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("Такого пользователь не существует!")
        );

        if (team.getMembers().stream().noneMatch(u -> u.getId().equals(user.getId()))) {
            throw new IllegalArgumentException("Пользователь не состоит в команде!");
        }

        List<User> members = team.getMembers();
        for(User userInTeam: members) {
            Integer notifications = userInTeam.getNotifications();
            userInTeam.setNotifications(notifications + 1);
        }

        userRepository.saveAll(members);
    }

    public void reAssignAdmin(Long userId, Long nextAdmin, Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new IllegalArgumentException("Команды с указанным ID не существует!")
        );

        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("Пользователь не найден!")
        );

        User nextAdminUser = userRepository.findById(nextAdmin).orElseThrow(
                () -> new IllegalArgumentException("Пользователь, которого назначают админом, не найден!")
        );

        // Проверяем, достаточно ли у него прав для переназначения
        if (!userId.equals(team.getLeaderId())) {
            throw new IllegalArgumentException("У вас недостаточно прав!");
        }

        team.setLeaderId(nextAdmin);
        team.setLeaderName(nextAdminUser.getUsername());
        teamRepository.save(team);
    }

    public Integer getUserNotifications(Long userId) {
        return userRepository.findById(userId).orElse(null).getNotifications();
    }

    public void resetNotifications(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        assert user != null;

        user.setNotifications(0);
        userRepository.save(user);
    }
}