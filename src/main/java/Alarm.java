import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Alarm {
    private long id;
    private LocalDateTime alarmTime;
    private boolean recurring;
    private String recurrencePattern;

    // Constructor chính để tạo Alarm từ dữ liệu (ví dụ: từ DAO)
    public Alarm(long id, LocalDateTime alarmTime, boolean recurring, String recurrencePattern) {
        if (alarmTime == null) {
            throw new IllegalArgumentException("Alarm time cannot be null");
        }
        this.id = id;
        this.alarmTime = alarmTime;
        // Gọi setter để đảm bảo logic nhất quán được áp dụng
        this.setRecurring(recurring); // Quan trọng: gọi setter
        if (this.recurring) {
            // Chỉ đặt pattern nếu thực sự là recurring và pattern hợp lệ
            this.setRecurrencePattern(recurrencePattern);
        } else {
            this.recurrencePattern = null; // Đảm bảo là null nếu không recurring
        }
    }

    // Constructor để tạo Alarm mới (ví dụ: từ UI, chưa có ID)
    public Alarm(LocalDateTime alarmTime, boolean recurring, String recurrencePattern) {
        this(0L, alarmTime, recurring, recurrencePattern); // Gọi constructor chính với ID mặc định là 0
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
        if (!this.recurring) {
            this.recurrencePattern = null; // Đảm bảo pattern là null nếu không recurring
        }
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        if (this.recurring) {
            if (recurrencePattern == null || recurrencePattern.trim().isEmpty()) {
                // Hoặc throw exception, hoặc đặt một giá trị mặc định hợp lệ nếu có
                // throw new IllegalArgumentException("Recurrence pattern cannot be null or empty for recurring alarms.");
                // Tạm thời cho phép null/empty pattern cho recurring alarm nếu logic của bạn cho phép
                // và sẽ được xử lý ở tầng DAO khi lưu (đặt là NULL trong DB nếu empty)
                this.recurrencePattern = (recurrencePattern == null || recurrencePattern.trim().isEmpty()) ? null : recurrencePattern.trim().toUpperCase();

            } else {
                this.recurrencePattern = recurrencePattern.trim().toUpperCase();
            }
        } else {
            this.recurrencePattern = null; // Không cho phép set pattern nếu không recurring
        }
    }

    public boolean shouldTrigger(LocalDateTime now) {
        if (alarmTime == null || now == null) return false;
        return !now.isBefore(alarmTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alarm alarm = (Alarm) o;
        if (id != 0L && alarm.id != 0L) {
            return id == alarm.id;
        }
        // So sánh dựa trên các thuộc tính nếu chưa có ID (cho mục đích logic, không phải định danh DB)
        return recurring == alarm.recurring &&
                Objects.equals(alarmTime, alarm.alarmTime) &&
                Objects.equals(recurrencePattern, alarm.recurrencePattern);
    }

    @Override
    public int hashCode() {
        if (id != 0L) {
            return Objects.hash(id);
        }
        return Objects.hash(alarmTime, recurring, recurrencePattern);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String timeStr = (alarmTime != null) ? alarmTime.format(formatter) : "N/A";
        // recurrencePattern giờ đây sẽ là null nếu không recurring, nên không cần (recurrencePattern == null ? "" : ...)
        return "Alarm{" +
                "id=" + id +
                ", alarmTime=" + timeStr +
                ", recurring=" + recurring +
                ", recurrencePattern='" + recurrencePattern + '\'' + // Sẽ hiển thị 'null' nếu là null
                '}';
    }

    public String getFrequency() {
        if (recurring && recurrencePattern != null && !recurrencePattern.isEmpty()) {
            return recurrencePattern;
        }
        return "ONCE";
    }
}