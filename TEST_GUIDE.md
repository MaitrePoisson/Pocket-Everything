# Guide de test — Grab-Mob

## Lancer

- **Client de développement** : `./gradlew runClient` (JDK 21). Créer un monde créatif ou survie.
- **Installation réelle** : NeoForge 21.1.x pour Minecraft 1.21.1, puis `deliverable/grabmob-1.0.0.jar` dans `mods/`.
- **Serveur dédié** : même jar dans `mods/` du serveur (`./gradlew runServer` en développement).
- **Vérification automatique** : `bash .athanor/check.sh` (build, 22 tests JUnit, 38 GameTests, captures d'écran, copie du jar dans `deliverable/`).

## Commandes de jeu utiles

`/give @s grabmob:captured_entity` donne un objet vide (sans créature : inoffensif). `/summon minecraft:villager ~ ~ ~` et `/gamemode survival|creative` aident aux tests.

## Contrôles

| Action | Geste |
|---|---|
| Capturer | Sneak + clic droit, main vide, sur la créature |
| Relâcher | Clic droit avec l'objet sur un bloc |

## À tester à la main

### Capture
- [ ] Vache, mouton, cochon, poule, abeille, golem de fer : disparaît, l'objet apparaît dans la main/l'inventaire.
- [ ] Sans Sneak, ou avec un objet en main : aucune capture, interaction normale (traire, monter, échanger).
- [ ] Un autre joueur, le dragon, le Wither, un bateau, un wagonnet, un cadre, un support d'armure : jamais capturés, aucun message, l'interaction normale du jeu se produit (les non-vivants et la liste noire ne sont pas interceptés).
- [ ] Animal apprivoisé d'un autre joueur : refusé avec le message « This creature cannot be captured. » ; le sien : capturé.
- [ ] Zone de spawn protégée (joueur non opérateur) : refusé avec le message « This creature cannot be captured. ». Mode Aventure ou Spectateur : le geste n'est pas intercepté, aucune capture, interaction normale.
- [ ] Survie et créatif, inventaire plein : la capture réussit quand même, car la main est vide (geste obligatoire) et l'objet y est placé ; il n'est jamais perdu.

### Villageois
- [ ] Villageois avec métier, niveau et échanges (ou 2-3 échanges déjà utilisés) : capturer, relâcher, ouvrir l'échange : métier, niveau, offres et utilisations identiques.
- [ ] Villageois nommé (étiquette) : nom conservé, infobulle de l'objet « Captured Entity: nom ».
- [ ] Villageois bébé : reste bébé. Variante de biome conservée.
- [ ] Villageois lié à un lit ou un poste de travail : après capture ils sont libres de nouveau pour un autre villageois.

### Conservation
- [ ] Vie partielle conservée (infobulle « Health: x / y »). Cheval sellé, loup apprivoisé, mouton teint, chat/renard/lapin à variante.
- [ ] Mob équipé (armure, objet en main) : équipement conservé.

### Relâche
- [ ] Clic droit sur le dessus d'un bloc : la créature apparaît dessus, l'objet est consommé (survie) ; en créatif aussi.
- [ ] Clic sur le côté d'un mur : apparaît à côté. Sous un plafond trop bas pour la créature : échec avec le même message, l'objet est gardé.
- [ ] Hostile en difficulté Pacifique : refus avec le message « This creature cannot be released here. », objet gardé.
- [ ] Particules à la relâche.

### Rendu (client)
- [ ] Le modèle exact de la créature est visible dans l'inventaire (rotation douce), en main à la première et à la troisième personne (F5), au sol et dans un cadre.
- [ ] Grandes entités (golem, cheval) : lisibles, pas démesurées ; bébés plus petits.
- [ ] Plusieurs créatures différentes dans la barre rapide : chaque objet montre sa propre créature.

### Bras visible
- [ ] Première personne : avec un objet capturé en main, le bras du joueur (avec sa peau) est visible et tient la créature, sans qu'elle flotte. Vérifier avec une abeille, une vache, un golem et un villageois bébé.
- [ ] Le bras suit l'équipement de l'objet, le balancement de marche et les mouvements de caméra, sans décalage.
- [ ] Main secondaire (objet en main gauche) et joueur gaucher (Options > Compétences > Bras principal) : le bon bras apparaît, à la bonne place.
- [ ] Troisième personne (F5, de face et de dos) : le bras du modèle du joueur tient l'objet.
- [ ] Main vide, ou autre objet en main : rendu inchangé par rapport à Minecraft.

### Configuration
- [ ] `config/grabmob-common.toml` : `allowHostile=false` interdit zombies, creepers... ; `extraBlacklist=["minecraft:cow"]` interdit les vaches.

### Serveur dédié et multijoueur
- [ ] Le serveur démarre sans erreur avec le jar. Un client avec le mod capture/relâche, et un second joueur voit la créature réapparaître.
- [ ] Un client sans le mod : refus attendu à la connexion (le mod ajoute un objet et un composant de données, mod requis des deux côtés ; à confirmer en jeu).
- [ ] Villageois à beaucoup d'échanges (60 offres ≈ 11 Ko) : la capture ne déconnecte personne.
