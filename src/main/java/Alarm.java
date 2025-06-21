import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Alarm {
    private long id;
    private LocalDateTime alarmTime;
    private boolean recurring;
    private String recurrencePattern;

    // Constructor chính để tạo Alarm từ dữ liệu
    public Alarm(long id, LocalDateTime alarmTime, boolean recurring, String recurrencePattern) {
        if (alarmTime == null) {
            throw new IllegalArgumentException("Alarm time cannot be null");
        }
        this.id = id;
        this.alarmTime = alarmTime;

        this.setRecurring(recurring);
        if (this.recurring) {

            this.setRecurrencePattern(recurrencePattern);
        } else {
            this.recurrencePattern = null;
        }
    }

    // Constructor để tạo Alarm mới (ví dụ: từ UI, chưa có ID)
    public Alarm(LocalDateTime alarmTime, boolean recurring, String recurrencePattern) {
        this(0L, alarmTime, recurring, recurrencePattern);
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
            this.recurrencePattern = null;
        }
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        if (this.recurring) {
            if (recurrencePattern == null || recurrencePattern.trim().isEmpty()) {
                this.recurrencePattern = (recurrencePattern == null || recurrencePattern.trim().isEmpty()) ? null : recurrencePattern.trim().toUpperCase();
            } else {
                this.recurrencePattern = recurrencePattern.trim().toUpperCase();
            }
        } else {
            this.recurrencePattern = null;
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
        return "Alarm{" +
                "id=" + id +
                ", alarmTime=" + timeStr +
                ", recurring=" + recurring +
                ", recurrencePattern='" + recurrencePattern + '\'' +
                '}';
    }

    public String getFrequency() {
        if (recurring && recurrencePattern != null && !recurrencePattern.isEmpty()) {
            return recurrencePattern;
        }
        return "ONCE";
    }
}