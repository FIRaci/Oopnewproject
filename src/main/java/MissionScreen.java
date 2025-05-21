import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// import java.util.Comparator; // Không cần import riêng nếu dùng lambda trực tiếp
import java.util.List;

public class MissionScreen extends JPanel {
    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel missionContainer;
    private JButton deleteButton;
    private boolean deleteMode = false;

    public MissionScreen(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        deleteButton = new JButton("🗑");
        deleteButton.addActionListener(e -> toggleDeleteMode());
        topPanel.add(deleteButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        missionContainer = new JPanel(new GridLayout(0, 3, 20, 20));
        missionContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = new JScrollPane(missionContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(100);

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

        List<Note> missions = controller.getMissions();
        LocalDateTime consistencyNow = LocalDateTime.now(); // Mốc thời gian nhất quán

        missions.sort((n1, n2) -> {
            boolean n1Completed = n1.isMissionCompleted();
            boolean n2Completed = n2.isMissionCompleted();

            // Logic xác định task quá hạn dùng consistencyNow
            boolean n1IsOverdue = !n1Completed && n1.getAlarm() != null && !n1.getAlarm().isRecurring() && n1.getAlarm().getAlarmTime().isBefore(consistencyNow);
            boolean n2IsOverdue = !n2Completed && n2.getAlarm() != null && !n2.getAlarm().isRecurring() && n2.getAlarm().getAlarmTime().isBefore(consistencyNow);

            // Xác định hạng mục của mỗi task: 1 (Còn hạn), 2 (Hết hạn), 3 (Done)
            int category1 = n1Completed ? 3 : (n1IsOverdue ? 2 : 1);
            int category2 = n2Completed ? 3 : (n2IsOverdue ? 2 : 1);

            // Sắp xếp theo hạng mục chính
            if (category1 != category2) {
                return Integer.compare(category1, category2);
            }

            // Nếu cùng hạng mục, sắp xếp phụ
            switch (category1) {
                case 1: // Cả hai đều CÒN HẠN (Chưa hoàn thành, Chưa quá hạn)
                    boolean n1HasAlarm = n1.getAlarm() != null;
                    boolean n2HasAlarm = n2.getAlarm() != null;

                    if (n1HasAlarm && n2HasAlarm) { // Cả hai có alarm (trong tương lai)
                        int alarmCompare = n1.getAlarm().getAlarmTime().compareTo(n2.getAlarm().getAlarmTime());
                        if (alarmCompare != 0) return alarmCompare; // Alarm sớm hơn lên trước
                    } else if (n1HasAlarm) { // Chỉ n1 có alarm
                        return -1; // Task có alarm lên trước
                    } else if (n2HasAlarm) { // Chỉ n2 có alarm
                        return 1;
                    }
                    // Nếu không có alarm, hoặc alarm giống nhau: sắp xếp theo ngày sửa đổi (mới nhất trước)
                    return n2.getModificationDate().compareTo(n1.getModificationDate());

                case 2: // Cả hai đều HẾT HẠN (Chưa hoàn thành, Đã quá hạn)
                    // Hết hạn thì chắc chắn có alarm. Sắp xếp theo thời gian alarm (sớm nhất/quá hạn lâu nhất trước)
                    int alarmCompareOverdue = n1.getAlarm().getAlarmTime().compareTo(n2.getAlarm().getAlarmTime());
                    if (alarmCompareOverdue != 0) return alarmCompareOverdue;
                    // Nếu alarm quá hạn trùng nhau: sắp xếp theo ngày sửa đổi (mới nhất trước)
                    return n2.getModificationDate().compareTo(n1.getModificationDate());

                case 3: // Cả hai đều DONE
                    // Sắp xếp theo ngày sửa đổi (hoàn thành/sửa đổi mới nhất lên trước trong nhóm done)
                    return n2.getModificationDate().compareTo(n1.getModificationDate());
            }
            return 0; // Trường hợp không thể xảy ra nếu logic switch-case đầy đủ
        });

        System.out.println("Refreshing missions (Sorted by: Still Due -> Overdue -> Done): " + missions.size() + " missions.");
        for (Note note : missions) {
            // Truyền consistencyNow vào để tô màu cũng dùng chung mốc thời gian này
            JPanel missionPanel = createMissionPanel(note, consistencyNow);
            missionContainer.add(missionPanel);
        }

        missionContainer.revalidate();
        missionContainer.repaint();
    }

    // Thay đổi signature để nhận currentTime
    private JPanel createMissionPanel(Note note, LocalDateTime currentTime) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setPreferredSize(new Dimension(350, 180));
        panel.setMaximumSize(new Dimension(350, 180));

        Color missionPanelBackgroundColor = UIManager.getColor("Panel.background");
        boolean isCompleted = note.isMissionCompleted();
        // Sử dụng currentTime được truyền vào để xác định isOverdue
        boolean isOverdue = !isCompleted &&
                note.getAlarm() != null &&
                note.getAlarm().getAlarmTime().isBefore(currentTime) && // <- SỬ DỤNG currentTime
                !note.getAlarm().isRecurring();

        if (isCompleted) {
            missionPanelBackgroundColor = new Color(220, 255, 220); // Xanh
        } else if (isOverdue) {
            missionPanelBackgroundColor = new Color(230, 230, 230); // Xám
        }
        panel.setBackground(missionPanelBackgroundColor);

        // ... (Phần còn lại của createMissionPanel giữ nguyên) ...
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(missionPanelBackgroundColor);
        controlPanel.setOpaque(true);

        JCheckBox completeCheckbox = new JCheckBox("Done");
        completeCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        completeCheckbox.setSelected(note.isMissionCompleted());
        completeCheckbox.setOpaque(false);
        completeCheckbox.addActionListener(e -> {
            controller.completeMission(note, completeCheckbox.isSelected());
            refreshMissions();
        });
        controlPanel.add(completeCheckbox, BorderLayout.WEST);

        if (deleteMode) {
            JCheckBox deleteCheckbox = new JCheckBox("Delete");
            deleteCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            deleteCheckbox.setOpaque(false);
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
                    toggleDeleteMode();
                }
            });
        }
        panel.add(controlPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(missionPanelBackgroundColor);
        contentPanel.setOpaque(true);

        JLabel titleLabel = new JLabel(note.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel contentLabel = new JLabel("<html>" + truncateText(note.getMissionContent(), 100) + "</html>");
        contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        contentPanel.add(contentLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBackground(missionPanelBackgroundColor);
        infoPanel.setOpaque(true);

        infoPanel.add(new JLabel("Created: " + note.getFormattedModificationDate()));
        String alarmText = note.getAlarm() != null ? formatAlarm(note.getAlarm()) : "No Alarm";
        JLabel alarmLabel = new JLabel("Alarm: " + alarmText);
        alarmLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        alarmLabel.setForeground(Color.BLUE);
        alarmLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        alarmLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!note.isMissionCompleted() || note.getAlarm() != null) {
                    showAlarmDialog(note);
                }
            }
        });
        infoPanel.add(alarmLabel);
        contentPanel.add(infoPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!deleteMode && e.getButton() == MouseEvent.BUTTON1) {
                    Component sourceParent = e.getComponent();
                    Point pointInControlPanel = SwingUtilities.convertPoint(sourceParent, e.getPoint(), controlPanel);
                    Point pointInInfoPanel = SwingUtilities.convertPoint(sourceParent, e.getPoint(), infoPanel);

                    if (controlPanel.contains(pointInControlPanel)) {
                        return;
                    }
                    if (infoPanel.contains(pointInInfoPanel)) {
                        Point pointInAlarmLabel = SwingUtilities.convertPoint(sourceParent, e.getPoint(), alarmLabel);
                        if (alarmLabel.contains(pointInAlarmLabel)) {
                            return;
                        }
                    }

                    MissionDialog dialog = new MissionDialog(mainFrame);
                    dialog.setMission(note.getMissionContent());
                    dialog.setTitle("Edit Mission: " + note.getTitle());
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

    // Các phương thức showAlarmDialog, formatAlarm, truncateText giữ nguyên
    private void showAlarmDialog(Note note) {
        JDialog dialog = new JDialog(mainFrame, "Alarm Details", true);
        dialog.setResizable(false);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Alarm alarm = note.getAlarm();
        LocalDateTime initialDateTime = alarm != null ? alarm.getAlarmTime() : LocalDateTime.now().withSecond(0).withNano(0);
        String alarmType = alarm != null && alarm.isRecurring() ? alarm.getFrequency() : "ONCE";

        JLabel currentAlarmLabel = new JLabel("Current: " + (alarm != null ? formatAlarm(alarm) : "No alarm set"));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        dialog.add(currentAlarmLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        dialog.add(new JLabel("Set Alarm Type:"), gbc);
        String[] alarmTypes = {"ONCE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        JComboBox<String> typeComboBox = new JComboBox<>(alarmTypes);
        typeComboBox.setSelectedItem(alarmType);
        gbc.gridx = 1;
        dialog.add(typeComboBox, gbc);

        JPanel dateTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField dateField = new JTextField(10);
        dateField.setText(initialDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        SpinnerNumberModel hourModel = new SpinnerNumberModel(initialDateTime.getHour(), 0, 23, 1);
        JSpinner hourSpinner = new JSpinner(hourModel);
        ((JSpinner.DefaultEditor) hourSpinner.getEditor()).getTextField().setColumns(2);

        SpinnerNumberModel minuteModel = new SpinnerNumberModel(initialDateTime.getMinute(), 0, 59, 1);
        JSpinner minuteSpinner = new JSpinner(minuteModel);
        ((JSpinner.DefaultEditor) minuteSpinner.getEditor()).getTextField().setColumns(2);

        Runnable updateDateTimePanelLambda = () -> {
            dateTimePanel.removeAll();
            String selectedType = (String) typeComboBox.getSelectedItem();
            if ("ONCE".equals(selectedType)) {
                dateTimePanel.add(new JLabel("Date:"));
                dateTimePanel.add(dateField);
                dateField.setText(initialDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            dateTimePanel.add(new JLabel(" H:"));
            dateTimePanel.add(hourSpinner);
            dateTimePanel.add(new JLabel(" M:"));
            dateTimePanel.add(minuteSpinner);
            dateTimePanel.revalidate();
            dateTimePanel.repaint();
            dialog.pack();
        };

        typeComboBox.addActionListener(e -> updateDateTimePanelLambda.run());
        updateDateTimePanelLambda.run();

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        dialog.add(dateTimePanel, gbc);

        JButton updateButton = new JButton(alarm != null ? "Update Alarm" : "Set Alarm");
        updateButton.addActionListener(e -> {
            try {
                String selectedTypeStr = (String) typeComboBox.getSelectedItem();
                boolean isRecurringFlag = !"ONCE".equals(selectedTypeStr);
                LocalDateTime newTime;
                int hour = (int) hourSpinner.getValue();
                int minute = (int) minuteSpinner.getValue();

                if ("ONCE".equals(selectedTypeStr)) {
                    DateTimeFormatter inputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    LocalDate dateOnly = LocalDate.parse(dateField.getText(), inputDateFormatter);
                    LocalDateTime datePart = dateOnly.atStartOfDay();
                    newTime = datePart.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                    if (newTime.isBefore(LocalDateTime.now())) {
                        JOptionPane.showMessageDialog(dialog, "Alarm time for 'ONCE' type must be in the future.", "Warning", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } else {
                    LocalDateTime baseDateForRecurring = (alarm != null && alarm.isRecurring()) ? alarm.getAlarmTime() : LocalDateTime.now();
                    newTime = baseDateForRecurring.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                }

                Alarm newAlarm = new Alarm(newTime, isRecurringFlag, selectedTypeStr);
                controller.setAlarm(note, newAlarm);
                refreshMissions();
                dialog.dispose();
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format! Please use yyyy-MM-dd.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error setting alarm: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        gbc.gridy = 3;
        dialog.add(updateButton, gbc);

        JButton deleteAlarmButton = new JButton("Delete Alarm");
        deleteAlarmButton.setEnabled(alarm != null);
        deleteAlarmButton.addActionListener(e -> {
            controller.setAlarm(note, null);
            refreshMissions();
            dialog.dispose();
        });
        gbc.gridy = 4;
        dialog.add(deleteAlarmButton, gbc);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        gbc.gridy = 5;
        dialog.add(cancelButton, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    private String formatAlarm(Alarm alarm) {
        DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        DateTimeFormatter formatterShort = DateTimeFormatter.ofPattern("HH:mm");
        if (alarm.isRecurring()) {
            return alarm.getAlarmTime().format(formatterShort) + " (" + alarm.getFrequency() + ")";
        } else {
            return alarm.getAlarmTime().format(formatterFull) + " (ONCE)";
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text == null ? "" : text;
        return text.substring(0, maxLength) + "...";
    }
}