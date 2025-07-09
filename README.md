# Application Générateur de Média IA

Cette application web, construite avec Spring Boot, permet aux utilisateurs de soumettre des scénarios pour générer automatiquement des vidéos, des séquences d'images, ou des bandes dessinées. Elle est conçue pour interagir avec différentes IA (Mistral, ChatGPT, DeepSeek - simulation actuelle) et optimiser les médias pour des plateformes cibles spécifiques (TikTok, Instagram) en format vertical.

## Fonctionnalités Principales

*   **Soumission de Scénarios** : Interface pour saisir un scénario, choisir l'IA, le type de média, et la plateforme cible.
*   **Tableau de Bord des Demandes** : Visualisation de toutes les demandes avec leur statut (Not Yet, Go, Running, Fail, Success), dates de mise à jour, et actions possibles.
*   **Traitement des Demandes** :
    *   Les demandes passent par différents statuts (Not Yet, Go, Formatting Prompt, Prompt Generated, Running, Fail, Success).
    *   **Formatage de Prompt** : Une action permet de générer (simulé) un prompt détaillé pour une IA à partir du scénario et du type de média. Le statut passe à `FORMATTING_PROMPT` puis `PROMPT_GENERATED`.
    *   **Génération de Média** : Le traitement de la génération de média (déclenché par le bouton "Go Média") est actuellement simulé (délais et résultats aléatoires).
    *   Les deux types de traitement peuvent être déclenchés manuellement depuis le tableau de bord.
*   **Optimisation des Médias** : (Conceptuel) Les médias générés seraient optimisés pour un affichage vertical.
*   **Interface Utilisateur** : Style inspiré de Material Design avec icônes et tooltips. Le prompt formaté est visible via un tooltip.

## Technologies Utilisées

*   **Backend** : Spring Boot 3.5.3
    *   Spring Web
    *   Spring WebFlux (pour `WebClient`)
    *   Spring Data JPA
    *   Thymeleaf (moteur de template côté serveur)
*   **Base de données** : H2 (en mémoire, pour le développement)
*   **Appels API Externe** :
    *   `WebClient` pour appeler l'API OpenAI (ChatGPT).
*   **Frontend (Styling & Base)** :
    *   HTML5, CSS3
    *   Material Icons (via Google Fonts)
    *   Police Roboto (via Google Fonts)
    *   JavaScript simple pour interactions de base
*   **Build Tool** : Maven
*   **Tests** : JUnit 5, Mockito, `MockWebServer` (pour tester `ChatGptService`)
*   **CI** : GitHub Actions (compilation et packaging)

## Configuration (dans `src/main/resources/application.properties`)

*   **OpenAI API Key**:
    `openai.api.key=`
    (Doit être configurée avec une clé valide pour que les appels réels à ChatGPT fonctionnent. Si laissée vide ou avec la valeur placeholder `SIMULATED_KEY_PLACEHOLDER`, le service `ChatGptService` retournera une réponse simulée.)
*   **OpenAI API URL**:
    `openai.api.url=https://api.openai.com/v1`
*   **OpenAI Model**:
    `openai.model=gpt-3.5-turbo`
*   Autres configurations JPA, H2 Console, et logging.

## Structure du Projet (Principaux Répertoires)

```
.
├── .github/workflows/build.yml  # Workflow GitHub Actions
├── pom.xml                      # Fichier de configuration Maven
├── src
│   ├── main
│   │   ├── java/com/example/mediagenerator
│   │   │   ├── MediaGeneratorApplication.java  # Classe principale Spring Boot
│   │   │   ├── config/                       # Configurations Spring (ex: AppConfig pour WebClient)
│   │   │   ├── controller/                   # Contrôleurs Web
│   │   │   ├── dto/                          # Data Transfer Objects (généraux et pour OpenAI)
│   │   │   │   └── openai/
│   │   │   ├── model/                        # Entités JPA et Enums
│   │   │   ├── repository/                   # Répertoires Spring Data JPA
│   │   │   └── service/                      # Logique métier (MediaRequestService, ChatGptService)
│   │   └── resources
│   │       ├── application.properties        # Configuration de l'application
│   │       ├── static/                       # Ressources statiques (CSS, JS, images)
│   │       │   ├── css/style.css
│   │       │   └── js/script.js
│   │       └── templates/                    # Vues Thymeleaf
│   │           ├── dashboard.html
│   │           └── fragments/status-icon.html
│   └── test
│       └── java/com/example/mediagenerator   # Classes de test
│           ├── MediaGeneratorApplicationTests.java
│           └── service/MediaRequestServiceTest.java
└── README.md (ce fichier)
```
*(Note: D'autres fichiers `index.html`, `style.css` et `script.js` peuvent être présents à la racine s'ils proviennent d'un exercice précédent. Ils ne font pas partie de l'application Spring Boot Media Generator actuelle.)*

