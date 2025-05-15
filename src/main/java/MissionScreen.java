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

        // Top panel with delete button
        JPanel topPanel = new JPanel(new BorderLayout());
        deleteButton = new JButton("🗑");
        deleteButton.addActionListener(e -> toggleDeleteMode());
        topPanel.add(deleteButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Mission container with GridLayout (3 columns)
        missionContainer = new JPanel(new GridLayout(0, 3, 20, 20)); // 0 hàng để tự động, 3 cột, khoảng cách 20px
        missionContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = new JScrollPane(missionContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // Cho phép scroll ngang

        // Tăng tốc độ cuộn
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(100);

        // Loại bỏ giới hạn chiều rộng tối đa để scroll ngang hoạt động
        // missionContainer.setMaximumSize(new Dimension(1110, Integer.MAX_VALUE)); // Đã bỏ

        add(scrollPane, BorderLayout.CENTER);

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
            JPanel missionPanel = createMissionPanel(note);
            missionContainer.add(missionPanel);
        }

        missionContainer.revalidate();
        missionContainer.repaint();
    }

    private JPanel createMissionPanel(Note note) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setPreferredSize(new Dimension(350, 180));
        panel.setMaximumSize(new Dimension(350, 180)); // Giới hạn kích thước tối đa

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
        dialog.setSize(300, 250);
        dialog.setResizable(false);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Alarm alarm = note.getAlarm();
        DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String alarmTimeStr = alarm != null ? alarm.getAlarmTime().format(formatterFull) : LocalDateTime.now().format(formatterFull);
        String alarmType = alarm != null && alarm.isRecurring() ? alarm.getFrequency() : "ONCE";

        // Hiển thị thông tin alarm
        JLabel alarmLabel = new JLabel("Alarm Time: " + alarmTimeStr);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        dialog.add(alarmLabel, gbc);

        // Tùy chọn loại alarm
        String[] alarmTypes = {"ONCE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        JComboBox<String> typeComboBox = new JComboBox<>(alarmTypes);
        typeComboBox.setSelectedItem(alarmType);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        dialog.add(new JLabel("Alarm Type:"), gbc);
        gbc.gridx = 1;
        dialog.add(typeComboBox, gbc);

        // Panel chứa các trường ngày giờ với kích thước cố định
        JPanel dateTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateTimePanel.setPreferredSize(new Dimension(250, 30));
        JTextField dateField = new JTextField(10);
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

        // Đặt giá trị ban đầu
        if (alarm != null) {
            dateField.setText(alarm.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            hourSpinner.setValue(alarm.getAlarmTime().getHour());
            minuteSpinner.setValue(alarm.getAlarmTime().getMinute());
        } else {
            LocalDateTime now = LocalDateTime.now();
            dateField.setText(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            hourSpinner.setValue(now.getHour());
            minuteSpinner.setValue(now.getMinute());
        }

        // Thêm thành phần ban đầu
        if ("ONCE".equals(alarmType)) {
            dateField.setText(alarm != null ? alarm.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            dateTimePanel.add(new JLabel("Date (yyyy-MM-dd):"));
            dateTimePanel.add(dateField);
        }
        dateTimePanel.add(new JLabel("Hour:"));
        dateTimePanel.add(hourSpinner);
        dateTimePanel.add(new JLabel("Minute:"));
        dateTimePanel.add(minuteSpinner);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        dialog.add(dateTimePanel, gbc);

        // Xử lý thay đổi loại alarm
        typeComboBox.addActionListener(e -> {
            String selectedType = (String) typeComboBox.getSelectedItem();
            dateTimePanel.removeAll();
            if ("ONCE".equals(selectedType)) {
                dateField.setText(alarm != null ? alarm.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                dateTimePanel.add(new JLabel("Date (yyyy-MM-dd):"));
                dateTimePanel.add(dateField);
            }
            dateTimePanel.add(new JLabel("Hour:"));
            dateTimePanel.add(hourSpinner);
            dateTimePanel.add(new JLabel("Minute:"));
            dateTimePanel.add(minuteSpinner);
            dateTimePanel.revalidate();
            dateTimePanel.repaint();
        });

        // Nút cập nhật alarm
        JButton updateButton = new JButton("Update Alarm");
        updateButton.addActionListener(e -> {
            try {
                String selectedType = (String) typeComboBox.getSelectedItem();
                boolean isRecurring = !"ONCE".equals(selectedType);
                LocalDateTime newTime;

                int hour = (int) hourSpinner.getValue();
                int minute = (int) minuteSpinner.getValue();
                if ("ONCE".equals(selectedType)) {
                    LocalDateTime date = LocalDateTime.parse(dateField.getText() + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    newTime = date.withHour(hour).withMinute(minute).withSecond(0);
                } else {
                    LocalDateTime baseDate = alarm != null ? alarm.getAlarmTime() : LocalDateTime.now();
                    newTime = baseDate.withHour(hour).withMinute(minute).withSecond(0);
                }

                Alarm newAlarm = new Alarm(newTime, isRecurring, selectedType);
                controller.setAlarm(note, newAlarm);
                refreshMissions();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format! Use yyyy-MM-dd", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        dialog.add(updateButton, gbc);

        // Nút xóa alarm
        JButton deleteButton = new JButton("Delete Alarm");
        deleteButton.setEnabled(alarm != null);
        deleteButton.addActionListener(e -> {
            controller.setAlarm(note, null);
            refreshMissions();
            dialog.dispose();
        });
        gbc.gridy = 4;
        dialog.add(deleteButton, gbc);

        // Nút hủy
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        gbc.gridy = 5;
        dialog.add(cancelButton, gbc);

        dialog.setMinimumSize(new Dimension(300, 250));
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
        return text.substring(0, maxLength) + "...";
    }
}