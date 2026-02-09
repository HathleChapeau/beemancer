/**
 * ============================================================
 * [TerminalTasksTabRenderer.java]
 * Description: Rendu de l'onglet Tasks du Storage Terminal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation              |
 * |--------------------------|----------------------|--------------------------|
 * | TaskDisplayData          | Donnees taches       | Affichage liste          |
 * | StorageTerminalMenu      | Source donnees       | getTaskDisplayData()     |
 * | StorageTaskCancelPacket  | Annulation tache     | Envoi packet             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageTerminalScreen.java (composition)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import com.chapeau.beemancer.core.network.packets.StorageTaskCancelPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rendu et interaction de l'onglet Tasks.
 * Affiche les taches groupees par Request/Automation avec scroll et cancel.
 */
public class TerminalTasksTabRenderer {

    private static final int TASK_LIST_X = 82;
    private static final int TASK_LIST_Y = 6;
    private static final int TASK_ROW_HEIGHT = 26;
    private static final int TASK_CANCEL_SIZE = 10;
    private static final int MAX_VISIBLE = 6;

    private int taskScrollOffset = 0;
    private final int guiWidth;

    public TerminalTasksTabRenderer(int guiWidth) {
        this.guiWidth = guiWidth;
    }

    public int getTaskScrollOffset() {
        return taskScrollOffset;
    }

    public void setTaskScrollOffset(int offset) {
        this.taskScrollOffset = offset;
    }

    public void render(GuiGraphics g, Font font, StorageTerminalMenu menu,
                       int x, int y, int mouseX, int mouseY) {
        List<TaskDisplayData> tasks = menu.getTaskDisplayData();
        GroupedTasks grouped = groupTasks(tasks);

        int currentY = y + TASK_LIST_Y;
        int rendered = 0;
        int skipped = 0;

        // Bee count header
        if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
            int activeBees = menu.getActiveTaskCount();
            int maxBees = menu.getMaxBees();
            int beeColor = activeBees >= maxBees ? 0xFFFF6666 : 0xFFAADDFF;
            g.drawString(font, Component.literal("Bees: " + activeBees + "/" + maxBees),
                x + TASK_LIST_X, currentY, beeColor, false);
            currentY += 12;
            rendered++;
        }
        skipped++;

        // Requests section
        currentY = renderSection(g, font, x, currentY, mouseX, mouseY,
            "gui.beemancer.tasks.requests", 0xFFFFAA00,
            grouped.requestRoots, grouped.childrenMap,
            rendered, skipped);
        rendered = lastRendered;
        skipped = lastSkipped;

