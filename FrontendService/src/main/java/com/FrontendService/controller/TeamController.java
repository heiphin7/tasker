package com.FrontendService.controller;

import com.FrontendService.model.Subtask;
import com.FrontendService.model.Team;
import com.FrontendService.model.TeamTask;
import com.FrontendService.model.dto.CreateTeamDTO;
import com.FrontendService.model.dto.CreateTeamTaskDto;
import com.FrontendService.model.dto.TeamInfoDto;
import com.FrontendService.service.JwtService;
import com.FrontendService.service.TaskKafkaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/teams")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    private final TaskKafkaService taskKafkaService;
    private final JwtService jwtService;

    @GetMapping
    public String getTeamsPage(Model model,
                               HttpServletRequest request) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);
        List<Team> userTeams = taskKafkaService.getAllTeamsByUser(userId);

        taskKafkaService.resetNotificationCount(userId);

        model.addAttribute("userTeams", userTeams);
        model.addAttribute("createTeamDTO", new CreateTeamDTO());
        return "teams";
    }

    @GetMapping("/{teamId}")
    public String getTeamPage(Model model, @PathVariable("teamId") Long teamId,
                              HttpServletRequest request) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);
        TeamInfoDto teamInfo = taskKafkaService.getTeamInfo(userId, teamId);
        try {
            List<TeamTask> teamTasks = taskKafkaService.getTeamTasks(teamId);
            model.addAttribute("teamTasks", teamTasks);
        } catch (Exception e) {
            return "redirect:/teams";
        }

        if (teamInfo == null) {
            return "redirect:/teams";
        }

        model.addAttribute("userId", userId);
        model.addAttribute("members", teamInfo.getMembers());
        model.addAttribute("team", teamInfo.getTeam());
        model.addAttribute("createTeamTaskDto", new CreateTeamTaskDto());
        if (teamInfo.getIsCorporate()) {
            return "team-details";
        }
        return "team-details-non-corporate";
    }

    @PostMapping("/create")
    public String createTeam(@ModelAttribute CreateTeamDTO dto,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        if (dto.getMembersName() == null || dto.getMembersName().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please add at least one member!");
            return "redirect:/teams";
        }

        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);
        String result = taskKafkaService.createTeam(dto, userId);

        if (result.startsWith("ERROR")) {
            redirectAttributes.addFlashAttribute("errorMessage", result.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Команда успешно создана!");
        }

        return "redirect:/teams";
    }

    @PostMapping("/invite/{teamId}")
    public String inviteMember(RedirectAttributes redirectAttributes,
                               HttpServletRequest request,
                               @PathVariable("teamId") Long teamId,
                               @RequestParam Long adminId,
                               @RequestParam String username) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);
        String response = taskKafkaService.addMembers(username, teamId);

        if (response.startsWith("ERROR:")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", "Вы успешно добавили нового пользователя!");
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/delete/{teamId}/{memberId}")
    public String deleteMember(RedirectAttributes redirectAttributes,
                               @PathVariable("memberId") Long memberId,
                               @PathVariable("teamId") Long teamId,
                               HttpServletRequest request) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);

        if (userId.equals(memberId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Вы не можете удалить себя же, для этого используйте \"Выйти из Команды\"");
            return "redirect:/teams/" + teamId;
        }

        String response = taskKafkaService.deleteMember(memberId, teamId, userId);

        if (response.startsWith("ERROR:") ) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", "Вы успешно удалили участника!");
        }

        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/quit/{teamId}")
    public String quitFromTeam(RedirectAttributes redirectAttributes,
                               @PathVariable("teamId") Long teamId,
                               HttpServletRequest request) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);

        String response = taskKafkaService.quitFromTeam(userId, teamId);

        if (response.startsWith("ERROR:")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", response);
        }

        return "redirect:/teams";
    }

    @PostMapping("/leave/admin/{teamId}")
    public String leaveTeamAsAdmin(RedirectAttributes redirectAttributes,
                                   @PathVariable("teamId") Long teamId,
                                   HttpServletRequest request,
                                   @RequestParam(value = "nextAdmin", required = false) Long nextAdmin) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);

        String response = taskKafkaService.leaveTeamAsAdmin(userId, teamId, nextAdmin);

        if (response.startsWith("ERROR:")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", response);
        }

        return "redirect:/teams";
    }

    @PostMapping("/create/task/{teamId}")
    public String createTeamTask(@ModelAttribute CreateTeamTaskDto dto,
                                 RedirectAttributes redirectAttributes,
                                 @PathVariable("teamId") Long teamId,
                                 HttpServletRequest request) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);

        String response = taskKafkaService.createTeamTask(userId, teamId, dto);

        if (response.startsWith("ERROR")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", "Задача успешно создана!");
        }

        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/edit/task")
    public String editTeamTask(@CookieValue("refreshToken") String token,
                               RedirectAttributes redirectAttributes,
                               @ModelAttribute TeamTask editedTask,
                               @RequestParam("teamId") Long teamId
                               ) {
        taskKafkaService.editTeamTask(editedTask, teamId);
        redirectAttributes.addFlashAttribute("message", "Задача успешно редактирована!");
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/delete/task")
    public String deleteTeamTask(@CookieValue("refreshToken") String token,
                                 RedirectAttributes redirectAttributes,
                                 @RequestParam("taskId") Long taskId,
                                 @RequestParam("teamId") Long teamId) {
        Long userId = jwtService.getUserIdFromToken(token);
        String response = taskKafkaService.deleteTeamTask(taskId, userId, teamId);

        if (response.startsWith("ERROR")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", "Задача успешно удалена!");
        }

        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/create/subtask")
    public String createSubtask(@CookieValue("refreshToken") String token,
                                RedirectAttributes redirectAttributes,
                                @RequestParam("teamId") Long teamId,
                                @RequestParam("taskId") Long taskId,
                                @ModelAttribute Subtask subtask) {
        Long userId = jwtService.getUserIdFromToken(token);
        String response = taskKafkaService.addSubtask(teamId, userId, subtask, taskId);

        if (response.startsWith("ERROR")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", "Подзадача успешно создана!");
        }

        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/delete/subtask")
    public String deleteSubtask(@RequestParam("teamId")   String teamId,
                                @RequestParam("taskId")   String taskId,
                                @RequestParam("subtaskId")String subtaskId,
                                RedirectAttributes redirectAttributes ) {
        Long teamIdLong = Long.parseLong(teamId);
        Long taskIdLong = Long.parseLong(taskId);
        Long subtaskIdLong = Long.parseLong(subtaskId);

        String response = taskKafkaService.deleteSubtask(subtaskIdLong, teamIdLong, taskIdLong);

        if (response.startsWith("ERROR")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", "Подзадача успешно удалена!");
        }

        return "redirect:/teams/" + teamId;
    }


    @PostMapping("/update/subtask")
    public String updateTask(@CookieValue("refreshToken") String token,
                             RedirectAttributes redirectAttributes,
                             @RequestParam("teamId") Long teamId,
                             @RequestParam("taskId") Long taskId,
                             @ModelAttribute Subtask updatedSubtask) {
        Long userId = jwtService.getUserIdFromToken(token);
        String response = taskKafkaService.updateSubtask(teamId, taskId, updatedSubtask, userId);

        if (response.startsWith("ERROR")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", "Подзадача успешно обновлена!");
        }
        return "redirect:/teams/" + teamId;
    }

    // Передача админства
    @PostMapping("/assign/admin")
    public String reAssignAdmin(@CookieValue("refreshToken") String token,
                                @RequestParam("teamId") Long teamId,
                                @RequestParam("newAdminId") Long newAdminId,
                                RedirectAttributes redirectAttributes) {
        Long userId = jwtService.getUserIdFromToken(token);
        String response = taskKafkaService.reAssignAdmin(userId, newAdminId, teamId);

        if (response.startsWith("ERROR")) {
            redirectAttributes.addFlashAttribute("errorMessage", response.substring(6));
        } else {
            redirectAttributes.addFlashAttribute("message", "Вы успешно переназначили админа!");
        }

        return "redirect:/teams/" + teamId ;
    }
}