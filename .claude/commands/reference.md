---
description: Chercher une implémentation de référence dans Create ou Cobblemon
argument-hint: <feature-ou-pattern-recherché>
allowed-tools: Read, Grep, Glob
---

# Recherche référence: $ARGUMENTS

## Utiliser MCP mcmodding

1. **search_mod_examples**: query="$ARGUMENTS", mod="Create" ou "Cobblemon"
2. **get_example**: récupérer le code complet si trouvé
3. **search_neoforge_docs**: documentation officielle si besoin

## Analyser et résumer

```
Feature trouvée: [nom]
Mod source: [Create/Cobblemon]
Fichiers concernés:
- [path]: [rôle]

Pattern utilisé:
[description du pattern]

Dépendances clés:
- [dep]: [pourquoi]

Adaptation Beemancer:
[ce qu'on garde, ce qu'on adapte]
```

## Si rien trouvé
Chercher dans la doc NeoForge officielle via `search_neoforge_docs`.
