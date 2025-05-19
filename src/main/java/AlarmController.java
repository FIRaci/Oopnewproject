// AlarmController.java
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlarmController {
    private final NoteController noteController;
    private final MainFrame mainFrame;
    private final ScheduledExecutorService scheduler;
    private Clip clip;
    private final ConcurrentHashMap<Long, LocalDateTime> recentlyTriggeredAlarms = new ConcurrentHashMap<>();
    private static final int RE_TRIGGER_DELAY_SECONDS = 5;

    public AlarmController(NoteController noteController, MainFrame mainFrame) {
        this.noteController = noteController;
        this.mainFrame = mainFrame;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        System.out.println("[AlarmController] INFO: AlarmController instance created.");
        startAlarmChecker();
    }

    private void startAlarmChecker() {
        System.out.println("[AlarmController] INFO: Starting alarm checker scheduler (1-second interval).");
        scheduler.scheduleAtFixedRate(this::checkAlarms, 0, 1, TimeUnit.SECONDS);
    }

    private void checkAlarms() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        List<Note> notes;
        try {
            notes = noteController.getNotes();
        } catch (Exception e) {
            System.err.println("[" + now.format(DateTimeFormatter.ISO_LOCAL_TIME) + "] ERROR: AlarmController - Error fetching notes: " + e.getMessage());
            return;
        }

        if (notes == null) return;

        recentlyTriggeredAlarms.entrySet().removeIf(entry ->
                entry.getValue().isBefore(now.minusSeconds(RE_TRIGGER_DELAY_SECONDS))
        );

        for (Note note : notes) {
            Alarm alarm = note.getAlarm();

            if (alarm == null) {
                if (note.getAlarmId() != null && note.getAlarmId() > 0) {
                    System.err.println("    !!!! CRITICAL PROBLEM for Note \"" + note.getTitle() + "\": Note.getAlarm() is NULL, but Note.getAlarmId() is " + note.getAlarmId() + ". Alarm cannot trigger!");
                }
                continue;
            }

            if (alarm.getAlarmTime() == null) {
                if (note.getAlarmId() != null && note.getAlarmId() > 0) {
                    System.err.println("    !!!! PROBLEM for Note \"" + note.getTitle() + "\": Alarm object exists but its alarmTime is NULL. Alarm cannot trigger!");
                }
                continue;
            }

            if (recentlyTriggeredAlarms.containsKey(alarm.getId())) {
                continue;
            }

            if (!note.isMissionCompleted() && alarm.shouldTrigger(now)) {
                recentlyTriggeredAlarms.put(alarm.getId(), now);

                System.out.println("    >>>> SUCCESS: TRIGGERING ALARM FOR NOTE: \"" + note.getTitle() + "\" (AlarmID: " + alarm.getId() + ") at " + now.format(DateTimeFormatter.ISO_LOCAL_TIME) +
                        " (Alarm time was: " + alarm.getAlarmTime().format(DateTimeFormatter.ISO_LOCAL_TIME) + ")");
                final Note noteToProcess = note;
                final Alarm alarmToProcess = alarm;

                SwingUtilities.invokeLater(() -> {
                    triggerAlarm(noteToProcess);
                    if (!alarmToProcess.isRecurring()) {
                        System.out.println("    INFO: (InvokeLater) Requesting DB clear for non-recurring alarm: \"" + noteToProcess.getTitle() + "\" (AlarmID: " + alarmToProcess.getId() + ")");
                        noteController.setAlarm(noteToProcess, null);
                    }
                });

                if (alarmToProcess.isRecurring()) {
                    updateAlarmTimeAndSave(noteToProcess, alarmToProcess);
                }
            }
        }
    }

    private void triggerAlarm(Note note) {
        System.out.println("    DEBUG: triggerAlarm() called for note: " + note.getTitle());

        final Alarm currentAlarmStateInNote = note.getAlarm(); // Lấy trạng thái alarm để kiểm tra nếu cần

        Runnable onDialogDispose = () -> {
            // Nếu muốn xóa khỏi recentlyTriggeredAlarms ngay khi dialog đóng (để test lại nhanh hơn)
            // và chỉ áp dụng cho báo thức không lặp lại (vì báo thức lặp lại đã được reschedule)
            if(currentAlarmStateInNote != null && !currentAlarmStateInNote.isRecurring()){
                System.out.println("    DEBUG: Non-recurring alarm dialog closed for note: \"" + note.getTitle() + "\". Removing from recentlyTriggeredAlarms (AlarmID: " + currentAlarmStateInNote.getId() + ")");
                recentlyTriggeredAlarms.remove(currentAlarmStateInNote.getId());
            } else if (currentAlarmStateInNote != null) {
                System.out.println("    DEBUG: Recurring alarm dialog closed for note: \"" + note.getTitle() + "\" (AlarmID: " + currentAlarmStateInNote.getId() + ")");
            }
            System.out.println("    DEBUG: Notification dialog general cleanup for note: \"" + note.getTitle() + "\"");
        };

        // SỬA Ở ĐÂY: Gọi playSound() TRƯỚC khi setVisible(true) cho dialog modal
        playSound();

        AlarmNotificationDialog dialog = new AlarmNotificationDialog(mainFrame, note, this, onDialogDispose);
        dialog.setVisible(true); // Lệnh này sẽ block cho đến khi dialog đóng (vì dialog là modal)
        // Khi dialog đóng, phương thức dispose() của nó sẽ được gọi và dừng âm thanh.
    }

    private void playSound() {
        System.out.println("    DEBUG: playSound() called.");
        stopAndCloseClip(); // Dừng và đóng clip cũ trước khi phát mới

        try {
            URL soundResourceUrl = getClass().getResource("/sound/Doctor.wav");
            if (soundResourceUrl == null) {
                File soundFileDev = new File("src/main/resources/sound/Doctor.wav");
                if (soundFileDev.exists()) {
                    soundResourceUrl = soundFileDev.toURI().toURL();
                } else {
                    System.err.println("    ERROR: Alarm sound file '/sound/Doctor.wav' not found.");
                    return;
                }
            }

            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundResourceUrl);
            clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
            System.out.println("    INFO: Sound started for Doctor.wav");
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    System.out.println("    DEBUG: Sound clip event STOP, closing line for: " + event.getLine().toString());
                    Clip c = (Clip) event.getSource();
                    if (c.isOpen()) {
                        c.close();
                    }
                }
            });
        } catch (UnsupportedAudioFileException e) {
            System.err.println("    ERROR: Could not play sound - Unsupported audio file format: " + e.getMessage());
            System.err.println("           Please ensure 'Doctor.wav' is a standard PCM WAV file.");
        } catch (Exception e) {
            System.err.println("    ERROR: Could not play sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopAndCloseClip() {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            if (clip.isOpen()) {
                clip.close();
            }
            clip = null;
        }
    }

    private void updateAlarmTimeAndSave(Note note, Alarm alarm) {
        if (alarm.isRecurring() && alarm.getAlarmTime() != null) {
            String pattern = alarm.getRecurrencePattern() != null ? alarm.getRecurrencePattern().toUpperCase() : "";
            LocalDateTime currentAlarmTime = alarm.getAlarmTime();
            LocalDateTime newTime = currentAlarmTime;
            boolean needsUpdate = false;

            switch (pattern) {
                case "DAILY": newTime = currentAlarmTime.plusDays(1); needsUpdate = true; break;
                case "WEEKLY": newTime = currentAlarmTime.plusWeeks(1); needsUpdate = true; break;
                case "MONTHLY": newTime = currentAlarmTime.plusMonths(1); needsUpdate = true; break;
                case "YEARLY": newTime = currentAlarmTime.plusYears(1); needsUpdate = true; break;
                default:
                    if (alarm.isRecurring() && (pattern == null || pattern.isEmpty())) {
                        System.err.println("    WARNING: Recurring alarm for note '" + note.getTitle() + "' has invalid/empty pattern. Cannot update time.");
                    }
                    return;
            }

            if (needsUpdate) {
                Alarm updatedAlarmInstance = new Alarm(alarm.getId(), newTime, true, alarm.getRecurrencePattern());
                System.out.println("    INFO: Recurring alarm for note '" + note.getTitle() + "' (AlarmID: " + alarm.getId() + ") next trigger time: " + newTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                noteController.setAlarm(note, updatedAlarmInstance);
            }
        }
    }

    public void stopSoundAndScheduler() {
        System.out.println("[AlarmController] INFO: stopSoundAndScheduler() called.");
        stopAndCloseClip();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("[AlarmController] INFO: Scheduler stopped.");
    }

    private static class AlarmNotificationDialog extends JDialog {
        private transient AlarmController alarmControllerInstance;
        private transient Runnable onDisposeCallback;

        public AlarmNotificationDialog(Frame owner, Note note, AlarmController controllerInstance, Runnable onDisposeCallback) {
            super(owner, "⏰ Alarm Notification ⏰", true);
            this.alarmControllerInstance = controllerInstance;
            this.onDisposeCallback = onDisposeCallback;

            setLayout(new BorderLayout(10, 10));
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setAlwaysOnTop(true);

            JPanel mainContentPanel = new JPanel(new BorderLayout(5, 8));
            mainContentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            JLabel titleLabel = new JLabel("<html><b>" + note.getTitle() + "</b></html>", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            mainContentPanel.add(titleLabel, BorderLayout.NORTH);
            String missionText;
            if (note.getMissionContent() != null && !note.getMissionContent().isEmpty()) {
                missionText = "Nhiệm vụ: " + note.getMissionContent() +
                        (note.isMissionCompleted() ? " (Đã hoàn thành)" : " (Chưa xong)");
            } else {
                missionText = "Không có nội dung nhiệm vụ cụ thể.";
            }
            JTextArea contentArea = new JTextArea(missionText);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            contentArea.setEditable(false);
            contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            contentArea.setOpaque(false);
            JScrollPane scrollPane = new JScrollPane(contentArea);
            scrollPane.setBorder(BorderFactory.createEtchedBorder());
            mainContentPanel.add(scrollPane, BorderLayout.CENTER);

            Alarm displayAlarm = note.getAlarm();
            if (displayAlarm != null && displayAlarm.getAlarmTime() != null) {
                JLabel alarmTimeLabel = new JLabel("Thời gian báo thức: " +
                        displayAlarm.getAlarmTime().format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy")),
                        SwingConstants.CENTER);
                alarmTimeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                mainContentPanel.add(alarmTimeLabel, BorderLayout.SOUTH);
            }
            add(mainContentPanel, BorderLayout.CENTER);

            JButton okButton = new JButton("OK");
            okButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            okButton.addActionListener(e -> {
                dispose(); // Gọi dispose() đã được override, nó sẽ dừng nhạc
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
            buttonPanel.add(okButton);
            add(buttonPanel, BorderLayout.SOUTH);
            pack();
            int minWidth = 350; int minHeight = 200; int maxWidth = 500; int maxHeight = 350;
            setSize(Math.min(maxWidth, Math.max(minWidth, getWidth() + 20)),
                    Math.min(maxHeight, Math.max(minHeight, getHeight() + 20)));
            setLocationRelativeTo(owner);
        }

        @Override
        public void dispose() {
            if (alarmControllerInstance != null) {
                System.out.println("    DEBUG: AlarmNotificationDialog.dispose() asking AlarmController to stop sound.");
                alarmControllerInstance.stopAndCloseClip();
            }
            super.dispose();
            if (this.onDisposeCallback != null) {
                this.onDisposeCallback.run();
            }
        }
    }
}