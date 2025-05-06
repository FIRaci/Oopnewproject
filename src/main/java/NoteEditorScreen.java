import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;

public class NoteEditorScreen extends JPanel {
    private final MainFrame mainFrame;
    private final NoteController controller;
    private Note note;
    private JTextArea contentArea;
    private JTextField titleField;
    private JLabel wordCountLabel;
    private JPanel tagPanel;

    public NoteEditorScreen(MainFrame mainFrame, NoteController controller, Note note) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        this.note = note;
        setLayout(new BorderLayout(10, 10));
        initUI();
    }

    public void setNote(Note note) {
        this.note = note;
        titleField.setText(note.getTitle());
        contentArea.setText(note.getContent());
        updateTagDisplay();
        updateWordCount();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new FlowLayout());
        titleField = new JTextField(note.getTitle());
        titleField.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topPanel.add(titleField);
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        topPanel.add(backButton);
        add(topPanel, BorderLayout.NORTH);

        contentArea = new JTextArea(note.getContent());
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                note.setContent(contentArea.getText());
                updateWordCount();
            }
        });
        JScrollPane scrollPane = new JScrollPane(contentArea);
        add(scrollPane, BorderLayout.CENTER);

        tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        updateTagDisplay();

        JButton autoTagButton = new JButton("Auto Tag");
        autoTagButton.addActionListener(e -> {
            System.out.println("Auto Tag Placeholder");
        });

        JButton addTagButton = new JButton("Add Tag");
        addTagButton.addActionListener(e -> {
            String tagName = JOptionPane.showInputDialog(mainFrame, "Enter tag name:");
            if (tagName != null && !tagName.trim().isEmpty()) {
                controller.addTag(note, new Tag(tagName));
                updateTagDisplay();
            }
        });

        JButton alarmButton = new JButton("Set Alarm");
        alarmButton.addActionListener(e -> {
            String timeStr = JOptionPane.showInputDialog(mainFrame, "Enter alarm time (yyyy-MM-dd HH:mm):");
            if (timeStr != null && !timeStr.trim().isEmpty()) {
                try {
                    LocalDateTime alarmTime = LocalDateTime.parse(timeStr + ":00", java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    controller.setAlarm(note, new Alarm(alarmTime, true, "ONCE"));
                    JOptionPane.showMessageDialog(mainFrame, "Alarm set for " + alarmTime);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainFrame, "Invalid time format!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveNote());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(alarmButton);
        buttonPanel.add(addTagButton);
        buttonPanel.add(autoTagButton);
        buttonPanel.add(saveButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(tagPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Status Bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        wordCountLabel = new JLabel("Words: " + note.getWordCount());
        wordCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        statusPanel.add(wordCountLabel, BorderLayout.WEST);

        JLabel modifiedLabel = new JLabel("Last modified: " + note.getFormattedModificationDate());
        modifiedLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        statusPanel.add(modifiedLabel, BorderLayout.EAST);

        bottomPanel.add(statusPanel, BorderLayout.NORTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateWordCount() {
        int count = contentArea.getText().trim().split("\\s+").length;
        if (contentArea.getText().trim().isEmpty()) count = 0;
        wordCountLabel.setText("Words: " + count);
    }

    private void updateTagDisplay() {
        tagPanel.removeAll();
        for (Tag tag : note.getTags()) {
            JPanel tagItem = new JPanel(new BorderLayout());
            tagItem.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            tagItem.setBackground(new Color(240, 240, 240));

            JLabel tagLabel = new JLabel(tag.getName());
            JButton removeButton = new JButton("x");
            removeButton.setMargin(new Insets(0, 5, 0, 5));
            removeButton.addActionListener(e -> {
                controller.removeTag(note, tag);
                updateTagDisplay();
            });

            tagItem.add(tagLabel, BorderLayout.CENTER);
            tagItem.add(removeButton, BorderLayout.EAST);
            tagPanel.add(tagItem);
        }
        tagPanel.revalidate();
        tagPanel.repaint();
    }

    private void saveNote() {
        try {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Title cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            controller.updateNote(note, title, contentArea.getText());
            if (!controller.getNotes().contains(note)) {
                controller.addNote(note);
            }
            JOptionPane.showMessageDialog(mainFrame, "Note saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            mainFrame.showMainMenuScreen();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(mainFrame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
