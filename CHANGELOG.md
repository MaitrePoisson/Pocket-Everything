# Journal des changements

Format : `MAJEUR.MINEUR.PATCH` (règles dans le README, section « Versionnage »). Les versions les plus récentes sont en haut.

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
