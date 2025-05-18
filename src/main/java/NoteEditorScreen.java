import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException; // Có thể cần nếu bạn muốn bắt lỗi cụ thể ở đây

// Giả sử các lớp Dialog và Service đã được import đúng
// import com.yourpackage.AlarmDialog;
// import com.yourpackage.MissionDialog;
// import com.yourpackage.TranslateDisplayDialog;
// import com.yourpackage.TranslationService;

public class NoteEditorScreen extends JPanel {
    private static final String SAVE_LABEL = "Save"; // Giữ nguyên hằng số gốc
    private static final String BACK_LABEL = "Back";
    private static final String ADD_TAG_LABEL = "Add Tag";
    private static final String ADD_ALARM_LABEL = "Add Alarm"; // Sẽ thay đổi text động
    private static final String EDIT_MISSION_LABEL = "Edit Mission"; // Sẽ thay đổi text động
    private static final String TRANSLATE_LABEL = "Translate";

    private final MainFrame mainFrame;
    private final NoteController controller;
    private Note note; // Note hiện tại đang được chỉnh sửa hoặc tạo mới
    private JTextField titleField;
    private JTextArea contentField;
    private JPanel tagPanel; // Đây là panel FlowLayout gốc của bạn để chứa các tag items
    private JLabel wordCountLabel;
    private JLabel modifiedLabel;
    private UndoManager undoManager;

    // Các nút cần tham chiếu để cập nhật text động
    private JButton alarmButtonReference;
    private JButton missionButtonReference;


