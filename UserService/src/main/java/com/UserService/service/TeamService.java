package com.UserService.service;

import com.UserService.model.Team;
import com.UserService.model.User;
import com.UserService.repository.TeamRepository;
import com.UserService.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public Team findById(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(
                () -> new IllegalArgumentException("Команды с указанным ID не существует!")
        );
    }

    public List<User> getMembersById(Long teamId) {
        return teamRepository.findById(teamId).orElse(null).getMembers();
    }

    @Transactional
    public void deleteMemberFromTeam(Long memberId, Long teamId, Long userId) throws IllegalArgumentException {
        Team team = teamRepository.findById(teamId).orElseThrow(() ->
                new IllegalArgumentException("Указанной команды не существует!")
        );
        User user = userRepository.findById(memberId).orElseThrow(() ->
                new IllegalArgumentException("Указанного пользователя не существует!")
        );

        List<User> memberList = team.getMembers();
        boolean removed = memberList.removeIf(u -> u.getId().equals(memberId));

        if (!removed) {
            throw new IllegalArgumentException("Пользователь не найден в команде!");
        }

        team.setMembersCount(memberList.size());
        teamRepository.save(team);
    }

    @Transactional
    public void addMemberToTeam(String username, Long teamId) throws IllegalArgumentException {
        Team team = teamRepository.findById(teamId).orElseThrow(() ->
                new IllegalArgumentException("Указанной команды не существует!")
        );

        log.info("Team when adding: " + team);

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new IllegalArgumentException("Пользователя с таким Username не существует!")
        );

        // Инициализируем перед тем как получить
        Hibernate.initialize(team.getMembers());

        List<User> memberList = team.getMembers();
        if (memberList.contains(user)) {
            throw new IllegalArgumentException("Пользователь уже в команде!");
        }

        memberList.add(user);
        team.setMembersCount(memberList.size());
        teamRepository.save(team);
    }

    @Transactional
    public void quitFromTeam(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() ->
                new IllegalArgumentException("Указанной команды не существует!"));
        if (!team.getMembers().removeIf(u -> u.getId().equals(userId))) {
            throw new IllegalArgumentException("Вы не состоите в этой команде!");
        }
        team.setMembersCount(team.getMembers().size());
        teamRepository.save(team);
    }

    @Transactional
    public void leaveTeamAsAdmin(Long userId, Long teamId, Long nextAdmin) {
        Team team = teamRepository.findById(teamId).orElseThrow(() ->
                new IllegalArgumentException("Указанной команды не существует!"));

        if (!team.getLeaderId().equals(userId)) {
            throw new IllegalArgumentException("Вы не являетесь администратором команды!");
        }

        if (nextAdmin == null) {
            teamRepository.delete(team);
            return;
        }

        User newAdmin = userRepository.findById(nextAdmin).orElseThrow(() ->
                new IllegalArgumentException("Указанный новый администратор не найден!"));
        if (!team.getMembers().contains(newAdmin)) {
            throw new IllegalArgumentException("Новый администратор не является членом команды!");
        }

        List<User> userList = team.getMembers();
        User userToDelete = null;

        for (User user: userList) {
            if (user.getId().equals(userId)) {
                userToDelete = user;
            }
        }

        userList.remove(userToDelete);

        team.setLeaderId(nextAdmin);
        team.setLeaderName(newAdmin.getUsername());
        team.setMembers(userList);
        teamRepository.save(team);
    }

}
