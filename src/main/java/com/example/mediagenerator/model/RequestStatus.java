package com.example.mediagenerator.model;

public enum RequestStatus {
    NOT_YET,    // La demande est créée mais pas encore prête à être traitée
    GO,         // La demande est prête pour traitement
    RUNNING,    // La demande est en cours de traitement
    FAIL,       // Le traitement de la demande a échoué
    SUCCESS     // Le traitement de la demande a réussi
}
