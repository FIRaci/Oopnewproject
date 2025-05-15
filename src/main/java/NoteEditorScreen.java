import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
// Đảm bảo import TranslationService nếu nó nằm ở package khác
// Ví dụ: import hust.soict.hedspi.noteapp.service.TranslationService;
// Nếu TranslationService cùng package thì không cần import tường minh

public class NoteEditorScreen extends JPanel {
    private static final String SAVE_LABEL = "Save";
    private static final String BACK_LABEL = "Back";
    private static final String ADD_TAG_LABEL = "Add Tag";
    private static final String ADD_ALARM_LABEL = "Add Alarm";
    private static final String EDIT_MISSION_LABEL = "Edit Mission";
    private static final String TRANSLATE_LABEL = "Translate";

    private final MainFrame mainFrame;
    private final NoteController controller;
    private Note note;
    private JTextField titleField;
    private JTextArea contentField;
    private JPanel tagPanel;
    private JLabel wordCountLabel;
    private JLabel modifiedLabel;
    private UndoManager undoManager;

    public NoteEditorScreen(MainFrame mainFrame, NoteController controller, Note note) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        this.note = note != null ? note : new Note("New Note", "", false);
        initializeUI();
        setupShortcuts();
    }

    public void setNote(Note note) {
        this.note = note != null ? note : new Note("New Note", "", false);
        titleField.setText(this.note.getTitle());
        contentField.setText(this.note.getContent());
        updateTagDisplay();
        updateWordCount();
        updateModifiedTime();
        if (undoManager != null) { // Reset undo manager for new note
            undoManager.discardAllEdits();
        }
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

    private void updateWordCount() {
        String text = contentField.getText();
        int count = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        wordCountLabel.setText("Words: " + count);
    }

    private void updateModifiedTime() {
        modifiedLabel.setText("Last modified: " + note.getFormattedModificationDate());
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel with title field and back button
        JPanel topPanel = new JPanel(new BorderLayout());
        titleField = new JTextField(note.getTitle());
        titleField.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topPanel.add(new JLabel("Title:"), BorderLayout.WEST);
        topPanel.add(titleField, BorderLayout.CENTER);

        JButton backButton = new JButton(BACK_LABEL);
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        topPanel.add(backButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Content area
        contentField = new JTextArea(note.getContent());
        contentField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentField.setLineWrap(true);
        contentField.setWrapStyleWord(true);
        contentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateWordCount();
            }
        });

        // Khởi tạo UndoManager cho contentField
        undoManager = new UndoManager();
        contentField.getDocument().addUndoableEditListener(undoManager);

        add(new JScrollPane(contentField), BorderLayout.CENTER);

        // Tag panel
        tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        tagPanel.setBorder(BorderFactory.createTitledBorder("Tags"));
        updateTagDisplay();

        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        wordCountLabel = new JLabel("Words: " + note.getWordCount());
        wordCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        statusPanel.add(wordCountLabel, BorderLayout.WEST);

        modifiedLabel = new JLabel("Last modified: " + note.getFormattedModificationDate());
        modifiedLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        statusPanel.add(modifiedLabel, BorderLayout.EAST);

        // Bottom panel with tags and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusPanel, BorderLayout.NORTH);
        bottomPanel.add(tagPanel, BorderLayout.CENTER);
        bottomPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
        updateWordCount(); // Initial word count
        updateModifiedTime(); // Initial modified time
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Add Tag button
        JButton addTagButton = new JButton(ADD_TAG_LABEL);
        addTagButton.addActionListener(e -> {
            String tagName = JOptionPane.showInputDialog(mainFrame, "Enter tag name:");
            if (tagName != null && !tagName.trim().isEmpty()) {
                controller.addTag(note, new Tag(tagName));
                updateTagDisplay();
            }
        });
        buttonPanel.add(addTagButton);

        // Add Alarm button
        JButton addAlarmButton = new JButton(ADD_ALARM_LABEL);
        addAlarmButton.addActionListener(e -> {
            AlarmDialog dialog = new AlarmDialog(mainFrame);
            dialog.setVisible(true);
            Alarm alarm = dialog.getResult();
            if (alarm != null) {
                controller.setAlarm(note, alarm);
                JOptionPane.showMessageDialog(mainFrame, "Alarm set successfully!");
            }
        });
        buttonPanel.add(addAlarmButton);

        // Edit Mission button
        JButton editMissionButton = new JButton(EDIT_MISSION_LABEL);
        editMissionButton.addActionListener(e -> {
            MissionDialog dialog = new MissionDialog(mainFrame);
            dialog.setMission(note != null ? note.getMissionContent() : "");
            dialog.setVisible(true);
            String result = dialog.getResult();
            if (result != null && note != null) {
                controller.updateMission(note, result);
            }
        });
        buttonPanel.add(editMissionButton);

        // Translate button - MODIFIED
        JButton translateButton = new JButton(TRANSLATE_LABEL);
        translateButton.addActionListener(e -> {
            String contentToTranslate = contentField.getText();
            if (contentToTranslate == null || contentToTranslate.trim().isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Không có nội dung để dịch.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Danh sách ngôn ngữ để chọn
            String[] languages = {"Vietnamese", "English", "French", "Spanish", "German", "Japanese", "Chinese", "Korean"};
            String targetLanguage = (String) JOptionPane.showInputDialog(
                    mainFrame,
                    "Chọn ngôn ngữ đích:",
                    "Dịch ngôn ngữ",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    languages,
                    languages[0] // Ngôn ngữ mặc định là Vietnamese
            );

            if (targetLanguage != null) { // Người dùng đã chọn một ngôn ngữ
                translateButton.setEnabled(false);
                translateButton.setText("Đang dịch...");

                TranslationService.translate(contentToTranslate, targetLanguage, new TranslationService.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        SwingUtilities.invokeLater(() -> {
                            translateButton.setEnabled(true);
                            translateButton.setText(TRANSLATE_LABEL);

                            JTextArea resultArea = new JTextArea(translatedText);
                            resultArea.setWrapStyleWord(true);
                            resultArea.setLineWrap(true);
                            resultArea.setEditable(false);
                            resultArea.setBackground(mainFrame.getBackground());
                            JScrollPane scrollPane = new JScrollPane(resultArea);
                            scrollPane.setPreferredSize(new Dimension(400, 200));

                            JOptionPane.showMessageDialog(
                                    mainFrame,
                                    scrollPane,
                                    "Translation Result (" + targetLanguage + ")",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        SwingUtilities.invokeLater(() -> {
                            translateButton.setEnabled(true);
                            translateButton.setText(TRANSLATE_LABEL);
                            JOptionPane.showMessageDialog(
                                    mainFrame,
                                    "Lỗi dịch: " + errorMessage,
                                    "Lỗi",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        });
                    }
                });
            }
            // Nếu người dùng nhấn Cancel (targetLanguage == null), không làm gì thêm
        });
        buttonPanel.add(translateButton);

        // Save button
        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.addActionListener(e -> saveNote());
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private void saveNote() {
        try {
            String newTitle = titleField.getText().trim();
            String newContent = contentField.getText(); // Không trim() content ở đây để giữ nguyên định dạng
            if (newTitle.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Title cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            note.setTitle(newTitle);
            note.setContent(newContent);
            note.updateModificationDate();
            if (!controller.getNotes().contains(note)) {
                controller.addNote(note);
            } else {
                controller.updateNote(note, newTitle, newContent);
            }
            updateModifiedTime();
            JOptionPane.showMessageDialog(mainFrame, "Note saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            mainFrame.showMainMenuScreen();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving note: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // Ctrl + S: Lưu ghi chú
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "saveNote");
        actionMap.put("saveNote", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveNote();
            }
        });

        // Ctrl + T: Mở dialog thêm tag
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK), "addTag");
        actionMap.put("addTag", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String tagName = JOptionPane.showInputDialog(mainFrame, "Enter tag name:");
                if (tagName != null && !tagName.trim().isEmpty()) {
                    controller.addTag(note, new Tag(tagName));
                    updateTagDisplay();
                }
            }
        });

        // Esc: Quay lại MainMenuScreen
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "goBack");
        actionMap.put("goBack", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                mainFrame.showMainMenuScreen();
            }
        });

        // Ctrl + Z: Hoàn tác trong contentField
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });

        // Ctrl + G: Mở dialog đặt báo thức
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "addAlarm");
        actionMap.put("addAlarm", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                AlarmDialog dialog = new AlarmDialog(mainFrame);
                dialog.setVisible(true);
                Alarm alarm = dialog.getResult();
                if (alarm != null) {
                    controller.setAlarm(note, alarm);
                    JOptionPane.showMessageDialog(mainFrame, "Alarm set successfully!");
                }
            }
        });
    }
}