import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Note {
    public LocalDateTime getModificationDate() {
        return updatedAt;
    }

    // Thêm enum để phân biệt loại Note
    public enum NoteType {
        TEXT, // Ghi chú văn bản thông thường
        DRAWING // Ghi chú dạng bản vẽ
    }

    private long id;
    private String title;
    private String content; // Sẽ dùng cho TEXT, có thể null cho DRAWING
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isFavorite;
    private boolean isMission;
    private boolean isMissionCompleted;
    private String missionContent;

    private long folderId;
    private transient Folder folder;

    private transient List<Tag> tags;

    private Long alarmId;
    private transient Alarm alarm;

    // Trường mới cho loại Note và dữ liệu bản vẽ
    private NoteType noteType;
    private String drawingData; // Dùng để lưu trữ dữ liệu bản vẽ, ví dụ Base64 của ảnh PNG

    // Các trường folderName, tagNames không còn được sử dụng trực tiếp bởi DataStorage mới
    // String folderName; // Sẽ được quản lý qua folderId và transient Folder
    // List<String> tagNames; // Sẽ được quản lý qua List<Tag> và transient Tag

    /**
     * Constructor cho ghi chú văn bản mới.
     */
    public Note(String title, String content, boolean isFavorite) {
        this(0L, title, content, LocalDateTime.now(), LocalDateTime.now(),
                0L, isFavorite, false, false, "", null, new ArrayList<>(),
                NoteType.TEXT, null); // Mặc định là TEXT, drawingData là null
    }

    /**
     * Constructor để tạo Note mới với loại cụ thể (ví dụ khi tạo Draw Panel).
     */
    public Note(String title, NoteType type, Folder initialFolder) {
        this(0L, title, (type == NoteType.TEXT ? "" : null), // content null cho DRAWING
                LocalDateTime.now(), LocalDateTime.now(),
                (initialFolder != null ? initialFolder.getId() : 0L),
                false, false, false, "", null, new ArrayList<>(),
                type, (type == NoteType.DRAWING ? "" : null)); // drawingData rỗng cho DRAWING mới
        if (initialFolder != null) {
            this.folder = initialFolder;
        }
    }


    /**
     * Constructor đầy đủ để tạo Note từ dữ liệu (ví dụ từ DataStorage).
     */
    public Note(long id, String title, String content,
                LocalDateTime createdAt, LocalDateTime updatedAt,
                long folderId, boolean isFavorite,
                boolean isMission, boolean isMissionCompleted, String missionContent,
                Long alarmId, List<Tag> tags,
                NoteType noteType, String drawingData // Thêm các trường mới
    ) {
        if (title == null || title.trim().isEmpty()) {
            // Gán tiêu đề mặc định nếu title từ JSON là null/rỗng khi deserialize
            this.title = (id == 0) ? "Untitled Note" : "Note ID " + id;
        } else {
            this.title = title;
        }
        this.id = id;
        this.content = content; // Có thể null nếu là DRAWING
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : this.createdAt;
        this.folderId = folderId;
        this.isFavorite = isFavorite;
        this.isMission = isMission;
        this.isMissionCompleted = isMissionCompleted;
        this.missionContent = missionContent == null ? "" : missionContent;
        this.alarmId = alarmId;
        this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
        this.noteType = (noteType != null) ? noteType : NoteType.TEXT; // Mặc định là TEXT nếu null
        this.drawingData = drawingData; // Có thể null nếu là TEXT
    }


    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            // Không throw exception, nhưng có thể log hoặc xử lý khác nếu cần
            this.title = "Untitled"; // Hoặc giữ nguyên title cũ nếu đang update
        } else {
            this.title = title;
        }
        updateUpdatedAt();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content; // Cho phép content là null (ví dụ cho drawing note)
        updateUpdatedAt();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFormattedCreationDate() {
        if (createdAt == null) return "N/A";
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getFormattedModificationDate() {
        if (updatedAt == null) return "N/A";
        return updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
        updateUpdatedAt();
    }

    public boolean isMission() {
        return isMission;
    }

    public void setMission(boolean mission) {
        isMission = mission;
        updateUpdatedAt();
    }

    public boolean isMissionCompleted() {
        return isMissionCompleted;
    }

    public void setMissionCompleted(boolean missionCompleted) {
        isMissionCompleted = missionCompleted;
        if (missionCompleted && this.alarm != null) { // Nếu hoàn thành mission và có alarm đang active
            // Cân nhắc việc có nên tự động xóa alarm không.
            // Hiện tại, logic này nằm trong NoteController.completeMission
            // this.alarm = null;
            // this.alarmId = null;
        }
        updateUpdatedAt();
    }

    public String getMissionContent() {
        return missionContent != null ? missionContent : "";
    }

    public void setMissionContent(String missionContent) {
        this.missionContent = missionContent != null ? missionContent : "";
        setMission(!this.missionContent.isEmpty());
        updateUpdatedAt();
    }

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
        updateUpdatedAt();
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
        if (folder != null) {
            this.folderId = folder.getId();
        } else {
            this.folderId = 0L;
        }
        updateUpdatedAt();
    }

    public List<Tag> getTags() {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
        updateUpdatedAt();
    }

    public void addTag(Tag tag) {
        if (tag != null && !getTags().contains(tag)) {
            getTags().add(tag);
            updateUpdatedAt();
        }
    }

    public boolean removeTag(Tag tag) {
        if (tag != null && getTags().remove(tag)) {
            updateUpdatedAt();
            return true; // Sửa: trả về true nếu xóa thành công
        }
        return false;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
        if (alarmId == null && this.alarm != null) { // Nếu alarmId bị set thành null, cũng clear transient alarm
            this.alarm = null;
        }
        updateUpdatedAt();
    }

    public Alarm getAlarm() {
        return alarm;
    }

    public void setAlarm(Alarm alarm) {
        this.alarm = alarm;
        if (alarm != null) {
            this.alarmId = alarm.getId();
        } else {
            this.alarmId = null;
        }
        updateUpdatedAt();
    }

    // Getters and Setters cho các trường mới
    public NoteType getNoteType() {
        if (noteType == null) return NoteType.TEXT; // Mặc định an toàn
        return noteType;
    }

    public void setNoteType(NoteType noteType) {
        this.noteType = noteType;
        updateUpdatedAt();
    }

    public String getDrawingData() {
        return drawingData;
    }

    public void setDrawingData(String drawingData) {
        this.drawingData = drawingData;
        updateUpdatedAt();
    }

    public int getWordCount() {
        if (noteType == NoteType.DRAWING || content == null || content.trim().isEmpty()) {
            return 0;
        }
        String[] words = content.trim().split("\\s+");
        return words.length;
    }

    public void updateUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        if (id != 0L && note.id != 0L) {
            return id == note.id;
        }
        // Nếu một trong hai hoặc cả hai chưa có ID (note mới), so sánh dựa trên các thuộc tính khác
        // Điều này có thể cần xem xét lại nếu title không phải là duy nhất
        return Objects.equals(title, note.title) &&
                Objects.equals(createdAt, note.createdAt) && // Thêm createdAt để tăng tính duy nhất cho note mới
                Objects.equals(noteType, note.noteType); // Phân biệt theo loại
    }

    @Override
    public int hashCode() {
        if (id != 0L) {
            return Objects.hash(id);
        }
        return Objects.hash(title, createdAt, noteType); // Thêm noteType vào hashCode
    }

    // Phương thức này có thể không còn cần thiết nếu getUpdatedAt() được dùng trực tiếp
    // public LocalDateTime getModificationDate() {
    //     return updatedAt;
    // }
}
