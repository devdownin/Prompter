package com.example.mediagenerator.dto;

import com.example.mediagenerator.model.MediaType;
import com.example.mediagenerator.model.TargetPlatform;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MediaRequestDto {

    private String scenario;
    private String selectedIAs; // Peut être une chaîne d'IA séparées par des virgules
    private MediaType mediaType;
    private TargetPlatform targetPlatform;

    // Pas besoin d'ID, statut, dates, etc., car c'est pour la création.
    // Des validateurs (par exemple @NotEmpty, @NotNull) pourraient être ajoutés ici.
}
