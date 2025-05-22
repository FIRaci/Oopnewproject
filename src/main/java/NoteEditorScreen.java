// File: NoteEditorScreen.java
import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
// Assuming Note, Tag, MainFrame, NoteController, AIService,
// AlarmDialog, Alarm, MissionDialog, Folder are correctly defined and imported.

public class NoteEditorScreen extends JPanel {
    private static final String SAVE_LABEL = "Lưu";
    private static final String BACK_LABEL = "Quay Lại";
    private static final String ADD_TAG_LABEL = "Thêm Tag";
    private static final String ADD_ALARM_LABEL = "Đặt Báo thức";
    private static final String EDIT_ALARM_LABEL = "Sửa Báo thức";
    private static final String SET_MISSION_LABEL = "Đặt Nhiệm vụ";
    private static final String EDIT_MISSION_LABEL = "Sửa Nhiệm vụ";
    private static final String TRANSLATE_LABEL = "Dịch";
    private static final String SUMMARY_LABEL = "Tóm tắt";
    private static final String AI_AUTO_TAG_LABEL = "AI Auto Tag";

    private final MainFrame mainFrame;
    private final NoteController controller;
    private Note note;

    private JTextField titleField;
    private JTextArea contentField;
    private JPanel tagPanelContainer; // Renamed for clarity, this will hold the JScrollPane
    private JPanel actualTagDisplayPanel; // The panel with FlowLayout for tags
    private JLabel wordCountLabel;
    private JLabel modifiedLabel;
    private UndoManager undoManager;

    private JButton alarmButtonReference;
    private JButton missionButtonReference;
    private JButton aiAutoTagButtonReference;
    private JButton translateButtonReference;
    private JButton summaryButtonReference;

