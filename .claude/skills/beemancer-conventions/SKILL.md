---
name: beemancer-conventions
description: Conventions et standards du projet Beemancer. Toujours appliquer pour tout nouveau code.
allowed-tools: Read, Grep, Glob, Edit, Write
---

# Conventions Beemancer

## Toujours appliquer ces règles

---

## 1. Header obligatoire

Chaque fichier Java commence par:

```java
/**
 * ============================================================
 * [NomDuFichier.java]
 * Description: [Une ligne résumant le rôle]
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | [Classe]            | [Pourquoi]           | [Comment utilisée]             |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - [Liste des fichiers qui dépendent de celui-ci]
 * 
 * ============================================================
 */
```

---

## 2. Limites de fichier

- **Maximum 150 lignes** (hors commentaires et imports)
- Si plus → diviser en plusieurs fichiers
- Un fichier = une responsabilité

---

## 3. Structure des packages

```
com.beemancer/
├── core/           # Fondations universelles (utilisé partout)
├── common/         # Logique partagée client/serveur
├── client/         # Client uniquement
├── content/        # Features spécifiques
└── datagen/        # Génération de data
```

**Règle**: Si réutilisable ailleurs → `core/` ou `common/`

---

## 4. Nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Classe | PascalCase | `BeeHiveBlock` |
| Méthode | camelCase + verbe | `createBee()`, `canHarvest()` |
| Constante | SCREAMING_SNAKE | `MOD_ID`, `MAX_BEES` |
| Package | lowercase | `com.beemancer.content.hive` |
| Fichier registre | Beemancer[Type]s | `BeemancerBlocks` |

---

## 5. Imports

```java
// BON
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;

// MAUVAIS
import net.minecraft.world.level.block.*;
```

- Pas de wildcard `*`
- Grouper par origine (minecraft, neoforge, beemancer)

---

## 6. Dépendances universelles

Avant de créer un helper, vérifier s'il existe dans `core/util/`:
- `BeemancerConstants` — Constantes globales
- `BeemancerTags` — Références aux tags
- `MathHelper` — Fonctions math
- `NBTHelper` — Manipulation NBT
- `InventoryHelper` — Manipulation inventaires

Si nouveau helper utile partout → l'ajouter dans `core/util/`

---

## 7. Registres

Format d'ajout:

```java
// --- [CATÉGORIE] ---
public static final DeferredItem<Item> NOM_ITEM = ITEMS.register("nom_item",
    () -> new NomItem(new Item.Properties()));
```

Toujours:
1. Commenter la catégorie
2. Mettre à jour Structure.txt après ajout

---

## 8. Structure.txt

**Toujours mettre à jour après**:
- Création de fichier
- Ajout au registre
- Nouvelle dépendance universelle

Sections à modifier:
- Section 4: Tableaux registres
- Section 5: Features implémentées
- Section 9: Changelog

---

## 9. Références Create/Cobblemon

Avant d'implémenter une feature:
1. Chercher une feature similaire dans Create ou Cobblemon
2. Analyser leur implémentation
3. Adapter pour Beemancer (pas copier aveuglément)
4. Documenter la source d'inspiration