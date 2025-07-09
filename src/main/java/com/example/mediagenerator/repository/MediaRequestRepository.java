package com.example.mediagenerator.repository;

import com.example.mediagenerator.model.MediaRequest;
import com.example.mediagenerator.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRequestRepository extends JpaRepository<MediaRequest, Long> {

    // Méthode pour trouver les demandes par statut, utile pour le processeur de tâches
    List<MediaRequest> findByStatus(RequestStatus status);

    // Spring Data JPA peut inférer des requêtes basées sur le nom de la méthode.
    // Par exemple, pour trier par date de mise à jour du statut :
    List<MediaRequest> findAllByOrderByStatusUpdateDateDesc();
    List<MediaRequest> findAllByOrderByCreationDateDesc();

    // D'autres méthodes de recherche personnalisées pourront être ajoutées ici si nécessaire.
}
