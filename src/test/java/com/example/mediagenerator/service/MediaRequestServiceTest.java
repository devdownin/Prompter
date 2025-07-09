package com.example.mediagenerator.service;

import com.example.mediagenerator.dto.MediaRequestDto;
import com.example.mediagenerator.model.MediaRequest;
import com.example.mediagenerator.model.MediaType;
import com.example.mediagenerator.model.RequestStatus;
import com.example.mediagenerator.model.TargetPlatform;
import com.example.mediagenerator.repository.MediaRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaRequestServiceTest {

    @Mock
    private MediaRequestRepository mediaRequestRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private MediaRequestService mediaRequestService;

    @Captor
    private ArgumentCaptor<MediaRequest> mediaRequestCaptor;

    private MediaRequestDto sampleDto;
    private MediaRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleDto = new MediaRequestDto();
        sampleDto.setScenario("Test Scenario");
        sampleDto.setSelectedIAs("TestIA");
        sampleDto.setMediaType(MediaType.VIDEO);
        sampleDto.setTargetPlatform(TargetPlatform.TIKTOK);

        sampleRequest = new MediaRequest(
            "Test Scenario",
            "TestIA",
            MediaType.VIDEO,
            TargetPlatform.TIKTOK
        );
        sampleRequest.setId(1L);
        sampleRequest.setCreationDate(LocalDateTime.now().minusDays(1));
        sampleRequest.setStatusUpdateDate(LocalDateTime.now());
    }

    @Test
    void submitNewRequest_shouldSaveAndReturnRequest() {
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenAnswer(invocation -> {
            MediaRequest reqToSave = invocation.getArgument(0);
            reqToSave.setId(1L);
            reqToSave.setCreationDate(LocalDateTime.now());
            reqToSave.setStatusUpdateDate(LocalDateTime.now());
            return reqToSave;
        });

        MediaRequest result = mediaRequestService.submitNewRequest(sampleDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Scenario", result.getScenario());
        assertEquals(RequestStatus.NOT_YET, result.getStatus());
        verify(mediaRequestRepository, times(1)).save(any(MediaRequest.class));
    }

    @Test
    void setRequestStatusToGo_whenRequestExistsAndStatusIsNutYet_shouldUpdateStatus() {
        sampleRequest.setStatus(RequestStatus.NOT_YET);
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenReturn(sampleRequest);

        Optional<MediaRequest> result = mediaRequestService.setRequestStatusToGo(1L);

        assertTrue(result.isPresent());
        assertEquals(RequestStatus.GO, result.get().getStatus());
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, times(1)).save(sampleRequest);
    }

    @Test
    void setRequestStatusToGo_whenRequestNotFound_shouldReturnEmpty() {
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<MediaRequest> result = mediaRequestService.setRequestStatusToGo(1L);
        assertFalse(result.isPresent());
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, never()).save(any(MediaRequest.class));
    }

    @Test
    void setRequestStatusToGo_whenRequestNotInNotYetStatus_shouldReturnEmpty() {
        sampleRequest.setStatus(RequestStatus.RUNNING);
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        Optional<MediaRequest> result = mediaRequestService.setRequestStatusToGo(1L);
        assertFalse(result.isPresent());
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, never()).save(any(MediaRequest.class));
    }

    @Test
    void updateRequestStatus_shouldUpdateFieldsAndSave() {
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenReturn(sampleRequest);

        String errorMessage = "Test Error";
        String mediaPath = "/path/to/media.mp4";
        String formattedPrompt = "Test Prompt";

        mediaRequestService.updateRequestStatus(1L, RequestStatus.FAIL, errorMessage, mediaPath, formattedPrompt);

        assertEquals(RequestStatus.FAIL, sampleRequest.getStatus());
        assertEquals(errorMessage, sampleRequest.getErrorMessage());
        assertEquals(mediaPath, sampleRequest.getGeneratedMediaPath());
        assertEquals(formattedPrompt, sampleRequest.getFormattedPrompt());


        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, times(1)).save(sampleRequest);
    }

    @Test
    void formatRequestToPrompt_whenRequestExistsAndEligible_andGeminiSucceeds_shouldSetPromptGenerated() {
        sampleRequest.setStatus(RequestStatus.NOT_YET);
        String mockPromptContent = "Mocked Gemini prompt content.";
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenAnswer(invocation -> {
            MediaRequest savedReq = invocation.getArgument(0);
            // Return a new instance to simulate behavior of a real save returning a new or detached entity
            MediaRequest returnedReq = new MediaRequest(savedReq.getScenario(), savedReq.getSelectedIAs(), savedReq.getMediaType(), savedReq.getTargetPlatform());
            returnedReq.setId(savedReq.getId());
            returnedReq.setStatus(savedReq.getStatus());
            returnedReq.setFormattedPrompt(savedReq.getFormattedPrompt());
            returnedReq.setErrorMessage(savedReq.getErrorMessage());
            returnedReq.setCreationDate(savedReq.getCreationDate());
            returnedReq.setStatusUpdateDate(LocalDateTime.now());
            return returnedReq;
        });
        when(geminiService.generateFormattedPrompt(sampleRequest.getScenario(), sampleRequest.getMediaType()))
                .thenReturn(Mono.just(mockPromptContent));

        Optional<MediaRequest> result = mediaRequestService.formatRequestToPrompt(1L);

        assertTrue(result.isPresent());
        MediaRequest updatedRequest = result.get();
        assertEquals(RequestStatus.PROMPT_GENERATED, updatedRequest.getStatus());
        assertEquals(mockPromptContent, updatedRequest.getFormattedPrompt());
        assertNull(updatedRequest.getErrorMessage());

        verify(mediaRequestRepository, times(2)).save(mediaRequestCaptor.capture());
        List<MediaRequest> savedRequests = mediaRequestCaptor.getAllValues();
        assertEquals(RequestStatus.FORMATTING_PROMPT, savedRequests.get(0).getStatus());
        assertEquals(RequestStatus.PROMPT_GENERATED, savedRequests.get(1).getStatus());

        verify(geminiService, times(1)).generateFormattedPrompt(sampleRequest.getScenario(), sampleRequest.getMediaType());
    }

    @Test
    void formatRequestToPrompt_whenRequestExistsAndEligible_andGeminiFails_shouldSetStatusToFail() {
        sampleRequest.setStatus(RequestStatus.NOT_YET);
        String errorMessageFromGemini = "Erreur: Gemini API error.";
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenAnswer(invocation -> {
            MediaRequest savedReq = invocation.getArgument(0);
            MediaRequest returnedReq = new MediaRequest(savedReq.getScenario(), savedReq.getSelectedIAs(), savedReq.getMediaType(), savedReq.getTargetPlatform());
            returnedReq.setId(savedReq.getId());
            returnedReq.setStatus(savedReq.getStatus());
            returnedReq.setFormattedPrompt(savedReq.getFormattedPrompt());
            returnedReq.setErrorMessage(savedReq.getErrorMessage());
            returnedReq.setCreationDate(savedReq.getCreationDate());
            returnedReq.setStatusUpdateDate(LocalDateTime.now());
            return returnedReq;
        });

        when(geminiService.generateFormattedPrompt(sampleRequest.getScenario(), sampleRequest.getMediaType()))
                .thenReturn(Mono.just(errorMessageFromGemini));

        Optional<MediaRequest> result = mediaRequestService.formatRequestToPrompt(1L);

        assertTrue(result.isPresent());
        MediaRequest updatedRequest = result.get();
        assertEquals(RequestStatus.FAIL, updatedRequest.getStatus());
        assertEquals(errorMessageFromGemini, updatedRequest.getErrorMessage());
        assertNull(updatedRequest.getFormattedPrompt());

        verify(mediaRequestRepository, times(2)).save(mediaRequestCaptor.capture());
        List<MediaRequest> savedRequests = mediaRequestCaptor.getAllValues();
        assertEquals(RequestStatus.FORMATTING_PROMPT, savedRequests.get(0).getStatus());
        assertEquals(RequestStatus.FAIL, savedRequests.get(1).getStatus());
        verify(geminiService, times(1)).generateFormattedPrompt(sampleRequest.getScenario(), sampleRequest.getMediaType());
    }

    @Test
    void formatRequestToPrompt_whenGeminiServiceThrowsException_shouldSetStatusToFail() {
        sampleRequest.setStatus(RequestStatus.NOT_YET);
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiService.generateFormattedPrompt(sampleRequest.getScenario(), sampleRequest.getMediaType()))
                .thenReturn(Mono.error(new RuntimeException("Simulated network error")));

        Optional<MediaRequest> result = mediaRequestService.formatRequestToPrompt(1L);

        assertTrue(result.isPresent());
        MediaRequest updatedRequest = result.get();
        assertEquals(RequestStatus.FAIL, updatedRequest.getStatus());
        assertTrue(updatedRequest.getErrorMessage().contains("Simulated network error"));

        verify(mediaRequestRepository, times(2)).save(any(MediaRequest.class));
    }


    @Test
    void formatRequestToPrompt_whenRequestNotFound_shouldReturnEmpty() {
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<MediaRequest> result = mediaRequestService.formatRequestToPrompt(1L);
        assertFalse(result.isPresent());
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, never()).save(any(MediaRequest.class));
    }

    @Test
    void formatRequestToPrompt_whenRequestNotEligibleStatus_shouldReturnEmpty() {
        sampleRequest.setStatus(RequestStatus.RUNNING);
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        Optional<MediaRequest> result = mediaRequestService.formatRequestToPrompt(1L);
        assertFalse(result.isPresent());
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, never()).save(any(MediaRequest.class));
    }

    // Removed formatRequestToPrompt_whenInterruptedExceptionOccurs_shouldSetStatusToFail
}
