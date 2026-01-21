---
description: Mettre à jour Structure.txt après implémentation d'une feature
argument-hint: <nom-feature>
allowed-tools: Read, Write, Edit
---

# Mise à jour Structure.txt: $ARGUMENTS

## Lire Structure.txt actuel

## Mettre à jour Section 4 — Registres

Ajouter dans le tableau approprié:
```
| [id] | [Classe] | [Feature] | OK |
```

## Mettre à jour Section 5 — Features implémentées

Ajouter le bloc complet:
```
--------------------------------------------------------------------------------
FEATURE: $ARGUMENTS
--------------------------------------------------------------------------------
Description: [une ligne]
Statut: COMPLÈTE / EN TEST

Scripts créés:
- [path/File.java] : [rôle]

Dépendances utilisées:
- [Dep] : [pourquoi]

Registres modifiés:
- BeemancerItems: +[ids]
- BeemancerBlocks: +[ids]

Assets créés:
- textures/[...]
- models/[...]

Référence Create/Cobblemon: [feature source]
--------------------------------------------------------------------------------
```

## Mettre à jour Section 9 — Changelog

```
--------------------------------------------------------------------------------
[DATE] - $ARGUMENTS
--------------------------------------------------------------------------------
Ajouts:
- [fichiers créés]

Modifications:
- [fichiers modifiés]
--------------------------------------------------------------------------------
```
