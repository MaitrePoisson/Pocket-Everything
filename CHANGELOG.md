# Journal des changements

Format : `MAJEUR.MINEUR.PATCH` (règles dans le README, section « Versionnage »). Les versions les plus récentes sont en haut.

## 1.1.1 — 2026-10-30

- Branche `quilt-1.21.1` : portage du mod sur Quilt (Quilt Loom 1.11.1, Quilt Loader 0.30.1, Fabric API 0.116.17+1.21.1 chargée par Quilt Loader), `quilt.mod.json`, jar `pocket-everything-quilt-1.21.1.jar`. Même version du mod que `main` (1.1.1).
- Mêmes gestes, protections, messages et rendus que la version NeoForge et Fabric ; configuration `config/pocket_everything.json` (au lieu du fichier TOML commun), mixins pour l'échange de mains et la pose de bras.
- 42 tests JUnit et 87 GameTests (fabric-gametest-api sur Quilt Loader) verts, serveur dédié démarré sans erreur, 28 captures client identiques à `main`.
- Demande normale (+1 sur le patch) : le jar se nomme `pocket-everything-<loader>-<version-minecraft>.jar` (`pocket-everything-neoforge-1.21.1.jar`), le loader et la version de Minecraft venant de `mod_loader` et `minecraft_version` dans `gradle.properties`. La version du mod n'est plus dans le nom du fichier.
- Aucun changement de comportement ni de métadonnées du mod.

## 1.1.0 — 2026-10-29

- Gros ajout (règle : +1 sur le numéro du milieu) : le port de blocs ne se limite plus aux coffres. Toute block entity de l'étiquette `carryable_chests` se prend et se repose avec son contenu ; la **Toolbox de Create** est portable par défaut.
- Nouvelle étiquette `never_carryable` (prioritaire, modifiable par datapack) : `create:item_vault` exclu.
- Le bloc porté est rendu avec son vrai modèle ; repli sur le coffre simple pour un bloc inconnu.
- 13 GameTests (bloc de test hors du jar) : aller-retour, verrou, protections, exclusion, imbrication, menus, mort, dimension. Total 86 GameTests, 38 tests JUnit.
- README : liste des blocs portables et section « Ajouter un bloc par datapack ».

## 1.0.3 — 2026-10-29

- Première demande livrée selon la règle de versionnage : classée « demande normale » (ajustement du processus de livraison, aucune fonctionnalité de jeu) donc +1 sur le patch, 1.0.2 → 1.0.3.
- Aucun changement de comportement du mod ; jar `pocket-everything-1.0.3.jar`, tag `v1.0.3`.

## 1.0.2 — 2026-10-29

- Mise en place du versionnage : source de vérité `mod_version` (`gradle.properties`), ce journal, la ligne de version au démarrage, un test de cohérence et les tags git `vX.Y.Z`.
- README : section « Versionnage », suppression de marqueurs de fusion oubliés en fin de fichier.
- Version alignée sur 1.0.2 à la demande de l'utilisateur (le dépôt était en 1.0.1, aucun numéro sauté).

## 1.0.1 — 2026-10-29

- Le jar corrigé est reconnaissable (1.0.1 au lieu de 1.0.0) : l'ancien jar encore présent dans `mods/` provoquait « requires neoforge 21.1.265 or above ».
- Consigne dans le README et le TEST_GUIDE : supprimer tout ancien `pocket-everything-*.jar` de `mods/` (client et serveur).
- Compatibilité validée sur NeoForge 21.1.1, 21.1.100, 21.1.200, 21.1.235 et 21.1.265.

## 1.0.0 — 2026-10-28

- Première version : capture, transport et relâche de mobs et PNJ avec toutes leurs propriétés, rendu du bras tenant l'entité capturée, transport de coffres à deux mains, configuration, 73 GameTests.
- 2026-10-28 : renommage Grab-Mob → Pocket-Everything, logo et README.
- 2026-10-29 : plage NeoForge élargie à `[21.1.1,22)` (la borne basse suivait par erreur la version de compilation 21.1.265).
