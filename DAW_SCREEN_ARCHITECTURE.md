# DAW Screen Architecture - Design Document

**Date**: 2026-03-04
**Status**: Ready for Implementation
**Reference Patterns**: StorageTerminalScreen, ResonatorScreen, CodexScreen

---

## 📋 Overview

**Goal**: Create a Digital Audio Workstation (DAW) interface in Minecraft for the Apica mod.

**Key Findings**:
- ✅ WaveformRenderer already exists (audio rendering pattern)
- ✅ ResonationNoteRenderer exists (musical notes pattern)
- ✅ StorageTerminalScreen provides multi-tab pattern (270×230)
- ✅ AbstractApicaScreen reduces boilerplate by 90%
- ✅ GuiRenderHelper has 40+ ready-to-use rendering methods

---

## 🏗️ Architecture Stack

```
┌─────────────────────────────────────────────┐
│ DawScreen extends AbstractApicaScreen        │ ← GUI Rendering
├─────────────────────────────────────────────┤
│ DawMenu extends ApicaMenu                   │ ← Logic + Data Sync
├─────────────────────────────────────────────┤
│ DawBlockEntity implements BlockEntity       │ ← Persistance + Ticks
├─────────────────────────────────────────────┤
│ DawBlock extends BaseEntityBlock            │ ← Game Integration
└─────────────────────────────────────────────┘
```

---

## 📐 Layout Design

### Target Dimensions
```
Total: 270×230 pixels (matches StorageTerminalScreen)

┌─────────────────────────────────────┐
│ [Piano] [Drums] [Effects] [Settings]│ ← Top Tabs (using GuiRenderHelper.renderTabBar)
├─────────────────────────────────────┤
│                                     │
│  PIANO ROLL / WAVEFORM DISPLAY      │ ← 270×100px (main content area)
│  (Custom rendering zone)            │
│                                     │
├─────────────────────────────────────┤
│ Minecraft Player Inventory (9×3)    │ ← Standard (handled by AbstractApicaScreen)
│ Hotbar (9×1)                        │
└─────────────────────────────────────┘
```

### Tab Content Areas

#### Piano Tab (Default)
```
┌────────────────────────────────────┐
│ [Waveform/Grid View]               │
│ ┌────────────────────────────────┐ │
│ │ VERTICAL SCALE (Octaves 0-8)   │ │ ← Use NotchedGaugeWidget pattern
│ │ ┌──────────────────────────┐  │ │
│ │ │ PIANO ROLL               │  │ │
│ │ │ (Notes as rectangles)    │  │ │
│ │ │ (Timeline horizontal)    │  │ │
│ │ └──────────────────────────┘  │ │
│ └────────────────────────────────┘ │
└────────────────────────────────────┘
```

#### Drums Tab
```
┌────────────────────────────────────┐
│ [Drum Pattern Grid]                │
│ ┌────────────────────────────────┐ │
│ │ Drum Tracks (Kick, Hat, etc)   │ │
│ │ Step Sequencer (16 steps)      │ │
│ └────────────────────────────────┘ │
└────────────────────────────────────┘
```

#### Effects Tab
```
┌────────────────────────────────────┐
│ [Effect Chains]                    │
│ ┌────────────────────────────────┐ │
│ │ Reverb | Delay | EQ Sliders    │ │
│ │ (Knobs using GuiRenderHelper)  │ │
│ └────────────────────────────────┘ │
└────────────────────────────────────┘
```

#### Settings Tab
```
┌────────────────────────────────────┐
│ [Configuration]                    │
│ ┌────────────────────────────────┐ │
│ │ BPM: [____] | Scale: [Dropdown]│ │
│ │ Master Volume: [Bar]           │ │
│ └────────────────────────────────┘ │
└────────────────────────────────────┘
```

---

## 🎨 Color Scheme

**Primary Palette** (based on Resonator + Apica standards):

```
Background:     0xFF1A1A2E  (Dark blue, Resonator style)
Grid:           0xFF333333  (Dark gray)
Grid Lines:     0xFF555555  (Medium gray, faint)
Inactive Notes: 0xFF2A2A4E  (Darker blue)
Active Notes:   0xFF00FF88  (Bright green - active/selected)
Secondary:      0xFF0099FF  (Bright blue - alternate)
Tertiary:       0xFF6666FF  (Purple - effects/aux)
Text:           0xFFDDDDDD  (Light gray)
Hover:          0xFFFFFF00  (Yellow highlight)
```

