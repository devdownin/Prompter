document.addEventListener('DOMContentLoaded', function() {
    // Tooltips pour les boutons d'action dans le tableau "Lignes Scénario"
    const actionButtons = document.querySelectorAll('#scenario-lines-section .button-urgent, #scenario-lines-section .button-secondary');
    actionButtons.forEach(button => {
        let tooltipText = '';
        if (button.classList.contains('button-urgent')) {
            tooltipText = 'Ceci est une action urgente !';
        } else if (button.classList.contains('button-secondary')) {
            tooltipText = 'Ceci est une action secondaire.';
        }

        if (tooltipText) {
            button.setAttribute('title', tooltipText);
            // Optionnel: Implémentation d'un tooltip custom si nécessaire,
            // mais l'attribut 'title' standard est suffisant pour commencer.
        }
    });

    // Logique de base pour la gestion du formulaire de prompt (exemple)
    const promptForm = document.getElementById('prompt-form');
    const promptsList = document.getElementById('prompts-list');

    if (promptForm && promptsList) {
        promptForm.addEventListener('submit', function(event) {
            event.preventDefault(); // Empêche la soumission standard du formulaire

            const title = document.getElementById('prompt-title').value;
            const context = document.getElementById('prompt-context').value;
            const task = document.getElementById('prompt-task').value;
            const persona = document.getElementById('prompt-persona').value;
            const format = document.getElementById('prompt-format').value;
            const constraints = document.getElementById('prompt-constraints').value;

            // Construction simple du prompt (ceci serait plus élaboré dans une vraie app)
            let generatedPrompt = `## ${title}\n\n**Contexte:**\n${context}\n\n**Tâche:**\n${task}\n`;
            if (persona) generatedPrompt += `\n**Persona de l'IA:** ${persona}\n`;
            if (format) generatedPrompt += `\n**Format de sortie:** ${format}\n`;
            if (constraints) generatedPrompt += `\n**Contraintes:**\n${constraints}\n`;

            // Affichage du prompt généré
            const promptEntry = document.createElement('article');
            promptEntry.classList.add('prompt-entry');

            const promptTitleElement = document.createElement('h3');
            promptTitleElement.textContent = title;
            promptEntry.appendChild(promptTitleElement);

            const promptPreElement = document.createElement('pre');
            promptPreElement.textContent = generatedPrompt.substring(generatedPrompt.indexOf('\n\n') + 2); // N'affiche pas le titre redondant dans le <pre>
            promptEntry.appendChild(promptPreElement);

            // Nettoyer le message "Aucun prompt" s'il existe
            const noPromptsMessage = promptsList.querySelector('p');
            if (noPromptsMessage && noPromptsMessage.textContent.includes('Aucun prompt généré')) {
                promptsList.innerHTML = ''; // Vide la liste avant d'ajouter le premier prompt
            }

            promptsList.appendChild(promptEntry);
            promptsList.appendChild(document.createElement('hr'));


            // Réinitialiser le formulaire
            promptForm.reset();
        });
    }

    // Plus d'interactivité peut être ajoutée ici
    // Par exemple, la sauvegarde des prompts dans le localStorage, etc.
});
