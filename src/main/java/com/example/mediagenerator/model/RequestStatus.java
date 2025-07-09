package com.example.mediagenerator.model;

public enum RequestStatus {
    NOT_YET,    // La demande est créée mais pas encore prête à être traitée
    GO,         // La demande est prête pour traitement (génération média)
    FORMATTING_PROMPT, // La demande est en cours de formatage en prompt pour une IA
    PROMPT_GENERATED,  // Le prompt a été généré avec succès
    RUNNING,    // La génération du média principal est en cours de traitement
    FAIL,       // Le traitement (génération prompt ou média) de la demande a échoué
    SUCCESS     // Le traitement de la génération du média a réussi
}
