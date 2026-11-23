package com.clicklease.app.web;

import com.clicklease.app.bureau.BureauCallRegistry;
import com.clicklease.app.domain.ApplicationStatus;
import com.clicklease.app.domain.LeaseApplication;
import com.clicklease.app.service.ApplicationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class ApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

    private final ApplicationService applicationService;
    private final BureauCallRegistry bureauCallRegistry;

    public ApplicationController(ApplicationService applicationService,
                                 BureauCallRegistry bureauCallRegistry) {
        this.applicationService = applicationService;
        this.bureauCallRegistry = bureauCallRegistry;
    }

    @GetMapping("/")
    public String showForm(Model model) {
        if (!model.containsAttribute("applicationForm")) {
            ApplicationForm form = new ApplicationForm();
            form.setIdempotencyKey(UUID.randomUUID().toString());
            model.addAttribute("applicationForm", form);
        }
        return "apply";
    }

    @PostMapping("/applications")
    public String submitApplication(@Valid @ModelAttribute ApplicationForm applicationForm,
                                    BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            logger.info("DEBUG MSG: Form validation failed with {} error(s)",
                    bindingResult.getErrorCount());
            if (applicationForm.getIdempotencyKey() == null
                    || applicationForm.getIdempotencyKey().isBlank()) {
                applicationForm.setIdempotencyKey(UUID.randomUUID().toString());
            }
            return "apply";
        }
        LeaseApplication application = applicationService.submit(applicationForm);
        logger.info("DEBUG MSG: Submission handled -> application id={} status={}",
                application.getId(), application.getStatus());
        return "redirect:/applications/" + application.getId() + "?submitted";
    }

    @GetMapping("/applications")
    public String listApplications(@RequestParam(required = false) ApplicationStatus status,
                                   Model model) {
        model.addAttribute("applications", applicationService.listApplications(status));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("bureauStats", bureauCallRegistry);
        return "applications";
    }

    @GetMapping("/applications/{id}")
    public String applicationDetail(@PathVariable Long id,
                                    @RequestParam(required = false) String submitted,
                                    Model model) {
        model.addAttribute("app", applicationService.getById(id));
        model.addAttribute("justSubmitted", submitted != null);
        return "application-detail";
    }

    @PostMapping("/applications/{id}/retry")
    public String retryDecision(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LeaseApplication application = applicationService.retryDecision(id);
        logger.info("DEBUG MSG: Retry complete for application id={} -> status={}",
                id, application.getStatus());
        return "redirect:/applications/" + id;
    }
}
