import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelpScreen extends JDialog {

    public HelpScreen(Frame owner) {
        super(owner, "Help", true); // Modal dialog
        initializeUI();
    }

    private void initializeUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Tạo JTabbedPane cho 3 tab
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Tab 1: Shortcuts
        JScrollPane shortcutsPanel = createShortcutsPanel();
        tabbedPane.addTab("Shortcuts", shortcutsPanel);

        // Tab 2: Contributors
        JScrollPane contributorsPanel = createContributorsPanel();
        tabbedPane.addTab("Contributors", contributorsPanel);

        // Tab 3: Guide
        JPanel guidePanel = createGuidePanel();
        tabbedPane.addTab("Guide", guidePanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Nút Close
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Thiết lập kích thước và vị trí
        pack();
        setMinimumSize(new Dimension(400, Math.min(600, getHeight()))); // Chiều rộng tăng nhẹ để phù hợp với tab
        setLocationRelativeTo(getOwner());
        setResizable(true);
    }

    private JScrollPane createShortcutsPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        Map<String, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put("Ctrl + S", "Save current note");
        shortcuts.put("Ctrl + N", "Add a new note");
        shortcuts.put("Ctrl + T", "Add tag to current note");
        shortcuts.put("Ctrl + G", "Summarize note content");
        shortcuts.put("Ctrl + M", "Set/Edit mission for current note");
        shortcuts.put("Ctrl + D", "Translate note content");
        shortcuts.put("Ctrl + U", "Summarize note content");
        shortcuts.put("Ctrl + I", "AI Auto Tag current note");
        shortcuts.put("Ctrl + F", "Add a new folder (Main Screen)");
        shortcuts.put("Ctrl + Z", "Undo last action in editor");
        shortcuts.put("Esc", "Go back");
        shortcuts.put("---", "---");
        shortcuts.put("Ctrl + 1", "Switch to Notes screen");
        shortcuts.put("Ctrl + 2", "Switch to Missions screen");
        shortcuts.put("Ctrl + W", "Toggle light/dark theme");
        shortcuts.put("Ctrl + Q", "Exit application");

        Font keyFont = new Font("Segoe UI", Font.BOLD, 13);
        Font descriptionFont = new Font("Segoe UI", Font.PLAIN, 13);
        Color keyColor = UIManager.getColor("Label.foreground");
        Color descriptionColor = UIManager.getColor("Label.foreground");

        for (Map.Entry<String, String> entry : shortcuts.entrySet()) {
            if (entry.getKey().equals("---")) {
                contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                JSeparator separator = new JSeparator();
                contentPanel.add(separator);
                contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                continue;
            }

            JPanel shortcutEntryPanel = new JPanel(new BorderLayout(10, 0));
            JLabel keyLabel = new JLabel(entry.getKey());
            keyLabel.setFont(keyFont);
            keyLabel.setForeground(keyColor);

            JLabel descriptionLabel = new JLabel(entry.getValue());
            descriptionLabel.setFont(descriptionFont);
            descriptionLabel.setForeground(descriptionColor);

            shortcutEntryPanel.add(keyLabel, BorderLayout.WEST);
            shortcutEntryPanel.add(descriptionLabel, BorderLayout.CENTER);
            contentPanel.add(shortcutEntryPanel);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        return scrollPane;
    }

    private JScrollPane createContributorsPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Nội dung mẫu cho Contributors
        String[] contributors = {
                "Liem-san - Leader",
                "Shirin - AI Developer + Principal Funder",
                "FIRaci - Frontend Developer + Database Developer",
                "Yui - Bug Fixer + Tester",
                "Thanh - ???",
                "Missing some one here bruh! Do you know where he is ?"
        };

        Font contributorFont = new Font("Segoe UI", Font.PLAIN, 13);
        Color contributorColor = UIManager.getColor("Label.foreground");

        for (String contributor : contributors) {
            JLabel contributorLabel = new JLabel(contributor);
            contributorLabel.setFont(contributorFont);
            contributorLabel.setForeground(contributorColor);
            contentPanel.add(contributorLabel);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        return scrollPane;
    }

    private JPanel createGuidePanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextArea guideText = new JTextArea();
        guideText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        guideText.setLineWrap(true);
        guideText.setWrapStyleWord(true);
        guideText.setEditable(false);
        guideText.setBackground(contentPanel.getBackground());
        guideText.setText(
                "Welcome to XiNoClo!\n\n" +
                        "Getting Started:\n" +
                        "- Create a new note: Use Ctrl+N to start a new note.\n" +
                        "- Organize notes: Add notes to folders using Ctrl+F to create folders.\n" +
                        "- Add tags: Use Ctrl+T to add tags to your notes for better organization.\n" +
                        "- Set alarms: Use Ctrl+G to summarize or Ctrl+M to set missions for reminders.\n" +
                        "- Translate content: Use Ctrl+D to translate your note to various languages.\n" +
                        "- AI Auto Tag: Use Ctrl+I to let AI suggest tags based on content.\n" +
                        "- Save your work: Always save your notes with Ctrl+S.\n\n" +
                        "Tips:\n" +
                        "- Use Ctrl+Z to undo changes and Ctrl+Y to redo.\n" +
                        "- Switch between Notes and Missions screens with Ctrl+1 and Ctrl+2.\n" +
                        "- Toggle between light and dark themes using Ctrl+W.\n" +
                        "- Exit the application with Ctrl+Q."
        );

        JScrollPane scrollPane = new JScrollPane(guideText);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        return contentPanel;
    }

    public static void showDialog(Frame owner) {
        HelpScreen dialog = new HelpScreen(owner);
        dialog.setVisible(true);
    }
}