import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Note {
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private LocalDateTime modificationDate;
    private boolean isFavorite;
    private boolean isMission;
    private boolean isMissionCompleted;
    private String missionContent; // Nội dung mission
    private transient Folder folder;
    private transient List<Tag> tags;
    private Alarm alarm;
    String folderName;
    List<String> tagNames;

    public Note(String title, String content, boolean isFavorite) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        this.title = title;
        this.content = content != null ? content : "";
        this.creationDate = LocalDateTime.now();
        this.modificationDate = LocalDateTime.now();
        this.isFavorite = isFavorite;
        this.isMission = false;
        this.isMissionCompleted = false;
        this.missionContent = "";
        this.tags = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        this.title = title;
        updateModificationDate();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content != null ? content : "";
        updateModificationDate();
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(LocalDateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getFormattedCreationDate() {
        return creationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getFormattedModificationDate() {
        return modificationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
        updateModificationDate();
    }

    public boolean isMission() {
        return isMission;
    }

    public void setMission(boolean mission) {
        this.isMission = mission;
        updateModificationDate();
    }

    public boolean isMissionCompleted() {
        return isMissionCompleted;
    }

    public void setMissionCompleted(boolean missionCompleted) {
        this.isMissionCompleted = missionCompleted;
        updateModificationDate();
    }

    public String getMissionContent() {
        return missionContent != null ? missionContent : "";
    }

    public void setMissionContent(String missionContent) {
        this.missionContent = missionContent != null ? missionContent : "";
        updateModificationDate();
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
        updateModificationDate();
    }

    public List<Tag> getTags() {
        return tags != null ? tags : (tags = new ArrayList<>());
    }

    public void addTag(Tag tag) {
        if (tag != null && !getTags().contains(tag)) {
            getTags().add(tag);
            updateModificationDate();
        }
    }

    public void removeTag(Tag tag) {
        if (tag != null) {
            getTags().remove(tag);
            updateModificationDate();
        }
    }

    public Alarm getAlarm() {
        return alarm;
    }

    public void setAlarm(Alarm alarm) {
        this.alarm = alarm;
        updateModificationDate();
    }

    public int getWordCount() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        String[] words = content.trim().split("\\s+");
        return words.length;
    }

    private void updateModificationDate() {
        this.modificationDate = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return title.equals(note.title) &&
                creationDate.equals(note.creationDate);
    }

    @Override
    public int hashCode() {
        return title.hashCode() + creationDate.hashCode();
    }
}