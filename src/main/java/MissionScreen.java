import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
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
        setPreferredSize(new Dimension(800, 500)); // Giữ nguyên kích thước khung
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
        missionContainer.setPreferredSize(new Dimension(800, 500)); // Khớp với khung
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

        // Add mission panels
        for (Note note : controller.getMissions()) {
            missionContainer.add(createMissionPanel(note));
        }

        // Add "Add Mission" panel
        JPanel addMissionPanel = new JPanel(new BorderLayout());
        addMissionPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        addMissionPanel.setPreferredSize(new Dimension(350, 180)); // Tăng gấp đôi
        JButton addButton = new JButton("+");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 32)); // Tăng font để khớp kích thước
        addButton.addActionListener(e -> {
            MissionDialog dialog = new MissionDialog(mainFrame);
            dialog.setVisible(true);
            String result = dialog.getResult();
            if (result != null && !result.trim().isEmpty()) {
                Note newNote = new Note("New Mission", "", false);
                newNote.setMissionContent(result);
                newNote.setMission(true);
                controller.addNote(newNote);
                refreshMissions();
            }
        });
        addMissionPanel.add(addButton, BorderLayout.CENTER);
        missionContainer.add(addMissionPanel);

        missionContainer.revalidate();
        missionContainer.repaint();
    }

    private JPanel createMissionPanel(Note note) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setPreferredSize(new Dimension(350, 180)); // Tăng gấp đôi kích thước panel

        // Kiểm tra trạng thái xám
        boolean isGrayed = note.getAlarm() != null && note.getAlarm().getAlarmTime().isBefore(LocalDateTime.now()) && !note.getAlarm().isRecurring();
        if (isGrayed) {
            panel.setBackground(Color.LIGHT_GRAY);
        }

        // Checkbox hoàn thành
        JCheckBox completeCheckbox = new JCheckBox("Done");
        completeCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Tăng font để khớp kích thước
        completeCheckbox.setSelected(note.isMissionCompleted());
        completeCheckbox.addActionListener(e -> {
            controller.completeMission(note, completeCheckbox.isSelected());
            refreshMissions();
        });
        panel.add(completeCheckbox, BorderLayout.NORTH);

        // Nội dung mission
        JPanel contentPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(note.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20)); // Tăng font để khớp kích thước
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel contentLabel = new JLabel("<html>" + truncateText(note.getMissionContent(), 100) + "</html>"); // Tăng giới hạn ký tự
        contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Tăng font để khớp kích thước
        contentPanel.add(contentLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.add(new JLabel("Created: " + note.getFormattedModificationDate()));
        String alarmText = note.getAlarm() != null ? note.getAlarm().toString() : "No Alarm";
        JLabel alarmLabel = new JLabel("Alarm: " + truncateText(alarmText, 40)); // Tăng giới hạn ký tự
        alarmLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Tăng font để khớp kích thước
        infoPanel.add(alarmLabel);
        contentPanel.add(infoPanel, BorderLayout.SOUTH);

        panel.add(contentPanel, BorderLayout.CENTER);

        // Checkbox xóa (chỉ hiển thị ở delete mode)
        if (deleteMode) {
            JCheckBox deleteCheckbox = new JCheckBox("Delete");
            deleteCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Tăng font để khớp kích thước
            deleteCheckboxes.add(deleteCheckbox);
            panel.add(deleteCheckbox, BorderLayout.SOUTH);
            deleteCheckbox.addActionListener(e -> {
                if (deleteCheckbox.isSelected()) {
                    controller.deleteNote(note);
                    refreshMissions();
                }
            });
        }

        // Click để chỉnh sửa
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

    // Hàm cắt ngắn nội dung để vừa khung lớn hơn
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}