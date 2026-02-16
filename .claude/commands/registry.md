---
description: Ajouter un élément aux registres Apica
argument-hint: <type:item|block|entity|blockentity> <id> <classe>
allowed-tools: Read, Write, Edit
---

# Ajout registre: $ARGUMENTS

## Process

1. **Parser**: type, id, classe
2. **Ouvrir** le registre: `core/registry/Apica[Type]s.java`
3. **Catégorie**: identifier ou créer
4. **Ajouter** avec format:

```java
// --- [CATÉGORIE] ---
public static final Deferred[Type]<[Classe]> [ID_UPPER] = [REGISTRY].register("[id_lower]",
    () -> new [Classe](new [Type].Properties()));
```

5. **Structure.txt**: Mettre à jour section 4 (tableau registres)

## Registres disponibles
- `item` → ApicaItems.java
- `block` → ApicaBlocks.java  
- `blockentity` → ApicaBlockEntities.java
- `entity` → ApicaEntities.java
