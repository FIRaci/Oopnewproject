// File: NoteEditorScreen.java
import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
// Giả sử các lớp Note, Tag, MainFrame, NoteController, AIService,
// AlarmDialog, Alarm, MissionDialog, Folder đã được định nghĩa và import đúng cách.
// Ví dụ:
// import com.yourproject.model.Note;
// import com.yourproject.model.Tag;
// import com.yourproject.model.Alarm;
// import com.yourproject.model.Folder;
// import com.yourproject.ui.MainFrame;
// import com.yourproject.controller.NoteController;
// import com.yourproject.service.AIService;
// import com.yourproject.ui.dialog.AlarmDialog;
// import com.yourproject.ui.dialog.MissionDialog;


public class NoteEditorScreen extends JPanel {
    // Constants from both versions
    private static final String SAVE_LABEL = "Save";
    private static final String BACK_LABEL = "Back";
    private static final String ADD_TAG_LABEL = "Add Tag";
    private static final String ADD_ALARM_LABEL = "Add Alarm"; // Base label, text will be dynamic
    private static final String EDIT_ALARM_LABEL = "Edit Alarm";
    private static final String SET_MISSION_LABEL = "Set Mission"; // Base label
    private static final String EDIT_MISSION_LABEL = "Edit Mission"; // Base label, text will be dynamic
    private static final String TRANSLATE_LABEL = "Translate";
    private static final String SUMMARY_LABEL = "Summary";
    private static final String AI_AUTO_TAG_LABEL = "AI Auto Tag";

    private final MainFrame mainFrame;
    private final NoteController controller;
    private Note note; // Note hiện tại đang được chỉnh sửa hoặc tạo mới

    private JTextField titleField;
    private JTextArea contentField;
    private JPanel tagPanel;
    private JLabel wordCountLabel;
    private JLabel modifiedLabel;
    private UndoManager undoManager;

    // Button references for dynamic text updates or enabling/disabling
    private JButton alarmButtonReference;
    private JButton missionButtonReference;
    private JButton aiAutoTagButtonReference;
    private JButton translateButtonReference;
    private JButton summaryButtonReference;

    public NoteEditorScreen(MainFrame mainFrame, NoteController controller, Note noteToEdit) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        this.note = (noteToEdit != null) ? noteToEdit : new Note("New Note", "", false); // Giả sử constructor Note(title, content, isMission)
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
        contentField.setCaretPosition(0); // Reset caret position to the beginning

        updateTagDisplay();
        updateWordCount();
        updateModifiedTime();
        updateDynamicButtonTexts();

