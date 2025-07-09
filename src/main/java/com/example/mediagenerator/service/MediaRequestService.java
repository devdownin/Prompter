package com.example.mediagenerator.service;

import com.example.mediagenerator.dto.MediaRequestDto;
import com.example.mediagenerator.model.MediaRequest;
import com.example.mediagenerator.model.RequestStatus;
import com.example.mediagenerator.repository.MediaRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random; // Pour la simulation

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaRequestService {

    private final MediaRequestRepository mediaRequestRepository;
    private final Random random = new Random(); // Pour simuler le succès/échec

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
    public void updateRequestStatus(Long id, RequestStatus status, String errorMessage, String generatedPath) {
        log.info("Updating status for request id {}: {} - Error: {} - Path: {}", id, status, errorMessage, generatedPath);
        mediaRequestRepository.findById(id).ifPresent(request -> {
            request.setStatus(status);
            request.setErrorMessage(errorMessage);
            request.setGeneratedMediaPath(generatedPath);
            // statusUpdateDate sera mis à jour automatiquement
            mediaRequestRepository.save(request);
        });
    }

    // Méthode pour simuler le traitement des tâches.
    // Dans une vraie application, cela impliquerait des appels à des IA, etc.
    // @Scheduled(fixedDelay = 10000) // Exécute toutes les 10 secondes
    public void processPendingRequests() {
        log.info("Checking for pending requests to process...");
        List<MediaRequest> pendingRequests = mediaRequestRepository.findByStatus(RequestStatus.GO);

        if (pendingRequests.isEmpty()) {
            log.info("No pending requests with status GO found.");
            return;
        }

        for (MediaRequest request : pendingRequests) {
            log.info("Processing request ID: {}", request.getId());
            // 1. Mettre à jour le statut à RUNNING
            updateRequestStatus(request.getId(), RequestStatus.RUNNING, null, null);

            // 2. Simuler le traitement (appel IA, génération média)
            try {
                // Simuler une durée de traitement
                Thread.sleep(5000 + random.nextInt(10000)); // Entre 5 et 15 secondes

                // Simuler succès ou échec
                if (random.nextBoolean()) {
                    log.info("Request ID: {} processed successfully.", request.getId());
                    updateRequestStatus(request.getId(), RequestStatus.SUCCESS, null, "/simulated/output/media_" + request.getId() + ".mp4");
                } else {
                    log.warn("Request ID: {} failed to process.", request.getId());
                    updateRequestStatus(request.getId(), RequestStatus.FAIL, "Simulated IA processing error.", null);
                }
            } catch (InterruptedException e) {
                log.error("Processing interrupted for request ID: {}", request.getId(), e);
                Thread.currentThread().interrupt(); // Rétablir le statut d'interruption
                updateRequestStatus(request.getId(), RequestStatus.FAIL, "Processing was interrupted.", null);
            } catch (Exception e) {
                log.error("Unexpected error during processing for request ID: {}", request.getId(), e);
                updateRequestStatus(request.getId(), RequestStatus.FAIL, "Unexpected error: " + e.getMessage(), null);
            }
        }
    }
}