    public NoteEditorScreen(MainFrame mainFrame, NoteController controller, Note noteToEdit) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        this.note = (noteToEdit != null) ? noteToEdit : new Note("Ghi chú mới", Note.NoteType.TEXT, controller.getCurrentFolder());
        initializeUI();
        setNoteFields(this.note);
        setupShortcuts();
    }

    public void setNote(Note noteToSet) {
        // If null is passed, create a new TEXT note associated with the current folder in controller
        this.note = (noteToSet != null) ? noteToSet : new Note("Ghi chú mới", Note.NoteType.TEXT, controller.getCurrentFolder());
        setNoteFields(this.note);
    }


    private void setNoteFields(Note currentNote) {
        titleField.setText(currentNote.getTitle());
        contentField.setText(currentNote.getContent());
        contentField.setCaretPosition(0);

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
            boolean hasAlarm = note != null && note.getAlarm() != null && note.getAlarm().getId() > 0;
            alarmButtonReference.setText(hasAlarm ? EDIT_ALARM_LABEL : ADD_ALARM_LABEL);
        }
        if (missionButtonReference != null) {
            boolean isMissionSet = note != null && note.isMission() && note.getMissionContent() != null && !note.getMissionContent().isEmpty();
            missionButtonReference.setText(isMissionSet ? EDIT_MISSION_LABEL : SET_MISSION_LABEL);
        }
    }

    private void updateTagDisplay() {
        if (actualTagDisplayPanel == null) return;
        actualTagDisplayPanel.removeAll();
        if (note != null && note.getTags() != null && !note.getTags().isEmpty()) {
            for (Tag tag : note.getTags()) {
                JPanel tagItem = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1)); // Tighter spacing
                tagItem.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(1, 3, 1, 1) // Inner padding
                ));
                // Use UIManager color for consistency with theme
                tagItem.setBackground(UIManager.getColor("Panel.background"));


                JLabel tagLabel = new JLabel(tag.getName());
                tagLabel.setFont(tagLabel.getFont().deriveFont(11f));

                JButton removeButton = new JButton("x");
                removeButton.setMargin(new Insets(0, 1, 0, 1)); // Minimal margin
                removeButton.setFont(removeButton.getFont().deriveFont(9f));
                removeButton.setFocusPainted(false);
                // Make button less visually intrusive
                removeButton.setContentAreaFilled(false);
                removeButton.setBorderPainted(false);
                removeButton.setForeground(Color.RED);


                removeButton.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(mainFrame,
                            "Xóa tag '" + tag.getName() + "' khỏi ghi chú này?",
                            "Xác Nhận Xóa Tag", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        controller.removeTag(note, tag);
                        updateTagDisplay();
                    }
                });
                tagItem.add(tagLabel);
                tagItem.add(removeButton);
                actualTagDisplayPanel.add(tagItem);
            }
        } else {
            JLabel noTagsLabel = new JLabel("Chưa có tag nào.");
            noTagsLabel.setForeground(Color.GRAY);
            noTagsLabel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5)); // Padding for the label
            actualTagDisplayPanel.add(noTagsLabel);
        }
        actualTagDisplayPanel.revalidate();
        actualTagDisplayPanel.repaint();
        // Also revalidate the scroll pane container if it's separate
        if (tagPanelContainer != null) {
            tagPanelContainer.revalidate();
            tagPanelContainer.repaint();
        }
    }


    private void updateWordCount() {
        if (contentField == null || wordCountLabel == null) return;
        String text = contentField.getText();
        int count = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        wordCountLabel.setText("Số từ: " + count);
    }

    private void updateModifiedTime() {
        if (note != null && modifiedLabel != null) {
            modifiedLabel.setText("Sửa đổi lần cuối: " + note.getFormattedModificationDate());
        } else if (modifiedLabel != null) {
            modifiedLabel.setText("Sửa đổi lần cuối: N/A");
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // More padding

        // Top Panel: Title and Back Button
        JPanel topPanel = new JPanel(new BorderLayout(15, 0)); // Increased gap
        titleField = new JTextField();
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18)); // Larger title font
        JLabel titleLabel = new JLabel("Tiêu đề:");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,5)); // Margin for label
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(titleField, BorderLayout.CENTER);

        JButton backButton = new JButton(BACK_LABEL);
        backButton.setToolTipText("Quay lại màn hình chính (Esc)");
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        topPanel.add(backButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel: Content Area
        contentField = new JTextArea();
        contentField.setFont(new Font("Segoe UI", Font.PLAIN, 15)); // Slightly larger content font
        contentField.setLineWrap(true);
        contentField.setWrapStyleWord(true);
        contentField.setMargin(new Insets(5,5,5,5)); // Padding inside text area
        contentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateWordCount();
            }
        });
        undoManager = new UndoManager();
        contentField.getDocument().addUndoableEditListener(undoManager);
        JScrollPane contentScrollPane = new JScrollPane(contentField);
        contentScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Nội dung"),
                BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        add(contentScrollPane, BorderLayout.CENTER);

        // Bottom Panel: Status, Tags, Buttons
        JPanel bottomOuterPanel = new JPanel(new BorderLayout(0, 10)); // Increased gap

        // Status Panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        wordCountLabel = new JLabel("Số từ: 0");
        modifiedLabel = new JLabel("Sửa đổi lần cuối: N/A");
        statusPanel.add(wordCountLabel, BorderLayout.WEST);
        statusPanel.add(modifiedLabel, BorderLayout.EAST);
        bottomOuterPanel.add(statusPanel, BorderLayout.NORTH);

        // Tag Panel Container (this will hold the scroll pane)
        tagPanelContainer = new JPanel(new BorderLayout());
        tagPanelContainer.setBorder(BorderFactory.createTitledBorder("Tags"));

        actualTagDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3)); // Panel that actually holds tags
        actualTagDisplayPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        JScrollPane tagScrollPane = new JScrollPane(actualTagDisplayPanel);
        tagScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tagScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Usually not needed for tags
        tagScrollPane.setPreferredSize(new Dimension(0, 55)); // Adjusted preferred height
        tagScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove scrollpane border if TitledBorder is on container

        tagPanelContainer.add(tagScrollPane, BorderLayout.CENTER);
        bottomOuterPanel.add(tagPanelContainer, BorderLayout.CENTER);


        // Button Panel
        bottomOuterPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        add(bottomOuterPanel, BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        // Using GridBagLayout for more control over button sizes and spacing
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4); // Padding around buttons
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make buttons expand horizontally a bit

        int gridx = 0;

        JButton addTagButton = new JButton(ADD_TAG_LABEL);
        addTagButton.setToolTipText("Thêm tag mới cho ghi chú này (Ctrl+T)");
        addTagButton.addActionListener(e -> handleAddTagAction());
        gbc.gridx = gridx++; buttonPanel.add(addTagButton, gbc);

        aiAutoTagButtonReference = new JButton(AI_AUTO_TAG_LABEL);
        aiAutoTagButtonReference.setToolTipText("AI gợi ý tag cho ghi chú này (Ctrl+I)");
        aiAutoTagButtonReference.addActionListener(e -> handleAiAutoTagAction());
        gbc.gridx = gridx++; buttonPanel.add(aiAutoTagButtonReference, gbc);

        alarmButtonReference = new JButton(ADD_ALARM_LABEL);
        alarmButtonReference.setToolTipText("Đặt hoặc sửa báo thức cho ghi chú (Ctrl+G)");
        alarmButtonReference.addActionListener(e -> handleSetAlarmAction());
        gbc.gridx = gridx++; buttonPanel.add(alarmButtonReference, gbc);

        missionButtonReference = new JButton(SET_MISSION_LABEL);
        missionButtonReference.setToolTipText("Đặt hoặc sửa nhiệm vụ cho ghi chú (Ctrl+M)");
        missionButtonReference.addActionListener(e -> handleEditMissionAction());
        gbc.gridx = gridx++; buttonPanel.add(missionButtonReference, gbc);

        translateButtonReference = new JButton(TRANSLATE_LABEL);
        translateButtonReference.setToolTipText("Dịch nội dung ghi chú (Ctrl+D)");
        translateButtonReference.addActionListener(e -> handleTranslateAction());
        gbc.gridx = gridx++; buttonPanel.add(translateButtonReference, gbc);

        summaryButtonReference = new JButton(SUMMARY_LABEL);
        summaryButtonReference.setToolTipText("Tóm tắt nội dung ghi chú (Ctrl+U)");
        summaryButtonReference.addActionListener(e -> handleSummaryAction());
        gbc.gridx = gridx++; buttonPanel.add(summaryButtonReference, gbc);

        // Add a flexible spacer to push the Save button to the right
        gbc.gridx = gridx++;
        gbc.weightx = 1.0; // This component will take up extra horizontal space
        buttonPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0; // Reset weight

        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.setToolTipText("Lưu các thay đổi (Ctrl+S)");
        saveButton.addActionListener(e -> saveNote());
        gbc.gridx = gridx++;
        gbc.anchor = GridBagConstraints.EAST; // Anchor save button to the right
        buttonPanel.add(saveButton, gbc);

        return buttonPanel;
    }

    private void handleAddTagAction() {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không thể thêm tag: Ghi chú chưa được tải.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (note.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrame, "Vui lòng lưu ghi chú trước khi thêm tag.", "Ghi chú chưa được lưu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String tagName = JOptionPane.showInputDialog(mainFrame, "Nhập tên tag:", "Thêm Tag", JOptionPane.PLAIN_MESSAGE);
        if (tagName != null && !tagName.trim().isEmpty()) {
            boolean tagExistsInNote = note.getTags().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(tagName.trim()));
            if (tagExistsInNote) {
                JOptionPane.showMessageDialog(mainFrame, "Tag '" + tagName.trim() + "' đã có trong ghi chú này.", "Tag Đã Tồn Tại", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            controller.addTag(note, new Tag(tagName.trim()));
            updateTagDisplay();
        }
    }

    private void handleAiAutoTagAction() {
        if (note == null || note.getId() <= 0) { // Check if note is saved
            JOptionPane.showMessageDialog(mainFrame, "Vui lòng lưu ghi chú trước khi sử dụng AI Auto Tag.", "Ghi chú chưa được lưu", JOptionPane.WARNING_MESSAGE);
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
                                // Check if tag already exists on the note before adding
                                boolean tagExists = note.getTags().stream().anyMatch(t -> t.getName().equalsIgnoreCase(trimmedTagName));
                                if (!tagExists) {
                                    controller.addTag(note, new Tag(trimmedTagName));
                                    tagsAddedCount++;
                                }
                            }
                        }
                        updateTagDisplay();
                        if (tagsAddedCount > 0) {
                            JOptionPane.showMessageDialog(mainFrame, tagsAddedCount + " AI tag(s) đã được thêm!", "AI Auto Tag Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, "AI không gợi ý tag mới hoặc các tag đã tồn tại.", "AI Auto Tag", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(mainFrame, "AI không gợi ý tag nào.", "AI Auto Tag", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(mainFrame, "Không thể đặt báo thức: Ghi chú chưa được tải.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (note.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrame, "Vui lòng lưu ghi chú trước khi đặt báo thức.", "Ghi chú chưa được lưu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AlarmDialog alarmDialog = new AlarmDialog(mainFrame, note.getAlarm()); // Pass current alarm
        alarmDialog.setVisible(true);

        if (alarmDialog.isOkPressed()) {
            Alarm resultAlarm = alarmDialog.getResult();
            controller.setAlarm(note, resultAlarm); // Controller handles if resultAlarm is null (delete) or new/updated
        }
        updateDynamicButtonTexts();
    }

    private void handleEditMissionAction() {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không thể sửa nhiệm vụ: Ghi chú chưa được tải.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (note.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrame, "Vui lòng lưu ghi chú trước khi sửa nhiệm vụ.", "Ghi chú chưa được lưu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MissionDialog missionDialog = new MissionDialog(mainFrame);
        missionDialog.setMission(note.getMissionContent());
        missionDialog.setVisible(true);

        if (missionDialog.isSaved()) {
            String resultMissionContent = missionDialog.getResult();
            controller.updateMission(note, resultMissionContent); // Controller handles null or empty content
        }
        updateDynamicButtonTexts();
    }

    private void handleTranslateAction() {
        String contentToTranslate = contentField.getText();
        if (contentToTranslate == null || contentToTranslate.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Không có nội dung để dịch.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
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
                JOptionPane.PLAIN_MESSAGE, null, languages, languages[1]);

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
        // Now uses the new StyledResultDialog
        StyledResultDialog.showDialog(mainFrame, title, textContent);
    }

    private void saveNote() {
        String newTitle = titleField.getText().trim();
        String newContent = contentField.getText();

        if (newTitle.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Tiêu đề không được để trống!", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            titleField.requestFocus();
            return;
        }

        this.note.setTitle(newTitle);
        this.note.setContent(newContent);
        this.note.updateUpdatedAt(); // Ensure modification date is updated

        try {
            if (this.note.getId() > 0) {
                controller.updateExistingNote(this.note.getId(), this.note);
            } else {
                if (this.note.getFolderId() <= 0 && controller.getCurrentFolder() != null) {
                    this.note.setFolder(controller.getCurrentFolder()); // This will also set folderId
                } else if (this.note.getFolderId() <= 0) {
                    Folder rootFolder = controller.getFolderByName("Root").orElse(null);
                    if (rootFolder != null) {
                        this.note.setFolder(rootFolder);
                    } else {
                        JOptionPane.showMessageDialog(mainFrame, "Không thể xác định thư mục. Vui lòng tạo thư mục 'Root'.", "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                controller.addNote(this.note);
            }
            mainFrame.showMainMenuScreen();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Đã xảy ra lỗi khi lưu note: " + e.getMessage(), "Lỗi Lưu Note", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "saveNoteShortcut");
        actionMap.put("saveNoteShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveNote(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK), "addTagShortcut");
        actionMap.put("addTagShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleAddTagAction(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "goBackShortcut");
        actionMap.put("goBackShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { mainFrame.showMainMenuScreen(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undoShortcut");
        actionMap.put("undoShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canUndo()) undoManager.undo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redoShortcut");
        actionMap.put("redoShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canRedo()) undoManager.redo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "setAlarmShortcut");
        actionMap.put("setAlarmShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleSetAlarmAction(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK), "missionShortcut");
        actionMap.put("missionShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleEditMissionAction(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK), "translateShortcut");
        actionMap.put("translateShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleTranslateAction(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK), "summaryShortcut");
        actionMap.put("summaryShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleSummaryAction(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "aiAutoTagShortcut");
        actionMap.put("aiAutoTagShortcut", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleAiAutoTagAction(); }
        });
    }
}
