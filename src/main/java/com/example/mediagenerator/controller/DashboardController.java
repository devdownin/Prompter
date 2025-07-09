package com.example.mediagenerator.controller;

import com.example.mediagenerator.dto.MediaRequestDto;
import com.example.mediagenerator.model.MediaRequest;
import com.example.mediagenerator.model.MediaType;
import com.example.mediagenerator.model.TargetPlatform;
import com.example.mediagenerator.service.MediaRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final MediaRequestService mediaRequestService;

    @GetMapping("/")
    public String showDashboard(Model model) {
        log.info("Accessing dashboard");
        List<MediaRequest> requests = mediaRequestService.getAllRequests();
        model.addAttribute("requests", requests);
        model.addAttribute("newRequestDto", new MediaRequestDto()); // Pour le formulaire
        model.addAttribute("mediaTypes", MediaType.values());
        model.addAttribute("targetPlatforms", TargetPlatform.values());
        // Ajouter ici d'autres attributs si nécessaire pour le tri, la pagination etc.
        return "dashboard"; // Nom de la vue Thymeleaf
    }

    @PostMapping("/request/submit")
    public String handleSubmitNewRequest(@ModelAttribute("newRequestDto") MediaRequestDto dto, RedirectAttributes redirectAttributes) {
        log.info("Submitting new request: {}", dto);
        try {
            MediaRequest savedRequest = mediaRequestService.submitNewRequest(dto);
            log.info("New request submitted successfully with ID: {}", savedRequest.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Nouvelle demande soumise avec succès ! ID: " + savedRequest.getId());
        } catch (Exception e) {
            log.error("Error submitting new request", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la soumission de la demande : " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/request/{id}/go")
    public String setRequestStatusToGo(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.info("Attempting to set request status to GO for ID: {}", id);
        mediaRequestService.setRequestStatusToGo(id)
            .ifPresentOrElse(
                request -> {
                    log.info("Request ID {} status set to GO.", id);
                    redirectAttributes.addFlashAttribute("successMessage", "Demande ID " + id + " passée à GO !");
                },
                () -> {
                    log.warn("Failed to set request ID {} to GO. It might not exist or not be in NOT_YET state.", id);
                    redirectAttributes.addFlashAttribute("errorMessage", "Impossible de passer la demande ID " + id + " à GO. Vérifiez son statut ou son existence.");
                }
            );
        return "redirect:/";
    }

    // Endpoint pour déclencher manuellement le traitement (pour démo/test)
    @PostMapping("/requests/process")
    public String triggerProcessing(RedirectAttributes redirectAttributes) {
        log.info("Manually triggering processing of pending media requests.");
        try {
            mediaRequestService.processPendingMediaRequests(); // Nom de méthode mis à jour
            redirectAttributes.addFlashAttribute("successMessage", "Traitement des demandes de média en attente déclenché.");
        } catch (Exception e) {
            log.error("Error during manual media processing trigger", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors du déclenchement manuel du traitement des médias : " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/request/{id}/format-prompt")
    public String formatRequestToPrompt(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.info("Attempting to format prompt for request ID: {}", id);
        try {
            mediaRequestService.formatRequestToPrompt(id)
                .ifPresentOrElse(
                    request -> {
                        if (request.getStatus() == com.example.mediagenerator.model.RequestStatus.PROMPT_GENERATED) {
                            log.info("Prompt formatting successful for request ID {}.", id);
                            redirectAttributes.addFlashAttribute("successMessage", "Prompt formaté avec succès pour la demande ID " + id + ".");
                        } else if (request.getStatus() == com.example.mediagenerator.model.RequestStatus.FAIL) {
                             log.warn("Prompt formatting failed for request ID {}. Error: {}", id, request.getErrorMessage());
                            redirectAttributes.addFlashAttribute("errorMessage", "Échec du formatage du prompt pour la demande ID " + id + ": " + request.getErrorMessage());
                        } else {
                            // Cas où le service retourne une entité mais le statut n'est ni PROMPT_GENERATED ni FAIL
                            // (par exemple, si la demande n'était pas dans un statut éligible au départ)
                            log.warn("Prompt formatting for request ID {} did not result in PROMPT_GENERATED or FAIL. Current status: {}", id, request.getStatus());
                            redirectAttributes.addFlashAttribute("infoMessage", "L'action de formatage du prompt pour la demande ID " + id + " n'a pas pu être complétée comme prévu (statut actuel: " + request.getStatus() + ").");
                        }
                    },
                    () -> {
                        // Cas où le service retourne Optional.empty()
                        log.warn("Failed to process prompt formatting for request ID {}. It might not exist or not be in an eligible state.", id);
                        redirectAttributes.addFlashAttribute("errorMessage", "Impossible de formater le prompt pour la demande ID " + id + ". Vérifiez son statut ou son existence.");
                    }
                );
        } catch (Exception e) {
            log.error("Error during prompt formatting for request ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur serveur lors du formatage du prompt pour la demande ID " + id + ": " + e.getMessage());
        }
        return "redirect:/";
    }
}
