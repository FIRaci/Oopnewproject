import java.util.*;
import java.util.stream.Collectors;

public class NoteManager {
    private final List<Note> notes;
    private final List<Folder> folders;
    private final List<Tag> tags;
    private final DataStorage dataStorage;

    public NoteManager() {
        notes = new ArrayList<>();
        folders = new ArrayList<>();
        tags = new ArrayList<>();
        dataStorage = new DataStorage("notes.json");

        Folder rootFolder = new Folder("Root");
        folders.add(rootFolder);

        dataStorage.load(this);

        Map<String, Folder> folderMap = new HashMap<>();
        for (Folder folder : folders) {
            folderMap.put(folder.getName(), folder);
        }
        for (Folder folder : folders) {
            if (folder.subFolderNames != null) {
                folder.subFolderNames.forEach(name -> {
                    Folder subFolder = folderMap.get(name);
                    if (subFolder != null) {
                        folder.addSubFolder(subFolder);
                    }
                });
            }
        }

        // Log danh sách notes sau khi tải
        System.out.println("NoteManager initialized. Total notes: " + notes.size());
        notes.forEach(note -> System.out.println("Note: " + note.getTitle() + ", Mission Content: " + note.getMissionContent()));
    }

    public void addNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }
        if (!notes.contains(note)) {
            notes.add(note);
            if (note.getFolder() == null) {
                note.setFolder(getRootFolder());
                getRootFolder().addNote(note);
            } else {
                note.getFolder().addNote(note);
            }
        }
        saveData();
    }

    public void deleteNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }
        if (notes.remove(note)) {
            if (note.getFolder() != null) {
                note.getFolder().removeNote(note);
            }
            saveData();
        }
    }

    public List<Note> getAllNotes() {
        return new ArrayList<>(notes);
    }

    public List<Note> getNotesInFolder(Folder folder) {
        if (folder == null) return new ArrayList<>();
        if (folder.getName().equals("Root")) {
            return notes.stream().filter(n -> !n.getFolder().getName().equals("Root")).collect(Collectors.toList());
        }
        return folder.getNotes().stream().filter(notes::contains).collect(Collectors.toList());
    }

    public void addNewFolder(String name) {
        Folder folder = new Folder(name);
        addFolder(folder);
    }

    public void addFolder(Folder folder) {
        if (folder == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }
        if (!folders.contains(folder)) {
            folders.add(folder);
            saveData();
        }
    }

    public void deleteFolder(Folder folder) {
        deleteFolder(folder, true);
    }

    public void deleteFolder(Folder folder, boolean deleteNotes) {
        if (folder == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }
        if (folder.equals(getRootFolder())) {
            throw new IllegalStateException("Cannot delete root folder");
        }
        folders.remove(folder);
        folder.deleteFolder(deleteNotes);
        if (!deleteNotes) {
            for (Note note : folder.getNotes()) {
                note.setFolder(getRootFolder());
            }
        }
        saveData();
    }

    public Folder getRootFolder() {
        return folders.get(0);
    }

    public List<Folder> getAllFolders() {
        return new ArrayList<>(folders);
    }

    public void addTag(Note note, Tag tag) {
        if (note == null || tag == null) {
            throw new IllegalArgumentException("Note or Tag cannot be null");
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
        note.addTag(tag);
        saveData();
    }

    public void removeTag(Note note, Tag tag) {
        if (note == null || tag == null) {
            throw new IllegalArgumentException("Note or Tag cannot be null");
        }
        note.removeTag(tag);
        if (note.getTags().isEmpty() && tags.contains(tag)) {
            tags.remove(tag);
        }
        saveData();
    }

    public List<Tag> getAllTags() {
        return new ArrayList<>(tags);
    }

    public List<Note> searchNotes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllNotes();
        }
        String lowerQuery = query.trim().toLowerCase();
        return notes.stream()
                .filter(note -> note.getTitle().toLowerCase().contains(lowerQuery) ||
                        note.getContent().toLowerCase().contains(lowerQuery) ||
                        note.getTags().stream().anyMatch(tag -> tag.getName().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
    }

    public List<Note> searchNotesByTag(Tag tag) {
        if (tag == null) {
            return new ArrayList<>();
        }
        return notes.stream()
                .filter(note -> note.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    public void moveNoteToFolder(Note note, Folder folder) {
        if (note != null && note.getFolder() != null) {
            note.getFolder().removeNote(note);
        }
        if (folder != null) {
            note.setFolder(folder);
            folder.addNote(note);
        } else {
            note.setFolder(getRootFolder());
            getRootFolder().addNote(note);
        }
        note.setFolder(folder);
        saveData();
    }

    public List<Note> getSortedNotes() {
        return notes.stream()
                .sorted(Comparator.comparing(Note::isFavorite, Comparator.reverseOrder())
                        .thenComparing(note -> !note.isMission() || note.isMissionCompleted())
                        .thenComparing(Note::getCreationDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    public int getTotalNotesCount() {
        return notes.size();
    }

    public int getOpenMissionsCount() {
        return (int) notes.stream()
                .filter(Note::isMission)
                .filter(note -> !note.isMissionCompleted())
                .count();
    }

    public Map<String, Integer> getFolderNoteCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Folder folder : folders) {
            counts.put(folder.getName(), folder.getNotes().size());
        }
        return counts;
    }

    void saveData() {
        dataStorage.save(this);
    }

    public Optional<Folder> getFolderByName(String name) {
        return folders.stream().filter(f -> f.getName().equals(name)).findFirst();
    }
}