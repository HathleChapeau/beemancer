# CLAUDE.md — Beemancer

## Projet
- **Nom**: Beemancer
- **Version Minecraft**: 1.21.1
- **Type**: Mod Minecraft (NeoForge/Forge)
- **Références architecturales**: Create, Cobblemon

---

## Fichier Structure.txt — OBLIGATOIRE

### Avant chaque tâche
1. **Lire `Structure.txt`** pour connaître l'état actuel du projet
2. Identifier les fichiers existants réutilisables
3. Identifier les dépendances universelles disponibles
4. Vérifier les features déjà implémentées (éviter doublons)

### Après chaque implémentation
**OBLIGATOIRE**: Mettre à jour `Structure.txt` avec:

1. **Section 4 (Registres)** — Ajouter les nouveaux items/blocs/entités dans les tableaux
2. **Section 5 (Features implémentées)** — Documenter la feature complète:
   ```
   FEATURE: [Nom]
   Description: [...]
   Scripts créés: [...]
   Dépendances: [...]
   Registres modifiés: [...]
   Assets: [...]
   Référence: [...]
   ```
3. **Section 7 (Dépendances universelles)** — Si nouveau utilitaire créé
4. **Section 9 (Changelog)** — Entrée datée des modifications

### Format de mise à jour
```markdown
## Mise à jour Structure.txt

### Registres ajoutés
- BeemancerItems: +[item_id]
- BeemancerBlocks: +[block_id]

### Feature documentée
[Copier le bloc complet]

### Changelog
[DATE] - [Feature]
Ajouts: [liste]
```

---

## Règles Absolues

### 1. Modularité
- **Un script = une responsabilité**
- Maximum ~150 lignes par fichier (hors commentaires)
- Privilégier la composition à l'héritage
- Dépendances universelles > dépendances spécifiques

### 2. Code Complet — JAMAIS de TODO
**RÈGLE ABSOLUE**: Ne JAMAIS laisser de TODO, FIXME, ou code incomplet.
- Chaque script doit être 100% fonctionnel à la livraison
- Si une fonctionnalité est complexe, l'implémenter entièrement ou ne pas la commencer
- Si des informations manquent, demander AVANT de coder, pas après
- Les commentaires de type `// TODO: ...` sont INTERDITS
- Un code livré doit compiler ET fonctionner, sans exception

### 3. Commentaires Obligatoires
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

### 4. Structure de Packages
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
- **Lire Structure.txt** avant de commencer
- Créer les dépendances universelles d'abord
- Un commit logique par script
- **Mettre à jour Structure.txt** après chaque fichier créé

### Phase 5: Finalisation (OBLIGATOIRE)
Après chaque feature complétée:

```markdown
## Mise à jour Structure.txt effectuée

### Section 4 — Registres
[Items/Blocs/Entités ajoutés]

### Section 5 — Feature documentée
FEATURE: [Nom]
Description: [...]
Statut: [...]
Scripts créés: [...]
[...]

### Section 9 — Changelog
[DATE] - [Feature]
[Modifications]
```

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

- [ ] Structure.txt lu avant implémentation
- [ ] Bloc d'en-tête présent et complet
- [ ] Dépendances documentées
- [ ] Fichier < 150 lignes
- [ ] Une seule responsabilité
- [ ] Pas de code dupliqué (extraire en util si besoin)
- [ ] Imports organisés (pas de wildcard `*`)
- [ ] **ZÉRO TODO/FIXME** — Code 100% complet et fonctionnel
- [ ] Si espace vide demandé, mettre PLACEHOLDER
- [ ] **Structure.txt mis à jour** (registres, features, changelog)

---

## Ressources

### Fichiers projet
- **Structure.txt**: État actuel du projet, registres, features implémentées
- **CLAUDE.md**: Ce fichier — règles et processus

### Dépôts de référence
- Create: `https://github.com/Creators-of-Create/Create`
- Cobblemon: `https://gitlab.com/cable-mc/cobblemon`

### Documentation
- NeoForge Docs: `https://docs.neoforged.net/`
- Minecraft Wiki (technique): `https://minecraft.wiki/`

---

## Règle Anti-Inversion d'Intent

Quand l'utilisateur signale un bug ou un comportement manquant:
- **"X ne marche pas"** = CORRIGER X, pas le supprimer
- **"il manque X"** = AJOUTER X
- **"X fait Y au lieu de Z"** = CHANGER le comportement, garder la feature
- En cas de doute, **DEMANDER** avant de coder

**INTERDIT**: Supprimer une feature pour "corriger" un bug.

---

## Coordonnées 3D et Modèles

### Convention Coordonnées Minecraft
- Blockstate multipart: le modèle de base est orienté NORD (z=0)
- Les rotations y=90/180/270 réorientent vers E/S/W
- Les éléments d'un modèle JSON:
  - from/to en pixels (0-16 = 1 bloc)
  - Valeurs < 0 ou > 16 = dépasse du bloc
  - Pour un indicateur SUR le pipe, rester dans [0-16]

### Convention BEWLR (BlockEntityWithoutLevelRenderer)
- Le BEWLR ajoute ses propres transforms (translate, scale, flip)
- Quand un renderer externe (pedestal, etc.) appelle renderStatic():
  - Les transforms du BEWLR s'EMPILENT sur ceux du renderer
  - Toujours compenser les offsets du BEWLR
  - Pour une rotation sur soi-même: translate au pivot → rotate → translate inverse

