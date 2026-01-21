# CLAUDE.md — Beemancer

## Projet
- **Nom**: Beemancer
- **Version Minecraft**: 1.21.1
- **Type**: Mod Minecraft (NeoForge/Forge)
- **Références architecturales**: Create, Cobblemon

---

## Règles Absolues

### 1. Modularité
- **Un script = une responsabilité**
- Maximum ~150 lignes par fichier (hors commentaires)
- Privilégier la composition à l'héritage
- Dépendances universelles > dépendances spécifiques

### 2. Commentaires Obligatoires
Chaque fichier commence par un bloc d'en-tête:

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
 * | ExempleRegistry     | Accès registres      | Récupération des items/blocs   |
 * | BeemancerConfig     | Configuration mod    | Paramètres ajustables          |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - [Liste des fichiers qui dépendent de celui-ci]
 * 
 * ============================================================
 */
```

### 3. Structure de Packages
```
com.beemancer/
├── Beemancer.java                 # Point d'entrée unique
├── core/                          # Fondations universelles
│   ├── registry/                  # Registres (items, blocs, entities)
│   ├── config/                    # Configuration
│   ├── network/                   # Packets réseau
│   └── util/                      # Utilitaires partagés
├── common/                        # Logique serveur/client partagée
│   ├── block/                     # Blocs et BlockEntities
│   ├── item/                      # Items
│   ├── entity/                    # Entités
│   ├── capability/                # Capabilities (énergie, inventaire, etc.)
│   └── data/                      # DataComponents, codecs
├── client/                        # Client uniquement
│   ├── renderer/                  # Renderers
│   ├── model/                     # Modèles
│   ├── gui/                       # Screens et menus
│   └── particle/                  # Particules
├── content/                       # Contenu spécifique au mod
│   └── [feature]/                 # Un sous-package par feature majeure
└── datagen/                       # Générateurs de data
```

---

## Processus d'Implémentation

### Phase 1: Compréhension
Avant toute implémentation, fournir:

```markdown
## Résumé Feature: [Nom]

### Besoin exprimé
[Reformulation du besoin en 2-3 phrases]

### Questions de clarification
- [Si ambiguïté]

### Validation requise: OUI/NON
```

### Phase 2: Recherche Références
Pour chaque feature, documenter:

```markdown
## Analyse Références: [Nom Feature]

### Feature similaire dans Create/Cobblemon
- **Mod**: [Create/Cobblemon]
- **Feature**: [Nom de la feature similaire]
- **Localisation**: [Chemin des fichiers sources]

### Scripts concernés
| Fichier | Rôle | Dépendances clés |
|---------|------|------------------|
| [path]  | [x]  | [deps]           |

### Pattern d'implémentation utilisé
[Description technique: comment ils ont résolu le problème]

### Adaptation pour Beemancer
[Ce qu'on garde, ce qu'on adapte]
```

### Phase 3: Architecture Technique
Fournir avant le code:

```markdown
## Architecture: [Nom Feature]

### Scripts à créer
| Fichier | Responsabilité | Priorité |
|---------|----------------|----------|
| [path]  | [rôle]         | [1-3]    |

### Scripts existants à modifier
| Fichier | Modification | Impact |
|---------|--------------|--------|
| [path]  | [quoi]       | [où]   |

### Hiérarchie des appels
```
EntryPoint
└── FeatureManager
    ├── ComponentA.method()
    └── ComponentB.method()
        └── UtilHelper.helper()
```

### Dépendances
| Type | Nom | Existe | À créer |
|------|-----|--------|---------|
| Registry | BeeRegistry | ❌ | ✅ |
| Util | MathHelper | ✅ | ❌ |

### Ordre d'implémentation
1. [Dépendances universelles]
2. [Core logic]
3. [Integration]
4. [Tests]
```

### Phase 4: Implémentation
- Créer les dépendances universelles d'abord
- Un commit logique par script
- Tests unitaires si applicable

---

## Conventions de Code

### Nommage
```java
// Classes: PascalCase descriptif
public class BeeEntityRenderer {}

// Méthodes: camelCase, verbe d'action
public void registerBee() {}
public boolean canHarvest() {}
public Bee createBee() {}

// Constantes: SCREAMING_SNAKE_CASE
public static final String MOD_ID = "beemancer";

// Registres: suffixe _REGISTRY ou _REGISTER
public static final DeferredRegister<Item> ITEMS = ...;
```

### Patterns à suivre (inspirés Create/Cobblemon)

#### Registres (style Create)
```java
// Un fichier par type de registre
// core/registry/BeemancerItems.java
public class BeemancerItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(Registries.ITEM, Beemancer.MOD_ID);
    
    // Regroupement logique avec commentaires
    // --- Outils ---
    public static final DeferredItem<Item> BEE_STAFF = ITEMS.register(...);
    
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
```

#### BlockEntity (style Create)
```java
// Séparer: Block, BlockEntity, Renderer
// common/block/hive/ApiaryBlock.java
// common/block/hive/ApiaryBlockEntity.java  
// client/renderer/ApiaryRenderer.java
```

#### Capabilities (style Cobblemon)
```java
// Interface + Implémentation séparées
// common/capability/IBeeHolder.java
// common/capability/BeeHolderCapability.java
```

---

## Templates Réutilisables

### Template Block Simple
Référence: Create `CasingBlock`

### Template BlockEntity avec Inventaire
Référence: Create `BasinBlockEntity`

### Template Entity Custom
Référence: Cobblemon `PokemonEntity`

### Template Capability
Référence: Cobblemon capabilities système

### Template Packet Réseau
Référence: Create `AllPackets`

---

## Checklist Avant Commit

- [ ] Bloc d'en-tête présent et complet
- [ ] Dépendances documentées
- [ ] Fichier < 150 lignes
- [ ] Une seule responsabilité
- [ ] Pas de code dupliqué (extraire en util si besoin)
- [ ] Imports organisés (pas de wildcard `*`)
- [ ] Pas de TODO non documenté

---

## Ressources

### Dépôts de référence
- Create: `https://github.com/Creators-of-Create/Create`
- Cobblemon: `https://gitlab.com/cable-mc/cobblemon`

### Documentation
- NeoForge Docs: `https://docs.neoforged.net/`
- Minecraft Wiki (technique): `https://minecraft.wiki/`

---

## Notes Importantes

1. **Toujours chercher dans Create/Cobblemon avant d'inventer** — Ces mods ont résolu la plupart des problèmes courants.

2. **Dépendances universelles prioritaires** — Si un helper peut servir ailleurs, le mettre dans `core/util/`.

3. **Pas d'optimisation prématurée** — Code lisible d'abord, optimiser si profilage le justifie.

4. **Demander clarification si doute** — Mieux vaut une question que du code inutile.
