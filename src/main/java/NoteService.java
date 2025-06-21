import java.util.ArrayList;
import java.util.List;


public class NoteService {
    private final NoteManager noteManager;

    public NoteService(NoteManager noteManager) {
        if (noteManager == null) {
            throw new IllegalArgumentException("NoteManager cannot be null.");
        }
        this.noteManager = noteManager;
    }

    public long ensureAlarmHasId(Alarm alarm) {
        if (alarm == null) {
            throw new IllegalArgumentException("Alarm object cannot be null.");
        }
        if (alarm.getId() == 0) {
            alarm.setId(noteManager.generateNewAlarmId());
        }
        return alarm.getId();
    }


    public Alarm getAlarmById(long alarmId) {
        if (alarmId <= 0) {
            return null;
        }
        for (Note note : noteManager.getAllNotes()) {
            if (note.getAlarm() != null && note.getAlarm().getId() == alarmId) {
                return note.getAlarm();
            }
        }
        return null;
    }

    public void deleteAlarm(long alarmId) {
        if (alarmId <= 0) {
            System.out.println("Warning: Attempted to delete alarm with invalid ID: " + alarmId);
            return;
        }
        boolean alarmRemovedFromAnyNote = false;
        List<Note> allNotes = noteManager.getAllNotes();
        for (Note note : allNotes) {
            if (note.getAlarm() != null && note.getAlarm().getId() == alarmId) {
                note.setAlarm(null);
                noteManager.updateNote(note);
                alarmRemovedFromAnyNote = true;
                System.out.println("Removed alarm (ID: " + alarmId + ") from note: " + note.getTitle());
            }
        }
        if (!alarmRemovedFromAnyNote) {
            System.out.println("No note found associated with alarm ID: " + alarmId + ". Alarm might have already been removed or never existed.");
        }
    }


    // --- Phương thức quản lý Note ---

    public Note createNewNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }

        if (note.getFolder() == null || note.getFolder().getId() == 0) {
            Folder rootFolder = noteManager.getRootFolder();
            note.setFolder(rootFolder);
            note.setFolderId(rootFolder.getId());
        } else {
            Folder managedFolder = noteManager.getFolderById(note.getFolderId());
            if (managedFolder == null) {
                System.err.println("Warning: Folder with ID " + note.getFolderId() + " not found in NoteManager. Assigning to Root.");
                managedFolder = noteManager.getRootFolder();
            }
            note.setFolder(managedFolder);
            note.setFolderId(managedFolder.getId());
        }

        List<Tag> resolvedTags = new ArrayList<>();
        if (note.getTags() != null) {
            for (Tag tag : note.getTags()) {
                resolvedTags.add(noteManager.getOrCreateTag(tag.getName()));
            }
        }
        note.setTags(resolvedTags);

        if (note.getAlarm() != null) {
            ensureAlarmHasId(note.getAlarm());
            note.setAlarmId(note.getAlarm().getId());
        } else {
            note.setAlarmId(null);
        }

        noteManager.addNote(note);
        return note;
    }

    public Note getNoteDetails(long noteId) {
        if (noteId <= 0) return null;
        return noteManager.getNoteById(noteId);
    }

    public List<Note> getAllNotesForDisplay() {
        return noteManager.getAllNotes();
    }

    public void updateExistingNote(Note note) {
        if (note == null || note.getId() <= 0) {
            throw new IllegalArgumentException("Note for update cannot be null and must have a valid ID.");
        }

        if (note.getFolder() == null || note.getFolder().getId() == 0) {
            Folder rootFolder = noteManager.getRootFolder();
            note.setFolder(rootFolder);
            note.setFolderId(rootFolder.getId());
        } else {
            Folder managedFolder = noteManager.getFolderById(note.getFolderId());
            if (managedFolder == null) {
                System.err.println("Warning: Folder for note update with ID " + note.getFolderId() + " not found. Assigning to Root.");
                managedFolder = noteManager.getRootFolder();
            }
            note.setFolder(managedFolder);
            note.setFolderId(managedFolder.getId());
        }



        List<Tag> resolvedTags = new ArrayList<>();
        if (note.getTags() != null) {
            for (Tag tag : note.getTags()) {
                resolvedTags.add(noteManager.getOrCreateTag(tag.getName()));
            }
        }
        note.setTags(resolvedTags);

        Alarm currentAlarmObjectOnNote = note.getAlarm();
        if (currentAlarmObjectOnNote != null) {
            ensureAlarmHasId(currentAlarmObjectOnNote);
            note.setAlarmId(currentAlarmObjectOnNote.getId());
        } else {
            note.setAlarmId(null);
        }

        noteManager.updateNote(note);
    }

    public void deleteExistingNote(long noteId) {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Invalid note ID for delete: " + noteId);
        }
        noteManager.deleteNote(noteId);
    }

    // --- Các phương thức quản lý Folder ---
    public List<Folder> getAllFolders() {
        return noteManager.getAllFolders();
    }

    public Folder getFolderById(long folderId) {
        if (folderId <= 0) return null;
        return noteManager.getFolderById(folderId);
    }

    public Folder getFolderByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return noteManager.getFolderByName(name.trim()).orElse(null);
    }

    public Folder createNewFolder(Folder folder) {
        if (folder == null || folder.getName() == null || folder.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Folder or folder name cannot be null or empty.");
        }
        noteManager.addFolder(folder);
        return folder;
    }

    public void updateExistingFolder(Folder folder) {
        if (folder == null || folder.getId() <= 0) {
            throw new IllegalArgumentException("Folder to update must not be null and must have a valid ID.");
        }
        if (folder.getName() == null || folder.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name for update cannot be null or empty.");
        }
        noteManager.updateFolder(folder);
    }

    public void deleteExistingFolder(long folderId, boolean moveNotesToRoot) throws Exception {
        if (folderId <= 0) {
            throw new IllegalArgumentException("Invalid folder ID for delete: " + folderId);
        }
        noteManager.deleteFolder(folderId, moveNotesToRoot);
    }

    // --- Các phương thức quản lý Tag ---
    public List<Tag> getAllTags() {
        return noteManager.getAllTags();
    }

    public Tag getTagByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        return noteManager.getTagByName(name.trim());
    }

    public Tag getOrCreateTag(String tagName) {
        return noteManager.getOrCreateTag(tagName);
    }

    public void updateTag(Tag tag) {
        if (tag == null || tag.getId() <= 0) {
            throw new IllegalArgumentException("Tag to update must not be null and must have a valid ID.");
        }
        noteManager.updateTag(tag);
    }

    public void deleteTag(long tagId) {
        if (tagId <= 0) {
            throw new IllegalArgumentException("Invalid tag ID for delete: " + tagId);
        }
        noteManager.deleteTag(tagId);
    }

    public Note getNoteById(long id) {
        return noteManager.getNoteById(id);
    }

    public NoteManager getNoteManager() {
        return noteManager;
    }
}
