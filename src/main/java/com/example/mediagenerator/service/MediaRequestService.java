package com.example.mediagenerator.service;

import com.example.mediagenerator.dto.MediaRequestDto;
import com.example.mediagenerator.model.MediaRequest;
import com.example.mediagenerator.model.RequestStatus;
import com.example.mediagenerator.repository.MediaRequestRepository;
// import lombok.RequiredArgsConstructor; // Remplacé par @Autowired pour le constructeur
// import lombok.extern.slf4j.Slf4j; // Removing Lombok
import org.slf4j.Logger; // Manual SLF4J
import org.slf4j.LoggerFactory; // Manual SLF4J
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random; // Pour la simulation

@Service
// @Slf4j // Removing Lombok
public class MediaRequestService {

    private static final Logger log = LoggerFactory.getLogger(MediaRequestService.class); // Manual logger

    private final MediaRequestRepository mediaRequestRepository;
    private final GeminiService geminiService; // Injection du nouveau service
    private final Random random = new Random(); // Conservé pour la simulation de processPendingMediaRequests

    @Autowired
    public MediaRequestService(MediaRequestRepository mediaRequestRepository, GeminiService geminiService) {
        this.mediaRequestRepository = mediaRequestRepository;
        this.geminiService = geminiService;
    }

    @Transactional
    public MediaRequest submitNewRequest(MediaRequestDto dto) {
        log.info("Submitting new media request for scenario: {}", dto.getScenario().substring(0, Math.min(dto.getScenario().length(), 50)) + "...");
        MediaRequest mediaRequest = new MediaRequest(
                dto.getScenario(),
                dto.getSelectedIAs(),
                dto.getMediaType(),
                dto.getTargetPlatform()
        );
        // status, creationDate et statusUpdateDate sont gérés automatiquement ou dans le constructeur
        return mediaRequestRepository.save(mediaRequest);
    }

    @Transactional(readOnly = true)
    public List<MediaRequest> getAllRequests() {
        log.debug("Fetching all media requests");
        return mediaRequestRepository.findAllByOrderByCreationDateDesc(); // Trie par date de création la plus récente
    }

    @Transactional(readOnly = true)
    public Optional<MediaRequest> getRequestById(Long id) {
        log.debug("Fetching media request with id: {}", id);
        return mediaRequestRepository.findById(id);
    }

    @Transactional
    public Optional<MediaRequest> setRequestStatusToGo(Long id) {
        log.info("Setting request status to GO for id: {}", id);
        Optional<MediaRequest> requestOptional = mediaRequestRepository.findById(id);
        if (requestOptional.isPresent()) {
            MediaRequest request = requestOptional.get();
            if (request.getStatus() == RequestStatus.NOT_YET) {
                request.setStatus(RequestStatus.GO);
                // statusUpdateDate sera mis à jour automatiquement par @UpdateTimestamp
                return Optional.of(mediaRequestRepository.save(request));
            } else {
                log.warn("Request {} is not in NOT_YET state, cannot set to GO. Current state: {}", id, request.getStatus());
                return Optional.empty(); // Ou lever une exception
            }
        }
        log.warn("Request with id {} not found, cannot set to GO.", id);
        return Optional.empty();
    }

    @Transactional
    public void updateRequestStatus(Long id, RequestStatus status, String errorMessage, String generatedPath, String formattedPrompt) {
        log.info("Updating status for request id {}: Status={}, Error='{}', Path='{}', Prompt='{}'", id, status, errorMessage, generatedPath, formattedPrompt != null ? formattedPrompt.substring(0, Math.min(formattedPrompt.length(), 30))+"..." : "null");
        mediaRequestRepository.findById(id).ifPresent(request -> {
            request.setStatus(status);
            if (errorMessage != null) request.setErrorMessage(errorMessage);
            if (generatedPath != null) request.setGeneratedMediaPath(generatedPath);
            if (formattedPrompt != null) request.setFormattedPrompt(formattedPrompt);
            // statusUpdateDate sera mis à jour automatiquement
            mediaRequestRepository.save(request);
        });
    }

