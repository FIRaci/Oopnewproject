import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Giữ lại nếu getFrequency() hoặc toString() dùng
import java.util.Objects;

public class Alarm {
    private long id; // ID của Alarm trong cơ sở dữ liệu
    private LocalDateTime alarmTime;
    private boolean recurring;
    private String recurrencePattern; // Ví dụ: "DAILY", "WEEKLY"

    /**
     * Constructor để tạo Alarm mới (chưa có ID từ CSDL).
     */
    public Alarm(LocalDateTime alarmTime, boolean recurring, String recurrencePattern) {
        if (alarmTime == null) {
            throw new IllegalArgumentException("Alarm time cannot be null");
        }
        if (recurring && (recurrencePattern == null || recurrencePattern.trim().isEmpty())) {
            throw new IllegalArgumentException("Recurrence pattern cannot be null or empty for recurring alarms");
        }
        this.id = 0L; // Giá trị mặc định cho Alarm mới
        this.alarmTime = alarmTime;
        this.recurring = recurring;
        this.recurrencePattern = recurrencePattern != null ? recurrencePattern.trim().toUpperCase() : null;
    }

    /**
     * Constructor để tạo Alarm từ dữ liệu đã có trong CSDL (có ID).
     */
    public Alarm(long id, LocalDateTime alarmTime, boolean recurring, String recurrencePattern) {
        if (alarmTime == null) {
            throw new IllegalArgumentException("Alarm time cannot be null");
        }
        if (recurring && (recurrencePattern == null || recurrencePattern.trim().isEmpty())) {
            throw new IllegalArgumentException("Recurrence pattern cannot be null or empty for recurring alarms");
        }
        this.id = id;
        this.alarmTime = alarmTime;
        this.recurring = recurring;
        this.recurrencePattern = recurrencePattern != null ? recurrencePattern.trim().toUpperCase() : null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(LocalDateTime alarmTime) {
        if (alarmTime == null) {
            throw new IllegalArgumentException("Alarm time cannot be null");
        }
        this.alarmTime = alarmTime;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
        // Nếu không lặp lại nữa, có thể muốn xóa pattern
        if (!recurring) {
            this.recurrencePattern = null;
        }
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        // Chỉ cho phép set pattern nếu là recurring alarm
        if (this.recurring && (recurrencePattern == null || recurrencePattern.trim().isEmpty())) {
            throw new IllegalArgumentException("Recurrence pattern cannot be null or empty for recurring alarms");
        }
        if (!this.recurring && recurrencePattern != null && !recurrencePattern.trim().isEmpty()){
            // Có thể throw lỗi hoặc tự động set recurring = true
            // For now, let's throw an error or log a warning.
            // Hoặc đơn giản là không cho set nếu không recurring:
            // throw new IllegalStateException("Cannot set recurrence pattern for non-recurring alarm.");
            // Hoặc là:
            this.recurrencePattern = null; // đảm bảo non-recurring thì pattern là null
        } else {
            this.recurrencePattern = recurrencePattern != null ? recurrencePattern.trim().toUpperCase() : null;
        }
    }

    public boolean shouldTrigger(LocalDateTime now) {
        if (alarmTime == null || now == null) return false;
        // Chỉ trigger nếu thời gian hiện tại bằng hoặc sau thời gian báo thức
        // và báo thức chưa "qua" nếu không lặp lại (logic này có thể phức tạp hơn tùy yêu cầu)
        return !now.isBefore(alarmTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alarm alarm = (Alarm) o;
        if (id != 0L && alarm.id != 0L) { // Nếu cả hai có ID hợp lệ
            return id == alarm.id;
        }
        // Nếu chưa có ID, so sánh dựa trên các thuộc tính khác (logic cũ của bạn)
        return recurring == alarm.recurring &&
                Objects.equals(alarmTime, alarm.alarmTime) &&
                Objects.equals(recurrencePattern, alarm.recurrencePattern);
    }

    @Override
    public int hashCode() {
        if (id != 0L) { // Nếu có ID hợp lệ
            return Objects.hash(id);
        }
        return Objects.hash(alarmTime, recurring, recurrencePattern);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String timeStr = (alarmTime != null) ? alarmTime.format(formatter) : "N/A";
        return "Alarm{" +
                "id=" + id +
                ", alarmTime=" + timeStr +
                ", recurring=" + recurring +
                ", recurrencePattern='" + (recurrencePattern == null ? "" : recurrencePattern) + '\'' +
                '}';
    }

    /**
     * Trả về tần suất lặp lại, ví dụ "DAILY", "WEEKLY", hoặc "ONCE" nếu không lặp lại.
     * Được sử dụng trong UI.
     */
    public String getFrequency() {
        if (recurring && recurrencePattern != null && !recurrencePattern.isEmpty()) {
            return recurrencePattern;
        }
        return "ONCE"; // Hoặc có thể trả về chuỗi rỗng nếu không muốn hiển thị "ONCE"
    }
}