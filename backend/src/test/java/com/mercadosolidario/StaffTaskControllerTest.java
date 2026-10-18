package com.mercadosolidario;

import com.mercadosolidario.model.Role;
import com.mercadosolidario.model.User;
import com.mercadosolidario.repository.RoleRepository;
import com.mercadosolidario.repository.StaffTaskRepository;
import com.mercadosolidario.repository.UserRepository;
import com.mercadosolidario.service.InvitationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffTaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private StaffTaskRepository tasks;
    @MockBean private InvitationEmailSender emailSender;

    private User pastor;
    private User rh;
    private User diacono;

    @BeforeEach
    void setUp() {
        tasks.deleteAll();
        pastor = saveUser("pastor", "pastor@test.org", "Pr. Pastor", "ADMIN");
        rh = saveUser("rh", "rh@test.org", "Maria RH", "RH_TESOURARIA");
        diacono = saveUser("diacono", "diacono@test.org", "João Diácono", "DIACONO");
    }

    @Test
    @WithMockUser(username = "pastor", roles = "ADMIN")
    void adminAssignsTaskAndEmailGoesToAssigneeWithCreatorInCopy() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assigneeId": %d, "title": "Conferir cestas", "description": "Separar itens faltantes"}
                    """.formatted(rh.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.assigneeName").value("Maria RH"))
            .andExpect(jsonPath("$.status").value("ABERTA"))
            .andExpect(jsonPath("$.emailSent").value(true));

        verify(emailSender).sendWithCopy(
                org.mockito.ArgumentMatchers.eq("rh@test.org"),
                org.mockito.ArgumentMatchers.eq("pastor@test.org"),
                anyString(), anyString());

        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Conferir cestas"));
    }

    @Test
    @WithMockUser(username = "diacono", roles = "DIACONO")
    void diaconoCannotAssignTasksButSeesOwnTasks() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assigneeId": %d, "title": "Tarefa"}
                    """.formatted(rh.getId())))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tasks/assignable-users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void assigneeCanCompleteTask() throws Exception {
        Long taskId = createTaskAsRh();

        mockMvc.perform(patch("/api/tasks/" + taskId + "/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONCLUIDA"))
            .andExpect(jsonPath("$.completedAt").isString());
    }

    @Test
    @WithMockUser(username = "diacono", roles = "DIACONO")
    void unrelatedUserCannotCompleteTask() throws Exception {
        Long taskId = createTaskDirectly(rh, pastor);

        mockMvc.perform(patch("/api/tasks/" + taskId + "/complete"))
            .andExpect(status().isBadRequest());
    }

    private Long createTaskAsRh() {
        return createTaskDirectly(rh, rh);
    }

    private Long createTaskDirectly(User assignee, User creator) {
        var task = new com.mercadosolidario.model.StaffTask();
        task.setTitle("Tarefa de teste");
        task.setAssignee(assignee);
        task.setCreatedBy(creator);
        return tasks.save(task).getId();
    }

    private User saveUser(String username, String email, String fullName, String roleName) {
        Role role = roles.findByName(roleName).orElseGet(() -> {
            Role created = new Role();
            created.setName(roleName);
            return roles.save(created);
        });
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword("$2a$10$fakehashfakehashfakehashfakehashfakehashfakehash");
        user.setActive(true);
        user.getRoles().add(role);
        return users.save(user);
    }
}
