<<<<<<< HEAD
<p align="center">
  <img src="docs/images/logo.png" alt="Logo du projet Pocket-Everything : un villageois, un cochon et un coffre posés sur une main" width="200">
</p>

# Pocket-Everything

Mod **NeoForge 1.21.1** (Java 21) qui permet de **capturer n'importe quelle créature** (mob, animal, villageois) à mains nues et de la transporter sous forme d'objet, puis de la relâcher ailleurs avec toutes ses propriétés intactes. Il permet aussi de **porter un coffre dans les deux mains**, contenu compris, et de le reposer à l'identique.

![Une vache capturée et un coffre porté, vus à la première personne](docs/images/apercu.png)

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Configuration](#configuration)
- [Structure du projet](#structure-du-projet)
- [Compiler et développer](#compiler-et-développer)
- [Tests](#tests)
- [Contribuer](#contribuer)
- [Licence](#licence)

## Fonctionnalités

**Capture de créatures** (objet `pocket_everything:captured_entity`)
- Capture par un geste simple : accroupi + clic droit, main vide, sur la créature.
- Conservation de tout l'état de l'entité (NBT) : points de vie, nom, variante (couleur, race…), âge, inventaire et équipement, métier, niveau et échanges des villageois, propriétaire des animaux apprivoisés, selle…
- Relâche sur n'importe quel bloc, avec refus propres (espace trop bas, hostile en difficulté Pacifique) qui gardent l'objet.
- Rendu 3D exact de la créature capturée (inventaire, main, sol, cadre), à une échelle adaptée à sa taille, et bras du joueur visible qui la tient.
- Liste noire modifiable (joueurs, dragon, Wither, bateaux, wagonnets, cadres, tableaux, support d'armure, nœud de laisse).

**Coffre porté** (objet `pocket_everything:carried_chest`)
- Prise d'un coffre ou d'un coffre piégé, contenu, nom, orientation et table de butin non ouverte conservés ; les coffres déjà posés se prennent sans migration.
- Coffre tenu à deux bras, en première et troisième personne, visible des autres joueurs ; main secondaire bloquée pendant le port.
- Objet lâché ou laissé à la mort invulnérable et sans disparition.
- Pose et prise transactionnelles : rien n'est perdu ni dupliqué en cas d'échec.

**Général** : textes en anglais et en français, configuration commune client/serveur, protections respectées (zone de spawn, coffre verrouillé, modes Aventure et Spectateur).

## Prérequis

| Pour… | Il faut |
|---|---|
| Jouer | Minecraft Java Edition **1.21.1** et **NeoForge 21.1.x** (de 21.1.1 à 21.1.265, voir [Versions de NeoForge prises en charge](#versions-de-neoforge-prises-en-charge)), Java 21 |
| Compiler | **JDK 21** et accès à Internet (Gradle 8.14.3 est téléchargé par le script `gradlew`) |
| Lancer le jeu de développement | Compte Minecraft non requis : le client de développement démarre en mode hors ligne |

Le mod est requis des deux côtés : le client pour l'affichage, le serveur pour la logique.

### Versions de NeoForge prises en charge

| Version de NeoForge | Prise en charge |
|---|---|
| 21.1.1 à 21.1.265 (toute la branche 21.1.x pour Minecraft 1.21.1) | Oui. Testées : 21.1.1 (la plus ancienne), 21.1.100, 21.1.200 et 21.1.265 (la plus récente) : tests JUnit, 73 GameTests sur serveur dédié et chargement du client, tout est vert. |
| 21.0.x et 20.x | Non : autre version de Minecraft. |
| 21.2 et plus | Non : autre version de Minecraft (la plage déclarée est `[21.1.1,22)`). |

Auparavant, le mod exigeait NeoForge 21.1.265 ou plus récent (la borne basse de la plage était recopiée de la version de compilation), ce qui le faisait refuser sur les autres versions 21.1.x avec « requires neoforge 21.1.265 or above ». La borne basse est maintenant `neo_min_version` dans `gradle.properties`, distincte de `neo_version` (version de compilation).

## Installation

### Utiliser le mod

1. Installer NeoForge 21.1.x pour Minecraft 1.21.1.
2. Copier `deliverable/pocket-everything-1.0.0.jar` dans le dossier `mods/` du client **et** du serveur.
3. Lancer le jeu ou le serveur avec le profil NeoForge. Le fichier de configuration `config/pocket_everything-common.toml` est créé au premier démarrage.

### Compiler le jar depuis les sources

Depuis la racine du projet (le dossier qui contient `build.gradle`) :

```bash
./gradlew build
```

Sous Windows, utiliser `gradlew.bat build` (cmd) ou `.\gradlew.bat build` (PowerShell). Le jar est produit dans `build/libs/pocket-everything-1.0.0.jar`.

Si plusieurs Java sont installés, `JAVA_HOME` doit pointer sur un JDK 21 ; à défaut, Gradle télécharge lui-même un JDK 21 (greffon *foojay*).

## Utilisation

### Capturer et relâcher une créature

| Action | Geste |
|---|---|
| Capturer | Accroupi (Sneak) + clic droit, **main vide**, sur la créature |
| Relâcher | Clic droit avec l'objet sur un bloc |

- La capture fonctionne en mode Survie ou Créatif, même inventaire plein (l'objet arrive dans la main vide). Elle est ignorée en Aventure et Spectateur, en zone de spawn protégée, et refusée pour les animaux apprivoisés d'un autre joueur.
- La créature réapparaît sur la face visée du bloc ; l'objet est consommé.
- L'infobulle de l'objet indique la créature, son nom et sa vie (par exemple « Captured Entity: Cow » en anglais, « Entité capturée : Vache » en français).

### Porter un coffre

| Action | Geste |
|---|---|
| Prendre | Accroupi + clic droit, **les deux mains vides**, sur un coffre ou un coffre piégé |
| Poser | Clic droit avec le coffre porté sur un bloc |

- Coffre double : seule la moitié visée est prise, l'autre devient un coffre simple.
- Refus avec message : coffre verrouillé, zone protégée, main secondaire occupée, coffre contenant déjà un coffre porté (même caché dans un shulker), contenu de plus de 1 Mio.
- La ligne de commande permet de s'en procurer un exemplaire vide pour essayer : `/give @s pocket_everything:carried_chest`.

### Essayer le mod en développement

```bash
./gradlew runClient
```

Créer un monde, puis par exemple :

```
/gamemode survival
/summon minecraft:villager ~ ~ ~
```

Se mettre accroupi et faire un clic droit sur le villageois avec la main vide : il disparaît et l'objet apparaît dans la main. Un clic droit sur le sol le relâche avec son métier, son niveau et ses échanges. Le guide `TEST_GUIDE.md` détaille une liste complète de vérifications manuelles.

Pour un serveur de développement : `./gradlew runServer` (le serveur ne s'arrête pas seul : taper `stop` dans sa console).

## Configuration

Fichier `config/pocket_everything-common.toml` (créé au premier lancement, dans `run/config/` en développement) :

| Option | Défaut | Rôle |
|---|---|---|
| `allowHostile` | `true` | Autorise la capture de monstres hostiles (zombies, creepers…). |
| `carryChestsEnabled` | `true` | Active la prise et la pose de coffres. À `false`, le geste ouvre le coffre normalement. |
| `extraBlacklist` | `[]` | Identifiants d'entités supplémentaires interdits, par exemple `["minecraft:warden"]`. |

La liste noire de base est une étiquette de données modifiable par datapack : `data/pocket_everything/tags/entity_type/blacklist.json`. Les blocs portables sont dans `data/pocket_everything/tags/block/carryable_chests.json` (`minecraft:chest` et `minecraft:trapped_chest`).

## Structure du projet

```
build.gradle, gradle.properties   Construction (ModDevGradle) et versions du mod
src/main/java/fr/pocketeverything/
  PocketEverything.java                    Point d'entrée du mod
  capture/                        Capture et relâche des créatures
  chest/                          Prise et pose des coffres
  config/                         Options de configuration
  data/                           Composants de données des deux objets
  event/                          Gestionnaires d'événements (clics, main secondaire…)
  item/                           Objets Captured Entity et Carried Chest
  registry/                       Enregistrement des objets et des composants
  client/                         Rendu (modèles, bras, échelle) — côté client seulement
src/main/resources/               Langues, modèles, étiquettes, descripteur du mod, logo.png et pack.png
src/test/                         Tests unitaires JUnit 5
src/gametest/                     Tests de jeu (GameTests), hors du jar
src/dev/                          Outil de captures d'écran du client de développement
tools/                            Générateur de la structure vide des GameTests, déclinaisons du logo (make_logo.py)
deliverable/                      Jar prêt à installer
docs/                             Crédits, logo et images du README
TEST_GUIDE.md                     Guide de test manuel
```

## Compiler et développer

| Commande | Effet |
|---|---|
| `./gradlew build` | Compile, exécute les tests unitaires, produit `build/libs/pocket-everything-1.0.0.jar` |
| `./gradlew runClient` | Lance un client Minecraft de développement avec le mod |
| `./gradlew runServer` | Lance un serveur de développement |
| `./gradlew runScreenshot` | Lance le client, prend des captures dans `build/screenshots/` |

La version du mod, celle de Minecraft et celle de NeoForge sont dans `gradle.properties`.

## Tests

```bash
./gradlew test               # tests unitaires JUnit 5 (src/test)
./gradlew runGameTestServer  # tests de jeu (src/gametest), code de sortie 1 si l'un échoue
```

Les tests de jeu simulent des joueurs dans un monde réel : capture, relâche, villageois, coffre rempli, sauvegarde et rechargement, mort et récupération, changement de dimension, serveur dédié, cas limites.

## Contribuer

1. Créer une branche à partir de `main`.
2. Faire un changement ciblé, accompagné d'un test (JUnit pour la logique pure, GameTest pour le comportement en jeu).
3. Vérifier que `./gradlew build` et `./gradlew runGameTestServer` réussissent.
4. Ouvrir une demande de fusion en décrivant le comportement modifié et la manière de le tester.

Les messages de commit sont courts, à l'impératif, et décrivent ce que le commit apporte. Tout contenu ajouté doit être original ou sous licence libre, avec sa source notée dans `docs/CREDITS.md`.

## Licence

[MIT](LICENSE). Aucune ressource tierce : voir `docs/CREDITS.md`.
=======
# Pocket-Everything
>>>>>>> 765a50eade7bbe4463df7d593ecaec1ac97c4ab0
