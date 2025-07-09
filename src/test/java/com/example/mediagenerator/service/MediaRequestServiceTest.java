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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaRequestServiceTest {

    @Mock
    private MediaRequestRepository mediaRequestRepository;

    @InjectMocks
    private MediaRequestService mediaRequestService;

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
        // Arrange
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenAnswer(invocation -> {
            MediaRequest reqToSave = invocation.getArgument(0);
            reqToSave.setId(1L); // Simulate saving and getting an ID
            reqToSave.setCreationDate(LocalDateTime.now());
            reqToSave.setStatusUpdateDate(LocalDateTime.now());
            return reqToSave;
        });

        // Act
        MediaRequest result = mediaRequestService.submitNewRequest(sampleDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Scenario", result.getScenario());
        assertEquals(RequestStatus.NOT_YET, result.getStatus());
        verify(mediaRequestRepository, times(1)).save(any(MediaRequest.class));
    }

    @Test
    void setRequestStatusToGo_whenRequestExistsAndStatusIsNutYet_shouldUpdateStatus() {
        // Arrange
        sampleRequest.setStatus(RequestStatus.NOT_YET);
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenReturn(sampleRequest);

        // Act
        Optional<MediaRequest> result = mediaRequestService.setRequestStatusToGo(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(RequestStatus.GO, result.get().getStatus());
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, times(1)).save(sampleRequest);
    }

    @Test
    void setRequestStatusToGo_whenRequestNotFound_shouldReturnEmpty() {
        // Arrange
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<MediaRequest> result = mediaRequestService.setRequestStatusToGo(1L);

        // Assert
        assertFalse(result.isPresent());
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, never()).save(any(MediaRequest.class));
    }

    @Test
    void setRequestStatusToGo_whenRequestNotInNotYetStatus_shouldReturnEmpty() {
        // Arrange
        sampleRequest.setStatus(RequestStatus.RUNNING); // Not in NOT_YET
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));

        // Act
        Optional<MediaRequest> result = mediaRequestService.setRequestStatusToGo(1L);

        // Assert
        assertFalse(result.isPresent()); // Service implementation returns Optional.empty()
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, never()).save(any(MediaRequest.class));
    }

    @Test
    void updateRequestStatus_shouldUpdateFieldsAndSave() {
        // Arrange
        when(mediaRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(mediaRequestRepository.save(any(MediaRequest.class))).thenReturn(sampleRequest);

        String errorMessage = "Test Error";
        String mediaPath = "/path/to/media.mp4";

        // Act
        mediaRequestService.updateRequestStatus(1L, RequestStatus.FAIL, errorMessage, mediaPath);

        // Assert
        assertEquals(RequestStatus.FAIL, sampleRequest.getStatus());
        assertEquals(errorMessage, sampleRequest.getErrorMessage());
        assertEquals(mediaPath, sampleRequest.getGeneratedMediaPath());
        verify(mediaRequestRepository, times(1)).findById(1L);
        verify(mediaRequestRepository, times(1)).save(sampleRequest);
    }
}