**Fallback to Honey theme** if dark theme too moody:
```
Background:     0xFFF3E1BB  (Codex cream, like ResonatorScreen)
Grid:           0xFF8B8B8B  (Apica gray)
Active:         0xFFE8A317  (Honey gold)
Text:           0xFF404040  (Dark gray)
```

---

## 🎵 Rendering Components

### 1. Waveform Display
**Library**: WaveformRenderer (already exists!)
```java
// In DawScreen.renderPianoTab()
WaveformRenderer.renderWaveform(g, x + 20, y + 10, 230, 50,
    frequencyData, amplitudeData);
```

### 2. Piano Roll Grid
```java
// Octave scale (0-8) = 8 × 12 notes = 96 possible pitches
int octaves = 8;
int notesPerOctave = 12;
int noteHeight = (height - 20) / (octaves * notesPerOctave);

for (int octave = 0; octave < octaves; octave++) {
    for (int note = 0; note < notesPerOctave; note++) {
        int noteY = y + octave * notesPerOctave * noteHeight + note * noteHeight;

        // Render grid line
        if (note == 0 || note == 5) { // C and G are thicker
            g.fill(x, noteY, x + width, noteY + 1, 0xFF666666);
        } else {
            g.fill(x, noteY, x + width, noteY + 1, 0xFF333333);
        }

        // Render note if exists
        if (hasNote(octave, note)) {
            int noteDuration = getNoteLength(octave, note);
            g.fill(x + 10, noteY + 1, x + 10 + noteDuration, noteY + noteHeight - 1,
                   isSelected(octave, note) ? 0xFF00FF88 : 0xFF0099FF);
        }
    }
}

// Render timeline (vertical lines)
for (int beat = 0; beat < numBeats; beat++) {
    int beatX = x + (beat * beatsPerPixel);
    g.fill(beatX, y, beatX + 1, y + height, 0xFF555555);
}
```

### 3. Knobs (Effects Tab)
```java
// Use GuiRenderHelper pattern + custom rotation
public void renderKnob(GuiGraphics g, int x, int y, float value) {
    // value = 0.0 → 1.0
    g.pose().pushPose();
    g.pose().translate(x + 8, y + 8, 0);
    g.pose().mulPose(Axis.ZP.rotationDegrees(value * 270 - 135)); // -135 to +135

    // Draw knob shape
    g.fill(-2, -6, 2, -4, 0xFFE8A317); // Indicator

    // Draw background circle
    drawCircle(g, x, y, 8, 0xFF333333);

    g.pose().popPose();
}
```

### 4. Sliders (Volume, BPM)
```java
// Use GuiRenderHelper.renderProgressBar()
GuiRenderHelper.renderProgressBar(g, x, y, 100, 8, volumeRatio,
    0xFFE8A317, 0xFFFFD700); // Gold gradient
```

---

## 🔄 Data Flow

### ClientSide ↔ ServerSide Sync

```
DawBlockEntity (server)
    ├─ ContainerData[] (8 values)
    │   ├─ [0] activeTab (int 0-3)
    │   ├─ [1] BPM (int 60-180)
    │   ├─ [2] masterVolume (int 0-100)
    │   ├─ [3] selectedNote (int 0-95)
    │   ├─ [4] isPlaying (int 0-1)
    │   ├─ [5-7] reserved
    │
    └─ nbt save/load:
        ├─ notes[] (list of NoteData)
        │   ├─ pitch (int 0-95)
        │   ├─ startTick (long)
        │   └─ duration (long)
        ├─ instruments (map)
        └─ effects (map)

DawScreen (client)
    ├─ Read from ContainerData
    └─ Send packets on interaction:
        ├─ SelectNotePacket
        ├─ NoteEditPacket
        ├─ BPMChangePacket
        └─ PlayPacket
```

---

## 📦 Implementation Order

### Phase 1: Core Scaffold
1. [ ] Create DawBlock, DawBlockEntity, DawMenu, DawScreen (empty shells)
2. [ ] Register in ApicaBlocks, ApicaBlockEntities, ApicaMenus
3. [ ] Register in ClientSetup (registerScreens)
4. [ ] Test: Block places, screen opens, no errors

### Phase 2: Basic Layout
1. [ ] Implement texture bg (1 PNG, 270×230)
2. [ ] Add top tabs using GuiRenderHelper.renderTabBar()
3. [ ] Add ContainerData sync (activeTab)
4. [ ] Test: Tabs switch, data syncs

