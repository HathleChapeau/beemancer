---
name: create-reference
description: Patterns et architecture du mod Create. Référence pour implémenter des features similaires (machines, processing, animations).
allowed-tools: Read, Grep, Glob
---

# Référence Create

## Quand utiliser
- Implémentation de machines/blocs interactifs
- Systèmes de processing
- Animations et renderers
- Gestion d'inventaires complexes
- Systèmes modulaires

---

## Architecture Create

```
create/
├── AllBlocks.java              # Registre centralisé
├── AllBlockEntityTypes.java    # Registre BlockEntities
├── AllItems.java               # Registre Items
│
├── content/                    # Contenu par feature
│   ├── contraptions/           # Machines mobiles
│   ├── processing/             # Machines de traitement
│   ├── kinetics/               # Énergie rotative
│   └── logistics/              # Transport d'items
│
├── foundation/                 # Base réutilisable
│   ├── block/                  # Blocs de base
│   ├── blockEntity/            # SmartBlockEntity
│   ├── item/                   # Items de base
│   └── utility/                # Helpers
│
└── infrastructure/             # Config, network, etc.
```

---

## Pattern: SmartBlockEntity (Create)

Create utilise un système de "behaviours" modulaires:

```java
// Concept simplifié
public class SmartBlockEntity extends BlockEntity {
    private List<BlockEntityBehaviour> behaviours = new ArrayList<>();
    
    public void addBehaviour(BlockEntityBehaviour behaviour) {
        behaviours.add(behaviour);
    }
    
    public <T extends BlockEntityBehaviour> T getBehaviour(Class<T> type) {
        // Retourne le behaviour du type demandé
    }
}
```

**Avantages**:
- Composition > Héritage
- Réutilisation facile
- Testabilité

---

## Pattern: Registre centralisé (Create)

Create utilise des classes `All*` pour centraliser:

```java
public class AllBlocks {
    public static final BlockEntry<ExampleBlock> EXAMPLE = 
        REGISTRATE.block("example", ExampleBlock::new)
            .properties(p -> p.strength(2.0f))
            .blockstate((ctx, prov) -> /* ... */)
            .item()
            .build()
            .register();
}
```

**Pour Beemancer**: Adapter en `Beemancer*` avec DeferredRegister standard.

---

## Pattern: Processing Recipe (Create)

Create définit des types de recettes custom:

```java
public class ProcessingRecipe extends Recipe<RecipeWrapper> {
    protected final List<ProcessingOutput> results;
    protected final int processingDuration;
    // ...
}
```

**Éléments clés**:
- Serializer custom
- RecipeType enregistré
- Integration JEI/REI

---

## Patterns à adapter pour Beemancer

| Create | Beemancer |
|--------|-----------|
| SmartBlockEntity | BaseBeemancerBlockEntity |
| BlockEntityBehaviour | Capability ou interface |
| AllBlocks | BeemancerBlocks |
| Registrate | DeferredRegister |

---

## Fichiers Create utiles à étudier

Pour machines:
- `BasinBlockEntity.java`
- `MechanicalCrafterBlockEntity.java`

Pour inventaires:
- `SmartInventory.java`

Pour rendering:
- `KineticBlockEntityRenderer.java`

Pour processing:
- `ProcessingRecipe.java`
- `SequencedAssemblyRecipe.java`