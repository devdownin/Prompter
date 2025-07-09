package com.example.mediagenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class MediaRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob // Large Object, pour les textes longs comme un scénario
    @Column(nullable = false, columnDefinition = "TEXT") // Assurer la compatibilité avec différentes BD
    private String scenario;

    // Pour stocker une liste d'IA, on peut utiliser une chaîne concaténée
    // ou une table de jointure si la relation devient plus complexe.
    // Pour l'instant, une chaîne simple suffira.
    private String selectedIAs; // Exemple: "ChatGPT,Mistral"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetPlatform targetPlatform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp // Géré automatiquement par Hibernate/JPA
    private LocalDateTime creationDate;

    @UpdateTimestamp // Géré automatiquement par Hibernate/JPA à chaque mise à jour de l'entité
    private LocalDateTime statusUpdateDate;

    private String generatedMediaPath; // Chemin vers le média généré, nullable

    private String errorMessage; // Pour stocker un message d'erreur en cas de statut FAIL

    @Lob
    @Column(columnDefinition = "TEXT")
    private String formattedPrompt; // Pour stocker le prompt formaté généré pour l'IA

    // Constructeur personnalisé si nécessaire pour initialiser certains champs
    public MediaRequest(String scenario, String selectedIAs, MediaType mediaType, TargetPlatform targetPlatform) {
        this.scenario = scenario;
        this.selectedIAs = selectedIAs;
        this.mediaType = mediaType;
        this.targetPlatform = targetPlatform;
        this.status = RequestStatus.NOT_YET; // Statut initial
    }
}