### Phase 3: Piano Roll
1. [ ] Implement piano roll grid rendering
2. [ ] Add note data structure
3. [ ] Add note selection + dragging
4. [ ] Add WaveformRenderer integration
5. [ ] Test: Notes display, can select/drag

### Phase 4: Effects + Polish
1. [ ] Add effect knobs (drum patterns, effects)
2. [ ] Add BPM/volume controls
3. [ ] Add tooltips
4. [ ] Test: All tabs work, no glitches

---

## 🚀 Key Code Snippets

### DawMenu (Minimal)
```java
public class DawMenu extends ApicaMenu {
    private final DawBlockEntity blockEntity;
    private final ContainerData data;

    public DawMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, (DawBlockEntity) playerInv.player.level()
            .getBlockEntity(buf.readBlockPos()));
    }

    public DawMenu(int id, Inventory playerInv, DawBlockEntity be) {
        super(ApicaMenus.DAW.get(), id);
        this.blockEntity = be;
        this.data = be.getContainerData();
        addDataSlots(data);
        addPlayerInventory(playerInv, 8, 140);
        addPlayerHotbar(playerInv, 8, 198);
    }
}
```

### DawScreen (Scaffold)
```java
public class DawScreen extends AbstractApicaScreen<DawMenu> {
    private enum Tab { PIANO, DRUMS, EFFECTS, SETTINGS }
    private Tab activeTab = Tab.PIANO;

    public DawScreen(DawMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 270, 130);
    }

    @Override
    protected ResourceLocation getTexture() {
        return ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID,
            "textures/gui/daw_bg.png");
    }

    @Override
    protected String getTitleKey() { return "container.apica.daw"; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float pT) {
        // Tabs
        String[] labels = { "Piano", "Drums", "Effects", "Settings" };
        int[] tabX = GuiRenderHelper.renderTabBar(g, font, x, y, 260, labels,
            activeTab.ordinal());

        // Content by tab
        switch (activeTab) {
            case PIANO -> renderPianoTab(g, x + 10, y + 20);
            case DRUMS -> renderDrumsTab(g, x + 10, y + 20);
            case EFFECTS -> renderEffectsTab(g, x + 10, y + 20);
            case SETTINGS -> renderSettingsTab(g, x + 10, y + 20);
        }
    }

    private void renderPianoTab(GuiGraphics g, int x, int y) {
        // TODO: Piano roll rendering
    }
    // ... other tabs ...
}
```

---

## ✅ Verification Checklist

Before each implementation phase:

- [ ] Menu ↔ Screen communication works (ContainerData)
- [ ] Tab switching (activeTab int synced)
- [ ] Mouse events captured (no Minecraft focus loss)
- [ ] Rendering doesn't lag (profile if needed)
- [ ] All texture assets exist
- [ ] Tooltips appear on hover
- [ ] Player inventory always visible/accessible
- [ ] Packets sent/received correctly
- [ ] No console errors/warnings

---

## 🔗 Reference Files

**In Codebase**:
- WaveformRenderer: `client/gui/screen/WaveformRenderer.java`
- ResonationNoteRenderer: `client/gui/screen/codex/ResonationNoteRenderer.java`
- GuiRenderHelper: `client/gui/GuiRenderHelper.java`
- StorageTerminalScreen: `client/gui/screen/storage/StorageTerminalScreen.java`
- AbstractApicaScreen: `client/gui/screen/AbstractApicaScreen.java`
- ResonatorScreen: `client/gui/screen/ResonatorScreen.java` (700 lines, full example)

**Documentation**:
- gui_exploration.md (detailed widget patterns)
- gui_patterns_reference.md (quick lookup)
- CLAUDE.md (project rules)

---

## 📊 Success Criteria

### MVP (Minimum Viable Product)
- ✅ Block places in world
- ✅ Screen opens (270×230)
- ✅ 4 tabs render without errors
- ✅ Tab switching works
- ✅ Piano roll grid displays (80×80 minimum)
- ✅ Data persists (NBT save/load)

### Full Release
- ✅ All MVP features
- ✅ Piano roll with note editing (add/remove/drag)
- ✅ Waveform display working
- ✅ Effects controls functional
- ✅ Tooltips for all elements
- ✅ Performance: 60 FPS on potato hardware
- ✅ Audio playback integrated (future)

---

**Status**: Ready for development. All patterns documented, references in place, architecture clear.

