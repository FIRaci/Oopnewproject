import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class NoteController {
    private final NoteManager noteManager;
    private Folder currentFolder;

    public NoteController() {
        noteManager = new NoteManager();
        currentFolder = noteManager.getRootFolder();
    }

    public List<Note> getSortedNotes() {
        return noteManager.getNotesInFolder(currentFolder).stream()
                .sorted(Comparator.comparing(Note::isFavorite, Comparator.reverseOrder())
                        .thenComparing(Note::getModificationDate, Comparator.reverseOrder()))
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
        if (note != null && note.getFolder() != null) {
            note.getFolder().removeNote(note);
        }
        if (folder != null) {
            note.setFolder(folder);
            folder.addNote(note);
        } else {
            note.setFolder(noteManager.getRootFolder());
            noteManager.getRootFolder().addNote(note);
        }
        noteManager.saveData(); // Đảm bảo lưu thay đổi
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

    public List<Note> getMissions() {
        List<Note> missions = noteManager.getAllNotes().stream()
                .filter(note -> !note.getMissionContent().isEmpty())
                .sorted((n1, n2) -> {
                    boolean n1Grayed = n1.getAlarm() != null && n1.getAlarm().getAlarmTime().isBefore(LocalDateTime.now()) && !n1.getAlarm().isRecurring();
                    boolean n2Grayed = n2.getAlarm() != null && n2.getAlarm().getAlarmTime().isBefore(LocalDateTime.now()) && !n2.getAlarm().isRecurring();
                    if (n1Grayed && !n2Grayed) return 1;
                    if (!n1Grayed && n2Grayed) return -1;
                    return n1.getModificationDate().compareTo(n2.getModificationDate());
                })
                .collect(Collectors.toList());
        System.out.println("Total missions found: " + missions.size());
        missions.forEach(note -> System.out.println("Mission: " + note.getTitle() + ", Content: " + note.getMissionContent()));
        return missions;
    }

    public void updateMission(Note note, String missionContent) {
        note.setMissionContent(missionContent);
        note.setMission(!missionContent.isEmpty());
        note.setModificationDate(LocalDateTime.now());
    }

    public void completeMission(Note note, boolean completed) {
        note.setMissionCompleted(completed);
        if (completed) {
            note.setAlarm(null);
        }
    }
}