import java.time.LocalDateTime;

public class Mission {
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private Alarm alarm;
    private boolean completed;
    private boolean grayedOut;

    public Mission(String title, String content, LocalDateTime creationDate) {
        this.title = title;
        this.content = content;
        this.creationDate = creationDate;
        this.completed = false;
        this.grayedOut = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Alarm getAlarm() {
        return alarm;
    }

    public void setAlarm(Alarm alarm) {
        this.alarm = alarm;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.alarm = null;
        }
    }

    public boolean isGrayedOut() {
        return grayedOut;
    }

    public void setGrayedOut(boolean grayedOut) {
        this.grayedOut = grayedOut;
    }

    public String getFormattedCreationDate() {
        return creationDate.toString();
    }
}