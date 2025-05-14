import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class MainMenuScreen extends JPanel {
    private static final String[] NOTE_COLUMNS = {"Title", "Favorite", "Mission", "Alarm", "Modified"};
    private static final String FOLDERS_TITLE = "Folders";
    private static final String ADD_FOLDER_LABEL = "Add Folder";
    private static final String ADD_NOTE_LABEL = "Add Note";
    private static final String SHOW_STATS_LABEL = "Show Stats";
    private static final String REFRESH_LABEL = "Refresh";
    private static final String TITLE_SEARCH_PLACEHOLDER = "Search by title";
    private static final String TAG_SEARCH_PLACEHOLDER = "Search by tag";

    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel folderPanel;
    private JTable noteTable;
    private JTextField titleSearchField;
    private JTextField tagSearchField;
    private JList<Folder> folderList;
    private List<Note> filteredNotes;
    private ImageIcon[] hourIcons; // Mảng lưu 24 biểu tượng giờ

    public MainMenuScreen(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        loadHourIcons(); // Tải biểu tượng
        initializeUI();
        setupShortcuts();
    }

    // Tải 24 biểu tượng giờ từ file hour_0.jpg đến hour_23.jpg
    private void loadHourIcons() {
        hourIcons = new ImageIcon[24];
        for (int i = 0; i < 24; i++) {
            try {
                hourIcons[i] = new ImageIcon(getClass().getResource("/icons/hour_" + i + ".jpg"));
                // Điều chỉnh kích thước biểu tượng nếu cần
                Image img = hourIcons[i].getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                hourIcons[i] = new ImageIcon(img);
            } catch (Exception e) {
                System.err.println("Không thể tải biểu tượng hour_" + i + ".jpg: " + e.getMessage());
                hourIcons[i] = null; // Biểu tượng mặc định nếu lỗi
            }
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        add(buildFolderPanel(), BorderLayout.WEST);
        add(buildNotesPanel(), BorderLayout.CENTER);
    }

    private JPanel buildFolderPanel() {
        folderPanel = new JPanel();
        folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.Y_AXIS));
        folderPanel.setBorder(BorderFactory.createTitledBorder(FOLDERS_TITLE));

        folderList = new JList<>(new DefaultListModel<>());
        folderList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Folder) {
                    Folder folder = (Folder) value;
                    StringBuilder displayText = new StringBuilder(folder.getName());
                    if (folder.isFavorite()) displayText.append(" ★");
                    if (folder.isMission()) displayText.append(" ✔");
                    if (isSelected) displayText.append(" (Selected)");
                    setText(displayText.toString());
                }
                return c;
            }
        });
        folderList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showFolderPopupMenu(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showFolderPopupMenu(e);
            }
        });
        folderList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && folderList.getSelectedValue() != null) {
                controller.selectFolder(folderList.getSelectedValue());
                populateNoteTableModel();
            }
        });
        folderPanel.add(new JScrollPane(folderList));

        JButton addFolderButton = createAddFolderButton();
        folderPanel.add(addFolderButton);

        refreshFolderPanel();
        return folderPanel;
    }

    private void showFolderPopupMenu(MouseEvent e) {
        int index = folderList.locationToIndex(e.getPoint());
        if (index >= 0) {
            folderList.setSelectedIndex(index);
            Folder folder = folderList.getSelectedValue();
            if (folder != null && !folder.getName().equals("Root")) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem renameItem = new JMenuItem("Rename");
                renameItem.addActionListener(ev -> {
                    String newName = JOptionPane.showInputDialog(mainFrame, "Enter new folder name:", folder.getName());
                    if (newName != null && !newName.trim().isEmpty()) {
                        controller.renameFolder(folder, newName);
                        refreshFolderPanel();
                    }
                });
                popup.add(renameItem);

                JCheckBoxMenuItem favoriteItem = new JCheckBoxMenuItem("Favorite", folder.isFavorite());
                favoriteItem.addActionListener(ev -> {
                    controller.setFolderFavorite(folder, !folder.isFavorite());
                    refreshFolderPanel();
                });
                popup.add(favoriteItem);

                JMenuItem deleteItem = new JMenuItem("Delete");
                deleteItem.addActionListener(ev -> {
                    int confirm = JOptionPane.showConfirmDialog(mainFrame,
                            "Delete folder " + folder.getName() + " and its notes?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        controller.deleteFolder(folder);
                        refreshFolderPanel();
                        populateNoteTableModel();
                    }
                });
                popup.add(deleteItem);

                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private JButton createAddFolderButton() {
        JButton addFolderButton = new JButton(ADD_FOLDER_LABEL);
        addFolderButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(mainFrame, "Enter folder name:");
            if (name != null && !name.trim().isEmpty()) {
                controller.addNewFolder(name);
                refreshFolderPanel();
            }
        });
        return addFolderButton;
    }

    private JPanel buildNotesPanel() {
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createTitledBorder("Notes"));

        noteTable = createNoteTable();
        notesPanel.add(new JScrollPane(noteTable), BorderLayout.CENTER);
        notesPanel.add(createNoteControlPanel(), BorderLayout.NORTH);

        populateNoteTableModel();
        return notesPanel;
    }

    private JTable createNoteTable() {
        DefaultTableModel model = new DefaultTableModel(NOTE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(250);
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.getColumnModel().getColumn(3).setMaxWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setMinWidth(130);

        // Renderer tùy chỉnh để căn giữa tất cả các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                return c;
            }
        };

        // Áp dụng renderer cho tất cả các cột
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Renderer đặc biệt cho cột Alarm để hiển thị biểu tượng
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setText(""); // Xóa text mặc định
                if (value instanceof Integer) { // Giá trị là giờ (0-23)
                    int hour = (int) value;
                    if (hour >= 0 && hour < 24 && hourIcons[hour] != null) {
                        label.setIcon(hourIcons[hour]); // Hiển thị biểu tượng tương ứng với giờ
                        label.setHorizontalAlignment(JLabel.CENTER);
                    }
                } else {
                    label.setIcon(null); // Không có alarm, xóa biểu tượng
                }
                label.setForeground(Color.BLUE);
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                return label;
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    int column = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && row < filteredNotes.size()) {
                        Note selectedNote = filteredNotes.get(row);
                        if (column == 2) { // Cột "Mission"
                            MissionDialog dialog = new MissionDialog(mainFrame);
                            dialog.setMission(selectedNote.getMissionContent());
                            dialog.setVisible(true);
                            String result = dialog.getResult();
                            if (result != null) {
                                controller.updateMission(selectedNote, result);
                                populateNoteTableModel();
                            }
                        } else if (column == 3) { // Cột "Alarm"
                            showAlarmDialog(selectedNote);
                        } else {
                            handleNoteDoubleClick(table);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopupMenu(e);
            }
        });

        return table;
    }

    private void showAlarmDialog(Note note) {
        JDialog dialog = new JDialog(mainFrame, "Alarm Details", true);
        dialog.setSize(300, 250); // Kích thước cố định
        dialog.setResizable(false); // Không cho thay đổi kích thước
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Thêm padding
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Alarm alarm = note.getAlarm();
        DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String alarmTimeStr = alarm != null ? alarm.getAlarmTime().format(formatterFull) : LocalDateTime.now().format(formatterFull);
        String alarmType = alarm != null && alarm.isRecurring() ? alarm.getFrequency() : "ONCE";

        // Hiển thị thông tin alarm
        JLabel alarmLabel = new JLabel("Alarm Time: " + alarmTimeStr);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        dialog.add(alarmLabel, gbc);

        // Tùy chọn loại alarm
        String[] alarmTypes = {"ONCE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        JComboBox<String> typeComboBox = new JComboBox<>(alarmTypes);
        typeComboBox.setSelectedItem(alarmType);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        dialog.add(new JLabel("Alarm Type:"), gbc);
        gbc.gridx = 1;
        dialog.add(typeComboBox, gbc);

        // Panel chứa các trường ngày giờ với kích thước cố định
        JPanel dateTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateTimePanel.setPreferredSize(new Dimension(250, 30)); // Giới hạn kích thước panel
        JTextField dateField = new JTextField(10);
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

        // Đặt giá trị ban đầu
        if (alarm != null) {
            dateField.setText(alarm.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            hourSpinner.setValue(alarm.getAlarmTime().getHour());
            minuteSpinner.setValue(alarm.getAlarmTime().getMinute());
        } else {
            LocalDateTime now = LocalDateTime.now();
            dateField.setText(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            hourSpinner.setValue(now.getHour());
            minuteSpinner.setValue(now.getMinute());
        }

        // Thêm thành phần ban đầu
        if ("ONCE".equals(alarmType)) {
            dateTimePanel.add(new JLabel("Date (yyyy-MM-dd):"));
            dateTimePanel.add(dateField);
        }
        dateTimePanel.add(new JLabel("Hour:"));
        dateTimePanel.add(hourSpinner);
        dateTimePanel.add(new JLabel("Minute:"));
        dateTimePanel.add(minuteSpinner);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        dialog.add(dateTimePanel, gbc);

        // Xử lý thay đổi loại alarm
        typeComboBox.addActionListener(e -> {
            String selectedType = (String) typeComboBox.getSelectedItem();
            dateTimePanel.removeAll();
            if ("ONCE".equals(selectedType)) {
                dateTimePanel.add(new JLabel("Date (yyyy-MM-dd):"));
                dateTimePanel.add(dateField);
            }
            dateTimePanel.add(new JLabel("Hour:"));
            dateTimePanel.add(hourSpinner);
            dateTimePanel.add(new JLabel("Minute:"));
            dateTimePanel.add(minuteSpinner);
            dateTimePanel.revalidate();
            dateTimePanel.repaint();
        });

        // Nút cập nhật alarm
        JButton updateButton = new JButton("Update Alarm");
        updateButton.addActionListener(e -> {
            try {
                String selectedType = (String) typeComboBox.getSelectedItem();
                boolean isRecurring = !"ONCE".equals(selectedType);
                LocalDateTime newTime;

                int hour = (int) hourSpinner.getValue();
                int minute = (int) minuteSpinner.getValue();
                if ("ONCE".equals(selectedType)) {
                    LocalDateTime date = LocalDateTime.parse(dateField.getText() + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    newTime = date.withHour(hour).withMinute(minute).withSecond(0);
                } else {
                    LocalDateTime baseDate = alarm != null ? alarm.getAlarmTime() : LocalDateTime.now();
                    newTime = baseDate.withHour(hour).withMinute(minute).withSecond(0);
                }

                Alarm newAlarm = new Alarm(newTime, isRecurring, selectedType);
                controller.setAlarm(note, newAlarm);
                populateNoteTableModel();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format! Use yyyy-MM-dd", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        dialog.add(updateButton, gbc);

        // Nút xóa alarm
        JButton deleteButton = new JButton("Delete Alarm");
        deleteButton.setEnabled(alarm != null);
        deleteButton.addActionListener(e -> {
            controller.setAlarm(note, null);
            populateNoteTableModel();
            dialog.dispose();
        });
        gbc.gridy = 4;
        dialog.add(deleteButton, gbc);

        // Nút hủy
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        gbc.gridy = 5;
        dialog.add(cancelButton, gbc);

        // Đặt kích thước tối thiểu để tránh co rút
        dialog.setMinimumSize(new Dimension(300, 250));
        dialog.pack();
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    private JPanel createNoteControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addNoteButton = new JButton(ADD_NOTE_LABEL);
        addNoteButton.addActionListener(e -> mainFrame.showAddNoteScreen());
        panel.add(addNoteButton);

        titleSearchField = new JTextField(15);
        titleSearchField.setText(TITLE_SEARCH_PLACEHOLDER);
        titleSearchField.setForeground(Color.GRAY);
        titleSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (titleSearchField.getText().equals(TITLE_SEARCH_PLACEHOLDER)) {
                    titleSearchField.setText("");
                    titleSearchField.setForeground(Color.BLACK);
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
        panel.add(titleSearchField);

        tagSearchField = new JTextField(15);
        tagSearchField.setText(TAG_SEARCH_PLACEHOLDER);
        tagSearchField.setForeground(Color.GRAY);
        tagSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (tagSearchField.getText().equals(TAG_SEARCH_PLACEHOLDER)) {
                    tagSearchField.setText("");
                    tagSearchField.setForeground(Color.BLACK);
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
        panel.add(tagSearchField);

        JButton statsButton = new JButton(SHOW_STATS_LABEL);
        statsButton.addActionListener(e -> mainFrame.openCanvasPanel());
        panel.add(statsButton);

        JButton refreshButton = new JButton(REFRESH_LABEL);
        refreshButton.addActionListener(e -> {
            populateNoteTableModel();
            refreshFolderPanel();
        });
        panel.add(refreshButton);

        return panel;
    }

    private void addSearchFieldListener(JTextField searchField) {
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { debouncePopulate(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { debouncePopulate(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { debouncePopulate(); }
            private void debouncePopulate() {
                if (debounceTimer != null && debounceTimer.isRunning()) debounceTimer.stop();
                debounceTimer = new Timer(300, evt -> populateNoteTableModel());
                debounceTimer.setRepeats(false);
                debounceTimer.start();
            }
            private Timer debounceTimer;
        });
    }

    private void populateNoteTableModel() {
        if (noteTable == null) return;
        DefaultTableModel model = (DefaultTableModel) noteTable.getModel();
        model.setRowCount(0);

        String titleQuery = titleSearchField.getText().equals(TITLE_SEARCH_PLACEHOLDER) ? "" : titleSearchField.getText().trim().toLowerCase();
        String tagQuery = tagSearchField.getText().equals(TAG_SEARCH_PLACEHOLDER) ? "" : tagSearchField.getText().trim().toLowerCase();

        filteredNotes = filterNotes(controller.getSortedNotes(), titleQuery, tagQuery);
        for (Note note : filteredNotes) {
            Object alarmValue = note.getAlarm() != null ? note.getAlarm().getAlarmTime().getHour() : null;
            model.addRow(new Object[]{
                    note.getTitle(),
                    note.isFavorite() ? "★" : "",
                    note.getMissionContent().isEmpty() ? "" : "✔ " + note.getMissionContent(),
                    alarmValue, // Truyền giờ của alarm (0-23) hoặc null nếu không có alarm
                    note.getFormattedModificationDate()
            });
        }
    }

    private List<Note> filterNotes(List<Note> notes, String titleQuery, String tagQuery) {
        List<Note> filtered = new ArrayList<>(notes);

        if (!titleQuery.isEmpty() && !tagQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(note -> note.getTitle().toLowerCase().contains(titleQuery) &&
                            note.getTags().stream().anyMatch(tag -> tag.getName().toLowerCase().contains(tagQuery)))
                    .collect(Collectors.toList());
        } else if (!titleQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(note -> note.getTitle().toLowerCase().contains(titleQuery))
                    .collect(Collectors.toList());
        } else if (!tagQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(note -> note.getTags().stream().anyMatch(tag -> tag.getName().toLowerCase().contains(tagQuery)))
                    .collect(Collectors.toList());
        }

        return filtered;
    }

    private void handleNoteDoubleClick(JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0 && row < filteredNotes.size()) {
            Note selectedNote = filteredNotes.get(row);
            mainFrame.showNoteDetailScreen(selectedNote);
        }
    }

    private void showPopupMenu(MouseEvent e) {
        int row = noteTable.rowAtPoint(e.getPoint());
        if (row >= 0 && row < noteTable.getRowCount()) {
            noteTable.setRowSelectionInterval(row, row);
            showNotePopup(e, filteredNotes.get(row));
        }
    }

    private void showNotePopup(MouseEvent e, Note note) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(ev -> {
            String newTitle = JOptionPane.showInputDialog(mainFrame, "Enter new title:", note.getTitle());
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                controller.renameNote(note, newTitle);
                populateNoteTableModel();
            }
        });
        popup.add(renameItem);

        JCheckBoxMenuItem favoriteItem = new JCheckBoxMenuItem("Favorite", note.isFavorite());
        favoriteItem.addActionListener(ev -> {
            controller.setNoteFavorite(note, !note.isFavorite());
            populateNoteTableModel();
        });
        popup.add(favoriteItem);

        JCheckBoxMenuItem missionItem = new JCheckBoxMenuItem("Mission", note.isMission());
        missionItem.addActionListener(ev -> {
            MissionDialog dialog = new MissionDialog(mainFrame);
            dialog.setMission(note.getMissionContent());
            dialog.setVisible(true);
            String result = dialog.getResult();
            if (result != null) {
                controller.updateMission(note, result);
                populateNoteTableModel();
            }
        });
        popup.add(missionItem);

        JMenuItem alarmItem = new JMenuItem("Set Alarm");
        alarmItem.addActionListener(ev -> {
            AlarmDialog dialog = new AlarmDialog(mainFrame);
            dialog.setVisible(true);
            Alarm alarm = dialog.getResult();
            if (alarm != null) {
                controller.setAlarm(note, alarm);
                populateNoteTableModel();
            }
        });
        popup.add(alarmItem);

        JMenuItem moveItem = new JMenuItem("Move");
        moveItem.addActionListener(ev -> {
            JComboBox<Folder> folderCombo = new JComboBox<>(controller.getFolders().toArray(new Folder[0]));
            folderCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Folder) {
                        Folder folder = (Folder) value;
                        setText(folder.getName());
                    }
                    return c;
                }
            });
            int result = JOptionPane.showConfirmDialog(mainFrame, folderCombo, "Move to Folder", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                controller.moveNoteToFolder(note, (Folder) folderCombo.getSelectedItem());
                populateNoteTableModel();
            }
        });
        popup.add(moveItem);

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(ev -> handleNoteDeletion(note));
        popup.add(deleteItem);

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void handleNoteDeletion(Note note) {
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Delete " + note.getTitle() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            controller.deleteNote(note);
            populateNoteTableModel();
        }
    }

    public void refresh() {
        populateNoteTableModel();
        refreshFolderPanel();
    }

    public void refreshFolderPanel() {
        DefaultListModel<Folder> model = (DefaultListModel<Folder>) folderList.getModel();
        model.clear();
        List<Folder> folders = controller.getFolders();

        Folder rootFolder = folders.stream()
                .filter(f -> f.getName().equals("Root"))
                .findFirst()
                .orElse(null);
        if (rootFolder != null) {
            model.addElement(rootFolder);
        }

        List<Folder> otherFolders = folders.stream()
                .filter(f -> !f.getName().equals("Root"))
                .sorted(Comparator.comparing(Folder::isFavorite, Comparator.reverseOrder())
                        .thenComparing(Folder::isMission, Comparator.reverseOrder())
                        .thenComparing(Folder::getName))
                .collect(Collectors.toList());
        for (Folder folder : otherFolders) {
            model.addElement(folder);
        }
        folderList.repaint();
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // Ctrl + N: Tạo ghi chú mới
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "addNote");
        actionMap.put("addNote", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                mainFrame.showAddNoteScreen();
            }
        });

        // Ctrl + R: Làm mới giao diện
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), "refresh");
        actionMap.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
            }
        });

        // Ctrl + F: Tạo thư mục mới
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "addFolder");
        actionMap.put("addFolder", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String name = JOptionPane.showInputDialog(mainFrame, "Enter folder name:");
                if (name != null && !name.trim().isEmpty()) {
                    controller.addNewFolder(name);
                    refreshFolderPanel();
                }
            }
        });
    }
}