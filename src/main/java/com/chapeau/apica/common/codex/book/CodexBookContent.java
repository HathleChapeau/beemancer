/**
 * ============================================================
 * [CodexBookContent.java]
 * Description: Contenu modulaire d'une page du Codex Book pour un node
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Sections modulaires  | Liste ordonnée de contenus     |
 * | HeaderSection       | En-tête par défaut   | Génération de contenu vierge   |
 * | StickyNote          | Notes collantes      | Données des sticky notes       |
 * | Gson                | Parsing JSON         | Chargement data-driven         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookManager (cache et chargement)
 * - CodexBookScreen (affichage du contenu et sticky notes)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodexBookContent {

    public enum StickyNotesDisplay { SIDE, PAGE }

    private final String nodeId;
    private final List<CodexBookSection> sections;
    private final List<StickyNote> stickyNotes;
    private final StickyNotesDisplay stickyNotesDisplay;

    public CodexBookContent(String nodeId, List<CodexBookSection> sections, List<StickyNote> stickyNotes, StickyNotesDisplay stickyNotesDisplay) {
        this.nodeId = nodeId;
        this.sections = Collections.unmodifiableList(sections);
        this.stickyNotes = Collections.unmodifiableList(stickyNotes);
        this.stickyNotesDisplay = stickyNotesDisplay;
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<CodexBookSection> getSections() {
        return sections;
    }

    public List<StickyNote> getStickyNotes() {
        return stickyNotes;
    }

    public StickyNotesDisplay getStickyNotesDisplay() {
        return stickyNotesDisplay;
    }

    /**
     * Crée un contenu par défaut (page vierge) avec uniquement un HeaderSection.
     * @param nodeId L'identifiant du node
     * @return Un contenu minimal avec en-tête seul
     */
    public static CodexBookContent createDefault(String nodeId) {
        List<CodexBookSection> defaultSections = new ArrayList<>();
        defaultSections.add(new HeaderSection());
        return new CodexBookContent(nodeId, defaultSections, List.of(), StickyNotesDisplay.SIDE);
    }

    /**
     * Parse le contenu depuis un objet JSON.
     * Format attendu:
     * {
     *   "sections": [
     *     {"type": "header"},
     *     {"type": "text", "key": "codex.apica.book.xxx.text1"},
     *     {"type": "image", "texture": "apica:...", "width": 64, "height": 64}
     *   ]
     * }
     * @param nodeId L'identifiant du node
     * @param json L'objet JSON
     * @return Le contenu parsé
     */
    public static CodexBookContent fromJson(String nodeId, JsonObject json) {
        List<CodexBookSection> sections = new ArrayList<>();

        if (json.has("sections")) {
            JsonArray sectionsArray = json.getAsJsonArray("sections");
            for (JsonElement element : sectionsArray) {
                JsonObject sectionJson = element.getAsJsonObject();
                CodexBookSection section = CodexBookSection.fromJson(sectionJson);
                sections.add(section);
            }
        }

        if (sections.isEmpty()) {
            sections.add(new HeaderSection());
        }

        List<StickyNote> stickyNotes = new ArrayList<>();
        if (json.has("sticky_notes")) {
            JsonArray notesArray = json.getAsJsonArray("sticky_notes");
            for (JsonElement element : notesArray) {
                stickyNotes.add(StickyNote.fromJson(element.getAsJsonObject()));
            }
        }

        StickyNotesDisplay display = StickyNotesDisplay.SIDE;
        if (json.has("sticky_notes_display")) {
            String displayStr = json.get("sticky_notes_display").getAsString();
            if ("page".equalsIgnoreCase(displayStr)) {
                display = StickyNotesDisplay.PAGE;
            }
        }

        return new CodexBookContent(nodeId, sections, stickyNotes, display);
    }
}
