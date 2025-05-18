import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Note {
    private long id;
    private String title;
    private String content;
    private LocalDateTime createdAt; // Đổi tên từ creationDate
    private LocalDateTime updatedAt; // Đổi tên từ modificationDate
    private boolean isFavorite;
    private boolean isMission;
    private boolean isMissionCompleted;
    private String missionContent;

    private long folderId; // ID của thư mục chứa note
    private transient Folder folder; // Đối tượng Folder, transient cho DB, hữu ích cho runtime

    private transient List<Tag> tags; // Danh sách Tag, transient cho DB, populate qua join table

    // Thay vì lưu đối tượng Alarm, ta sẽ lưu ID của nó.
    // Đối tượng Alarm đầy đủ có thể được load riêng khi cần.
    private Long alarmId; // ID của Alarm, có thể null
    private transient Alarm alarm; // Đối tượng Alarm, transient, hữu ích cho runtime

    // Các trường folderName và tagNames có thể loại bỏ nếu không còn dùng cho DataStorage (JSON)
    // String folderName;
    // List<String> tagNames;

    /**
     * Constructor chính để tạo Note mới (chưa có ID từ CSDL).
     */
    public Note(String title, String content, boolean isFavorite) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        this.id = 0L; // ID mặc định cho note mới
        this.title = title;
        this.content = content != null ? content : "";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isFavorite = isFavorite;
        this.isMission = false;
        this.isMissionCompleted = false;
        this.missionContent = "";
        this.tags = new ArrayList<>();
        this.folderId = 0L; // Mặc định, hoặc ID của thư mục "Uncategorized" nếu có
        this.alarmId = null;
    }

    /**
     * Constructor đầy đủ để tạo Note từ dữ liệu CSDL (bao gồm tất cả các trường cần thiết).
     */
    public Note(long id, String title, String content,
                LocalDateTime createdAt, LocalDateTime updatedAt,
                long folderId, boolean isFavorite,
                boolean isMission, boolean isMissionCompleted, String missionContent,
                Long alarmId, List<Tag> tags // Thêm các trường còn thiếu
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.folderId = folderId;
        this.isFavorite = isFavorite;
        this.isMission = isMission;
        this.isMissionCompleted = isMissionCompleted;
        this.missionContent = missionContent;
        this.alarmId = alarmId;
        this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
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
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        this.title = title;
        updateUpdatedAt();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content != null ? content : "";
        updateUpdatedAt();
    }

    public LocalDateTime getCreatedAt() { // Đổi tên
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) { // Đổi tên
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { // Đổi tên
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) { // Đổi tên
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
        // Nếu hoàn thành mission, có thể bạn muốn xóa alarm liên quan
        if (missionCompleted) {
            this.alarm = null; // Logic này vẫn giữ nếu Alarm là object
            this.alarmId = null; // Và xóa cả alarmId
        }
        updateUpdatedAt();
    }

    public String getMissionContent() {
        return missionContent != null ? missionContent : "";
    }

    public void setMissionContent(String missionContent) {
        this.missionContent = missionContent != null ? missionContent : "";
        setMission(!this.missionContent.isEmpty()); // Tự động set isMission nếu content không rỗng
        updateUpdatedAt();
    }

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
        // if (this.folder != null && this.folder.getId() != folderId) {
        //     this.folder = null; // Reset transient folder object if ID changes
        // }
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
            this.folderId = 0L; // Hoặc một ID mặc định cho "no folder"
        }
        updateUpdatedAt();
    }

    public List<Tag> getTags() {
        return tags != null ? tags : (tags = new ArrayList<>());
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
        }
        return false;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
        // if (this.alarm != null && (alarmId == null || this.alarm.getId() != alarmId)) {
        //     this.alarm = null; // Reset transient alarm object if ID changes or is removed
        // }
        updateUpdatedAt();
    }

    public Alarm getAlarm() {
        // Nếu bạn muốn load Alarm object từ alarmId, đây là nơi để làm (tầng service)
        // Hoặc, nó được set từ bên ngoài sau khi note được tạo
        return alarm;
    }

    public void setAlarm(Alarm alarm) {
        this.alarm = alarm;
        if (alarm != null) {
            // Giả sử Alarm object của bạn cũng sẽ có getId() sau khi được cập nhật
            // this.alarmId = alarm.getId(); // Cần cập nhật Alarm.java để có getId()
        } else {
            this.alarmId = null;
        }
        updateUpdatedAt();
    }


    public int getWordCount() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        String[] words = content.trim().split("\\s+");
        return words.length;
    }

    void updateUpdatedAt() { // Đổi tên
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        if (id != 0L && note.id != 0L) { // Nếu cả hai có ID hợp lệ
            return id == note.id;
        }
        // Nếu chưa có ID (note mới), so sánh dựa trên title và thời gian tạo (logic cũ của bạn)
        // Hoặc có thể chỉ cần title nếu title là duy nhất trong một context nào đó
        return Objects.equals(title, note.title) &&
                Objects.equals(createdAt, note.createdAt);
    }

    @Override
    public int hashCode() {
        if (id != 0L) { // Nếu có ID hợp lệ
            return Objects.hash(id);
        }
        return Objects.hash(title, createdAt);
    }

    public LocalDateTime getModificationDate() {
        return updatedAt; // Assuming 'updatedAt' is non-null
    }
}