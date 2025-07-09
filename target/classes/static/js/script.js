document.addEventListener('DOMContentLoaded', function() {
    // Initialisation des tooltips (simple, via l'attribut title)
    // Si des tooltips plus complexes (style Material 3) sont nécessaires,
    // il faudrait une bibliothèque ou une implémentation custom.
    // Pour l'instant, les tooltips natifs du navigateur via `title` sont utilisés.

    // Animation simple pour les messages flash (disparition après quelques secondes)
    const flashMessages = document.querySelectorAll('.flash-message');
    flashMessages.forEach(function(message) {
        setTimeout(function() {
            message.style.opacity = '0';
            setTimeout(function() {
                message.style.display = 'none';
            }, 500); // Attendre la fin de la transition d'opacité
        }, 5000); // Disparaît après 5 secondes
    });

    // Logique pour les icônes de tooltip dans les en-têtes de tableau
    // (Actuellement, ils utilisent l'attribut title, ceci est un placeholder si on veut des tooltips custom)
    const tooltipIcons = document.querySelectorAll('th .tooltip-icon');
    tooltipIcons.forEach(icon => {
        icon.addEventListener('mouseover', () => {
            // Logique pour afficher un tooltip custom si nécessaire
            // Par exemple, créer un div, le positionner, etc.
            // Pour l'instant, on se fie à l'attribut 'title' de l'élément parent (th) ou de l'icône elle-même.
        });
        icon.addEventListener('mouseout', () => {
            // Logique pour cacher le tooltip custom
        });
    });

    // Logique pour les boutons d'action (ex: confirmation)
    // Exemple: Confirmation avant de passer une tâche à "GO" (non implémenté pour l'instant pour garder simple)
    /*
    const goButtons = document.querySelectorAll('.button.action-go');
    goButtons.forEach(button => {
        button.addEventListener('click', function(event) {
            if (!confirm('Êtes-vous sûr de vouloir lancer cette tâche ?')) {
                event.preventDefault();
            }
        });
    });
    */

    console.log("Media Generator Dashboard script loaded.");
});

// Fonction utilitaire si on veut des tooltips plus avancés plus tard
function createCustomTooltip(element, text) {
    // ...
}
