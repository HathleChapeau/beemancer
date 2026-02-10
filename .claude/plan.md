# Plan: Systeme de Crafting Automatique (Crafter)

## Resume Feature

Le systeme de crafting automatique permet aux joueurs de programmer des recettes de crafting (vanilla crafting table) et des recettes "machine" (non-vanilla) sur des **Crafting Papers**, de les charger dans un **Crafter** connecte au reseau de stockage, et de lancer des crafts automatiques via les abeilles de livraison. Le systeme gere les crafts imbriques (le crafter fait les crafts intermediaires en sequence) et coordonne avec les machines externes pour les crafts "machine".

---

## Phase 0: Crafting Paper (Item simple + recette)

### Items a creer
| Fichier | Responsabilite |
|---------|---------------|
| `common/item/crafting/CraftingPaperItem.java` | Item Crafting Paper standard (stackable 64) |

### Data Components a creer
| Fichier | Responsabilite |
|---------|---------------|
| `common/data/CraftingPaperData.java` | Record immuable stockant les donnees du craft (mode, inputs, output, craftId UUID) |
| `core/registry/BeemancerDataComponents.java` | Registre des DataComponentType pour le mod |

**CraftingPaperData** contient:
- `mode`: CRAFT ou MACHINE
- `inputs`: List<CraftSlotEntry> (item + slot position pour CRAFT, item + count pour MACHINE)
- `output`: ItemStack + count
- `craftId`: UUID unique identifiant ce paper

**CraftSlotEntry** record:
- `item`: ItemStack (count=1)
- `slot`: int (position 0-8 pour mode CRAFT, index pour MACHINE)
- `count`: int (toujours 1 pour CRAFT, variable pour MACHINE)

### Variantes de Crafting Paper
Le meme item `CraftingPaperItem` avec DataComponent different:
- **Crafting Paper** (vierge, pas de DataComponent)
- **Crafting Paper inscrit** (mode CRAFT, contient la recette 3x3)
- **Part Crafting Paper Input** (mode MACHINE, type INPUT)
- **Part Crafting Paper Output** (mode MACHINE, type OUTPUT)

Le shift-hover affiche les donnees (via `appendHoverText`).

### Recette
```json
crafting_paper.json: shaped recipe
  " S "
  "SPS"
  " S "
S = minecraft:stick, P = beemancer:honeyed_planks
→ Result: beemancer:crafting_paper x4
```

### Registres a modifier
- `BeemancerItems.java`: +CRAFTING_PAPER
- `BeemancerDataComponents.java`: +CRAFTING_PAPER_DATA (nouveau fichier)
- `BeemancerCreativeTabs.java`: ajouter Crafting Paper
- `Beemancer.java`: enregistrer BeemancerDataComponents

---

## Phase 1: Crafter Block + BlockEntity (base)

### Fichiers a creer
| Fichier | Responsabilite |
|---------|---------------|
| `common/block/crafting/CrafterBlock.java` | Bloc simple avec facing, ouvre le menu |
| `common/blockentity/crafting/CrafterBlockEntity.java` | BlockEntity: inventaire crafting papers, craft buffer, lien controller, logique craft |

### CrafterBlockEntity contient:
- `controllerPos`: BlockPos (lie a 1 seul controller, pas de relay)
- `paperSlot`: ItemStackHandler(16) - stockage des crafting papers inscrits
- `craftGrid`: ItemStackHandler(9) - ghost items de la table de craft (mode craft)
- `resultSlot`: ItemStack - resultat du craft (ghost, non-interactif)
- `inscribeOutputSlot`: ItemStackHandler(1) - slot output inscribe
- `machineInputSlots`: List<MachineSlotEntry> - liste dynamique des inputs machine
- `machineOutputItem/Count`: ItemStack + int
- `mode`: enum CRAFT/MACHINE
- `currentCraftTask`: CraftTask en cours (null si idle)
- `craftBuffer`: ItemStackHandler(27) - buffer interne pour items en transit
- `active`: boolean

