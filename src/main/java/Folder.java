import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Folder {
    private long id;
    private String name;
    private transient List<Note> notes;
    private transient List<Folder> subFolders;
    List<String> subFolderNames;
    private boolean favorite;

    public Folder(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        this.id = 0;
        this.name = name;
        this.notes = new ArrayList<>();
        this.subFolders = new ArrayList<>();
        this.subFolderNames = new ArrayList<>();
        this.favorite = false;
    }

    public Folder(long id, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        this.id = id;
        this.name = name;
        this.notes = new ArrayList<>();
        this.subFolders = new ArrayList<>();
        this.subFolderNames = new ArrayList<>();
        this.favorite = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
            if (this.subFolderNames == null) {
                this.subFolderNames = new ArrayList<>();
            }
            if (!this.subFolderNames.contains(subFolder.getName())) {
                this.subFolderNames.add(subFolder.getName());
            }
        }
    }

    public void removeSubFolder(Folder subFolder) {
        getSubFolders().remove(subFolder);
        if (this.subFolderNames != null && subFolder != null) {
            this.subFolderNames.remove(subFolder.getName());
        }
    }

    public List<String> getSubFolderNames() {
        if (this.subFolderNames == null) {
            this.subFolderNames = new ArrayList<>();
        }
        return this.subFolderNames;
    }

    public void setSubFolderNames(List<String> subFolderNames) {
        this.subFolderNames = subFolderNames;
    }


    public void deleteFolder(boolean deleteNotes) {
        if (deleteNotes) {
            getNotes().clear();
        } else {
            getNotes().forEach(note -> note.setFolder(null));
        }
        getSubFolders().clear();
        if (this.subFolderNames != null) {
            this.subFolderNames.clear();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Folder folder = (Folder) o;
        if (id != 0 && folder.id != 0) {
            return id == folder.id;
        }
        return Objects.equals(name, folder.name);
    }

    @Override
    public int hashCode() {
        if (id != 0) {
            return Objects.hash(id);
        }
        return Objects.hash(name);
    }

    public void setMission(boolean isMission) {
        getNotes().forEach(note -> note.setMission(isMission));
        getSubFolders().forEach(folder -> folder.setMission(isMission));
    }

    public boolean isMission() {
        return getNotes().stream().anyMatch(Note::isMission);
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}