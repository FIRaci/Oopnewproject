import java.util.ArrayList;
import java.util.List;

public class Folder {
    private String name;
    private transient List<Note> notes; // transient để Gson bỏ qua
    private transient List<Folder> subFolders; // transient để Gson bỏ qua
    List<String> subFolderNames; // Trường tạm để deserialize

    public Folder(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        this.name = name;
        this.notes = new ArrayList<>();
        this.subFolders = new ArrayList<>();
        this.subFolderNames = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        this.name = name;
    }

    public List<Note> getNotes() {
        return notes != null ? notes : (notes = new ArrayList<>());
    }

    public void addNote(Note note) {
        if (note != null && !getNotes().contains(note)) {
            getNotes().add(note);
            note.setFolder(this);
        }
    }

    public void removeNote(Note note) {
        if (note != null) {
            getNotes().remove(note);
            if (note.getFolder() == this) {
                note.setFolder(null);
            }
        }
    }

    public List<Folder> getSubFolders() {
        return subFolders != null ? subFolders : (subFolders = new ArrayList<>());
    }

    public void addSubFolder(Folder subFolder) {
        if (subFolder != null && !getSubFolders().contains(subFolder)) {
            getSubFolders().add(subFolder);
        }
    }

    public void removeSubFolder(Folder subFolder) {
        getSubFolders().remove(subFolder);
    }

    public void deleteFolder(boolean deleteNotes) {
        if (deleteNotes) {
            getNotes().clear();
        } else {
            getNotes().forEach(note -> note.setFolder(null));
        }
        getSubFolders().clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Folder folder = (Folder) o;
        return name.equals(folder.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public void setMission(boolean isMission) {
        getNotes().forEach(note -> note.setMission(isMission));
        getSubFolders().forEach(folder -> folder.setMission(isMission));
    }

    public boolean isMission() {
        return getNotes().stream().anyMatch(Note::isMission);
    }

    public boolean isFavorite() {
        return getNotes().stream().anyMatch(Note::isFavorite);
    }

    public void setFavorite(boolean isFavorite) {
        getNotes().forEach(note -> note.setFavorite(isFavorite));
        getSubFolders().forEach(folder -> folder.setFavorite(isFavorite));
    }
}