        if (undoManager != null) {
            undoManager.discardAllEdits();
        }
    }

    private void updateDynamicButtonTexts() {
        if (alarmButtonReference != null) {
            // Giả sử Note có phương thức getAlarm() trả về Alarm object hoặc null
            // và Alarm có getId() trả về ID ( > 0 nếu đã lưu)
            boolean hasAlarm = note != null && note.getAlarm() != null && note.getAlarm().getId() > 0;
            alarmButtonReference.setText(hasAlarm ? EDIT_ALARM_LABEL : ADD_ALARM_LABEL);
        }
        if (missionButtonReference != null) {
            boolean isMissionSet = note != null && note.isMission() && note.getMissionContent() != null && !note.getMissionContent().isEmpty();
            missionButtonReference.setText(isMissionSet ? EDIT_MISSION_LABEL : SET_MISSION_LABEL);
        }
    }

    private void updateTagDisplay() {
        if (tagPanel == null) return;
        tagPanel.removeAll();
        if (note != null && note.getTags() != null && !note.getTags().isEmpty()) {
            for (Tag tag : note.getTags()) {
                JPanel tagItem = new JPanel(new BorderLayout(3, 0));
                tagItem.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                tagItem.setBackground(new Color(240, 240, 240));

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
                        controller.removeTag(note, tag); // Controller sẽ update note trong DB và có thể cập nhật object 'note'
                        updateTagDisplay(); // Refresh UI
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
            modifiedLabel.setText("Last modified: " + note.getFormattedModificationDate()); // Giả sử Note có getFormattedModificationDate()
        } else if (modifiedLabel != null) {
            modifiedLabel.setText("Last modified: N/A");
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Panel: Title and Back Button
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        titleField = new JTextField();
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        JLabel titleLabel = new JLabel("Title:");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(titleField, BorderLayout.CENTER);

        JButton backButton = new JButton(BACK_LABEL);
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        topPanel.add(backButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel: Content Area
        contentField = new JTextArea();
        contentField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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
        wordCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        modifiedLabel = new JLabel("Last modified: N/A");
        modifiedLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        statusPanel.add(wordCountLabel, BorderLayout.WEST);
        statusPanel.add(modifiedLabel, BorderLayout.EAST);
        bottomOuterPanel.add(statusPanel, BorderLayout.NORTH);

        // Tag Panel
        tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        tagPanel.setBorder(BorderFactory.createTitledBorder("Tags"));
        JScrollPane tagScrollPane = new JScrollPane(tagPanel);
        tagScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); // AS_NEEDED is better
        tagScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tagScrollPane.setPreferredSize(new Dimension(0, 65)); // Giới hạn chiều cao một chút
        bottomOuterPanel.add(tagScrollPane, BorderLayout.CENTER);

        // Button Panel
        bottomOuterPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        add(bottomOuterPanel, BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5)); // Khoảng cách giữa các nút

        JButton addTagButton = new JButton(ADD_TAG_LABEL);
        addTagButton.setToolTipText("Add a new tag to this note (Ctrl+T)");
        addTagButton.addActionListener(e -> handleAddTagAction());
        buttonPanel.add(addTagButton);

        aiAutoTagButtonReference = new JButton(AI_AUTO_TAG_LABEL);
        aiAutoTagButtonReference.setToolTipText("Suggest tags for this note using AI");
        aiAutoTagButtonReference.addActionListener(e -> handleAiAutoTagAction());
        buttonPanel.add(aiAutoTagButtonReference);

        alarmButtonReference = new JButton(ADD_ALARM_LABEL); // Text sẽ được cập nhật
        alarmButtonReference.setToolTipText("Set or edit the alarm for this note (Ctrl+G)");
        alarmButtonReference.addActionListener(e -> handleSetAlarmAction());
        buttonPanel.add(alarmButtonReference);

        missionButtonReference = new JButton(SET_MISSION_LABEL); // Text sẽ được cập nhật
        missionButtonReference.setToolTipText("Set or edit the mission for this note (Ctrl+M)");
        missionButtonReference.addActionListener(e -> handleEditMissionAction());
        buttonPanel.add(missionButtonReference);

        translateButtonReference = new JButton(TRANSLATE_LABEL);
        translateButtonReference.setToolTipText("Translate the note content (Ctrl+D)");
        translateButtonReference.addActionListener(e -> handleTranslateAction());
        buttonPanel.add(translateButtonReference);

        summaryButtonReference = new JButton(SUMMARY_LABEL);
        summaryButtonReference.setToolTipText("Summarize the note content (Ctrl+U)");
        summaryButtonReference.addActionListener(e -> handleSummaryAction());
        buttonPanel.add(summaryButtonReference);

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
        if (note.getId() <= 0) { // Giả sử ID > 0 nghĩa là note đã được lưu
            JOptionPane.showMessageDialog(mainFrame, "Please save the note first to add tags.", "Note not saved", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String tagName = JOptionPane.showInputDialog(mainFrame, "Enter tag name:", "Add Tag", JOptionPane.PLAIN_MESSAGE);
        if (tagName != null && !tagName.trim().isEmpty()) {
            boolean tagExistsInNote = note.getTags().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(tagName.trim()));
            if (tagExistsInNote) {
                JOptionPane.showMessageDialog(mainFrame, "Tag '" + tagName.trim() + "' is already on this note.", "Tag Exists", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            controller.addTag(note, new Tag(tagName.trim()));
            updateTagDisplay();
        }
    }

    private void handleAiAutoTagAction() {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrame, "Please save the note first to use AI Auto Tag.", "Note not saved", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String contentToAnalyze = contentField.getText();
        if (contentToAnalyze == null || contentToAnalyze.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Không có nội dung để tạo tag.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        aiAutoTagButtonReference.setEnabled(false);
        aiAutoTagButtonReference.setText("Đang tạo tags...");

        AIService.generateTags(contentToAnalyze, new AIService.TagGenerationCallback() {
            @Override
            public void onSuccess(String generatedTagsString) {
                SwingUtilities.invokeLater(() -> {
                    aiAutoTagButtonReference.setEnabled(true);
                    aiAutoTagButtonReference.setText(AI_AUTO_TAG_LABEL);
                    if (generatedTagsString != null && !generatedTagsString.trim().isEmpty()) {
                        String[] tagNames = generatedTagsString.split(",");
                        int tagsAddedCount = 0;
                        for (String tagName : tagNames) {
                            String trimmedTagName = tagName.trim();
                            if (!trimmedTagName.isEmpty()) {
                                controller.addTag(note, new Tag(trimmedTagName)); // Controller nên xử lý trùng lặp nếu cần
                                tagsAddedCount++;
                            }
                        }
                        updateTagDisplay();
                        if (tagsAddedCount > 0) {
                            JOptionPane.showMessageDialog(mainFrame, tagsAddedCount + " AI tag(s) added successfully!", "AI Auto Tag Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, "AI did not suggest any new tags or tags were empty.", "AI Auto Tag", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(mainFrame, "AI did not suggest any tags.", "AI Auto Tag", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                SwingUtilities.invokeLater(() -> {
                    aiAutoTagButtonReference.setEnabled(true);
                    aiAutoTagButtonReference.setText(AI_AUTO_TAG_LABEL);
                    JOptionPane.showMessageDialog(mainFrame, errorMessage, "Lỗi AI Auto Tag", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }


    private void handleSetAlarmAction() {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrame, "Cannot set alarm: Note is not loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (note.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrame, "Please save the note first to set an alarm.", "Note not saved", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AlarmDialog alarmDialog = new AlarmDialog(mainFrame);
        // Nếu Note đã có Alarm, truyền vào Dialog để chỉnh sửa
        if (note.getAlarm() != null && note.getAlarm().getId() > 0) {
            alarmDialog.setAlarmToEdit(note.getAlarm()); // Giả sử AlarmDialog có phương thức này
        }

        alarmDialog.setVisible(true);
        Alarm resultAlarm = alarmDialog.getResult(); // `getResult` có thể trả về null nếu cancel

        if (alarmDialog.isOkPressed() && resultAlarm != null) { // isOkPressed là cờ tự thêm trong Dialog
            // Nếu note đã có alarm (ID > 0) và resultAlarm từ dialog là mới (ID = 0),
            // thì đây là trường hợp edit, ta nên giữ ID cũ.
            if (note.getAlarm() != null && note.getAlarm().getId() > 0 && resultAlarm.getId() == 0) {
                resultAlarm.setId(note.getAlarm().getId());
            }
            controller.setAlarm(note, resultAlarm); // Controller sẽ xử lý add mới hoặc update
        } else if (alarmDialog.isOkPressed() && resultAlarm == null) { // Người dùng có thể đã chọn xóa alarm trong dialog
            controller.setAlarm(note, null); // Yêu cầu controller xóa alarm
        }
        // Nếu người dùng bấm Cancel (isOkPressed = false), không làm gì cả.
        updateDynamicButtonTexts(); // Cập nhật text nút
    }

    private void handleEditMissionAction() {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrame, "Cannot edit mission: Note is not loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (note.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrame, "Please save the note first to edit its mission.", "Note not saved", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MissionDialog missionDialog = new MissionDialog(mainFrame); // Giả sử MissionDialog tồn tại
        missionDialog.setMission(note.getMissionContent()); // Truyền mission hiện tại (nếu có)
        missionDialog.setVisible(true);
        String resultMissionContent = missionDialog.getResult(); // Có thể trả về null nếu cancel

        // Kiểm tra xem người dùng có bấm Save trong dialog không (thường dialog sẽ có cờ báo)
        if (missionDialog.isSaved() && resultMissionContent != null) { // isSaved() là cờ bạn tự thêm vào MissionDialog
            controller.updateMission(note, resultMissionContent);
        }
        updateDynamicButtonTexts();
    }

    private void handleTranslateAction() {
        String contentToTranslate = contentField.getText();
        if (contentToTranslate == null || contentToTranslate.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Không có nội dung để dịch.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Danh sách ngôn ngữ mở rộng từ V2
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
            translateButtonReference.setEnabled(false);
            translateButtonReference.setText("Đang dịch...");

            AIService.translate(contentToTranslate, targetLanguage, new AIService.TranslationCallback() {
                @Override
                public void onSuccess(String translatedText) {
                    SwingUtilities.invokeLater(() -> {
                        translateButtonReference.setEnabled(true);
                        translateButtonReference.setText(TRANSLATE_LABEL);
                        showResultDialog("Kết quả dịch (" + targetLanguage + ")", translatedText);
                    });
                }
                @Override
                public void onError(String errorMessage) {
                    SwingUtilities.invokeLater(() -> {
                        translateButtonReference.setEnabled(true);
                        translateButtonReference.setText(TRANSLATE_LABEL);
                        JOptionPane.showMessageDialog(mainFrame, "Lỗi dịch: " + errorMessage, "Lỗi Dịch", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        }
    }

    private void handleSummaryAction() {
        String contentToSummarize = contentField.getText();
        if (contentToSummarize == null || contentToSummarize.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Không có nội dung để tóm tắt.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        summaryButtonReference.setEnabled(false);
        summaryButtonReference.setText("Đang tóm tắt...");

        AIService.summarize(contentToSummarize, new AIService.SummarizationCallback() {
            @Override
            public void onSuccess(String summarizedText) {
                SwingUtilities.invokeLater(() -> {
                    summaryButtonReference.setEnabled(true);
                    summaryButtonReference.setText(SUMMARY_LABEL);
                    showResultDialog("Tóm tắt nội dung", summarizedText);
                });
            }

            @Override
            public void onError(String errorMessage) {
                SwingUtilities.invokeLater(() -> {
                    summaryButtonReference.setEnabled(true);
                    summaryButtonReference.setText(SUMMARY_LABEL);
                    JOptionPane.showMessageDialog(mainFrame, errorMessage, "Lỗi Tóm Tắt", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void showResultDialog(String title, String textContent) {
        JTextArea resultArea = new JTextArea(textContent);
        resultArea.setWrapStyleWord(true);
        resultArea.setLineWrap(true);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resultArea.setBackground(this.getBackground()); // Hoặc một màu nền phù hợp
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(mainFrame, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveNote() {
        String newTitle = titleField.getText().trim();
        String newContent = contentField.getText(); // Không trim content để giữ định dạng

        if (newTitle.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Tiêu đề không được để trống!", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            titleField.requestFocus();
            return;
        }

        this.note.setTitle(newTitle);
        this.note.setContent(newContent);
        // Giả sử Note.setTitle và Note.setContent tự động gọi this.note.updateModificationDate();

        try {
            if (this.note.getId() > 0) { // Note đã tồn tại (có ID) -> Update
                controller.updateExistingNote(this.note.getId(), this.note);
            } else { // Note mới -> Add
                // Gán folderId nếu chưa có (logic từ V2)
                if (this.note.getFolderId() <= 0) {
                    Folder currentSelectedFolder = controller.getCurrentFolder(); // Giả sử controller có getCurrentFolder()
                    if (currentSelectedFolder != null && currentSelectedFolder.getId() > 0) {
                        this.note.setFolderId(currentSelectedFolder.getId());
                        // this.note.setFolder(currentSelectedFolder); // Nếu cần tham chiếu object
                    } else {
                        // Mặc định vào Root hoặc folder đầu tiên nếu không có folder nào được chọn
                        Folder rootFolder = controller.getFolderByName("Root").orElse(null); // Giả sử có getFolderByName
                        if (rootFolder != null && rootFolder.getId() > 0) {
                            this.note.setFolderId(rootFolder.getId());
                        } else {
                            // Xử lý trường hợp không tìm thấy Root folder (quan trọng)
                            JOptionPane.showMessageDialog(mainFrame, "Không thể xác định thư mục mặc định. Vui lòng tạo thư mục 'Root' hoặc chọn một thư mục.", "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                controller.addNote(this.note); // Controller sẽ xử lý việc lưu note mới và các object liên quan (như Alarm mới)
            }
            // Sau khi lưu thành công (controller nên thông báo), quay về màn hình chính
            mainFrame.showMainMenuScreen();
        } catch (Exception e) {
            e.printStackTrace();
            // Controller nên đã hiển thị lỗi chi tiết hơn, hoặc bạn có thể hiển thị lỗi chung ở đây
            JOptionPane.showMessageDialog(mainFrame, "Đã xảy ra lỗi khi lưu note: " + e.getMessage(), "Lỗi Lưu Note", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // Save Note
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "saveNoteShortcut");
        actionMap.put("saveNoteShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { saveNote(); }
        });

        // Add Tag
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK), "addTagShortcut");
        actionMap.put("addTagShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { handleAddTagAction(); }
        });

        // Go Back
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "goBackShortcut");
        actionMap.put("goBackShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { mainFrame.showMainMenuScreen(); }
        });

        // Undo
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undoShortcut");
        actionMap.put("undoShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canUndo()) undoManager.undo();
            }
        });

        // Redo
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redoShortcut");
        actionMap.put("redoShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canRedo()) undoManager.redo();
            }
        });

        // Set/Edit Alarm
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "setAlarmShortcut");
        actionMap.put("setAlarmShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { handleSetAlarmAction(); }
        });

        // Set/Edit Mission
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK), "missionShortcut");
        actionMap.put("missionShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { handleEditMissionAction(); }
        });

        // Translate
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK), "translateShortcut");
        actionMap.put("translateShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { handleTranslateAction(); }
        });

        // Summary (Ví dụ: Ctrl+U)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK), "summaryShortcut");
        actionMap.put("summaryShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { handleSummaryAction(); }
        });

        // AI Auto Tag (Ví dụ: Ctrl+I)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "aiAutoTagShortcut");
        actionMap.put("aiAutoTagShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { handleAiAutoTagAction(); }
        });
    }
}