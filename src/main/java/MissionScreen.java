import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MissionScreen extends JPanel {
    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel missionContainer;
    private JButton deleteButton;
    private boolean deleteMode = false;
    private final List<JCheckBox> deleteCheckboxes = new ArrayList<>();

    public MissionScreen(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(800, 500));
        setMinimumSize(new Dimension(800, 500));
        setMaximumSize(new Dimension(800, 500));

        // Top panel with delete button
        JPanel topPanel = new JPanel(new BorderLayout());
        deleteButton = new JButton("🗑");
        deleteButton.setToolTipText("Toggle Delete Mode");
        deleteButton.addActionListener(e -> toggleDeleteMode());
        topPanel.add(deleteButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Mission container with FlowLayout
        missionContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        missionContainer.setPreferredSize(new Dimension(800, 500));
        JScrollPane scrollPane = new JScrollPane(missionContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Tăng tốc độ cuộn
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);

        // Bọc scrollPane trong fixedPanel
        JPanel fixedPanel = new JPanel(new BorderLayout());
        fixedPanel.setPreferredSize(new Dimension(800, 500));
        fixedPanel.setMinimumSize(new Dimension(800, 500));
        fixedPanel.setMaximumSize(new Dimension(800, 500));
        fixedPanel.add(scrollPane, BorderLayout.CENTER);

        add(fixedPanel, BorderLayout.CENTER);

        refreshMissions();
    }

    private void toggleDeleteMode() {
        deleteMode = !deleteMode;
        deleteButton.setText(deleteMode ? "Done" : "🗑");
        refreshMissions();
    }

    public void refreshMissions() {
        missionContainer.removeAll();
        deleteCheckboxes.clear();

        // Add mission panels (notes with non-empty missionContent)
        List<Note> missions = controller.getMissions();
        System.out.println("Refreshing missions in MissionScreen: " + missions.size() + " missions found.");
        for (Note note : missions) {
            missionContainer.add(createMissionPanel(note));
        }

        missionContainer.revalidate();
        missionContainer.repaint();
    }

    private JPanel createMissionPanel(Note note) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setPreferredSize(new Dimension(350, 180));

        // Kiểm tra trạng thái xám
        boolean isGrayed = note.getAlarm() != null && note.getAlarm().getAlarmTime().isBefore(LocalDateTime.now()) && !note.getAlarm().isRecurring();
        if (isGrayed) {
            panel.setBackground(Color.LIGHT_GRAY);
        }

        // Panel cho các nút điều khiển (Done và Delete)
        JPanel controlPanel = new JPanel(new BorderLayout());
        JCheckBox completeCheckbox = new JCheckBox("Done");
        completeCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        completeCheckbox.setSelected(note.isMissionCompleted());
        completeCheckbox.addActionListener(e -> {
            controller.completeMission(note, completeCheckbox.isSelected());
            refreshMissions();
        });
        controlPanel.add(completeCheckbox, BorderLayout.WEST);

        if (deleteMode) {
            JCheckBox deleteCheckbox = new JCheckBox("Delete");
            deleteCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            deleteCheckboxes.add(deleteCheckbox);
            controlPanel.add(deleteCheckbox, BorderLayout.EAST);
            deleteCheckbox.addActionListener(e -> {
                if (deleteCheckbox.isSelected()) {
                    int option = JOptionPane.showConfirmDialog(mainFrame,
                            "Do you want to delete the entire note or just the mission?\n" +
                                    "Yes: Delete note (including mission and alarm)\n" +
                                    "No: Clear mission only\n" +
                                    "Cancel: Do nothing",
                            "Delete Confirmation", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        controller.deleteNote(note);
                    } else if (option == JOptionPane.NO_OPTION) {
                        controller.updateMission(note, "");
                    }
                    refreshMissions();
                }
            });
        }
        panel.add(controlPanel, BorderLayout.NORTH);

        // Nội dung mission
        JPanel contentPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(note.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel contentLabel = new JLabel("<html>" + truncateText(note.getMissionContent(), 100) + "</html>");
        contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        contentPanel.add(contentLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.add(new JLabel("Created: " + note.getFormattedModificationDate()));
        String alarmText = note.getAlarm() != null ? formatAlarm(note.getAlarm()) : "No Alarm";
        JLabel alarmLabel = new JLabel("Alarm: " + alarmText);
        alarmLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        alarmLabel.setForeground(Color.BLUE);
        alarmLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        alarmLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (note.getAlarm() != null) {
                    showAlarmDialog(note);
                }
            }
        });
        infoPanel.add(alarmLabel);
        contentPanel.add(infoPanel, BorderLayout.SOUTH);

        panel.add(contentPanel, BorderLayout.CENTER);

        // Click để chỉnh sửa mission
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!deleteMode) {
                    MissionDialog dialog = new MissionDialog(mainFrame);
                    dialog.setMission(note.getMissionContent());
                    dialog.setVisible(true);
                    String result = dialog.getResult();
                    if (result != null) {
                        controller.updateMission(note, result);
                        refreshMissions();
                    }
                }
            }
        });

        return panel;
    }

    private void showAlarmDialog(Note note) {
        JDialog dialog = new JDialog(mainFrame, "Alarm Details", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Alarm alarm = note.getAlarm();
        DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String alarmTimeStr = alarm.getAlarmTime().format(formatterFull);
        String alarmType = alarm.isRecurring() ? alarm.getFrequency() : "ONCE";

        // Hiển thị thông tin alarm
        JLabel alarmLabel = new JLabel("Alarm Time: " + alarmTimeStr + " (" + alarmType + ")");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        dialog.add(alarmLabel, gbc);

        JLabel typeLabel = new JLabel("Type: " + alarmType);
        gbc.gridy = 1;
        dialog.add(typeLabel, gbc);

        // Ô nhập để chỉnh sửa thời gian alarm
        JTextField timeField = new JTextField(alarmTimeStr, 20);
        gbc.gridy = 2;
        dialog.add(new JLabel("New Alarm Time (yyyy-MM-dd HH:mm:ss):"), gbc);
        gbc.gridy = 3;
        dialog.add(timeField, gbc);

        // Tùy chọn để chỉnh sửa loại alarm
        String[] alarmTypes = {"ONCE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        JComboBox<String> typeComboBox = new JComboBox<>(alarmTypes);
        typeComboBox.setSelectedItem(alarmType);
        gbc.gridy = 4;
        dialog.add(new JLabel("Alarm Type:"), gbc);
        gbc.gridy = 5;
        dialog.add(typeComboBox, gbc);

        // Nút cập nhật alarm
        JButton updateButton = new JButton("Update Alarm");
        updateButton.addActionListener(e -> {
            try {
                LocalDateTime newTime = LocalDateTime.parse(timeField.getText(), formatterFull);
                String newType = (String) typeComboBox.getSelectedItem();
                boolean isRecurring = !"ONCE".equals(newType);
                Alarm newAlarm = new Alarm(newTime, isRecurring, newType);
                controller.setAlarm(note, newAlarm);
                refreshMissions();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format! Use yyyy-MM-dd HH:mm:ss", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridy = 6;
        dialog.add(updateButton, gbc);

        // Nút xóa alarm
        JButton deleteButton = new JButton("Delete Alarm");
        deleteButton.addActionListener(e -> {
            controller.setAlarm(note, null);
            refreshMissions();
            dialog.dispose();
        });
        gbc.gridy = 7;
        dialog.add(deleteButton, gbc);

        // Nút hủy
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        gbc.gridy = 8;
        dialog.add(cancelButton, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    // Hàm định dạng alarm theo yêu cầu
    private String formatAlarm(Alarm alarm) {
        DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        DateTimeFormatter formatterShort = DateTimeFormatter.ofPattern("HH:mm");
        if (alarm.isRecurring()) {
            return alarm.getAlarmTime().format(formatterShort) + " (" + alarm.getFrequency() + ")";
        } else {
            return alarm.getAlarmTime().format(formatterFull) + " (ONCE)";
        }
    }

    // Hàm cắt ngắn nội dung để vừa khung lớn hơn
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            System.out.println("MissionScreen is now visible. Refreshing missions...");
            refreshMissions();
        }
    }
}