    public NoteEditorScreen(MainFrame mainFrame, NoteController controller, Note noteToEdit) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        // Nếu noteToEdit là null, tạo note mới. Ngược lại, chỉnh sửa note đã có.
        this.note = (noteToEdit != null) ? noteToEdit : new Note("New Note", "", false);
        initializeUI(); // Khởi tạo UI trước
        setNoteFields(this.note); // Sau đó điền dữ liệu và cập nhật text nút
        setupShortcuts();
    }

    public void setNote(Note noteToSet) {
        this.note = (noteToSet != null) ? noteToSet : new Note("New Note", "", false);
        setNoteFields(this.note);
    }

    private void setNoteFields(Note currentNote) {
        titleField.setText(currentNote.getTitle());
        contentField.setText(currentNote.getContent());
        contentField.setCaretPosition(0);
        updateTagDisplay();
        updateWordCount();
        updateModifiedTime();
        updateDynamicButtonTexts(); // Cập nhật text cho nút Alarm và Mission
        if (undoManager != null) {
            undoManager.discardAllEdits();
        }
    }

    private void updateDynamicButtonTexts() {
        if (alarmButtonReference != null) {
            alarmButtonReference.setText((note != null && note.getAlarmId() != null && note.getAlarmId() > 0) ? "Edit Alarm" : ADD_ALARM_LABEL);
        }
        if (missionButtonReference != null) {
            missionButtonReference.setText((note != null && note.isMission() && note.getMissionContent() != null && !note.getMissionContent().isEmpty()) ? EDIT_MISSION_LABEL : "Set Mission");
        }
    }

    private void updateTagDisplay() {
        if (tagPanel == null) return; // Kiểm tra null cho tagPanel
        tagPanel.removeAll();
        if (note != null && note.getTags() != null && !note.getTags().isEmpty()) {
            for (Tag tag : note.getTags()) {
                JPanel tagItem = new JPanel(new BorderLayout(3, 0)); // Khoảng cách nhỏ
                tagItem.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                tagItem.setBackground(new Color(240, 240, 240)); // Màu nền nhẹ

                JLabel tagLabel = new JLabel(tag.getName());
                tagLabel.setFont(tagLabel.getFont().deriveFont(11f));
                JButton removeButton = new JButton("x");
                removeButton.setMargin(new Insets(0, 2, 0, 2));
                removeButton.setFont(removeButton.getFont().deriveFont(9f));
                removeButton.setFocusPainted(false);
                removeButton.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(mainFrame,
                            "Remove tag '" + tag.getName() + "' from this note?",
                            "Confirm Remove Tag", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        controller.removeTag(note, tag); // Controller sẽ update note trong DB
                        // Note object 'this.note' có thể đã được controller cập nhật (nếu controller trả về note đã update)
                        // Hoặc chúng ta cần lấy lại note object mới từ controller sau khi removeTag
                        // Tạm thời giả định controller.removeTag đã cập nhật this.note hoặc refresh là đủ
                        updateTagDisplay();
                    }
                });
                tagItem.add(tagLabel, BorderLayout.CENTER);
                tagItem.add(removeButton, BorderLayout.EAST);
                tagPanel.add(tagItem);
            }
        } else {
            JLabel noTagsLabel = new JLabel("No tags yet.");
            noTagsLabel.setForeground(Color.GRAY);
            tagPanel.add(noTagsLabel);
        }
        tagPanel.revalidate();
        tagPanel.repaint();
    }

    private void updateWordCount() {
        if (contentField == null || wordCountLabel == null) return;
        String text = contentField.getText();
        int count = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        wordCountLabel.setText("Words: " + count);
    }

    private void updateModifiedTime() {
        if (note != null && modifiedLabel != null) {
            modifiedLabel.setText("Last modified: " + note.getFormattedModificationDate());
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Panel: Title and Back Button
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        titleField = new JTextField(); // Sẽ được set trong setNoteFields
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        topPanel.add(new JLabel("Title: "), BorderLayout.WEST);
        topPanel.add(titleField, BorderLayout.CENTER);

        JButton backButton = new JButton(BACK_LABEL);
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        topPanel.add(backButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel: Content Area
        contentField = new JTextArea(); // Sẽ được set trong setNoteFields
        contentField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
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

        // Bottom Panel: Status, Tags, Buttons
        JPanel bottomOuterPanel = new JPanel(new BorderLayout(0, 5));

        // Status Panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        wordCountLabel = new JLabel("Words: 0");
        wordCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0)); // Thêm padding trái
        modifiedLabel = new JLabel("Last modified: N/A");
        modifiedLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Thêm padding phải
        statusPanel.add(wordCountLabel, BorderLayout.WEST);
        statusPanel.add(modifiedLabel, BorderLayout.EAST);
        bottomOuterPanel.add(statusPanel, BorderLayout.NORTH);

        // Tag Panel (theo cấu trúc gốc của bạn)
        tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        tagPanel.setBorder(BorderFactory.createTitledBorder("Tags"));
        // JScrollPane cho tagPanel nếu có nhiều tag
        JScrollPane tagScrollPane = new JScrollPane(tagPanel);
        tagScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        tagScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tagScrollPane.setPreferredSize(new Dimension(0, 60)); // Giới hạn chiều cao
        bottomOuterPanel.add(tagScrollPane, BorderLayout.CENTER);

        // Button Panel
        bottomOuterPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        add(bottomOuterPanel, BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton addTagButton = new JButton(ADD_TAG_LABEL);
        addTagButton.setToolTipText("Add a new tag to this note (Ctrl+T)");
        addTagButton.addActionListener(e -> handleAddTagAction());
        buttonPanel.add(addTagButton);

        // Lưu tham chiếu nút để cập nhật text
        alarmButtonReference = new JButton(ADD_ALARM_LABEL); // Text sẽ được cập nhật trong setNoteFields
        alarmButtonReference.setToolTipText("Set or edit the alarm for this note (Ctrl+G)");
        alarmButtonReference.addActionListener(e -> handleSetAlarmAction());
        buttonPanel.add(alarmButtonReference);

        missionButtonReference = new JButton("Set Mission"); // Text sẽ được cập nhật
        missionButtonReference.setToolTipText("Set or edit the mission for this note");
        missionButtonReference.addActionListener(e -> handleEditMissionAction());
        buttonPanel.add(missionButtonReference);

        JButton translateButton = new JButton(TRANSLATE_LABEL);
        translateButton.setToolTipText("Translate the note content");
        translateButton.addActionListener(e -> handleTranslateAction());
        buttonPanel.add(translateButton);

        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.setToolTipText("Save changes (Ctrl+S)");
        saveButton.addActionListener(e -> saveNote());
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private void handleAddTagAction() {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrame, "Cannot add tag: Note is not loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String tagName = JOptionPane.showInputDialog(mainFrame, "Enter tag name:", "Add Tag", JOptionPane.PLAIN_MESSAGE);
        if (tagName != null && !tagName.trim().isEmpty()) {
            // Kiểm tra trùng tag (theo tên) trong object note hiện tại trước khi gọi controller
            boolean tagExistsInNote = note.getTags().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(tagName.trim()));
            if (tagExistsInNote) {
                JOptionPane.showMessageDialog(mainFrame, "Tag '" + tagName.trim() + "' is already on this note.", "Tag Exists", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            controller.addTag(note, new Tag(tagName.trim())); // Controller xử lý DB và cập nhật note object
            updateTagDisplay(); // Làm mới UI
        }
    }

    private void handleSetAlarmAction() {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrame, "Cannot set alarm: Note is not loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Sử dụng AlarmDialog.java (public class)
        AlarmDialog alarmDialog = new AlarmDialog(mainFrame);
        // TODO: AlarmDialog.java cần được cập nhật để có thể nhận và chỉnh sửa Alarm hiện tại.
        // Ví dụ: if (note.getAlarm() != null) alarmDialog.setAlarmToEdit(note.getAlarm());

        alarmDialog.setVisible(true);
        Alarm resultAlarm = alarmDialog.getResult(); // resultAlarm là object mới từ dialog

        if (resultAlarm != null) { // Cần kiểm tra null hoặc logic khác để quyết định khi nào tiếp tục
            if (resultAlarm != null) { // Người dùng đặt/sửa alarm
                // Nếu note đã có alarm, và dialog không trả về ID (nghĩa là nó tạo object mới)
                // thì ta cần gán ID của alarm cũ cho resultAlarm để service biết là update.
                if (note.getAlarm() != null && note.getAlarm().getId() > 0 && resultAlarm.getId() == 0) {
                    resultAlarm.setId(note.getAlarm().getId());
                }
                controller.setAlarm(note, resultAlarm);
            } else { // Người dùng có thể đã chọn xóa alarm từ trong AlarmDialog, hoặc dialog trả về null khi OK
                controller.setAlarm(note, null);
            }
            // Controller đã hiển thị thông báo thành công/lỗi
            updateDynamicButtonTexts(); // Cập nhật text nút "Edit Alarm" / "Add Alarm"
        }
        // Nếu người dùng bấm Cancel, không làm gì cả.
    }

    private void handleEditMissionAction() {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrame, "Cannot edit mission: Note is not loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        MissionDialog missionDialog = new MissionDialog(mainFrame);
        missionDialog.setMission(note.getMissionContent());
        missionDialog.setVisible(true);
        String resultMissionContent = missionDialog.getResult();

        if (resultMissionContent != null) { // Người dùng bấm Save
            controller.updateMission(note, resultMissionContent);
            updateDynamicButtonTexts(); // Cập nhật text nút "Edit Mission" / "Set Mission"
        }
    }

    private void handleTranslateAction() {
        String contentToTranslate = contentField.getText();
        if (contentToTranslate == null || contentToTranslate.trim().isEmpty()) {
            TranslateDisplayDialog.showMessage(mainFrame, "Thông báo", new JLabel("  Không có nội dung để dịch.  "));
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
        String targetLanguage = (String) JOptionPane.showInputDialog(mainFrame, "Chọn ngôn ngữ đích:", "Dịch ngôn ngữ",
                JOptionPane.PLAIN_MESSAGE, null, languages, languages[1]); // Mặc định là English

        if (targetLanguage != null) {
            // Tìm nút Translate một cách an toàn hơn thay vì dựa vào focus
            JButton translateButton = null;
            for (Component comp : ((JPanel) ((JPanel) this.getComponent(2)).getComponent(2)).getComponents()) { // Giả sử cấu trúc layout không đổi
                if (comp instanceof JButton && TRANSLATE_LABEL.equals(((JButton) comp).getText())) {
                    translateButton = (JButton) comp;
                    break;
                }
            }
            final JButton finalTranslateButton = translateButton; // Cho lambda

            if (finalTranslateButton != null) {
                finalTranslateButton.setEnabled(false);
                finalTranslateButton.setText("Đang dịch...");
            }

            TranslationService.translate(contentToTranslate, targetLanguage, new TranslationService.TranslationCallback() {
                @Override
                public void onSuccess(String translatedText) {
                    SwingUtilities.invokeLater(() -> {
                        if (finalTranslateButton != null) {
                            finalTranslateButton.setEnabled(true);
                            finalTranslateButton.setText(TRANSLATE_LABEL);
                        }
                        // Hiển thị kết quả dịch
                        JTextArea resultArea = new JTextArea(translatedText);
                        resultArea.setWrapStyleWord(true);
                        resultArea.setLineWrap(true);
                        resultArea.setEditable(false);
                        resultArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                        JScrollPane scrollPaneForResult = new JScrollPane(resultArea);
                        // Kích thước cho dialog dịch
                        int rows = Math.min(20, Math.max(5, translatedText.split("\r\n|\r|\n", -1).length + 2));
                        int cols = Math.min(80, Math.max(40, calculateApproxCols(translatedText, rows)));
                        resultArea.setRows(rows);
                        resultArea.setColumns(cols);
                        scrollPaneForResult.setPreferredSize(new Dimension(cols * 8, rows * 18)); // Ước lượng kích thước

                        TranslateDisplayDialog.showMessage(mainFrame, "Kết quả dịch (" + targetLanguage + ")", scrollPaneForResult);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    SwingUtilities.invokeLater(() -> {
                        if (finalTranslateButton != null) {
                            finalTranslateButton.setEnabled(true);
                            finalTranslateButton.setText(TRANSLATE_LABEL);
                        }
                        JOptionPane.showMessageDialog(mainFrame, "Lỗi dịch: " + errorMessage, "Lỗi Dịch", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        }
    }

    // Hàm calculateApproxCols giữ nguyên như bạn đã cung cấp
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
        String newTitle = titleField.getText().trim();
        String newContent = contentField.getText(); // Cho phép content rỗng

        if (newTitle.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Tiêu đề không được để trống!", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            titleField.requestFocus();
            return;
        }

        this.note.setTitle(newTitle);
        this.note.setContent(newContent);
        // Các setter trong Note.java đã tự gọi updateUpdatedAt()

        try {
            if (this.note.getId() > 0) { // Note đã tồn tại -> Update
                controller.updateExistingNote(this.note.getId(), this.note);
            } else { // Note mới -> Add
                // Gán folderId nếu chưa có
                if (this.note.getFolderId() <= 0) {
                    Folder currentSelectedFolder = controller.getCurrentFolder();
                    if (currentSelectedFolder != null && currentSelectedFolder.getId() > 0) {
                        this.note.setFolderId(currentSelectedFolder.getId());
                        this.note.setFolder(currentSelectedFolder);
                    } else {
                        Folder rootFolder = controller.getFolderByName("Root").orElse(null);
                        if (rootFolder != null && rootFolder.getId() > 0) {
                            this.note.setFolderId(rootFolder.getId());
                            this.note.setFolder(rootFolder);
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, "Không thể xác định thư mục. Vui lòng chọn một thư mục hoặc thử lại.", "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                // Nếu note có Alarm object mới (ID=0) được gán từ UI (qua handleSetAlarmAction)
                // thì controller.addNote sẽ gọi service, service sẽ lưu Alarm đó trước,
                // lấy ID rồi gán vào note.alarmId trước khi lưu note.
                controller.addNote(this.note);
            }
            // Controller đã hiển thị thông báo thành công/lỗi
            mainFrame.showMainMenuScreen();
        } catch (Exception e) { // Bắt Exception chung cho an toàn
            e.printStackTrace();
            // Controller đã hiển thị JOptionPane lỗi rồi.
        }
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "saveNoteShortcut");
        actionMap.put("saveNoteShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveNote();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK), "addTagShortcut");
        actionMap.put("addTagShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleAddTagAction();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "goBackShortcut");
        actionMap.put("goBackShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                mainFrame.showMainMenuScreen();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undoShortcut");
        actionMap.put("undoShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canUndo()) undoManager.undo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redoShortcut");
        actionMap.put("redoShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canRedo()) undoManager.redo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "setAlarmShortcut");
        actionMap.put("setAlarmShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleSetAlarmAction();
            }
        });

        // Thêm phím tắt Ctrl+D cho Translate
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK), "translateShortcut");
        actionMap.put("translateShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleTranslateAction();
            }
        });

        // Thêm phím tắt Ctrl+M cho Add/Edit Mission
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK), "missionShortcut");
        actionMap.put("missionShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleEditMissionAction();
            }
        });
    }
}
