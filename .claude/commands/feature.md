---
description: Implémenter une nouvelle feature Beemancer avec workflow complet
argument-hint: <nom-feature> <description>
allowed-tools: Read, Write, Edit, Grep, Glob, Bash(./gradlew:*)
---

# Feature: $ARGUMENTS

## Phase 1: Compréhension
Reformule le besoin en 2-3 phrases. Pose des questions si ambiguïté.

## Phase 2: Recherche références
1. **Lire Structure.txt** — État actuel du projet
2. **Chercher dans Create/Cobblemon** via MCP mcmodding:
   - Quelle feature similaire existe?
   - Quels fichiers sont concernés?
   - Quel pattern d'implémentation?

## Phase 3: Architecture technique
Fournir:
```
Scripts à créer:
- [path] : [rôle]

Scripts à modifier:
- [path] : [modification]

Dépendances:
- [existante/à créer] : [utilité]

Ordre d'implémentation:
1. [...]
```

## Phase 4: Implémentation
- Dépendances universelles d'abord
- Un fichier à la fois
- Header obligatoire avec tableau dépendances
- Max 150 lignes/fichier

## Phase 5: Finalisation
Mettre à jour Structure.txt:
- Section 4: Registres
- Section 5: Feature documentée
- Section 9: Changelog
