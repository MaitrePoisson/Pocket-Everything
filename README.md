# Grab-Mob

Mod **NeoForge 1.21.1** (Java 21) : capturez n'importe quel mob ou villageois à mains nues, transportez-le sous forme d'objet, puis relâchez-le ailleurs avec toutes ses propriétés intactes. Il permet aussi de **porter un coffre dans les deux mains**, contenu compris.

## Utilisation

- **Capturer** : accroupi (Sneak) + clic droit, main vide, sur une créature, en mode Survie ou Créatif (pas Aventure ni Spectateur, ni en zone de spawn protégée). Elle disparaît et l'objet *Captured Entity* (`grabmob:captured_entity`) arrive dans votre main, même inventaire plein. Les animaux apprivoisés d'un autre joueur sont refusés.
- **Relâcher** : clic droit avec l'objet sur un bloc. La créature réapparaît sur la face visée et l'objet est consommé (refus si l'espace est trop bas ou si un hostile est relâché en difficulté Pacifique : l'objet est alors gardé).
- **Conservé** : points de vie, nom, variante (couleur, race...), âge (bébés), inventaire et équipement, métier, niveau et échanges des villageois, propriétaire des animaux apprivoisés, selle, etc. (tout le NBT de l'entité).
- **Rendu** : l'objet affiche le modèle 3D exact de la créature capturée (GUI, main, sol, cadre), avec une échelle adaptée à sa taille.
- Les joueurs, les entités non vivantes (bateaux, wagonnets, cadres, tableaux, objets au sol...) et le Wither, le dragon, le support d'armure et le nœud de laisse ne sont pas capturables : voir `data/grabmob/tags/entity_type/blacklist.json`.

### Porter un coffre

- **Prendre** : accroupi (Sneak) + clic droit, **les deux mains vides**, sur un coffre ou un coffre piégé (tag `grabmob:carryable_chests`). Le coffre disparaît et l'objet *Carried Chest* (`grabmob:carried_chest`) arrive en main : tout le contenu, le nom, l'orientation et la table de butin non ouverte sont conservés à l'identique. Les coffres déjà posés se prennent sans migration.
- **Porter** : le coffre est tenu à deux bras (1re et 3e personne, visible des autres joueurs). Tant qu'on le porte, la main secondaire est bloquée (échange, pose, objets rangés dans l'inventaire).
- **Poser** : clic droit sur un bloc. Le coffre réapparaît avec son contenu intact (refus si la place est prise ou protégée : l'objet est gardé).
- **Refus** (avec message) : coffre verrouillé, zone protégée, main secondaire occupée, coffre contenant un coffre porté (même caché dans un shulker), contenu de plus de 1 Mio. En mode Aventure ou Spectateur, ou avec la main principale occupée, le geste est ignoré. Les joueurs qui ouvrent le coffre au moment de la prise voient leur menu fermé proprement.
- **Coffre double** : seule la moitié visée est prise ; l'autre moitié redevient un coffre simple.
- **Perte du joueur** : l'objet lâché ou laissé à la mort est invulnérable aux dégâts et ne disparaît pas avec le temps (sauf chute dans le vide).

## Installation

1. Installer NeoForge 21.1.x pour Minecraft 1.21.1.
2. Copier `deliverable/grabmob-1.0.0.jar` dans le dossier `mods/` du client **et** du serveur (le rendu est côté client, la logique côté serveur).

## Configuration

Fichier `config/grabmob-common.toml` :

- `allowHostile` (défaut `true`) : autorise la capture de monstres hostiles.
- `carryChestsEnabled` (défaut `true`) : active le port de coffres.
- `extraBlacklist` (défaut vide) : identifiants d'entités supplémentaires interdits, par exemple `["minecraft:warden"]`.

## Compiler

```
./gradlew build          # jar dans build/libs/
./gradlew test           # tests unitaires
./gradlew runGameTestServer
./gradlew runClient
```

JDK 21 requis. Les tests de jeu (GameTests) sont dans `src/gametest`, hors du jar.

## Licence

MIT. Aucun asset tiers : voir `docs/CREDITS.md`.
