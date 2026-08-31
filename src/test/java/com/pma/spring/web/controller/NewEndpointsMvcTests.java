package com.pma.spring.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pma.spring.web.dto.IntegrationRequestDto;
import com.pma.spring.web.dto.InvoiceRequest;
import com.pma.spring.web.dto.NotificationRequest;
import com.pma.spring.web.dto.PaymentRequest;
import com.pma.spring.web.dto.ProjectMemberRequest;
import com.pma.spring.web.dto.ProjectTaskRequest;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.service.BillingService;
import com.pma.spring.web.service.ProjectDomainService;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * End-to-end checks over the new HTTP surface.
 */
@MonolithTest
@AutoConfigureMockMvc
class NewEndpointsMvcTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void memberEndpointsListAndAssign() throws Exception {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        UserRegister teammate = BenchmarkTestData.newUser(userRepository, "teammate");
        ProjectRegister project = projectDomainService.createProject("MvcMembers", new Date(), null, owner.getId());

        mockMvc.perform(get("/projects/" + project.getProjectId() + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("OWNER"));

        ProjectMemberRequest request = new ProjectMemberRequest();
        request.setUserId(teammate.getId());
        request.setRole("REVIEWER");
        request.setActorId(owner.getId());

        mockMvc.perform(post("/projects/" + project.getProjectId() + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("REVIEWER"))
                .andExpect(jsonPath("$.userId").value(teammate.getId()));

        mockMvc.perform(get("/projects/" + project.getProjectId() + "/members/directory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void assigningAnUnknownUserReturnsBadRequest() throws Exception {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "mvc-bad-member");

        ProjectMemberRequest request = new ProjectMemberRequest();
        request.setUserId(999999);
        request.setRole("MEMBER");

        mockMvc.perform(post("/projects/" + project.getProjectId() + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void taskEndpointsCreateListAndSummarise() throws Exception {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("MvcTasks", new Date(), null, owner.getId());

        ProjectTaskRequest request = new ProjectTaskRequest();
        request.setAssignedTo(owner.getId());
        request.setTitle("Draft the plan");
        request.setPriority("HIGH");
        request.setEstimatedHours(12.0);
        request.setActorId(owner.getId());

        mockMvc.perform(post("/projects/" + project.getProjectId() + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Draft the plan"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(get("/projects/" + project.getProjectId() + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/projects/" + project.getProjectId() + "/tasks/effort"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedHours").value(12.0))
                .andExpect(jsonPath("$.openTasks").value(1));
    }

    @Test
    void invoiceAndPaymentEndpointsWorkTogether() throws Exception {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "mvc-billing");

        InvoiceRequest invoiceRequest = new InvoiceRequest();
        invoiceRequest.setProjectId(project.getProjectId());
        invoiceRequest.setCustomerId(customer.getId());
        invoiceRequest.setAmount(new BigDecimal("250.00"));
        invoiceRequest.setActorId(customer.getId());

        String created = mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andReturn().getResponse().getContentAsString();

        int invoiceId = objectMapper.readTree(created).get("id").asInt();

        mockMvc.perform(get("/invoices/" + invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(250.00));

        mockMvc.perform(get("/invoices").param("projectId", String.valueOf(project.getProjectId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setInvoiceId(invoiceId);
        paymentRequest.setAmount(new BigDecimal("250.00"));
        paymentRequest.setActorId(customer.getId());

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        mockMvc.perform(get("/payments").param("invoiceId", String.valueOf(invoiceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/invoices/project/" + project.getProjectId() + "/raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceCount").value(1))
                .andExpect(jsonPath("$.source").value("DatabaseHelper"));
    }

    @Test
    void unknownInvoiceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/invoices/999999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/payments/999999")).andExpect(status().isNotFound());
    }

    @Test
    void notificationEndpointsQueueAndReport() throws Exception {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "recipient");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "mvc-notify");

        NotificationRequest request = new NotificationRequest();
        request.setUserId(user.getId());
        request.setProjectId(project.getProjectId());
        request.setType("MVC_EVENT");
        request.setMessage("hello");

        mockMvc.perform(post("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MVC_EVENT"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/notifications").param("userId", String.valueOf(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/notifications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    void integrationEndpointsQueueAndReport() throws Exception {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "mvc-integration");

        IntegrationRequestDto request = new IntegrationRequestDto();
        request.setProjectId(project.getProjectId());
        request.setIntegrationType("REPORT_PUSH");
        request.setPayload("rows=10");

        mockMvc.perform(post("/integrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrationType").value("REPORT_PUSH"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/integrations").param("projectId", String.valueOf(project.getProjectId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/integrations/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    void reportingEndpointsRenderCrossDomainData() throws Exception {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("MvcReports", new Date(), null, owner.getId());
        Invoice invoice = billingService.createInvoice(project.getProjectId(), owner.getId(),
                new BigDecimal("80.00"), owner.getId());

        mockMvc.perform(get("/reports/projects/" + project.getProjectId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(project.getProjectId()))
                .andExpect(jsonPath("$.reportType").value("PROJECT_REPORT"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("projectName=MvcReports")));

        mockMvc.perform(get("/reports/projects/" + project.getProjectId() + "/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/reports/billing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvoiced").exists())
                .andExpect(jsonPath("$.outstanding").exists());

        mockMvc.perform(get("/reports/operational"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects").exists())
                .andExpect(jsonPath("$.invoices").exists())
                .andExpect(jsonPath("$.overdueInvoicesRaw").exists());

        mockMvc.perform(get("/reports/workload"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reports/changes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists());

        // The invoice above must show up in the billing report totals.
        billingService.findById(invoice.getId());
    }

    @Test
    void changeRequestEndpointsRaiseAndApprove() throws Exception {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("MvcChanges", new Date(), null, owner.getId());

        String body = "{\"projectId\":\"" + project.getProjectId() + "\",\"title\":\"Widen scope\","
                + "\"description\":\"details\",\"priority\":\"HIGH\",\"requesterId\":\"" + owner.getId() + "\"}";

        String created = mockMvc.perform(post("/change-requests")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();

        int changeRequestId = objectMapper.readTree(created).get("id").asInt();

        mockMvc.perform(post("/change-requests/" + changeRequestId + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approverId\":\"" + owner.getId() + "\",\"note\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/projects/" + project.getProjectId() + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_HOLD"));
    }

    @Test
    void operationsEndpointRunsTheWideCompletionWorkflow() throws Exception {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("MvcOps", new Date(), null, owner.getId());

        ProjectTaskRequest taskRequest = new ProjectTaskRequest();
        taskRequest.setAssignedTo(owner.getId());
        taskRequest.setTitle("Ship it");
        taskRequest.setPriority("HIGH");
        taskRequest.setEstimatedHours(4.0);
        taskRequest.setActorId(owner.getId());

        mockMvc.perform(post("/operations/projects/" + project.getProjectId() + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/operations/projects/" + project.getProjectId() + "/complete")
                .param("actorId", String.valueOf(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.closedTasks").value(1))
                .andExpect(jsonPath("$.reportSnapshotId").exists())
                .andExpect(jsonPath("$.integrationRequestId").exists());

        mockMvc.perform(get("/operations/projects/" + project.getProjectId() + "/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project").exists())
                .andExpect(jsonPath("$.billing").exists())
                .andExpect(jsonPath("$.integrations").exists());
    }

    @Test
    void existingLegacyEndpointsStillRespond() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/legacy/batch/stats"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/legacy/workflow/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/home"))
                .andExpect(status().isOk());
    }
}
