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
import java.util.Comparator;
import java.util.stream.Collectors;

public class MissionScreen extends JPanel {
    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel missionContainer;
    private JButton deleteButton;
    private boolean deleteMode = false;
    private JComboBox<String> filterComboBox;
    private JComboBox<String> sortComboBox;
    private JLabel filterLabel;
    private JLabel sortLabel;


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
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));

        JPanel filterSortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        // --- Filter ---
        filterLabel = new JLabel("🔍 Lọc:"); // Unicode for filter icon
        filterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        filterSortPanel.add(filterLabel);
        filterComboBox = new JComboBox<>(new String[]{FILTER_ALL, FILTER_INCOMPLETE, FILTER_OVERDUE, FILTER_COMPLETED});
        filterComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterComboBox.addActionListener(e -> refreshMissions());
        filterSortPanel.add(filterComboBox);

        filterSortPanel.add(Box.createHorizontalStrut(15));

        // --- Sort ---
        sortLabel = new JLabel("↕️ Sắp xếp:"); // Unicode for sort icon
        sortLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        filterSortPanel.add(sortLabel);
        sortComboBox = new JComboBox<>(new String[]{SORT_DEFAULT, SORT_DUE_DATE_ASC, SORT_DUE_DATE_DESC, SORT_MODIFIED_DATE_DESC});
        sortComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sortComboBox.addActionListener(e -> refreshMissions());
        filterSortPanel.add(sortComboBox);

        topPanel.add(filterSortPanel, BorderLayout.WEST);

        deleteButton = new JButton("🗑 Xóa");
        deleteButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14)); // Use a font that supports emojis well
        deleteButton.setToolTipText("Chuyển sang chế độ xóa nhiệm vụ");
        deleteButton.addActionListener(e -> toggleDeleteMode());
        topPanel.add(deleteButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        missionContainer = new JPanel();
        missionContainer.setLayout(new GridLayout(0, 3, 15, 15));
        missionContainer.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));

        JScrollPane scrollPane = new JScrollPane(missionContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBlockIncrement(80);

        add(scrollPane, BorderLayout.CENTER);
        refreshMissions();
    }

    private void toggleDeleteMode() {
        deleteMode = !deleteMode;
        deleteButton.setText(deleteMode ? "✅ Hoàn Tất Xóa" : "🗑 Xóa"); // Changed icon for "Done"
        deleteButton.setToolTipText(deleteMode ? "Hoàn tất và thoát chế độ xóa" : "Chuyển sang chế độ xóa nhiệm vụ");
        if(deleteMode) {
            deleteButton.setBackground(new Color(0xDC3545)); // A red color for delete mode
            deleteButton.setForeground(Color.WHITE);
        } else {
            deleteButton.setBackground(UIManager.getColor("Button.background"));
            deleteButton.setForeground(UIManager.getColor("Button.foreground"));
        }
        refreshMissions();
    }

    public void refreshMissions() {
        missionContainer.removeAll();
        LocalDateTime consistencyNow = LocalDateTime.now();

        List<Note> missions = controller.getMissions();

        String selectedFilter = (String) filterComboBox.getSelectedItem();
        if (selectedFilter != null) {
            missions = missions.stream().filter((Note note) -> {
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
            }).collect(Collectors.toList());
        }

        String selectedSort = (String) sortComboBox.getSelectedItem();
        if (selectedSort != null) {
            switch (selectedSort) {
                case SORT_DUE_DATE_ASC:
                    missions.sort(Comparator.comparing(
                            (Note note) -> (note.getAlarm() != null && note.getAlarm().getAlarmTime() != null) ? note.getAlarm().getAlarmTime() : LocalDateTime.MAX,
                            Comparator.nullsLast(LocalDateTime::compareTo)
                    ).thenComparing(Note::getModificationDate, Comparator.reverseOrder()));
                    break;
                case SORT_DUE_DATE_DESC:
                    missions.sort(Comparator.comparing(
                            (Note note) -> (note.getAlarm() != null && note.getAlarm().getAlarmTime() != null) ? note.getAlarm().getAlarmTime() : LocalDateTime.MIN,
                            Comparator.nullsFirst(LocalDateTime::compareTo)
                    ).reversed().thenComparing(Note::getModificationDate, Comparator.reverseOrder()));
                    break;
                case SORT_MODIFIED_DATE_DESC:
                    missions.sort(Comparator.comparing(Note::getModificationDate, Comparator.reverseOrder()));
                    break;
                case SORT_DEFAULT:
                default:
                    missions.sort((n1, n2) -> {
                        boolean n1Completed = n1.isMissionCompleted();
                        boolean n2Completed = n2.isMissionCompleted();
                        boolean n1IsOverdue = !n1Completed && n1.getAlarm() != null && !n1.getAlarm().isRecurring() && n1.getAlarm().getAlarmTime().isBefore(consistencyNow);
                        boolean n2IsOverdue = !n2Completed && n2.getAlarm() != null && !n2.getAlarm().isRecurring() && n2.getAlarm().getAlarmTime().isBefore(consistencyNow);

                        int cat1 = n1Completed ? 3 : (n1IsOverdue ? 2 : 1);
                        int cat2 = n2Completed ? 3 : (n2IsOverdue ? 2 : 1);

                        if (cat1 != cat2) return Integer.compare(cat1, cat2);

                        if (cat1 == 1) {
                            LocalDateTime t1 = (n1.getAlarm() != null && n1.getAlarm().getAlarmTime() != null) ? n1.getAlarm().getAlarmTime() : LocalDateTime.MAX;
                            LocalDateTime t2 = (n2.getAlarm() != null && n2.getAlarm().getAlarmTime() != null) ? n2.getAlarm().getAlarmTime() : LocalDateTime.MAX;
                            int alarmCompare = t1.compareTo(t2);
                            if (alarmCompare != 0) return alarmCompare;
                        } else if (cat1 == 2) {
                            LocalDateTime t1 = (n1.getAlarm() != null && n1.getAlarm().getAlarmTime() != null) ? n1.getAlarm().getAlarmTime() : LocalDateTime.MIN;
                            LocalDateTime t2 = (n2.getAlarm() != null && n2.getAlarm().getAlarmTime() != null) ? n2.getAlarm().getAlarmTime() : LocalDateTime.MIN;
                            int alarmCompare = t1.compareTo(t2);
                            if (alarmCompare != 0) return alarmCompare;
                        }
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
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new CompoundBorder(
                new LineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        Color missionPanelBackgroundColor = UIManager.getColor("Panel.background");
        boolean isCompleted = note.isMissionCompleted();
        boolean isOverdue = !isCompleted &&
                note.getAlarm() != null &&
                note.getAlarm().getAlarmTime().isBefore(currentTime) &&
                !note.getAlarm().isRecurring();

        Color titleColor = UIManager.getColor("Label.foreground");

        if (isCompleted) {
            missionPanelBackgroundColor = new Color(220, 255, 220);
            titleColor = new Color(70, 150, 70);
        } else if (isOverdue) {
            missionPanelBackgroundColor = new Color(255, 220, 220);
            titleColor = new Color(180, 50, 50);
        }
        panel.setBackground(missionPanelBackgroundColor);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setOpaque(false);

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
                        controller.deleteNote(note);
                    } else if (option == JOptionPane.NO_OPTION) {
                        controller.updateMission(note, "");
                    }
                    if (option != JOptionPane.CANCEL_OPTION) {
                        toggleDeleteMode();
                    } else {
                        deleteCheckbox.setSelected(false);
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
        contentArea.setOpaque(false);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentArea.setForeground(UIManager.getColor("TextArea.foreground"));
        contentArea.setBorder(BorderFactory.createEmptyBorder(2,0,2,0));
        JScrollPane contentScrollPane = new JScrollPane(contentArea);
        contentScrollPane.setOpaque(false);
        contentScrollPane.getViewport().setOpaque(false);
        contentScrollPane.setBorder(null);
        contentPanel.add(contentScrollPane, BorderLayout.CENTER);


        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        JLabel createdLabel = new JLabel("Sửa đổi: " + note.getFormattedModificationDate());
        createdLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        infoPanel.add(createdLabel);

        String alarmText = note.getAlarm() != null ? formatAlarm(note.getAlarm()) : "Chưa có báo thức";
        JLabel alarmLabel = new JLabel("⏰ " + alarmText);
        alarmLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        alarmLabel.setForeground(UIManager.getColor("Label.foreground"));
        if (note.getAlarm() != null) {
            alarmLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            alarmLabel.setToolTipText("Nhấn để sửa báo thức");
            alarmLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!note.isMissionCompleted() || note.getAlarm() != null) {
                        showAlarmDialog(note);
                    }
                }
            });
        }
        infoPanel.add(alarmLabel);
        contentPanel.add(infoPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (deleteMode) return;

                Component clickedComponent = panel.getComponentAt(e.getPoint());
                if (clickedComponent instanceof JCheckBox || clickedComponent instanceof JButton) {
                    return;
                }
                // Check if the click is on the alarmLabel or its parent (infoPanel)
                // This logic might need refinement if alarmLabel is deeply nested.
                if (alarmLabel.getBounds().contains(SwingUtilities.convertPoint(panel, e.getPoint(), alarmLabel.getParent())) && note.getAlarm() != null) {
                    if (alarmLabel.getBounds().contains(SwingUtilities.convertPoint(panel, e.getPoint(), alarmLabel))) { // More precise check
                        return; // Click was specifically on alarm label
                    }
                }


                MissionDialog dialog = new MissionDialog(mainFrame);
                if(mainFrame.getMouseEventDispatcher() != null) mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
                dialog.setMission(note.getMissionContent());
                dialog.setTitle("Sửa Nhiệm vụ: " + note.getTitle());
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    String result = dialog.getResult();
                    controller.updateMission(note, result);
                    refreshMissions();
                }
            }
        });
        return panel;
    }

    private void showAlarmDialog(Note note) {
        AlarmDialog alarmDialog = new AlarmDialog(mainFrame, note.getAlarm());
        if(mainFrame.getMouseEventDispatcher() != null) mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(alarmDialog);
        alarmDialog.setVisible(true);

        if (alarmDialog.isOkPressed()) {
            Alarm resultAlarm = alarmDialog.getResult();
            controller.setAlarm(note, resultAlarm);
            refreshMissions();
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
