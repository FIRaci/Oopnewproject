// File: MissionScreen.java
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate; // Keep this if showAlarmDialog or other parts use it
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Comparator; // Added this import
import java.util.stream.Collectors; // Ensure this is present for Collectors.toList()

public class MissionScreen extends JPanel {
    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel missionContainer;
    private JButton deleteButton;
    private boolean deleteMode = false;
    private JComboBox<String> filterComboBox;
    private JComboBox<String> sortComboBox;

    private static final String FILTER_ALL = "Tất cả Nhiệm vụ";
    private static final String FILTER_COMPLETED = "Đã Hoàn Thành";
    private static final String FILTER_INCOMPLETE = "Chưa Hoàn Thành";
    private static final String FILTER_OVERDUE = "Quá Hạn";

    private static final String SORT_DEFAULT = "Mặc định (Ưu tiên)";
    private static final String SORT_DUE_DATE_ASC = "Theo Ngày Hạn (Gần nhất)";
    private static final String SORT_DUE_DATE_DESC = "Theo Ngày Hạn (Xa nhất)";
    private static final String SORT_MODIFIED_DATE_DESC = "Theo Ngày Sửa Đổi (Mới nhất)";


    public MissionScreen(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Increased padding

        // Top Panel for controls
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0)); // Bottom margin for top panel

        // Filter and Sort Panel
        JPanel filterSortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterSortPanel.add(new JLabel("Lọc:"));
        filterComboBox = new JComboBox<>(new String[]{FILTER_ALL, FILTER_INCOMPLETE, FILTER_OVERDUE, FILTER_COMPLETED});
        filterComboBox.addActionListener(e -> refreshMissions());
        filterSortPanel.add(filterComboBox);

        filterSortPanel.add(Box.createHorizontalStrut(15)); // Spacer

        filterSortPanel.add(new JLabel("Sắp xếp:"));
        sortComboBox = new JComboBox<>(new String[]{SORT_DEFAULT, SORT_DUE_DATE_ASC, SORT_DUE_DATE_DESC, SORT_MODIFIED_DATE_DESC});
        sortComboBox.addActionListener(e -> refreshMissions());
        filterSortPanel.add(sortComboBox);

        topPanel.add(filterSortPanel, BorderLayout.WEST);


        deleteButton = new JButton("🗑 Xóa"); // Icon and text
        deleteButton.setToolTipText("Chuyển sang chế độ xóa nhiệm vụ");
        deleteButton.addActionListener(e -> toggleDeleteMode());
        topPanel.add(deleteButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        missionContainer = new JPanel();
        // Using MigLayout for more flexible grid-like layout that handles varying heights better
        // For MigLayout, you need to add the MigLayout JAR to your project.
        // If MigLayout is not available, fallback to GridLayout or another manager.
        // For simplicity, let's stick to GridLayout for now and advise user if more complex layout is needed.
        missionContainer.setLayout(new GridLayout(0, 3, 15, 15)); // rows, cols, hgap, vgap
        missionContainer.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));


        JScrollPane scrollPane = new JScrollPane(missionContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Usually not needed
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove scrollpane border

        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smoother scrolling
        scrollPane.getVerticalScrollBar().setBlockIncrement(80);

        add(scrollPane, BorderLayout.CENTER);
        refreshMissions();
    }

    private void toggleDeleteMode() {
        deleteMode = !deleteMode;
        deleteButton.setText(deleteMode ? "Hoàn Tất Xóa" : "🗑 Xóa");
        deleteButton.setToolTipText(deleteMode ? "Hoàn tất và thoát chế độ xóa" : "Chuyển sang chế độ xóa nhiệm vụ");
        refreshMissions(); // Re-render panels to show/hide delete checkboxes
    }

    public void refreshMissions() {
        missionContainer.removeAll();
        LocalDateTime consistencyNow = LocalDateTime.now();

        List<Note> missions = controller.getMissions(); // This already filters for isMission = true

        // Apply filtering
        String selectedFilter = (String) filterComboBox.getSelectedItem();
        if (selectedFilter != null) {
            missions = missions.stream().filter((Note note) -> { // Explicitly type 'note'
                boolean isCompleted = note.isMissionCompleted();
                boolean isOverdue = !isCompleted && note.getAlarm() != null &&
                        !note.getAlarm().isRecurring() &&
                        note.getAlarm().getAlarmTime().isBefore(consistencyNow);
                switch (selectedFilter) {
                    case FILTER_COMPLETED: return isCompleted;
                    case FILTER_INCOMPLETE: return !isCompleted;
                    case FILTER_OVERDUE: return isOverdue;
                    case FILTER_ALL:
                    default: return true;
                }
            }).collect(java.util.stream.Collectors.toList());
        }


        // Apply sorting
        String selectedSort = (String) sortComboBox.getSelectedItem();
        if (selectedSort != null) {
            switch (selectedSort) {
                case SORT_DUE_DATE_ASC:
                    missions.sort(Comparator.comparing(
                            (Note note) -> (note.getAlarm() != null && note.getAlarm().getAlarmTime() != null) ? note.getAlarm().getAlarmTime() : LocalDateTime.MAX, // Explicitly type 'note'
                            Comparator.nullsLast(LocalDateTime::compareTo)
                    ).thenComparing(Note::getModificationDate, Comparator.reverseOrder())); // Secondary sort by modified
                    break;
                case SORT_DUE_DATE_DESC:
                    missions.sort(Comparator.comparing(
                            (Note note) -> (note.getAlarm() != null && note.getAlarm().getAlarmTime() != null) ? note.getAlarm().getAlarmTime() : LocalDateTime.MIN, // Explicitly type 'note'
                            Comparator.nullsFirst(LocalDateTime::compareTo)
                    ).reversed().thenComparing(Note::getModificationDate, Comparator.reverseOrder()));
                    break;
                case SORT_MODIFIED_DATE_DESC:
                    missions.sort(Comparator.comparing(Note::getModificationDate, Comparator.reverseOrder()));
                    break;
                case SORT_DEFAULT:
                default:
                    // Default sorting logic (Still Due (by alarm) -> Overdue (by alarm) -> Done (by modified))
                    missions.sort((n1, n2) -> {
                        boolean n1Completed = n1.isMissionCompleted();
                        boolean n2Completed = n2.isMissionCompleted();
                        boolean n1IsOverdue = !n1Completed && n1.getAlarm() != null && !n1.getAlarm().isRecurring() && n1.getAlarm().getAlarmTime().isBefore(consistencyNow);
                        boolean n2IsOverdue = !n2Completed && n2.getAlarm() != null && !n2.getAlarm().isRecurring() && n2.getAlarm().getAlarmTime().isBefore(consistencyNow);

                        int cat1 = n1Completed ? 3 : (n1IsOverdue ? 2 : 1);
                        int cat2 = n2Completed ? 3 : (n2IsOverdue ? 2 : 1);

                        if (cat1 != cat2) return Integer.compare(cat1, cat2);

                        if (cat1 == 1) { // Both Still Due
                            LocalDateTime t1 = (n1.getAlarm() != null && n1.getAlarm().getAlarmTime() != null) ? n1.getAlarm().getAlarmTime() : LocalDateTime.MAX;
                            LocalDateTime t2 = (n2.getAlarm() != null && n2.getAlarm().getAlarmTime() != null) ? n2.getAlarm().getAlarmTime() : LocalDateTime.MAX;
                            int alarmCompare = t1.compareTo(t2);
                            if (alarmCompare != 0) return alarmCompare;
                        } else if (cat1 == 2) { // Both Overdue
                            LocalDateTime t1 = (n1.getAlarm() != null && n1.getAlarm().getAlarmTime() != null) ? n1.getAlarm().getAlarmTime() : LocalDateTime.MIN; // Should not be null if overdue
                            LocalDateTime t2 = (n2.getAlarm() != null && n2.getAlarm().getAlarmTime() != null) ? n2.getAlarm().getAlarmTime() : LocalDateTime.MIN;
                            int alarmCompare = t1.compareTo(t2);
                            if (alarmCompare != 0) return alarmCompare;
                        }
                        // For Done, or if alarms are same for Still Due/Overdue, sort by modification date desc
                        return n2.getModificationDate().compareTo(n1.getModificationDate());
                    });
                    break;
            }
        }


        for (Note note : missions) {
            JPanel missionPanel = createMissionPanel(note, consistencyNow);
            missionContainer.add(missionPanel);
        }

        missionContainer.revalidate();
        missionContainer.repaint();
    }

    private JPanel createMissionPanel(Note note, LocalDateTime currentTime) {
        JPanel panel = new JPanel(new BorderLayout(8, 8)); // Increased gaps
        panel.setBorder(new CompoundBorder(
                new LineBorder(UIManager.getColor("Component.borderColor"), 1, true), // Rounded border
                new EmptyBorder(10, 10, 10, 10) // Padding inside
        ));
        // panel.setPreferredSize(new Dimension(350, 180)); // Keep for consistency if GridLayout is used
        // panel.setMaximumSize(new Dimension(380, 200)); // Allow slightly more flexibility

        Color missionPanelBackgroundColor = UIManager.getColor("Panel.background");
        boolean isCompleted = note.isMissionCompleted();
        boolean isOverdue = !isCompleted &&
                note.getAlarm() != null &&
                note.getAlarm().getAlarmTime().isBefore(currentTime) &&
                !note.getAlarm().isRecurring();

        Color titleColor = UIManager.getColor("Label.foreground");

        if (isCompleted) {
            missionPanelBackgroundColor = new Color(220, 255, 220); // Light green
            titleColor = new Color(70, 150, 70); // Darker green for title
        } else if (isOverdue) {
            missionPanelBackgroundColor = new Color(255, 220, 220); // Light red/pink
            titleColor = new Color(180, 50, 50); // Darker red for title
        }
        panel.setBackground(missionPanelBackgroundColor);


        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setOpaque(false); // Make it transparent to show panel's background

        JCheckBox completeCheckbox = new JCheckBox("Hoàn thành");
        completeCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        completeCheckbox.setSelected(note.isMissionCompleted());
        completeCheckbox.setOpaque(false);
        completeCheckbox.addActionListener(e -> {
            controller.completeMission(note, completeCheckbox.isSelected());
            refreshMissions();
        });
        controlPanel.add(completeCheckbox, BorderLayout.WEST);

        if (deleteMode) {
            JCheckBox deleteCheckbox = new JCheckBox("Xóa?");
            deleteCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            deleteCheckbox.setForeground(Color.RED);
            deleteCheckbox.setOpaque(false);
            controlPanel.add(deleteCheckbox, BorderLayout.EAST);
            deleteCheckbox.addActionListener(e -> {
                if (deleteCheckbox.isSelected()) {
                    int option = JOptionPane.showConfirmDialog(mainFrame,
                            "Xóa toàn bộ ghi chú '" + note.getTitle() + "' hay chỉ xóa nhiệm vụ?\n" +
                                    "Yes: Xóa toàn bộ ghi chú (bao gồm nhiệm vụ và báo thức)\n" +
                                    "No: Chỉ xóa nội dung nhiệm vụ (giữ lại ghi chú và báo thức nếu có)\n" +
                                    "Cancel: Không làm gì",
                            "Xác Nhận Xóa", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        controller.deleteNote(note); // This will delete the note and associated mission/alarm
                    } else if (option == JOptionPane.NO_OPTION) {
                        controller.updateMission(note, ""); // Clear mission content, keeps the note
                    }
                    // No matter the choice (unless cancel), refresh and potentially exit delete mode
                    if (option != JOptionPane.CANCEL_OPTION) {
                        toggleDeleteMode(); // Exit delete mode after action
                    } else {
                        deleteCheckbox.setSelected(false); // Uncheck if cancelled
                    }
                }
            });
        }
        panel.add(controlPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(5,3));
        contentPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(note.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(titleColor);
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea contentArea = new JTextArea(truncateText(note.getMissionContent(), 120));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setOpaque(false); // Transparent background
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentArea.setForeground(UIManager.getColor("TextArea.foreground"));
        // Add a bit of margin if it's not inheriting from panel border
        contentArea.setBorder(BorderFactory.createEmptyBorder(2,0,2,0));
        contentPanel.add(new JScrollPane(contentArea) {{
            setOpaque(false);
            getViewport().setOpaque(false);
            setBorder(null);
        }}, BorderLayout.CENTER);


        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2)); // Small vgap
        infoPanel.setOpaque(false);

        JLabel createdLabel = new JLabel("Sửa đổi: " + note.getFormattedModificationDate());
        createdLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        infoPanel.add(createdLabel);

        String alarmText = note.getAlarm() != null ? formatAlarm(note.getAlarm()) : "Chưa có báo thức";
        JLabel alarmLabel = new JLabel("⏰ " + alarmText); // Added icon
        alarmLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        alarmLabel.setForeground(UIManager.getColor("Label.foreground")); // Use theme color
        if (note.getAlarm() != null) { // Only make it clickable if there's an alarm
            alarmLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            alarmLabel.setToolTipText("Nhấn để sửa báo thức");
            alarmLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!note.isMissionCompleted() || note.getAlarm() != null) { // Allow editing alarm even if completed
                        showAlarmDialog(note);
                    }
                }
            });
        }
        infoPanel.add(alarmLabel);
        contentPanel.add(infoPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        // Make the entire panel clickable to edit mission (if not in delete mode)
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (deleteMode) return; // Do nothing if in delete mode

                // Check if click was on an interactive component within the panel
                Component clickedComponent = panel.getComponentAt(e.getPoint());
                if (clickedComponent instanceof JCheckBox || clickedComponent instanceof JButton) {
                    return; // Let the checkbox/button handle its own action
                }
                if (alarmLabel.getBounds().contains(SwingUtilities.convertPoint(panel, e.getPoint(), alarmLabel.getParent())) && note.getAlarm() != null) {
                    return; // Click was on alarm label, let its listener handle
                }


                MissionDialog dialog = new MissionDialog(mainFrame);
                if(mainFrame.getMouseEventDispatcher() != null) mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
                dialog.setMission(note.getMissionContent());
                dialog.setTitle("Sửa Nhiệm vụ: " + note.getTitle());
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    String result = dialog.getResult();
                    controller.updateMission(note, result); // Controller handles null
                    refreshMissions();
                }
            }
        });
        return panel;
    }

    private void showAlarmDialog(Note note) {
        // Using the AlarmDialog class now
        AlarmDialog alarmDialog = new AlarmDialog(mainFrame, note.getAlarm());
        if(mainFrame.getMouseEventDispatcher() != null) mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(alarmDialog);
        alarmDialog.setVisible(true);

        if (alarmDialog.isOkPressed()) {
            Alarm resultAlarm = alarmDialog.getResult();
            controller.setAlarm(note, resultAlarm); // Controller handles if resultAlarm is null (delete) or new/updated
            refreshMissions(); // Refresh to show updated alarm status
        }
    }

    private String formatAlarm(Alarm alarm) {
        if (alarm == null || alarm.getAlarmTime() == null) return "N/A";
        DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
        DateTimeFormatter formatterShort = DateTimeFormatter.ofPattern("HH:mm");
        if (alarm.isRecurring()) {
            return alarm.getAlarmTime().format(formatterShort) + " (" + alarm.getRecurrencePattern() + ")";
        } else {
            return alarm.getAlarmTime().format(formatterFull);
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text == null ? "" : text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