---

## Vérification Avant Code

### Avant de modifier du code
1. **LIRE** le fichier complet, pas seulement la zone à modifier
2. **COMPRENDRE** le flux: qui appelle quoi, dans quel ordre
3. Si modification de multibloc/pattern: vérifier **TOUS** les fichiers qui référencent les positions (BlockEntity, Renderer, Pattern)
4. Si modification de rendu: comprendre la chaîne de transforms complète
5. Si correction de bug: reproduire mentalement le bug AVANT de coder

### Avant de répondre à un bug report
1. Relire le message utilisateur **2 fois**
2. Identifier: quel est le comportement ACTUEL?
3. Identifier: quel est le comportement VOULU?
4. Identifier: l'utilisateur veut-il **ajouter**, **modifier**, ou **supprimer**?
5. En cas de doute, DEMANDER

---

## Système de Particules — ParticleHelper (OBLIGATOIRE)

### Règle
**TOUJOURS** utiliser `ParticleHelper` (`core/util/ParticleHelper.java`) pour spawner des particules. **JAMAIS** appeler `level.addParticle()` ou `serverLevel.sendParticles()` directement dans le code métier.

### Quand améliorer ParticleHelper
Si un pattern de particules n'existe pas encore dans `ParticleHelper`:
1. **Ajouter la méthode** dans `ParticleHelper` (server-side dans la section `Génériques`, client-side dans la section `Client-side`)
2. **Documenter** la méthode avec Javadoc (paramètres, comportement)
3. **Utiliser** la nouvelle méthode depuis le code appelant

### API disponible

#### Patterns géométriques (server-side, `ServerLevel`)
| Méthode | Description |
|---------|-------------|
| `burst()` | Explosion de particules autour d'un point |
| `ring()` / `spawnRing()` | Cercle statique de particules |
| `orbitingRing()` | Cercle animé (rotation basée sur gameTime) |
| `spiral()` / `spawnSpiral()` | Spirale ascendante |
| `line()` / `spawnLine()` | Ligne entre deux points |
| `sphere()` / `spawnSphere()` | Répartition sphérique |
| `spawnParticles()` | Spawn générique avec spread/speed |
| `spawnWithMotion()` | Spawn avec vecteur de mouvement |

#### EffectType (presets thématiques)
`SUCCESS`, `FAILURE`, `MAGIC`, `HEAL`, `HONEY`, `SOUL`, `PORTAL`, `NATURE`, `FLAME`, `ELECTRIC`, `SCULK`

Utilisables avec les méthodes de haut niveau : `burst(level, center, EffectType, count)`, `ring(...)`, etc.

#### Client-side (`Level`)
| Méthode | Description |
|---------|-------------|
| `addParticle()` | Spawn client-side uniquement |
| `addAlwaysVisible()` | Spawn client-side visible à longue distance |

#### Résolution par ResourceLocation
Toutes les méthodes ont une surcharge acceptant un `ResourceLocation` au lieu d'un `ParticleOptions`.

### Bonnes pratiques
- **Server-side** (dans `serverTick`, événements) : utiliser les méthodes `ServerLevel` — visibles par tous les joueurs
- **Client-side** (dans renderers, si nécessaire) : utiliser `addParticle()` / `addAlwaysVisible()`
- **Custom particles** (`DustParticleOptions`, etc.) : passer directement en `ParticleOptions` aux méthodes génériques
- **Throttling** : toujours limiter la fréquence (ex: `gameTime % N == 0`) pour éviter le spam

---

## Render Types — RÈGLE ABSOLUE

**`translucent` et `cutout` sont deux options de rendering DIFFÉRENTES qui ne couvrent PAS les mêmes besoins. Ils ne sont PAS interchangeables.**

- **`cutout`** : pixels 100% opaques ou 100% transparents (verre classique, feuilles, etc.)
- **`translucent`** : pixels semi-transparents avec alpha blending (vitre teintée, miel, etc.)

**INTERDIT**: Si un problème de rendu survient avec `translucent`, la solution n'est JAMAIS de passer en `cutout`. Chercher la vraie cause (UVs, blockstate, face culling, modèle). Si la solution n'est pas connue, **dire qu'on ne sait pas** et **chercher sur internet** plutôt que d'inventer un fix incorrect.

---

## Notes Importantes

1. **Structure.txt est la source de vérité** — Toujours le consulter avant d'implémenter, toujours le mettre à jour après.

2. **Toujours chercher dans Create/Cobblemon avant d'inventer** — Ces mods ont résolu la plupart des problèmes courants.

3. **Dépendances universelles prioritaires** — Si un helper peut servir ailleurs, le mettre dans `core/util/`.

4. **Pas d'optimisation prématurée** — Code lisible d'abord, optimiser si profilage le justifie.

5. **Demander clarification si doute** — Mieux vaut une question que du code inutile.

6. **Ne pas inventer de solutions** — Si la cause d'un bug n'est pas claire, chercher sur internet (vanilla models, Mojira bug tracker, forums NeoForge) AVANT de proposer un fix. Admettre quand on ne sait pas.
