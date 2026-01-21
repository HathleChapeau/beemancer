---
name: neoforge-patterns
description: Patterns NeoForge 1.21.1 pour blocs, items, entities, registres. Utiliser pour toute implémentation de contenu Minecraft.
allowed-tools: Read, Grep, Glob, Bash(./gradlew:*)
---

# Patterns NeoForge 1.21.1

## Quand utiliser
- Création de blocs, items, entities
- Enregistrement dans les registres
- Création de BlockEntities
- Gestion des capabilities
- Data generation

---

## Pattern: DeferredRegister

```java
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(Registries.ITEM, MOD_ID);
    
    public static final DeferredItem<Item> EXAMPLE = ITEMS.register("example",
        () -> new Item(new Item.Properties()));
    
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
```

---

## Pattern: Block simple

```java
public class ExampleBlock extends Block {
    public ExampleBlock(Properties props) {
        super(props);
    }
}
```

---

## Pattern: Block avec BlockEntity

```java
// ExampleBlock.java
public class ExampleBlock extends BaseEntityBlock {
    public static final MapCodec<ExampleBlock> CODEC = simpleCodec(ExampleBlock::new);
    
    public ExampleBlock(Properties props) {
        super(props);
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExampleBlockEntity(pos, state);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
```

```java
// ExampleBlockEntity.java
public class ExampleBlockEntity extends BlockEntity {
    public ExampleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXAMPLE.get(), pos, state);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Sauvegarder données
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Charger données
    }
}
```

---

## Pattern: BlockEntity avec tick

```java
public class TickingBlockEntity extends BlockEntity {
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (be instanceof TickingBlockEntity tickingBe) {
            tickingBe.serverTick();
        }
    }
    
    private void serverTick() {
        // Logique de tick
    }
}

// Dans le Block:
@Override
public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TICKING.get(), TickingBlockEntity::tick);
}
```

---

## Pattern: Item avec comportement custom

```java
public class ExampleItem extends Item {
    public ExampleItem(Properties props) {
        super(props);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Logique d'utilisation
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
```

---

## Pattern: Enregistrement Creative Tab

```java
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MOD_ID + ".main"))
            .icon(() -> ModItems.EXAMPLE.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.EXAMPLE.get());
                output.accept(ModBlocks.EXAMPLE.get());
            })
            .build());
}
```

---

## Convention Beemancer

Toujours séparer:
1. `XxxBlock.java` — Le bloc
2. `XxxBlockEntity.java` — Le BlockEntity (si applicable)
3. `XxxRenderer.java` — Le renderer client (si applicable)