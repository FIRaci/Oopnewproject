import javax.sound.sampled.*;
import javax.swing.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlarmController {
    private final NoteManager noteManager;
    private final MainFrame mainFrame;
    private final ScheduledExecutorService scheduler;

    public AlarmController(NoteManager noteManager, MainFrame mainFrame) {
        this.noteManager = noteManager;
        this.mainFrame = mainFrame;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startAlarmChecker();
    }

    private void startAlarmChecker() {
        scheduler.scheduleAtFixedRate(this::checkAlarms, 0, 1, TimeUnit.SECONDS);
    }

    private void checkAlarms() {
        LocalDateTime now = LocalDateTime.now();
        for (Note note : noteManager.getAllNotes()) {
            Alarm alarm = note.getAlarm();
            if (alarm != null && alarm.isActive()) {
                LocalDateTime alarmTime = alarm.getAlarmTime();
                if (alarmTime.isBefore(now) || alarmTime.equals(now)) {
                    SwingUtilities.invokeLater(() -> triggerAlarm(note));
                    updateAlarmTime(note, alarm);
                }
            }
        }
    }

    private void triggerAlarm(Note note) {
        JOptionPane.showMessageDialog(
                mainFrame,
                "Alarm for note: " + note.getTitle(),
                "Alarm",
                JOptionPane.INFORMATION_MESSAGE
        );
        playSound();
    }

    private void playSound() {
        try {
            File soundFile = new File("src/main/resources/sound/alarm.wav");
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
        } catch (Exception e) {
            System.err.println("Could not play sound: " + e.getMessage());
        }
    }

    private void updateAlarmTime(Note note, Alarm alarm) {
        if (alarm.isRecurring()) {
            String pattern = alarm.getRecurrencePattern().toUpperCase();
            LocalDateTime newTime;
            switch (pattern) {
                case "DAILY":
                    newTime = alarm.getAlarmTime().plusDays(1);
                    break;
                case "WEEKLY":
                    newTime = alarm.getAlarmTime().plusWeeks(1);
                    break;
                case "MONTHLY":
                    newTime = alarm.getAlarmTime().plusMonths(1);
                    break;
                default:
                    note.setAlarm(null);
                    return;
            }
            alarm.setAlarmTime(newTime);
        } else {
            note.setAlarm(null);
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}