### Contraintes reseau:
- 1 crafter = 1 controller maximum (pas via relay)
- 1 controller = 1 crafter maximum
- Validation dans `StorageControllerBlockEntity.linkCrafter()` et `CrafterBlock.use()`

### Registres a modifier:
- `BeemancerBlocks.java`: +CRAFTER
- `BeemancerItems.java`: +CRAFTER (BlockItem)
- `BeemancerBlockEntities.java`: +CRAFTER
- `StorageNetworkRegistry.NetworkBlockType`: +CRAFTER

---

## Phase 2: Menu + Screen du Crafter

### Fichiers a creer
| Fichier | Responsabilite |
|---------|---------------|
| `common/menu/crafting/CrafterMenu.java` | Menu avec slots: paper reserve, craft grid (ghost), result, inscribe output, toggle mode/inscribe |
| `client/gui/screen/CrafterScreen.java` | Ecran avec toggle CRAFT/MACHINE, grille 3x3, bouton inscribe, zone scrollable machine |
| `core/network/packets/CrafterActionPacket.java` | Packet C2S multi-action (toggle mode, set ghost, inscribe, machine add/remove/edit) |

### Mode CRAFT (GUI):
- 1 slot pour reserve de crafting paper vierges (en haut a gauche)
- Grille 3x3 (ghost slots): clic gauche avec item en main → ghost image apparait (sans consommer l'item)
- 1 slot resultat (non-interactif, affiche le resultat si recette valide)
- Bouton Inscribe (grise si pas de recette valide ou pas de paper vierge)
- 1 slot output inscribe (pour recuperer le crafting paper inscrit)
- En dessous: inventaire du joueur

### Mode MACHINE (GUI):
- Zone scrollable avec bouton "+"
- Chaque ligne input: [slot ghost] [bouton -] [champ count] [bouton +] [bouton poubelle]
- Zone resultat: [slot ghost] [bouton -] [champ count] [bouton +]
- Bouton Inscribe → cree 2 Part Crafting Paper (input + output) dans 2 slots
- 1 slot pour reserve de crafting paper vierges
- 2 slots output inscribe

### Validation Inscribe (mode CRAFT):
1. Verifier qu'au moins 1 ghost slot non-vide
2. Construire un CraftingContainer temporaire avec les ghost items
3. Tester via `level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftInput, level)`
4. Si recette valide → resultat dans resultSlot, Inscribe debloque
5. Au clic Inscribe: consomme 1 crafting paper vierge → cree 1 crafting paper avec CraftingPaperData

### Validation Inscribe (mode MACHINE):
1. Au moins 1 input avec item + count > 0
2. Un output avec item + count > 0
3. Au clic Inscribe: consomme 2 crafting papers → cree Part Input + Part Output

### Registres a modifier:
- `BeemancerMenus.java`: +CRAFTER
- `ClientSetup.java`: register screen
- `BeemancerNetwork.java`: +CrafterActionPacket

---

## Phase 3: Integration Import/Export avec Part Crafting Paper

### Modifications a faire

**InterfaceFilter.java** - nouveau FilterMode:
- Ajouter `CRAFT` au FilterMode enum
- Quand mode CRAFT: le filtre contient un Part Crafting Paper (input ou output)
- Le filtre match les items du Part Crafting Paper

**NetworkInterfaceBlockEntity.java** - toggle craft:
- Nouveau boolean `craftMode`
- Quand toggle ON: les filtres existants sont rendus au joueur, mode passe en CRAFT
- Le slot de filtre accepte UNIQUEMENT les Part Crafting Paper
- Import + craftMode = accepte seulement Part Crafting Paper INPUT
- Export + craftMode = accepte seulement Part Crafting Paper OUTPUT

**ImportInterfaceBlockEntity.java** - comportement craft:
- Si craftMode: au lieu d'importer vers l'inventaire adjacent, importe vers le Crafter lie (via le craftId du Part Paper)
- Le crafter est identifie par le controller commun au Paper et a l'interface

**ExportInterfaceBlockEntity.java** - comportement craft:
- Si craftMode: au lieu d'exporter depuis l'inventaire adjacent, exporte depuis le Crafter (output du craft)
- Attend que le craft soit complete avant d'exporter

### Packet:
- Modifier `InterfaceActionPacket` pour ajouter `ACTION_TOGGLE_CRAFT_MODE`

---

## Phase 4: CraftTask + CraftManager (logique orchestration)

### Fichiers a creer
| Fichier | Responsabilite |
|---------|---------------|
| `common/block/crafting/CraftTask.java` | Tache de craft unitaire ou imbriquee avec etats et timeout |
| `common/blockentity/crafting/CraftManager.java` | Manager gerant la file de CraftTasks, resolution crafts imbriques, timeout |

### CraftTask contient:
- `taskId`: UUID
- `craftingPaperData`: CraftingPaperData (la recette)
- `state`: PENDING_RESOURCES / CRAFTING / WAITING_MACHINE / COMPLETED / CANCELLED
- `requiredPrimaryResources`: Map<ItemStack, Integer> - ressources primaires calculees
- `deliveredResources`: Map<ItemStack, Integer> - ressources deja livrees au crafter
- `subTasks`: List<CraftTask> - crafts intermediaires (imbriques)
- `parentTask`: CraftTask (null si racine)
- `lastDeliveryTick`: long - dernier tick ou un item a ete livre (pour timeout 2min)
- `createdTick`: long
- `machineExportDone`: boolean - pour MACHINE: les items ont-ils ete envoyes a la machine?
- `machineImportDone`: boolean - pour MACHINE: les items ont-ils ete recus de la machine?

### Resolution des crafts imbriques:
Quand un craft est demande:
1. Lister les ingredients de la recette
2. Pour chaque ingredient, verifier s'il est disponible dans le reseau (ressource primaire)
3. Si non disponible ET qu'un crafting paper inscrit dans le crafter peut le produire → creer un sous-CraftTask
4. Recursion: les sous-crafts peuvent eux-memes avoir des sous-crafts
5. Calculer l'ensemble des ressources primaires au plus bas niveau
6. Toutes les ressources primaires doivent etre livrees AVANT de commencer la sequence

### Ordre d'execution:
1. Abeilles apportent TOUTES les ressources primaires au crafter (craftBuffer)
2. Pour les crafts machine intermediaires: le crafter fait les crafts normaux necessaires
3. Puis les items sont envoyes aux machines (via export interface craftMode)
4. Attend les retours machine (via import interface craftMode)
5. Continue les crafts normaux avec les retours machine
6. Craft final → resultat dans craftBuffer
7. Abeille ramene le resultat au depot du controller

### Timeout (2 minutes):
- `lastDeliveryTick` est mis a jour chaque fois qu'un item est livre au crafter pour CE craft
- Si `gameTime - lastDeliveryTick > 2400` (2 minutes) → CraftTask annulee
- Si fait partie d'un craft imbrique: annule TOUS les crafts lies (parent + siblings)
- Items deja dans le craftBuffer sont renvoyes au reseau via une abeille de livraison

### Le crafter ne traite qu'un craft a la fois:
- Si un craft est en cours, les abeilles suivantes attendent devant le crafter
- `CrafterBlockEntity.isAvailable()` retourne false si craft en cours

### Capacite bypass:
- Si le craft produit plus d'items que la capacite max de l'abeille, l'abeille bypass sa limite et transporte tout d'un coup

---

## Phase 5: Integration Controller + Delivery System

### Modifications StorageControllerBlockEntity:
- `linkCrafter(BlockPos)` / `unlinkCrafter(BlockPos)`
- `getCrafterPos()`: BlockPos ou null
- Validation: max 1 crafter par controller
- Validation dans `validateNetworkBlocks()`: verifier le crafter aussi
- `getCrafter()`: helper pour obtenir le CrafterBlockEntity

### Modifications StorageNetworkRegistry:
- Ajouter `CRAFTER` a `NetworkBlockType`
- `getAllCrafters()`: methode helper

### Modifications StorageDeliveryManager:
- Supporter les CraftDeliveryTask (source = coffre, dest = crafter)
- Supporter le retour des items du crafter au depot (source = crafter, dest = coffre)
- La bee attend devant le crafter si un craft est en cours (`WAIT_AT_CRAFTER` state)

### Modifications DeliveryTask:
- Ajouter `craftTaskId`: UUID (lien vers CraftTask)
- Ajouter `isCraftReturn`: boolean (true si l'abeille ramene le resultat)

### Modifications DeliveryPhaseGoal:
- Phase `WAIT_AT_CRAFTER`: l'abeille arrive au crafter, attend que le craft soit termine
- Quand craft termine: l'abeille prend le resultat et rentre au depot
- Si l'abeille porte plus que sa capacite (craft bypass), elle transporte quand meme

---

## Phase 6: Assets (modeles, textures, blockstates, lang, loot)

### Fichiers a creer:
- `blockstates/crafter.json`
- `models/block/machines/crafter.json` (style iron_foundation-like)
- `models/item/crafter.json`
- `models/item/crafting_paper.json`
- `textures/block/crafter_front.png` (PLACEHOLDER)
- `textures/block/crafter_side.png` (PLACEHOLDER)
- `textures/block/crafter_top.png` (PLACEHOLDER)
- `textures/item/crafting_paper.png` (PLACEHOLDER)
- `data/beemancer/recipe/crafting/crafting_paper.json`
- `data/beemancer/loot_table/blocks/crafter.json`
- `lang/en_us.json` + `lang/fr_fr.json`: ajouts traductions

---

## Ordre d'implementation (phases)

### Batch 1 - Fondations (Phase 0 + registres)
1. `BeemancerDataComponents.java` (nouveau fichier registre)
2. `CraftingPaperData.java` (record immutable)
3. `CraftingPaperItem.java` (item avec tooltip)
4. Registrer dans BeemancerItems, Beemancer.java, CreativeTabs
5. Recette crafting_paper.json
6. Assets item crafting_paper

### Batch 2 - Crafter Block (Phase 1 + 2)
7. `CrafterBlock.java`
8. `CrafterBlockEntity.java` (base: slots, controller link, NBT)
9. `CrafterMenu.java`
10. `CrafterScreen.java` (mode CRAFT: ghost grid, result, inscribe)
11. `CrafterActionPacket.java`
12. Registrer bloc, BE, menu, packet, screen, assets

### Batch 3 - Mode Machine (Phase 2 suite)
13. Etendre CrafterScreen pour mode MACHINE (scrollable, +/-, etc.)
14. Etendre CrafterActionPacket pour actions MACHINE
15. Logique Inscribe mode MACHINE → Part Crafting Paper

### Batch 4 - Integration Import/Export (Phase 3)
16. Modifier InterfaceFilter + NetworkInterfaceBlockEntity (craftMode toggle)
17. Modifier InterfaceActionPacket (ACTION_TOGGLE_CRAFT_MODE)
18. Modifier NetworkInterfaceScreen (toggle craft)
19. Adapter Import/ExportInterfaceBlockEntity pour le flux craft

### Batch 5 - Orchestration (Phase 4 + 5)
20. `CraftTask.java` (etats, timeout, imbrication)
21. `CraftManager.java` (resolution, sequencing, timeout)
22. Modifier StorageControllerBlockEntity (link crafter, delegations)
23. Modifier StorageNetworkRegistry (CRAFTER type)
24. Modifier DeliveryTask + DeliveryPhaseGoal (WAIT_AT_CRAFTER, craft bypass)
25. Modifier StorageDeliveryManager (craft delivery support)

### Batch 6 - Finalisation (Phase 6)
26. Assets complets (textures PLACEHOLDER, blockstates, models, lang, loot, recette)
27. Mise a jour Structure.txt

---

## Estimation: ~25-30 fichiers crees/modifies
## Fichiers critiques: CraftTask.java, CraftManager.java, CrafterBlockEntity.java, CrafterMenu.java, CrafterScreen.java
