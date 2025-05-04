import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class NoteController {
    private final NoteManager noteManager;
    private Folder currentFolder;

    public NoteController() {
        noteManager = new NoteManager();
        currentFolder = noteManager.getRootFolder();
        initializeSampleData();
    }

    public void initializeSampleData() {
        if (noteManager.getAllNotes().isEmpty() && noteManager.getAllFolders().size() <= 1) {
            noteManager.addNewFolder("Work");
            noteManager.addNewFolder("Personal");
            noteManager.addNewFolder("Important");
            Note note1 = new Note("Shopping List", "Milk\nEggs\nBread", false);
            noteManager.addTag(note1, new Tag("groceries"));
            noteManager.addNote(note1);
            noteManager.moveNoteToFolder(note1, noteManager.getFolderByName("Personal").orElse(null));
            Note note2 = new Note("Project Ideas", "1. AI Chatbot\n2. Task Manager", true);
            noteManager.addTag(note2, new Tag("work"));
            noteManager.addNote(note2);
            noteManager.moveNoteToFolder(note2, noteManager.getFolderByName("Work").orElse(null));
        }
    }

    public List<Note> getSortedNotes() {
        return noteManager.getNotesInFolder(currentFolder).stream()
                .sorted(Comparator.comparing(Note::getModificationDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Note> searchNotes(String query) {
        return noteManager.getNotesInFolder(currentFolder).stream()
                .filter(note -> note.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        note.getContent().toLowerCase().contains(query.toLowerCase()) ||
                        note.getTags().stream().anyMatch(tag -> tag.getName().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public void selectFolder(Folder folder) {
        currentFolder = folder;
    }

    public List<Folder> getFolders() {
        return noteManager.getAllFolders();
    }

    public void addNewFolder(String name) {
        noteManager.addNewFolder(name);
    }

    public void deleteFolder(Folder folder) {
        noteManager.deleteFolder(folder);
    }

    public void renameFolder(Folder folder, String newName) {
        folder.setName(newName);
    }

    public void setFolderFavorite(Folder folder, boolean isFavorite) {
        folder.setFavorite(isFavorite);
    }

    public void setFolderMission(Folder folder, boolean isMission) {
        folder.setMission(isMission);
    }

    public void addNote(Note note) {
        if (note.getFolder() == null) {
            note.setFolder(currentFolder);
        }
        noteManager.addNote(note);
    }

    public void deleteNote(Note note) {
        noteManager.deleteNote(note);
    }

    public void updateNote(Note note, String title, String content) {
        note.setTitle(title);
        note.setContent(content);
        note.setModificationDate(LocalDateTime.now());
    }

    public void renameNote(Note note, String newTitle) {
        updateNote(note, newTitle, note.getContent());
    }

    public void setNoteFavorite(Note note, boolean isFavorite) {
        note.setFavorite(isFavorite);
    }

    public void setNoteMission(Note note, boolean isMission) {
        note.setMission(isMission);
    }

    public void addTag(Note note, Tag tag) {
        noteManager.addTag(note, tag);
    }

    public void removeTag(Note note, Tag tag) {
        noteManager.removeTag(note, tag);
    }

    public void moveNoteToFolder(Note note, Folder folder) {
        noteManager.moveNoteToFolder(note, folder);
    }

    public void setAlarm(Note note, Alarm alarm) {
        note.setAlarm(alarm);
    }

    public List<Note> getNotes() {
        return noteManager.getAllNotes();
    }

    public Optional<Folder> getFolderByName(String name) {
        return noteManager.getFolderByName(name);
    }

    public void changeTheme(String newTheme) {
        try {
            if ("dark".equalsIgnoreCase(newTheme)) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(new JLabel()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCurrentTheme() {
        if (UIManager.getLookAndFeel() instanceof FlatDarkLaf) {
            return "dark";
        } else if (UIManager.getLookAndFeel() instanceof FlatLightLaf) {
            return "light";
        } else {
            return "light";
        }
    }

    public NoteManager getNoteManager() {
        return noteManager;
    }
}