## Configuration Requise

*   Java JDK 17 ou supérieur
*   Apache Maven 3.6+ (ou utiliser le Maven Wrapper `./mvnw`)

## Compilation et Exécution

1.  **Cloner le dépôt** (si ce n'est pas déjà fait) :
    ```bash
    git clone <url-du-depot>
    cd <nom-du-repertoire-du-projet>
    ```

2.  **Compiler et packager l'application** (génère un fichier JAR) :
    Utiliser le Maven Wrapper fourni :
    ```bash
    ./mvnw clean package
    ```
    (Sur Windows, utilisez `mvnw.cmd clean package`)
    Les tests sont exécutés par défaut. Pour les sauter : `./mvnw clean package -DskipTests`

3.  **Exécuter l'application** :
    Une fois le package JAR créé (par exemple, `target/media-generator-0.0.1-SNAPSHOT.jar`), exécutez :
    ```bash
    java -jar target/media-generator-0.0.1-SNAPSHOT.jar
    ```

4.  **Accéder à l'application** :
    Ouvrez votre navigateur web et allez à l'adresse : `http://localhost:8080`

## Points d'Amélioration Possibles (Futur)

*   **Intégration réelle des APIs IA** : Remplacer la simulation (pour le formatage de prompt et la génération de média) par des appels réels aux services Mistral, ChatGPT, DeepSeek.
*   **Gestion avancée des erreurs et des nouvelles tentatives** pour les appels IA.
*   **Stockage persistant des médias générés et des prompts formatés** (ex: S3, stockage de fichiers local, ou amélioration de la base de données).
*   **Authentification et autorisation** des utilisateurs.
*   **Pagination et filtrage avancé** dans le tableau de bord.
*   **Implémentation complète des tooltips Material 3** et autres composants web Material 3.
*   **Internationalisation (i18n)** de l'interface.
*   **Configuration de base de données de production** (PostgreSQL, MySQL).
*   **Tests d'intégration plus complets.**

## Conformité avec le Prompt Initial

*   **Application web en Spring Boot** : Réalisé.
*   **Génération de vidéo, images, BD (concept)** : Structure en place pour définir ces types, la génération est simulée.
*   **Scénario, IA à contacter, type de média, média cible** : Champs présents dans le formulaire et l'entité.
*   **Statuts de demande et date de MàJ** : Implémentés.
*   **Déclenchement sur statut 'GO'** : Implémenté (traitement simulé).
*   **Optimisation verticale** : Conceptuel, pas d'implémentation de traitement d'image/vidéo.
*   **Tableau de bord avec données et colonnes triables** : Tableau de bord implémenté. Le tri des colonnes n'est pas interactivement implémenté via des clics sur les en-têtes pour l'instant, mais les données sont triées par défaut par date de création (via la requête JPA dans le service).
*   **Icônes et Tooltips** : Material Icons utilisés, tooltips basiques via l'attribut `title`.
*   **Thymeleaf, Spring Boot 3.5.3** : Utilisés.
*   **GitHub Actions pour compilation et package** : Fichier de workflow `build.yml` créé.
*   **Gestion d'erreur et traces** : Logging SLF4J et gestion des exceptions de base implémentés.
*   **Material 3 Expressive (concept)** : L'interface s'inspire de Material Design. L'utilisation directe des "Material 3 Web Components" nécessiterait une configuration frontend plus poussée qui sort du cadre d'une application Spring Boot/Thymeleaf simple pour cette première version.

Ce projet constitue une base fonctionnelle pour l'application décrite dans le prompt.
