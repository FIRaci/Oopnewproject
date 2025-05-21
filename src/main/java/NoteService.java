// import java.sql.SQLException; // Không còn cần thiết
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Có thể cần cho getFolderByName

/**
 * Service class for managing notes, folders, tags, and alarms.
 * Interacts with NoteManager for data operations.
 */
public class NoteService {
    private final NoteManager noteManager;

    public NoteService(NoteManager noteManager) {
        if (noteManager == null) {
            throw new IllegalArgumentException("NoteManager cannot be null.");
        }
        this.noteManager = noteManager;
    }

    // --- Phương thức quản lý Alarm ---
    // Alarm giờ được quản lý như một phần của Note.
    // Các thao tác với Alarm (thêm, sửa, xóa) sẽ được thực hiện thông qua việc cập nhật Note.

    /**
     * Ensures an Alarm object has an ID. If it's new (ID=0), a new ID is generated.
     * This method is typically called before associating an alarm with a note
     * or when a note containing this alarm is about to be saved.
     *
     * @param alarm The Alarm object.
     * @return The ID of the alarm (either existing or newly generated).
     */
    public long ensureAlarmHasId(Alarm alarm) {
        if (alarm == null) {
            throw new IllegalArgumentException("Alarm object cannot be null.");
        }
        if (alarm.getId() == 0) {
            alarm.setId(noteManager.generateNewAlarmId());
        }
        return alarm.getId();
    }

    /**
     * Retrieves an Alarm by its ID by searching through all notes.
     * @param alarmId The ID of the alarm to find.
     * @return The Alarm object if found, otherwise null.
     */
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

    /**
     * Deletes an alarm by its ID. This involves finding all notes associated with this alarmId,
     * setting their alarm reference to null, and then updating these notes.
     * The NoteManager does not store alarms independently.
     * @param alarmId The ID of the alarm to delete.
     */
    public void deleteAlarm(long alarmId) {
        if (alarmId <= 0) {
            System.out.println("Warning: Attempted to delete alarm with invalid ID: " + alarmId);
            return;
        }
        boolean alarmRemovedFromAnyNote = false;
        List<Note> allNotes = noteManager.getAllNotes(); // Get a mutable copy if necessary
        for (Note note : allNotes) {
            if (note.getAlarm() != null && note.getAlarm().getId() == alarmId) {
                note.setAlarm(null); // This also sets alarmId to null in Note's setter
                noteManager.updateNote(note); // Persist change to the note
                alarmRemovedFromAnyNote = true;
                System.out.println("Removed alarm (ID: " + alarmId + ") from note: " + note.getTitle());
            }
        }
        if (!alarmRemovedFromAnyNote) {
            System.out.println("No note found associated with alarm ID: " + alarmId + ". Alarm might have already been removed or never existed.");
        }
        // No direct alarm list in NoteManager to delete from, changes are saved via note updates.
    }


    // --- Phương thức quản lý Note ---

    public Note createNewNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }

        // Ensure folder is set, default to Root if not specified or invalid
        if (note.getFolder() == null || note.getFolder().getId() == 0) {
            Folder rootFolder = noteManager.getRootFolder();
            note.setFolder(rootFolder);
            note.setFolderId(rootFolder.getId());
        } else {
            // Ensure the folder object on the note is the one managed by NoteManager
            Folder managedFolder = noteManager.getFolderById(note.getFolderId());
            if (managedFolder == null) { // Should not happen if UI uses folders from NoteManager
                System.err.println("Warning: Folder with ID " + note.getFolderId() + " not found in NoteManager. Assigning to Root.");
                managedFolder = noteManager.getRootFolder();
            }
            note.setFolder(managedFolder);
            note.setFolderId(managedFolder.getId());
        }


        // Ensure tags are managed instances with IDs
        List<Tag> resolvedTags = new ArrayList<>();
        if (note.getTags() != null) {
            for (Tag tag : note.getTags()) {
                resolvedTags.add(noteManager.getOrCreateTag(tag.getName()));
            }
        }
        note.setTags(resolvedTags);

        // Ensure alarm has an ID if it exists
        if (note.getAlarm() != null) {
            ensureAlarmHasId(note.getAlarm());
            note.setAlarmId(note.getAlarm().getId());
        } else {
            note.setAlarmId(null);
        }

        noteManager.addNote(note); // NoteManager handles ID generation for the note itself
        return note; // Return the note, now with an ID
    }

    public Note getNoteDetails(long noteId) {
        if (noteId <= 0) return null;
        // NoteManager.getNoteById should return the note with its transient fields (Folder, Alarm, Tags) already populated
        return noteManager.getNoteById(noteId);
    }

    public List<Note> getAllNotesForDisplay() {
        // NoteManager.getAllNotes() should return notes with populated transient fields
        return noteManager.getAllNotes();
    }

    public void updateExistingNote(Note note) {
        if (note == null || note.getId() <= 0) {
            throw new IllegalArgumentException("Note for update cannot be null and must have a valid ID.");
        }

        // Ensure folder is valid and is the managed instance
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


        // Ensure tags are managed instances with IDs
        List<Tag> resolvedTags = new ArrayList<>();
        if (note.getTags() != null) {
            for (Tag tag : note.getTags()) {
                resolvedTags.add(noteManager.getOrCreateTag(tag.getName()));
            }
        }
        note.setTags(resolvedTags);

        // Handle Alarm: Ensure ID is set if alarm exists, or alarmId is null if alarm is null
        Alarm currentAlarmObjectOnNote = note.getAlarm();
        if (currentAlarmObjectOnNote != null) {
            ensureAlarmHasId(currentAlarmObjectOnNote);
            note.setAlarmId(currentAlarmObjectOnNote.getId());
        } else {
            note.setAlarmId(null); // Explicitly set alarmId to null if alarm object is removed
        }

        noteManager.updateNote(note);
    }

    public void deleteExistingNote(long noteId) {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Invalid note ID for delete: " + noteId);
        }
        // NoteManager's deleteNote will handle removing the note from its folder's list
        // and from the main notes list.
        // Associated Alarms are part of the Note object and will be removed when the Note is removed.
        // DataStorage will no longer save that Note or its Alarm.
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
        // NoteManager.addFolder will handle ID generation if folder.getId() is 0
        // and check for name uniqueness.
        noteManager.addFolder(folder);
        return folder; // folder object will have its ID set by NoteManager
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

    /**
     * Ensures a tag exists with the given name, creating it if necessary.
     * The returned tag will have a valid ID.
     * @param tagName The name of the tag.
     * @return The managed Tag object.
     */
    public Tag getOrCreateTag(String tagName) {
        return noteManager.getOrCreateTag(tagName);
    }

    /**
     * Updates an existing tag. Primarily for renaming.
     * @param tag The tag with updated information (must have a valid ID).
     */
    public void updateTag(Tag tag) {
        if (tag == null || tag.getId() <= 0) {
            throw new IllegalArgumentException("Tag to update must not be null and must have a valid ID.");
        }
        noteManager.updateTag(tag);
    }

    /**
     * Deletes a tag by its ID from the global list and removes it from all notes.
     * @param tagId The ID of the tag to delete.
     */
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
