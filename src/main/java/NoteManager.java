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
        // Thêm thư mục gốc mặc định
        Folder rootFolder = new Folder("Root");
        folders.add(rootFolder);
        // Tải dữ liệu từ JSON
        dataStorage.load(this);
        // Ánh xạ lại subfolders
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
    }

    // Quản lý ghi chú
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
        return folder.getNotes().stream().filter(notes::contains).collect(Collectors.toList());
    }

    // Quản lý thư mục
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

    // Quản lý thẻ
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

    // Tìm kiếm
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

    // Di chuyển ghi chú
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
        saveData();
    }

    // Sắp xếp ghi chú
    public List<Note> getSortedNotes() {
        return notes.stream()
                .sorted(Comparator.comparing(Note::isFavorite, Comparator.reverseOrder())
                        .thenComparing(note -> !note.isMission() || note.isMissionCompleted())
                        .thenComparing(Note::getCreationDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    // Thống kê
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

    private void saveData() {
        dataStorage.save(this);
    }

    public Optional<Folder> getFolderByName(String name) {
        return folders.stream().filter(f -> f.getName().equals(name)).findFirst();
    }
}