    @Transactional
    public Optional<MediaRequest> formatRequestToPrompt(Long id) {
        log.info("Attempting to format prompt for request ID: {}", id);
        Optional<MediaRequest> requestOptional = mediaRequestRepository.findById(id);

        if (requestOptional.isEmpty()) {
            log.warn("Request ID {} not found for prompt formatting.", id);
            return Optional.empty();
        }

        MediaRequest request = requestOptional.get();

        // Vérifier si le statut permet cette action
        if (!(request.getStatus() == RequestStatus.NOT_YET || request.getStatus() == RequestStatus.GO || request.getStatus() == RequestStatus.PROMPT_GENERATED)) {
            log.warn("Request ID {} is in status {} and cannot be formatted into a prompt at this stage.", id, request.getStatus());
            return Optional.empty(); // Ou lever une exception
        }

        // Mettre à jour le statut à FORMATTING_PROMPT
        request.setStatus(RequestStatus.FORMATTING_PROMPT);
        request.setErrorMessage(null); // Clear previous errors if any
        // Sauvegarde initiale du statut FORMATTING_PROMPT
        MediaRequest inProgressRequest = mediaRequestRepository.save(request);
        log.info("Request ID {} status set to FORMATTING_PROMPT.", id);

        try {
            // Appel au GeminiService
            String formattedPromptResult = geminiService.generateFormattedPrompt(
                    inProgressRequest.getScenario(),
                    inProgressRequest.getMediaType()
            ).block(java.time.Duration.ofSeconds(60)); // Bloquer pour 60 secondes max, ajuster si nécessaire

            if (formattedPromptResult != null && !formattedPromptResult.startsWith("Erreur")) {
                log.info("Prompt formatting successful for request ID: {}. Received prompt starting with: {}", id, formattedPromptResult.substring(0, Math.min(formattedPromptResult.length(), 70))+"...");
                inProgressRequest.setFormattedPrompt(formattedPromptResult);
                inProgressRequest.setStatus(RequestStatus.PROMPT_GENERATED);
                inProgressRequest.setErrorMessage(null); // Effacer les erreurs précédentes
            } else {
                log.warn("Prompt formatting failed for request ID: {}. Response from GeminiService: {}", id, formattedPromptResult);
                inProgressRequest.setStatus(RequestStatus.FAIL);
                inProgressRequest.setErrorMessage(formattedPromptResult != null ? formattedPromptResult : "Échec de la génération du prompt par le service Gemini.");
            }
            return Optional.of(mediaRequestRepository.save(inProgressRequest));

        } catch (Exception e) { // Cela inclut les exceptions si .block() timeout ou autres erreurs de l'appel réactif
            log.error("Error during prompt formatting call to Gemini service for request ID: {}", id, e);
            // Assurer que la requête est rechargée pour éviter des problèmes d'état détaché si l'exception vient de .block()
            MediaRequest requestToFail = mediaRequestRepository.findById(id).orElse(inProgressRequest);
            requestToFail.setStatus(RequestStatus.FAIL);
            requestToFail.setErrorMessage("Erreur lors de la communication avec le service Gemini pour le formatage du prompt: " + e.getMessage());
            return Optional.of(mediaRequestRepository.save(requestToFail));
        }
    }


    // Méthode pour simuler le traitement des tâches de génération de média.
    // @Scheduled(fixedDelay = 10000) // Exécute toutes les 10 secondes
    public void processPendingMediaRequests() {
        log.info("Checking for pending media generation requests (status GO)...");
        List<MediaRequest> pendingRequests = mediaRequestRepository.findByStatus(RequestStatus.GO);

        if (pendingRequests.isEmpty()) {
            log.info("No pending requests with status GO found.");
            return;
        }

        for (MediaRequest request : pendingRequests) {
            log.info("Processing request ID: {}", request.getId());
            // 1. Mettre à jour le statut à RUNNING
            updateRequestStatus(request.getId(), RequestStatus.RUNNING, null, null, request.getFormattedPrompt());

            // 2. Simuler le traitement (appel IA, génération média)
            try {
                // Simuler une durée de traitement
                Thread.sleep(5000 + random.nextInt(10000)); // Entre 5 et 15 secondes

                // Simuler succès ou échec
                if (random.nextBoolean()) {
                    log.info("Request ID: {} processed successfully.", request.getId());
                    updateRequestStatus(request.getId(), RequestStatus.SUCCESS, null, "/simulated/output/media_" + request.getId() + ".mp4", request.getFormattedPrompt());
                } else {
                    log.warn("Request ID: {} failed to process.", request.getId());
                    updateRequestStatus(request.getId(), RequestStatus.FAIL, "Simulated IA processing error.", null, request.getFormattedPrompt());
                }
            } catch (InterruptedException e) {
                log.error("Processing interrupted for request ID: {}", request.getId(), e);
                Thread.currentThread().interrupt(); // Rétablir le statut d'interruption
                updateRequestStatus(request.getId(), RequestStatus.FAIL, "Processing was interrupted.", null, request.getFormattedPrompt());
            } catch (Exception e) {
                log.error("Unexpected error during processing for request ID: {}", request.getId(), e);
                updateRequestStatus(request.getId(), RequestStatus.FAIL, "Unexpected error: " + e.getMessage(), null, request.getFormattedPrompt());
            }
        }
    }
}
