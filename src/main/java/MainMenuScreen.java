import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MainMenuScreen extends JPanel {
    private static final String[] NOTE_COLUMNS = {"Title", "Favorite", "Mission", "Alarm", "Modified"};
    private static final String FOLDERS_TITLE = "Thư mục";
    private static final String ADD_FOLDER_LABEL = "Thêm Thư mục";
    private static final String ADD_NOTE_LABEL = "Thêm ghi chú";
    private static final String ADD_DRAW_PANEL_LABEL = "Thêm Bản Vẽ";
    private static final String REFRESH_LABEL = "Làm mới";
    private static final String TITLE_SEARCH_PLACEHOLDER = "Tìm theo tiêu đề...";
    private static final String TAG_SEARCH_PLACEHOLDER = "Tìm theo tag...";

    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel folderPanel;
    private JTable noteTable;
    private JTextField titleSearchField;
    private JTextField tagSearchField;
    private JList<Folder> folderList;
    private DefaultListModel<Folder> folderListModel;
    private List<Note> filteredNotes;
    private ImageIcon[] hourIcons;
    private ListSelectionListener folderListSelectionHandler;

    public MainMenuScreen(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        loadAlarmIcons();
        initializeUI();
        setupShortcuts();
    }

    private void loadAlarmIcons() {
        hourIcons = new ImageIcon[24];
        ImageIcon spinnerIcon = null;

        try {
            java.net.URL imgUrl = getClass().getResource("/images/spinner.jpg");
            if (imgUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imgUrl);
                Image img = originalIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                spinnerIcon = new ImageIcon(img);
            } else {
                System.err.println("Không tìm thấy tài nguyên: /images/spinner.jpg");
                spinnerIcon = createDefaultIcon("S");
            }
        } catch (Exception e) {
            System.err.println("Không thể tải icon spinner.jpg: " + e.getMessage());
            spinnerIcon = createDefaultIcon("E");
        }

        for (int i = 0; i < 24; i++) {
            hourIcons[i] = spinnerIcon;
        }
    }

    private ImageIcon createDefaultIcon(String text) {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, 20, 20);
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (20 - fm.stringWidth(text)) / 2;
        int y = (20 - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);
        g2d.dispose();
        return new ImageIcon(image);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildFolderPanel(), BorderLayout.WEST);
        add(buildNotesPanel(), BorderLayout.CENTER);
    }

    private JPanel buildFolderPanel() {
        folderPanel = new JPanel();
        folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.Y_AXIS));
        folderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(FOLDERS_TITLE),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        folderPanel.setPreferredSize(new Dimension(220, 0));

        folderListModel = new DefaultListModel<>();
        folderList = new JList<>(folderListModel);
        folderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        folderList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Folder) {
                    Folder folder = (Folder) value;
                    StringBuilder displayText = new StringBuilder(folder.getName());
                    if (folder.isFavorite()) displayText.append(" ★");
                    setText(displayText.toString());
                    setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
                }
                return c;
            }
        });

        folderList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showFolderPopupMenu(e); }
            @Override
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showFolderPopupMenu(e); }
        });

        folderListSelectionHandler = e -> {
            if (!e.getValueIsAdjusting()) {
                Folder selectedFolder = folderList.getSelectedValue();
                if (controller != null && selectedFolder != null && selectedFolder.getId() != 0) {
                    controller.selectFolder(selectedFolder);
                } else if (controller != null && selectedFolder == null && !folderListModel.isEmpty()){
                    Folder root = controller.getFolderByName("Root").orElse(null);
                    if (root != null) controller.selectFolder(root);
                }
                populateNoteTableModel();
            }
        };
        folderList.addListSelectionListener(folderListSelectionHandler);

        JScrollPane scrollPane = new JScrollPane(folderList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        folderPanel.add(scrollPane);

        JButton addFolderButton = createAddFolderButton();
        JPanel buttonHolder = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonHolder.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        buttonHolder.add(addFolderButton);
        folderPanel.add(buttonHolder);

        refreshFolderPanel();
        return folderPanel;
    }

    public void refreshFolderPanel() {
        if (folderList == null || controller == null) return;

        Folder previouslySelectedFolder = folderList.getSelectedValue();
        long previouslySelectedId = (previouslySelectedFolder != null) ? previouslySelectedFolder.getId() : 0;
        if (previouslySelectedId == 0 && previouslySelectedFolder != null && "Root".equalsIgnoreCase(previouslySelectedFolder.getName())) {
            Folder rootFromCtrl = controller.getFolderByName("Root").orElse(null);
            if (rootFromCtrl != null) previouslySelectedId = rootFromCtrl.getId();
        }

        if (folderListSelectionHandler != null) {
            folderList.removeListSelectionListener(folderListSelectionHandler);
        }

        folderListModel.clear();
        List<Folder> foldersFromController = controller.getFolders();

        Folder rootFolder = foldersFromController.stream()
                .filter(f -> "Root".equalsIgnoreCase(f.getName()) && f.getId() != 0)
                .findFirst().orElse(null);

        if (rootFolder != null) {
            folderListModel.addElement(rootFolder);
        } else {
            System.err.println("[MainMenuScreen refreshFolderPanel] Lỗi: Không tìm thấy thư mục Root hợp lệ từ controller.");
        }

        List<Folder> otherFolders = foldersFromController.stream()
                .filter(f -> rootFolder == null || f.getId() != rootFolder.getId())
                .sorted(Comparator.comparing(Folder::isFavorite, Comparator.reverseOrder())
                        .thenComparing(Folder::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        for (Folder folder : otherFolders) {
            if (folder.getId() != 0) {
                folderListModel.addElement(folder);
            } else {
                System.err.println("[MainMenuScreen refreshFolderPanel] Cảnh báo: Bỏ qua thư mục '" + folder.getName() + "' vì không có ID hợp lệ.");
            }
        }

        int selectionIndex = -1;
        if (previouslySelectedId != 0) {
            for (int i = 0; i < folderListModel.getSize(); i++) {
                if (folderListModel.getElementAt(i).getId() == previouslySelectedId) {
                    selectionIndex = i;
                    break;
                }
            }
        }

        if (selectionIndex == -1 && controller.getCurrentFolder() != null) {
            Folder currentCtrlFolder = controller.getCurrentFolder();
            if (currentCtrlFolder != null && currentCtrlFolder.getId() != 0) {
                for (int i = 0; i < folderListModel.getSize(); i++) {
                    if (folderListModel.getElementAt(i).getId() == currentCtrlFolder.getId()) {
                        selectionIndex = i;
                        break;
                    }
                }
            }
        }

        if (selectionIndex == -1 && !folderListModel.isEmpty()) {
            selectionIndex = 0;
        }

        if (selectionIndex != -1) {
            folderList.setSelectedIndex(selectionIndex);
        }

        if (folderListSelectionHandler != null) {
            folderList.addListSelectionListener(folderListSelectionHandler);
        }
    }

    private void showFolderPopupMenu(MouseEvent e) {
        int index = folderList.locationToIndex(e.getPoint());
        if (index < 0) return;

        folderList.setSelectedIndex(index);
        Folder folder = folderList.getSelectedValue();

        if (folder != null && !"Root".equalsIgnoreCase(folder.getName()) && folder.getId() != 0) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem renameItem = new JMenuItem("Đổi tên");
            renameItem.addActionListener(ev -> {
                String newName = JOptionPane.showInputDialog(mainFrame, "Nhập tên thư mục mới:", folder.getName());
                if (newName != null && !newName.trim().isEmpty() && controller != null) {
                    controller.renameFolder(folder, newName.trim());
                    refreshFolderPanel();
                }
            });
            popup.add(renameItem);

            JCheckBoxMenuItem favoriteItem = new JCheckBoxMenuItem("Yêu thích", folder.isFavorite());
            favoriteItem.addActionListener(ev -> {
                if (controller != null) {
                    controller.setFolderFavorite(folder, !folder.isFavorite());
                    refreshFolderPanel();
                }
            });
            popup.add(favoriteItem);

            JMenuItem deleteItem = new JMenuItem("Xóa");
            deleteItem.addActionListener(ev -> {
                if (controller != null) {
                    controller.deleteFolder(folder);
                    refreshFolderPanel();
                    populateNoteTableModel();
                }
            });
            popup.add(deleteItem);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private JButton createAddFolderButton() {
        JButton addFolderButton = new JButton(ADD_FOLDER_LABEL);
        addFolderButton.setToolTipText("Thêm một thư mục mới (Ctrl+F)");
        addFolderButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(mainFrame, "Nhập tên thư mục:");
            if (name != null && !name.trim().isEmpty() && controller != null) {
                controller.addNewFolder(name.trim());
                refreshFolderPanel();
            }
        });
        return addFolderButton;
    }

    private JPanel buildNotesPanel() {
        JPanel notesPanel = new JPanel(new BorderLayout(5, 10));
        notesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Ghi chú & Bản vẽ"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        noteTable = createNoteTable();
        JScrollPane tableScrollPane = new JScrollPane(noteTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        notesPanel.add(tableScrollPane, BorderLayout.CENTER);
        notesPanel.add(createNoteControlPanel(), BorderLayout.NORTH);
        populateNoteTableModel();
        return notesPanel;
    }

    private JTable createNoteTable() {
        DefaultTableModel model = new DefaultTableModel(NOTE_COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        noteTable = new JTable(model);
        noteTable.setRowHeight(28);
        noteTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        noteTable.setFillsViewportHeight(true);
        noteTable.setIntercellSpacing(new Dimension(5, 2));

        noteTable.getColumnModel().getColumn(0).setPreferredWidth(250); // Title
        noteTable.getColumnModel().getColumn(1).setMaxWidth(70);      // Favorite
        noteTable.getColumnModel().getColumn(1).setMinWidth(60);
        noteTable.getColumnModel().getColumn(2).setPreferredWidth(300); // Mission
        noteTable.getColumnModel().getColumn(3).setMaxWidth(70);      // Alarm
        noteTable.getColumnModel().getColumn(3).setMinWidth(60);
        noteTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Modified
        noteTable.getColumnModel().getColumn(4).setMinWidth(130);


        DefaultTableCellRenderer titleRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Note) {
                    Note note = (Note) value;
                    String titleText = note.getTitle();
                    if (note.getNoteType() == Note.NoteType.DRAWING) {
                        titleText = "🎨 " + titleText;
                    } else {
                        titleText = "📄 " + titleText;
                    }
                    setText(titleText);
                } else {
                    setText(value != null ? value.toString() : "");
                }
                setHorizontalAlignment(JLabel.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                return this;
            }
        };
        noteTable.getColumnModel().getColumn(0).setCellRenderer(titleRenderer);

        DefaultTableCellRenderer centerRendererAll = new DefaultTableCellRenderer();
        centerRendererAll.setHorizontalAlignment(JLabel.CENTER);
        noteTable.getColumnModel().getColumn(1).setCellRenderer(centerRendererAll); // Favorite
        noteTable.getColumnModel().getColumn(3).setCellRenderer(centerRendererAll); // Alarm
        noteTable.getColumnModel().getColumn(4).setCellRenderer(centerRendererAll); // Modified


        DefaultTableCellRenderer missionRenderer = new DefaultTableCellRenderer();
        missionRenderer.setHorizontalAlignment(JLabel.LEFT);
        missionRenderer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        noteTable.getColumnModel().getColumn(2).setCellRenderer(missionRenderer);


        noteTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                label.setText("");
                label.setIcon(null);
                if (value instanceof Integer) {
                    int hour = (Integer) value;
                    if (hourIcons != null && hourIcons.length > 0 && hourIcons[0] != null) {
                        label.setIcon(hourIcons[0]);
                    } else {
                        label.setText("-");
                    }
                } else {
                    label.setText("-");
                }
                label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            }
        });

        noteTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                int row = noteTable.rowAtPoint(e.getPoint());
                if (row == -1) {
                    return;
                }
                int col = noteTable.columnAtPoint(e.getPoint());

                if (filteredNotes == null || row >= filteredNotes.size()) {
                    return;
                }
                Note selectedNote = filteredNotes.get(row);

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        handleNoteDoubleClick(noteTable);
                    } else if (e.getClickCount() == 1) {

                        if (col == 2 && selectedNote.isMission()) {
                            MissionDialog dialog = new MissionDialog(mainFrame);
                            dialog.setMission(selectedNote.getMissionContent());
                            dialog.setVisible(true);

                            if (dialog.isSaved()) {
                                String result = dialog.getResult();
                                if (controller != null) {
                                    controller.updateMission(selectedNote, result);
                                }
                                populateNoteTableModel();
                            }
                        }
                        else if (col == 3) {
                            showAlarmDialog(selectedNote);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = noteTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < noteTable.getRowCount()) {
                        noteTable.setRowSelectionInterval(row, row);
                        if (filteredNotes == null || row >= filteredNotes.size()) {
                            return;
                        }
                        Note selectedNote = filteredNotes.get(row);
                        if (controller != null) {
                            showNotePopup(e, selectedNote);
                        }
                    }
                }
            }
        });
        return noteTable;
    }

    private void showAlarmDialog(Note note) {
        if (note == null || controller == null) return;

        JDialog dialog = new JDialog(mainFrame, "Chi tiết Báo thức cho: " + note.getTitle(), true);
        if(mainFrame.getMouseEventDispatcher() != null) mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Alarm currentAlarm = note.getAlarm();
        long existingAlarmId = (currentAlarm != null && currentAlarm.getId() > 0) ? currentAlarm.getId() : 0L;
        LocalDateTime initialDateTimeToShow = (currentAlarm != null && currentAlarm.getAlarmTime() != null) ?
                currentAlarm.getAlarmTime() :
                LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0);
        String initialTypeStr = (currentAlarm != null && currentAlarm.isRecurring() && currentAlarm.getRecurrencePattern() != null) ?
                currentAlarm.getRecurrencePattern().toUpperCase() : "ONCE";
        if(currentAlarm != null && !currentAlarm.isRecurring()) initialTypeStr = "ONCE";


        JLabel currentInfoLabel = new JLabel("Hiện tại: " + (currentAlarm != null ? currentAlarm.toString() : "Chưa đặt báo thức."));
        currentInfoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        dialog.add(currentInfoLabel, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        dialog.add(new JLabel("Loại báo thức:"), gbc);
        String[] alarmTypes = {"ONCE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        JComboBox<String> typeComboBox = new JComboBox<>(alarmTypes);
        typeComboBox.setSelectedItem(initialTypeStr);
        gbc.gridx = 1;
        dialog.add(typeComboBox, gbc);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.setOpaque(false);
        JLabel dateLabelComponent = new JLabel("Ngày (yyyy-MM-dd):");
        JTextField dateField = new JTextField(10);
        dateField.setText(initialDateTimeToShow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        datePanel.add(dateLabelComponent);
        datePanel.add(dateField);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        dialog.add(datePanel, gbc);

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timePanel.setOpaque(false);
        timePanel.add(new JLabel("Thời gian (HH:mm):"));
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(initialDateTimeToShow.getHour(), 0, 23, 1));
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(initialDateTimeToShow.getMinute(), 0, 59, 1));
        timePanel.add(hourSpinner);
        timePanel.add(new JLabel(":"));
        timePanel.add(minuteSpinner);
        gbc.gridy++;
        dialog.add(timePanel, gbc);

        Runnable updatePanelsVisibility = () -> {
            boolean isOnce = "ONCE".equals(typeComboBox.getSelectedItem());
            datePanel.setVisible(isOnce);
            dialog.pack();
        };
        typeComboBox.addActionListener(e -> updatePanelsVisibility.run());
        updatePanelsVisibility.run();

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        buttonsPanel.setOpaque(false);
        JButton updateButton = new JButton(existingAlarmId > 0 ? "Cập Nhật" : "Đặt Báo Thức");
        updateButton.addActionListener(e -> {
            try {
                String selectedType = (String) typeComboBox.getSelectedItem();
                boolean isRecurring = !"ONCE".equals(selectedType);
                LocalDateTime newAlarmDateTime;

                int hour = (Integer) hourSpinner.getValue();
                int minute = (Integer) minuteSpinner.getValue();

                if ("ONCE".equals(selectedType)) {
                    LocalDate selectedDate = LocalDate.parse(dateField.getText(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    newAlarmDateTime = LocalDateTime.of(selectedDate, LocalTime.of(hour, minute));
                    if (newAlarmDateTime.isBefore(LocalDateTime.now())) {
                        JOptionPane.showMessageDialog(dialog, "Thời gian báo thức cho loại 'ONCE' phải ở tương lai.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } else {
                    LocalDate baseDateForRecurring = (currentAlarm != null && currentAlarm.isRecurring() && currentAlarm.getAlarmTime() != null) ?
                            currentAlarm.getAlarmTime().toLocalDate() : LocalDate.now();
                    newAlarmDateTime = LocalDateTime.of(baseDateForRecurring, LocalTime.of(hour, minute));
                }

                Alarm alarmToSet;
                if (existingAlarmId > 0 && currentAlarm != null) {
                    alarmToSet = currentAlarm;
                    alarmToSet.setAlarmTime(newAlarmDateTime);
                    alarmToSet.setRecurring(isRecurring);
                    alarmToSet.setRecurrencePattern(isRecurring ? selectedType.toUpperCase() : null);
                } else {
                    alarmToSet = new Alarm(newAlarmDateTime, isRecurring, isRecurring ? selectedType.toUpperCase() : null);
                }
                controller.setAlarm(note, alarmToSet);
                populateNoteTableModel();
                dialog.dispose();
            } catch (java.time.format.DateTimeParseException dtpe) {
                JOptionPane.showMessageDialog(dialog, "Định dạng ngày không hợp lệ! Vui lòng dùng yyyy-MM-dd.", "Lỗi Định Dạng Ngày", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Lỗi khi đặt báo thức: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton deleteButton = new JButton("Xóa Báo Thức");
        deleteButton.setEnabled(existingAlarmId > 0);
        deleteButton.addActionListener(e -> {
            if (existingAlarmId > 0) {
                controller.setAlarm(note, null);
                populateNoteTableModel();
            }
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Hủy");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonsPanel.add(updateButton);
        if (existingAlarmId > 0) buttonsPanel.add(deleteButton);
        buttonsPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        dialog.add(buttonsPanel, gbc);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(400, dialog.getHeight()));
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }


    private JPanel createNoteControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Add Note Button
        JButton addNoteButton = new JButton(ADD_NOTE_LABEL);
        addNoteButton.setToolTipText("Thêm ghi chú văn bản mới (Ctrl+N)");
        addNoteButton.addActionListener(e -> mainFrame.showAddNoteScreen());
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(addNoteButton, gbc);

        // Add Draw Panel Button
        JButton addDrawPanelButton = new JButton(ADD_DRAW_PANEL_LABEL);
        addDrawPanelButton.setToolTipText("Thêm một bản vẽ mới (Ctrl+Shift+N)");
        addDrawPanelButton.addActionListener(e -> mainFrame.showNewDrawScreen());
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(addDrawPanelButton, gbc);

        // Scanner Button
        try {
            ImageIcon scannerIcon = new ImageIcon(getClass().getResource("/images/Clara.jpg"));
            Image scaledIcon = scannerIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            JButton scannerButton = new JButton(new ImageIcon(scaledIcon));
            scannerButton.setToolTipText("Mở công cụ Scanner");
            scannerButton.addActionListener(e -> FloatingScannerTray.getInstance().setVisible(true));
            gbc.gridx = 2; gbc.gridy = 0;
            panel.add(scannerButton, gbc);
        } catch (Exception ex) {
            System.err.println("Không thể tải icon scanner: " + ex.getMessage());
            JButton scannerFallbackButton = new JButton("Scan");
            scannerFallbackButton.setToolTipText("Mở công cụ Scanner (icon lỗi)");
            scannerFallbackButton.addActionListener(e -> FloatingScannerTray.getInstance().setVisible(true));
            gbc.gridx = 2; gbc.gridy = 0;
            panel.add(scannerFallbackButton, gbc);
        }

        // Spacer
        gbc.gridx = 3; gbc.weightx = 0.1;
        panel.add(Box.createHorizontalStrut(10), gbc);
        gbc.weightx = 0;


        // Title Search Field
        titleSearchField = new JTextField(15);
        titleSearchField.setText(TITLE_SEARCH_PLACEHOLDER);
        titleSearchField.setForeground(Color.GRAY);
        titleSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (titleSearchField.getText().equals(TITLE_SEARCH_PLACEHOLDER)) {
                    titleSearchField.setText("");
                    titleSearchField.setForeground(UIManager.getColor("TextField.foreground"));
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (titleSearchField.getText().isEmpty()) {
                    titleSearchField.setText(TITLE_SEARCH_PLACEHOLDER);
                    titleSearchField.setForeground(Color.GRAY);
                }
            }
        });
        addSearchFieldListener(titleSearchField);
        gbc.gridx = 4; gbc.gridy = 0;
        panel.add(titleSearchField, gbc);

        // Tag Search Field
        tagSearchField = new JTextField(15);
        tagSearchField.setText(TAG_SEARCH_PLACEHOLDER);
        tagSearchField.setForeground(Color.GRAY);
        tagSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (tagSearchField.getText().equals(TAG_SEARCH_PLACEHOLDER)) {
                    tagSearchField.setText("");
                    tagSearchField.setForeground(UIManager.getColor("TextField.foreground"));
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (tagSearchField.getText().isEmpty()) {
                    tagSearchField.setText(TAG_SEARCH_PLACEHOLDER);
                    tagSearchField.setForeground(Color.GRAY);
                }
            }
        });
        addSearchFieldListener(tagSearchField);
        gbc.gridx = 5; gbc.gridy = 0;
        panel.add(tagSearchField, gbc);

        // Spacer to push stats and refresh to the right
        gbc.gridx = 6; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;


        // Refresh Button
        JButton refreshButton = new JButton(REFRESH_LABEL);
        refreshButton.setToolTipText("Làm mới danh sách (Ctrl+R)");
        refreshButton.addActionListener(e -> refresh());
        gbc.gridx = 8; gbc.gridy = 0;
        panel.add(refreshButton, gbc);

        panel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));

        return panel;
    }
    private void addSearchFieldListener(JTextField searchField) {
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private Timer debounceTimer;
            public void insertUpdate(javax.swing.event.DocumentEvent e) { debouncePopulate(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { debouncePopulate(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { debouncePopulate(); }
            private void debouncePopulate() {
                if (debounceTimer != null && debounceTimer.isRunning()) debounceTimer.stop();
                debounceTimer = new Timer(300, evt -> populateNoteTableModel());
                debounceTimer.setRepeats(false);
                debounceTimer.start();
            }
        });
    }

    private void populateNoteTableModel() {
        if (noteTable == null || controller == null) return;
        DefaultTableModel model = (DefaultTableModel) noteTable.getModel();
        model.setRowCount(0);

        String titleQuery = (titleSearchField != null && !TITLE_SEARCH_PLACEHOLDER.equals(titleSearchField.getText())) ?
                titleSearchField.getText().trim().toLowerCase() : "";
        String tagQuery = (tagSearchField != null && !TAG_SEARCH_PLACEHOLDER.equals(tagSearchField.getText())) ?
                tagSearchField.getText().trim().toLowerCase() : "";

        List<Note> notesToDisplay = controller.getSortedNotes();

        if (notesToDisplay != null) {
            filteredNotes = notesToDisplay.stream()
                    .filter(note -> {
                        boolean titleMatch = titleQuery.isEmpty() || (note.getTitle() != null && note.getTitle().toLowerCase().contains(titleQuery));
                        boolean tagMatch = tagQuery.isEmpty() || (note.getTags() != null && note.getTags().stream()
                                .anyMatch(tag -> tag.getName().toLowerCase().contains(tagQuery)));
                        return titleMatch && tagMatch;
                    })
                    .collect(Collectors.toList());

            for (Note note : filteredNotes) {
                Object alarmValue = (note.getAlarm() != null && note.getAlarm().getAlarmTime() != null) ?
                        note.getAlarm().getAlarmTime().getHour() : null;
                String missionDisplay = "";
                if (note.isMission() && note.getMissionContent() != null && !note.getMissionContent().isEmpty()) {
                    missionDisplay = "✔ " + note.getMissionContent();
                    if (note.isMissionCompleted()) {
                        missionDisplay += " (Xong)";
                    }
                }
                model.addRow(new Object[]{
                        note,
                        note.isFavorite() ? "★" : "",
                        missionDisplay,
                        alarmValue,
                        note.getFormattedModificationDate()
                });
            }
        } else {
            filteredNotes = new ArrayList<>();
        }
    }


    private void handleNoteDoubleClick(JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0 && filteredNotes != null && row < filteredNotes.size()) {
            Note selectedNote = filteredNotes.get(row);
            if (selectedNote.getNoteType() == Note.NoteType.DRAWING) {
                mainFrame.showEditDrawScreen(selectedNote);
            } else {
                mainFrame.showNoteDetailScreen(selectedNote);
            }
        }
    }
    private void showNotePopup(MouseEvent e, Note note) {
        if (note == null || controller == null) return;
        JPopupMenu popup = new JPopupMenu();

        JMenuItem renameItem = new JMenuItem("Đổi tên");
        renameItem.addActionListener(ev -> {
            String newTitle = JOptionPane.showInputDialog(mainFrame, "Nhập tiêu đề mới:", note.getTitle());
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                controller.renameNote(note, newTitle.trim());
                populateNoteTableModel();
            }
        });
        popup.add(renameItem);

        JCheckBoxMenuItem favoriteItem = new JCheckBoxMenuItem("Yêu thích", note.isFavorite());
        favoriteItem.addActionListener(ev -> {
            controller.setNoteFavorite(note, !note.isFavorite());
            populateNoteTableModel();
        });
        popup.add(favoriteItem);

        if (note.getNoteType() == Note.NoteType.TEXT) {
            JCheckBoxMenuItem missionItemOriginal = new JCheckBoxMenuItem("Nhiệm vụ", note.isMission());
            missionItemOriginal.addActionListener(ev -> {
                MissionDialog dialog = new MissionDialog(mainFrame);
                if(mainFrame.getMouseEventDispatcher() != null) mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
                dialog.setMission(note.getMissionContent());
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    String result = dialog.getResult();
                    controller.updateMission(note, result != null ? result.trim() : "");
                }
                populateNoteTableModel();
            });
            popup.add(missionItemOriginal);
        }

        JMenuItem alarmItem = new JMenuItem("Đặt/Sửa Báo thức");
        alarmItem.addActionListener(ev -> {
            showAlarmDialog(note);
        });
        popup.add(alarmItem);

        JMenuItem moveItem = new JMenuItem("Di chuyển");
        moveItem.addActionListener(ev -> {
            List<Folder> allFolders = controller.getFolders();
            Folder currentNoteFolder = note.getFolder();

            final Folder finalCurrentNoteFolder = currentNoteFolder;
            List<Folder> targetFolders = allFolders.stream()
                    .filter(f -> finalCurrentNoteFolder == null || f.getId() != finalCurrentNoteFolder.getId())
                    .collect(Collectors.toList());
            Folder rootFolder = controller.getFolderByName("Root").orElse(null);
            if (rootFolder != null && (finalCurrentNoteFolder == null || finalCurrentNoteFolder.getId() != rootFolder.getId())) {
                if (!targetFolders.stream().anyMatch(tf -> tf.getId() == rootFolder.getId())) {
                    targetFolders.add(0, rootFolder);
                }
            }


            if (targetFolders.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Không có thư mục khác để di chuyển đến.", "Di Chuyển Ghi Chú", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JComboBox<Folder> folderCombo = new JComboBox<>(targetFolders.toArray(new Folder[0]));
            folderCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Folder) setText(((Folder) value).getName());
                    return this;
                }
            });
            int result = JOptionPane.showConfirmDialog(mainFrame, folderCombo, "Di chuyển đến Thư mục", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                Folder selectedFolder = (Folder) folderCombo.getSelectedItem();
                if (selectedFolder != null) {
                    controller.moveNoteToFolder(note, selectedFolder);
                    populateNoteTableModel();
                }
            }
        });
        popup.add(moveItem);

        JMenuItem deleteItem = new JMenuItem("Xóa");
        deleteItem.addActionListener(ev -> handleNoteDeletion(note));
        popup.add(deleteItem);

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void handleNoteDeletion(Note note) {
        if (controller == null) return;
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Xóa '" + note.getTitle() + "'?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            controller.deleteNote(note);
            populateNoteTableModel();
        }
    }

    public void refresh() {
        if (controller == null) {
            System.err.println("MainMenuScreen: Controller is null, cannot refresh.");
            return;
        }
        System.out.println("MainMenuScreen: Refreshing...");
        refreshFolderPanel();
        populateNoteTableModel();
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "addTextNote");
        actionMap.put("addTextNote", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (mainFrame != null) mainFrame.showAddNoteScreen();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "addDrawPanel");
        actionMap.put("addDrawPanel", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (mainFrame != null) mainFrame.showNewDrawScreen();
            }
        });


        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), "refresh");
        actionMap.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "addFolder");
        actionMap.put("addFolder", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (controller == null || mainFrame == null) return;
                String name = JOptionPane.showInputDialog(mainFrame, "Enter folder name:");
                if (name != null && !name.trim().isEmpty()) {
                    controller.addNewFolder(name.trim());
                    refreshFolderPanel();
                }
            }
        });
    }
}