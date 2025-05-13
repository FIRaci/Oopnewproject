import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Alarm {
    private LocalDateTime alarmTime;
    private boolean recurring;
    private String recurrencePattern;

    public Alarm(LocalDateTime alarmTime, boolean recurring, String recurrencePattern) {
        if (alarmTime == null) {
            throw new IllegalArgumentException("Alarm time cannot be null");
        }
        if (recurring && (recurrencePattern == null || recurrencePattern.trim().isEmpty())) {
            throw new IllegalArgumentException("Recurrence pattern cannot be null or empty for recurring alarms");
        }
        this.alarmTime = alarmTime;
        this.recurring = recurring;
        this.recurrencePattern = recurrencePattern != null ? recurrencePattern.trim() : null;
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
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        if (recurring && (recurrencePattern == null || recurrencePattern.trim().isEmpty())) {
            throw new IllegalArgumentException("Recurrence pattern cannot be null or empty for recurring alarms");
        }
        this.recurrencePattern = recurrencePattern != null ? recurrencePattern.trim() : null;
    }

    public boolean shouldTrigger(LocalDateTime now) {
        if (alarmTime == null || now == null) return false;
        return now.isAfter(alarmTime) || now.isEqual(alarmTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alarm alarm = (Alarm) o;
        return recurring == alarm.recurring &&
                Objects.equals(alarmTime, alarm.alarmTime) &&
                Objects.equals(recurrencePattern, alarm.recurrencePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alarmTime, recurring, recurrencePattern);
    }

    @Override
    public String toString() {
        return "Alarm{time=" + alarmTime +
                ", recurring=" + recurring +
                ", pattern='" + recurrencePattern + "'}";
    }

    public String getFrequency() {
        return recurrencePattern != null ? recurrencePattern : "";
    }
}