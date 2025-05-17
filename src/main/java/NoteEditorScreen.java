import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
// import java.time.LocalDateTime; // Không thấy sử dụng trực tiếp trong code bạn gửi, có thể bỏ nếu không cần
// Đảm bảo import TranslationService và MessageDisplayDialog nếu chúng nằm ở package khác
// Ví dụ:
// import your.package.name.TranslationService;
// import your.package.name.MessageDisplayDialog;
// Nếu cùng package thì không cần import tường minh

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
        if (undoManager != null) {
            undoManager.discardAllEdits();
        }
    }

    private void updateTagDisplay() {
        tagPanel.removeAll();
        if (note != null && note.getTags() != null) { // Thêm kiểm tra null cho note và getTags
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
        if (note != null) { // Thêm kiểm tra null
            modifiedLabel.setText("Last modified: " + note.getFormattedModificationDate());
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        titleField = new JTextField(note.getTitle());
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16)); // Sử dụng Font.SANS_SERIF thay vì "Font.SANS_SERIF"
        topPanel.add(new JLabel("Title: "), BorderLayout.WEST); // Thêm khoảng trắng sau Title:
        topPanel.add(titleField, BorderLayout.CENTER);

        JButton backButton = new JButton(BACK_LABEL);
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        topPanel.add(backButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        contentField = new JTextArea(note.getContent());
        contentField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); // Sử dụng Font.SANS_SERIF
        contentField.setLineWrap(true);
        contentField.setWrapStyleWord(true);
        contentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateWordCount();
            }
        });

        undoManager = new UndoManager();
        contentField.getDocument().addUndoableEditListener(undoManager);

        add(new JScrollPane(contentField), BorderLayout.CENTER);

        tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        tagPanel.setBorder(BorderFactory.createTitledBorder("Tags"));
        updateTagDisplay();

        JPanel statusPanel = new JPanel(new BorderLayout());
        wordCountLabel = new JLabel("Words: 0"); // Khởi tạo với giá trị mặc định
        wordCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        statusPanel.add(wordCountLabel, BorderLayout.WEST);

        modifiedLabel = new JLabel("Last modified: N/A"); // Khởi tạo với giá trị mặc định
        modifiedLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        statusPanel.add(modifiedLabel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusPanel, BorderLayout.NORTH);
        bottomPanel.add(tagPanel, BorderLayout.CENTER);
        bottomPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
        updateWordCount();
        updateModifiedTime();
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addTagButton = new JButton(ADD_TAG_LABEL);
        addTagButton.addActionListener(e -> {
            String tagName = JOptionPane.showInputDialog(mainFrame, "Enter tag name:");
            if (tagName != null && !tagName.trim().isEmpty()) {
                if (note != null) { // Đảm bảo note không null
                    controller.addTag(note, new Tag(tagName));
                    updateTagDisplay();
                }
            }
        });
        buttonPanel.add(addTagButton);

        JButton addAlarmButton = new JButton(ADD_ALARM_LABEL);
        addAlarmButton.addActionListener(e -> {
            AlarmDialog dialog = new AlarmDialog(mainFrame); // Giả sử bạn có AlarmDialog
            dialog.setVisible(true);
            Alarm alarm = dialog.getResult();
            if (alarm != null && note != null) { // Đảm bảo note không null
                controller.setAlarm(note, alarm);
                JOptionPane.showMessageDialog(mainFrame, "Alarm set successfully!");
            }
        });
        buttonPanel.add(addAlarmButton);

        JButton editMissionButton = new JButton(EDIT_MISSION_LABEL);
        editMissionButton.addActionListener(e -> {
            MissionDialog dialog = new MissionDialog(mainFrame); // Giả sử bạn có MissionDialog
            dialog.setMission(note != null ? note.getMissionContent() : "");
            dialog.setVisible(true);
            String result = dialog.getResult();
            if (result != null && note != null) {
                controller.updateMission(note, result);
            }
        });
        buttonPanel.add(editMissionButton);

        JButton translateButton = new JButton(TRANSLATE_LABEL);
        translateButton.addActionListener(e -> {
            String contentToTranslate = contentField.getText();
            if (contentToTranslate == null || contentToTranslate.trim().isEmpty()) {
                // Sử dụng MessageDisplayDialog cho thông báo này nếu muốn đồng bộ
                TranslateDisplayDialog.showMessage(mainFrame,
                        "Thông báo",
                        new JLabel("  Không có nội dung để dịch.  ")); // Thêm padding cho JLabel
                // Hoặc giữ JOptionPane:
                // JOptionPane.showMessageDialog(mainFrame, "Không có nội dung để dịch.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] languages = {
                    "Vietnamese", "English", "French", "Spanish", "German",
                    "Japanese", "Chinese", "Korean", "Hindi", "Arabic",
                    "Portuguese", "Russian", "Bengali", "Urdu", "Indonesian",
                    "Turkish", "Italian", "Dutch", "Polish", "Swahili",
                    "Thai", "Malay", "Tagalog", "Persian (Farsi)", "Punjabi",
                    "Javanese", "Telugu", "Marathi", "Tamil"
            };
            String targetLanguage = (String) JOptionPane.showInputDialog(
                    mainFrame,
                    "Chọn ngôn ngữ đích:",
                    "Dịch ngôn ngữ",
                    JOptionPane.PLAIN_MESSAGE, // Để không có icon mặc định của JOptionPane
                    null,
                    languages,
                    languages[0]
            );

            if (targetLanguage != null) {
                translateButton.setEnabled(false);
                translateButton.setText("Translating...");

                TranslationService.translate(contentToTranslate, targetLanguage, new TranslationService.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        SwingUtilities.invokeLater(() -> {
                            translateButton.setEnabled(true);
                            translateButton.setText(TRANSLATE_LABEL);

                            // Chuẩn bị JTextArea và JScrollPane cho kết quả dịch
                            JTextArea resultArea = new JTextArea(translatedText);
                            resultArea.setWrapStyleWord(true);
                            resultArea.setLineWrap(true);
                            resultArea.setEditable(false);
                            resultArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                            // resultArea.setBackground(mainFrame.getBackground()); // Thử bỏ để L&F tự xử lý

                            JScrollPane scrollPaneForResult = new JScrollPane(resultArea);
                            // Thiết lập kích thước ưu tiên cho scrollPane
                            int rows = Math.min(15, Math.max(3, translatedText.split("\r\n|\r|\n", -1).length + 1));
                            int cols = Math.min(60, Math.max(30, calculateApproxCols(translatedText, rows)));
                            resultArea.setRows(rows); // Gợi ý số dòng
                            resultArea.setColumns(cols); // Gợi ý số cột
                            // scrollPaneForResult.setPreferredSize(new Dimension(450, 250)); // Hoặc đặt kích thước cố định nếu cần

                            TranslateDisplayDialog.showMessage(
                                    mainFrame, // Owner
                                    "Translation Result (" + targetLanguage + ")", // Title
                                    scrollPaneForResult // Component chứa nội dung
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
                                    JOptionPane.ERROR_MESSAGE // Icon lỗi chuẩn
                            );
                        });
                    }
                });
            }
        });
        buttonPanel.add(translateButton);

        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.addActionListener(e -> saveNote());
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    // Hàm phụ trợ ước lượng số cột cần thiết cho JTextArea
    private int calculateApproxCols(String text, int rows) {
        if (text == null || rows <= 0) return 30;
        int totalChars = text.length();
        int maxLineLength = 0;
        for (String line : text.split("\r\n|\r|\n", -1)) {
            if (line.length() > maxLineLength) {
                maxLineLength = line.length();
            }
        }
        return Math.max(30, Math.min(70, Math.max(maxLineLength, (totalChars / rows) + 5)));
    }


    private void saveNote() {
        try {
            if (note == null) { // Xử lý trường hợp note có thể null
                note = new Note("", "", false); // Tạo note mới nếu chưa có
                if (!controller.getNotes().contains(note)) { // Chỉ thêm nếu là note hoàn toàn mới
                    controller.addNote(note);
                }
            }
            String newTitle = titleField.getText().trim();
            String newContent = contentField.getText();
            if (newTitle.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Title cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            note.setTitle(newTitle);
            note.setContent(newContent);
            note.updateModificationDate(); // Gọi sau khi đã set title và content

            // Kiểm tra lại logic add/update
            // Nếu note này đã có trong controller (ví dụ, được truyền vào từ màn hình chính để sửa)
            // thì không cần add lại. Nếu là note mới hoàn toàn (this.note ban đầu là new Note(...))
            // thì cần add.
            // Logic hiện tại: nếu note đã có trong list thì update, nếu không thì add.
            // Điều này có thể cần xem lại tùy theo luồng tạo/sửa note của bạn.
            // Giả sử: nếu note.getId() (hoặc một định danh duy nhất) đã tồn tại thì update, ngược lại add.
            // Hiện tại, controller.updateNote() có thể đã xử lý việc này.

            // controller.addOrUpdateNote(note); // Một phương thức gộp nếu có
            // Hoặc giữ logic cũ nếu nó đúng với cách controller hoạt động
            if (!controller.getNotes().contains(note)) { // Giả sử note mới có ID null
                controller.addNote(note);
            } else {
                controller.updateNote(note, newTitle, newContent); // updateNote nên tự tìm note bằng ID
            }


            updateModifiedTime();
            JOptionPane.showMessageDialog(mainFrame, "Note saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            mainFrame.showMainMenuScreen();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving note: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // In lỗi ra console để debug
        }
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "saveNote");
        actionMap.put("saveNote", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveNote();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK), "addTag");
        actionMap.put("addTag", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String tagName = JOptionPane.showInputDialog(mainFrame, "Enter tag name:");
                if (tagName != null && !tagName.trim().isEmpty() && note != null) {
                    controller.addTag(note, new Tag(tagName));
                    updateTagDisplay();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "goBack");
        actionMap.put("goBack", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                mainFrame.showMainMenuScreen();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "addAlarm");
        actionMap.put("addAlarm", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                AlarmDialog dialog = new AlarmDialog(mainFrame);
                dialog.setVisible(true);
                Alarm alarm = dialog.getResult();
                if (alarm != null && note != null) {
                    controller.setAlarm(note, alarm);
                    JOptionPane.showMessageDialog(mainFrame, "Alarm set successfully!");
                }
            }
        });
    }
}