        // Spacing
        if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
            currentY += 4;
        }

        // Automation section
        renderSection(g, font, x, currentY, mouseX, mouseY,
            "gui.beemancer.tasks.automation", 0xFF44FF44,
            grouped.automationRoots, grouped.childrenMap,
            rendered, skipped);
    }

    // Temp state to avoid creating result objects for section rendering
    private int lastRendered;
    private int lastSkipped;

    private int renderSection(GuiGraphics g, Font font, int x, int currentY,
                              int mouseX, int mouseY,
                              String headerKey, int headerColor,
                              List<TaskDisplayData> roots,
                              Map<UUID, List<TaskDisplayData>> childrenMap,
                              int rendered, int skipped) {
        // Header
        if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
            g.drawString(font, Component.translatable(headerKey)
                .append(" (" + roots.size() + ")"),
                x + TASK_LIST_X, currentY, headerColor, false);
            currentY += 12;
            rendered++;
        }
        skipped++;

        if (roots.isEmpty()) {
            if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
                g.drawString(font, Component.translatable("gui.beemancer.tasks.empty"),
                    x + TASK_LIST_X + 4, currentY, 0xFF888888, false);
                currentY += TASK_ROW_HEIGHT;
                rendered++;
            }
            skipped++;
        } else {
            for (TaskDisplayData root : roots) {
                List<TaskDisplayData> children = childrenMap
                    .getOrDefault(root.taskId(), List.of());

                if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
                    renderTaskRow(g, font, x, currentY, root, root.count(), mouseX, mouseY);
                    currentY += TASK_ROW_HEIGHT;
                    rendered++;
                }
                skipped++;

                for (int i = 0; i < children.size(); i++) {
                    if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
                        boolean isLast = (i == children.size() - 1);
                        renderSubtaskRow(g, font, x, currentY, children.get(i), isLast);
                        currentY += TASK_ROW_HEIGHT;
                        rendered++;
                    }
                    skipped++;
                }
            }
        }

        lastRendered = rendered;
        lastSkipped = skipped;
        return currentY;
    }

    private void renderTaskRow(GuiGraphics g, Font font, int x, int currentY,
                               TaskDisplayData task, int totalCount, int mouseX, int mouseY) {
        int taskX = x + TASK_LIST_X;

        if (!task.template().isEmpty()) {
            g.renderItem(task.template(), taskX, currentY);
        }

        String itemName = task.template().getHoverName().getString();
        if (itemName.length() > 16) itemName = itemName.substring(0, 14) + "..";
        g.drawString(font, itemName + " x" + totalCount,
            taskX + 18, currentY + 4, 0xFFFFFF, false);

        int stateColor = getStateColor(task.state());
        String stateLabel = getStateLabel(task.state());
        int stateX = x + guiWidth - 60;
        g.drawString(font, stateLabel, stateX, currentY + 4, stateColor, false);

        String originChar = task.origin().equals("REQUEST") ? "\u2191" : "\u2699";
        int originColor = task.origin().equals("REQUEST") ? 0xFF4488FF : 0xFF44AA44;
        g.drawString(font, originChar, stateX - 10, currentY + 4, originColor, false);

        int cancelX = x + guiWidth - 18;
        int cancelY = currentY + 2;
        boolean hoverCancel = mouseX >= cancelX && mouseX < cancelX + TASK_CANCEL_SIZE &&
            mouseY >= cancelY && mouseY < cancelY + TASK_CANCEL_SIZE;
        g.drawString(font, "X", cancelX, cancelY,
            hoverCancel ? 0xFFFF4444 : 0xFFAA0000, false);

        if (!task.blockedReason().isEmpty()) {
            Component reasonText = Component.translatable(task.blockedReason());
            g.drawString(font, reasonText, taskX + 18, currentY + 14, 0xFFFF6666, false);
        } else if ("AUTOMATION".equals(task.origin()) && task.requesterPos() != null) {
            String posStr = task.requesterType() + " @ "
                + task.requesterPos().getX() + ", "
                + task.requesterPos().getY() + ", "
                + task.requesterPos().getZ();
            g.drawString(font, posStr, taskX + 18, currentY + 14, 0xFF888888, false);
        }
    }

    private void renderSubtaskRow(GuiGraphics g, Font font, int x, int currentY,
                                  TaskDisplayData task, boolean isLast) {
        int baseX = x + TASK_LIST_X;
        int indent = 12;

        String connector = isLast ? "\u2514" : "\u251C";
        g.drawString(font, connector, baseX + 2, currentY + 4, 0xFF666666, false);

        int taskX = baseX + indent;

        if (!task.template().isEmpty()) {
            g.renderItem(task.template(), taskX, currentY);
        }

        g.drawString(font, "x" + task.count(),
            taskX + 18, currentY + 4, 0xFFFFFFFF, false);

        int stateColor = getStateColor(task.state());
        String stateLabel = getStateLabel(task.state());
        int stateX = x + guiWidth - 60;
        g.drawString(font, stateLabel, stateX, currentY + 4, stateColor, false);
    }

    /**
     * Gere le clic sur les boutons cancel de l'onglet Tasks.
     * Retourne true si un cancel a ete effectue.
     */
    public boolean handleCancelClick(double mouseX, double mouseY,
                                     StorageTerminalMenu menu, int leftPos, int topPos) {
        List<TaskDisplayData> tasks = menu.getTaskDisplayData();
        GroupedTasks grouped = groupTasks(tasks);

        int x = leftPos;
        int y = topPos;
        int currentY = y + TASK_LIST_Y;
        int rendered = 0;
        int skipped = 0;

        // Skip bee count header
        if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
            currentY += 12;
            rendered++;
        }
        skipped++;

        // Skip requests header
        if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
            currentY += 12;
            rendered++;
        }
        skipped++;

        if (grouped.requestRoots.isEmpty()) {
            if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
                currentY += TASK_ROW_HEIGHT;
                rendered++;
            }
            skipped++;
        } else {
            for (TaskDisplayData root : grouped.requestRoots) {
                List<TaskDisplayData> children = grouped.childrenMap
                    .getOrDefault(root.taskId(), List.of());
                if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
                    if (checkCancelClick(mouseX, mouseY, x, currentY, menu, root)) return true;
                    currentY += TASK_ROW_HEIGHT;
                    rendered++;
                }
                skipped++;
                for (int i = 0; i < children.size(); i++) {
                    if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
                        currentY += TASK_ROW_HEIGHT;
                        rendered++;
                    }
                    skipped++;
                }
            }
        }

        // Spacing
        if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
            currentY += 4;
        }

        // Skip automation header
        if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
            currentY += 12;
            rendered++;
        }
        skipped++;

        if (!grouped.automationRoots.isEmpty()) {
            for (TaskDisplayData root : grouped.automationRoots) {
                List<TaskDisplayData> children = grouped.childrenMap
                    .getOrDefault(root.taskId(), List.of());
                if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
                    if (checkCancelClick(mouseX, mouseY, x, currentY, menu, root)) return true;
                    currentY += TASK_ROW_HEIGHT;
                    rendered++;
                }
                skipped++;
                for (int i = 0; i < children.size(); i++) {
                    if (skipped >= taskScrollOffset && rendered < MAX_VISIBLE) {
                        currentY += TASK_ROW_HEIGHT;
                        rendered++;
                    }
                    skipped++;
                }
            }
        }

        return false;
    }

    private boolean checkCancelClick(double mouseX, double mouseY, int x, int currentY,
                                     StorageTerminalMenu menu, TaskDisplayData task) {
        int cancelX = x + guiWidth - 18;
        int cancelY = currentY + 2;
        if (mouseX >= cancelX && mouseX < cancelX + TASK_CANCEL_SIZE &&
            mouseY >= cancelY && mouseY < cancelY + TASK_CANCEL_SIZE) {
            PacketDistributor.sendToServer(
                new StorageTaskCancelPacket(menu.getBlockPos(), task.taskId())
            );
            return true;
        }
        return false;
    }

    /**
     * Calcule le nombre total d'entrees pour le scroll.
     */
    public int getTotalEntries(StorageTerminalMenu menu) {
        GroupedTasks grouped = groupTasks(menu.getTaskDisplayData());
        int total = 3; // bee header + 2 section headers
        if (grouped.requestRoots.isEmpty()) {
            total += 1;
        } else {
            for (TaskDisplayData root : grouped.requestRoots) {
                total += 1;
                total += grouped.childrenMap.getOrDefault(root.taskId(), List.of()).size();
            }
        }
        if (grouped.automationRoots.isEmpty()) {
            total += 1;
        } else {
            for (TaskDisplayData root : grouped.automationRoots) {
                total += 1;
                total += grouped.childrenMap.getOrDefault(root.taskId(), List.of()).size();
            }
        }
        return total;
    }

    // --- Helpers ---

    private GroupedTasks groupTasks(List<TaskDisplayData> tasks) {
        List<TaskDisplayData> roots = new ArrayList<>();
        Map<UUID, List<TaskDisplayData>> childrenMap = new HashMap<>();

        for (TaskDisplayData task : tasks) {
            if (task.parentTaskId() == null) {
                roots.add(task);
            } else {
                childrenMap.computeIfAbsent(task.parentTaskId(), k -> new ArrayList<>()).add(task);
            }
        }

        List<TaskDisplayData> requestRoots = new ArrayList<>();
        List<TaskDisplayData> automationRoots = new ArrayList<>();
        for (TaskDisplayData root : roots) {
            if ("AUTOMATION".equals(root.origin())) {
                automationRoots.add(root);
            } else {
                requestRoots.add(root);
            }
        }

        return new GroupedTasks(requestRoots, automationRoots, childrenMap);
    }

    private record GroupedTasks(
        List<TaskDisplayData> requestRoots,
        List<TaskDisplayData> automationRoots,
        Map<UUID, List<TaskDisplayData>> childrenMap
    ) {}

    private int getStateColor(String state) {
        return switch (state) {
            case "FLYING" -> 0xFF44AAFF;
            case "QUEUED" -> 0xFFAAAAAA;
            case "COMPLETED" -> 0xFF00FF00;
            case "FAILED" -> 0xFFFF0000;
            default -> 0xFFFFFFFF;
        };
    }

    private String getStateLabel(String state) {
        return switch (state) {
            case "FLYING" -> Component.translatable("gui.beemancer.tasks.state.flying").getString();
            case "QUEUED" -> Component.translatable("gui.beemancer.tasks.state.queued").getString();
            case "COMPLETED" -> Component.translatable("gui.beemancer.tasks.state.completed").getString();
            case "FAILED" -> Component.translatable("gui.beemancer.tasks.state.failed").getString();
            default -> state;
        };
    }
}
