import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
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
import java.util.Objects; // Thêm import này
import java.util.stream.Collectors;

public class MainMenuScreen extends JPanel {
    private static final String[] NOTE_COLUMNS = {"Title", "Favorite", "Mission", "Alarm", "Modified"};
    private static final String FOLDERS_TITLE = "Thư mục"; // Đổi tên cho thân thiện
    private static final String ADD_FOLDER_LABEL = "Thêm Thư mục";
    private static final String ADD_NOTE_LABEL = "Thêm Ghi chú Chữ";
    private static final String ADD_DRAW_PANEL_LABEL = "Thêm Bản Vẽ";
    private static final String SHOW_STATS_LABEL = "Thống kê";
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
    private DefaultListModel<Folder> folderListModel; // Giữ tham chiếu đến model
    private List<Note> filteredNotes;
    private ImageIcon[] hourIcons;
    private ListSelectionListener folderListSelectionHandler; // Giữ tham chiếu đến listener

    public MainMenuScreen(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        loadHourIcons();
        initializeUI();
        setupShortcuts();
    }

    private void loadHourIcons() {
        hourIcons = new ImageIcon[24];
        for (int i = 0; i < 24; i++) {
            try {
                java.net.URL imgUrl = getClass().getResource("/icons/hour_" + i + ".jpg");
                if (imgUrl != null) {
                    hourIcons[i] = new ImageIcon(imgUrl);
                    Image img = hourIcons[i].getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                    hourIcons[i] = new ImageIcon(img);
                } else {
                    System.err.println("Không tìm thấy tài nguyên: /icons/hour_" + i + ".jpg");
                    hourIcons[i] = createDefaultIcon("H" + i);
                }
            } catch (Exception e) {
                System.err.println("Không thể tải icon hour_" + i + ".jpg: " + e.getMessage());
                hourIcons[i] = createDefaultIcon("E" + i);
            }
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
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(buildFolderPanel(), BorderLayout.WEST);
        add(buildNotesPanel(), BorderLayout.CENTER);
    }

    private JPanel buildFolderPanel() {
        folderPanel = new JPanel();
        folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.Y_AXIS));
        folderPanel.setBorder(BorderFactory.createTitledBorder(FOLDERS_TITLE));
        folderPanel.setPreferredSize(new Dimension(200, 0));

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

        // Tạo listener và lưu lại tham chiếu
        folderListSelectionHandler = e -> {
            if (!e.getValueIsAdjusting()) {
                Folder selectedFolder = folderList.getSelectedValue();
                // Chỉ gọi controller.selectFolder nếu selectedFolder thực sự hợp lệ
                if (controller != null && selectedFolder != null && selectedFolder.getId() != 0) {
                    System.out.println("[MainMenuScreen FolderListener] Đã chọn: " + selectedFolder.getName() + " (ID: " + selectedFolder.getId() + ")");
                    controller.selectFolder(selectedFolder);
                } else if (controller != null && selectedFolder == null && !folderListModel.isEmpty()){
                    // Nếu không có gì được chọn (ví dụ sau khi xóa), thử chọn Root
                    System.out.println("[MainMenuScreen FolderListener] Không có thư mục nào được chọn, thử chọn Root.");
                    Folder root = controller.getFolderByName("Root").orElse(null);
                    if (root != null) controller.selectFolder(root);
                } else if (controller != null && selectedFolder != null && selectedFolder.getId() == 0) {
                    System.out.println("[MainMenuScreen FolderListener] Thư mục được chọn có ID 0: " + selectedFolder.getName() + ". Bỏ qua select.");
                }
                populateNoteTableModel();
            }
        };
        folderList.addListSelectionListener(folderListSelectionHandler);

        folderPanel.add(new JScrollPane(folderList));
        JButton addFolderButton = createAddFolderButton();
        folderPanel.add(addFolderButton);

        refreshFolderPanel();
        return folderPanel;
    }

    public void refreshFolderPanel() {
        if (folderList == null || controller == null) return;
        System.out.println("[MainMenuScreen refreshFolderPanel] Bắt đầu làm mới danh sách thư mục.");

        Folder previouslySelectedFolder = folderList.getSelectedValue();
        long previouslySelectedId = (previouslySelectedFolder != null) ? previouslySelectedFolder.getId() : 0;
        if (previouslySelectedId == 0 && previouslySelectedFolder != null && "Root".equalsIgnoreCase(previouslySelectedFolder.getName())) {
            // Nếu folder chọn trước đó là Root (có thể là instance tạm thời), lấy ID Root thực sự
            Folder rootFromCtrl = controller.getFolderByName("Root").orElse(null);
            if (rootFromCtrl != null) previouslySelectedId = rootFromCtrl.getId();
        }


        // Tạm thời vô hiệu hóa listener
        if (folderListSelectionHandler != null) {
            folderList.removeListSelectionListener(folderListSelectionHandler);
        }

        folderListModel.clear();
        List<Folder> foldersFromController = controller.getFolders();

        Folder rootFolder = foldersFromController.stream()
                .filter(f -> "Root".equalsIgnoreCase(f.getName()) && f.getId() != 0) // Đảm bảo Root có ID
                .findFirst().orElse(null);

        if (rootFolder != null) {
            folderListModel.addElement(rootFolder);
        } else {
            System.err.println("[MainMenuScreen refreshFolderPanel] Lỗi: Không tìm thấy thư mục Root hợp lệ từ controller.");
            // Có thể tạo một Root tạm thời ở đây nếu cần, nhưng NoteManager nên đảm bảo Root luôn tồn tại
        }

        List<Folder> otherFolders = foldersFromController.stream()
                .filter(f -> rootFolder == null || f.getId() != rootFolder.getId())
                .sorted(Comparator.comparing(Folder::isFavorite, Comparator.reverseOrder())
                        .thenComparing(Folder::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        for (Folder folder : otherFolders) {
            if (folder.getId() != 0) { // Chỉ thêm các folder có ID hợp lệ
                folderListModel.addElement(folder);
            } else {
                System.err.println("[MainMenuScreen refreshFolderPanel] Cảnh báo: Bỏ qua thư mục '" + folder.getName() + "' vì không có ID hợp lệ.");
            }
        }

        // Cố gắng chọn lại folder
        int selectionIndex = -1;
        if (previouslySelectedId != 0) {
            for (int i = 0; i < folderListModel.getSize(); i++) {
                if (folderListModel.getElementAt(i).getId() == previouslySelectedId) {
                    selectionIndex = i;
                    break;
                }
            }
        }

        // Nếu không tìm thấy lựa chọn cũ, thử chọn currentFolder của controller
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

        // Nếu vẫn không có gì được chọn và danh sách không rỗng, chọn mục đầu tiên (thường là Root)
        if (selectionIndex == -1 && !folderListModel.isEmpty()) {
            selectionIndex = 0;
        }

        if (selectionIndex != -1) {
            folderList.setSelectedIndex(selectionIndex);
            // Không cần gọi controller.selectFolder() ở đây nữa,
            // vì listener sẽ được thêm lại và xử lý khi người dùng thực sự chọn.
            // Hoặc, nếu muốn cập nhật controller ngay:
            // Folder newlySelected = folderListModel.getElementAt(selectionIndex);
            // if (controller != null && newlySelected != null && newlySelected.getId() != 0) {
            //    controller.selectFolder(newlySelected);
            // }
        }

        // Thêm lại listener
        if (folderListSelectionHandler != null) {
            folderList.addListSelectionListener(folderListSelectionHandler);
        }
        System.out.println("[MainMenuScreen refreshFolderPanel] Hoàn tất làm mới danh sách thư mục. Mục được chọn (index): " + selectionIndex);
        // populateNoteTableModel(); // Gọi sau khi folder đã được chọn (listener sẽ làm điều này)
    }


    // ... (showFolderPopupMenu, createAddFolderButton, buildNotesPanel, createNoteTable, showAlarmDialog, createNoteControlPanel, addSearchFieldListener, populateNoteTableModel, handleNoteDoubleClick, showNotePopup, handleNoteDeletion, refresh, setupShortcuts giữ nguyên như phiên bản trước)
    private void showFolderPopupMenu(MouseEvent e) {
        int index = folderList.locationToIndex(e.getPoint());
        if (index < 0) return;

        folderList.setSelectedIndex(index);
        Folder folder = folderList.getSelectedValue();

        if (folder != null && !"Root".equalsIgnoreCase(folder.getName()) && folder.getId() != 0) { // Thêm kiểm tra folder.getId() != 0
            JPopupMenu popup = new JPopupMenu();
            JMenuItem renameItem = new JMenuItem("Đổi tên");
            renameItem.addActionListener(ev -> {
                String newName = JOptionPane.showInputDialog(mainFrame, "Nhập tên thư mục mới:", folder.getName());
                if (newName != null && !newName.trim().isEmpty() && controller != null) {
                    controller.renameFolder(folder, newName.trim());
                    refreshFolderPanel(); // Làm mới để cập nhật tên
                }
            });
            popup.add(renameItem);

            JCheckBoxMenuItem favoriteItem = new JCheckBoxMenuItem("Yêu thích", folder.isFavorite());
            favoriteItem.addActionListener(ev -> {
                if (controller != null) {
                    controller.setFolderFavorite(folder, !folder.isFavorite());
                    refreshFolderPanel(); // Làm mới để cập nhật trạng thái sao
                }
            });
            popup.add(favoriteItem);

            JMenuItem deleteItem = new JMenuItem("Xóa");
            deleteItem.addActionListener(ev -> {
                if (controller != null) {
                    controller.deleteFolder(folder); // Controller sẽ xử lý logic xóa và di chuyển notes
                    refreshFolderPanel();      // Làm mới danh sách folder
                    populateNoteTableModel();  // Làm mới danh sách note (vì currentFolder có thể đã thay đổi)
                }
            });
            popup.add(deleteItem);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private JButton createAddFolderButton() {
        JButton addFolderButton = new JButton(ADD_FOLDER_LABEL);
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
        JPanel notesPanel = new JPanel(new BorderLayout(5, 5));
        notesPanel.setBorder(BorderFactory.createTitledBorder("Ghi chú & Bản vẽ"));
        noteTable = createNoteTable();
        notesPanel.add(new JScrollPane(noteTable), BorderLayout.CENTER);
        notesPanel.add(createNoteControlPanel(), BorderLayout.NORTH);
        populateNoteTableModel();
        return notesPanel;
    }

    private JTable createNoteTable() {
        DefaultTableModel model = new DefaultTableModel(NOTE_COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        noteTable = new JTable(model);
        noteTable.setRowHeight(25);
        noteTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        noteTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        noteTable.getColumnModel().getColumn(1).setMaxWidth(60);
        noteTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        noteTable.getColumnModel().getColumn(3).setMaxWidth(60);
        noteTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        noteTable.getColumnModel().getColumn(4).setMinWidth(130);

        DefaultTableCellRenderer titleRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Giá trị 'value' giờ đây là đối tượng Note
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Note) { // Kiểm tra kiểu để an toàn
                    Note note = (Note) value;
                    String titleText = note.getTitle();
                    if (note.getNoteType() == Note.NoteType.DRAWING) {
                        titleText += " (DP)";
                    }
                    setText(titleText);
                } else {
                    setText(value != null ? value.toString() : "");
                }
                setHorizontalAlignment(JLabel.LEFT);
                return this;
            }
        };
        noteTable.getColumnModel().getColumn(0).setCellRenderer(titleRenderer);

        DefaultTableCellRenderer centerRendererAll = new DefaultTableCellRenderer();
        centerRendererAll.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < noteTable.getColumnModel().getColumnCount(); i++) {
            if (i != 2) {
                noteTable.getColumnModel().getColumn(i).setCellRenderer(centerRendererAll);
            }
        }
        DefaultTableCellRenderer missionRenderer = new DefaultTableCellRenderer();
        missionRenderer.setHorizontalAlignment(JLabel.LEFT);
        noteTable.getColumnModel().getColumn(2).setCellRenderer(missionRenderer);


        noteTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                label.setText("");
                label.setIcon(null);
                if (value instanceof Integer) {
                    int hour = (Integer) value;
                    if (hour >= 0 && hour < 24 && hourIcons != null && hourIcons[hour] != null) {
                        label.setIcon(hourIcons[hour]);
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
                if (e.getClickCount() == 2) {
                    handleNoteDoubleClick(noteTable);
                }
                int row = noteTable.rowAtPoint(e.getPoint());
                int col = noteTable.columnAtPoint(e.getPoint());

                if (row < 0 || filteredNotes == null || row >= filteredNotes.size()) return;
                Note selectedNote = filteredNotes.get(row);

                if (e.getClickCount() == 1 && col == 2 && selectedNote.isMission()) {
                    MissionDialog dialog = new MissionDialog(mainFrame);
                    if(mainFrame.getMouseEventDispatcher() != null) mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
                    dialog.setMission(selectedNote.getMissionContent());
                    dialog.setVisible(true);
                    if (dialog.isSaved()) {
                        String result = dialog.getResult();
                        if (controller != null) controller.updateMission(selectedNote, result);
                        populateNoteTableModel();
                    }
                } else if (e.getClickCount() == 1 && col == 3) {
                    showAlarmDialog(selectedNote);
                }
            }
            private void handleRightClick(MouseEvent e) {
                int row = noteTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < noteTable.getRowCount()) {
                    noteTable.setRowSelectionInterval(row, row);
                    if (filteredNotes != null && row < filteredNotes.size() && controller != null) {
                        showNotePopup(e, filteredNotes.get(row));
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) handleRightClick(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) handleRightClick(e);
            }
        });
        return noteTable;
    }

    private void showAlarmDialog(Note note) {
        if (note == null || controller == null) return;

        JDialog dialog = new JDialog(mainFrame, "Chi tiết Báo thức cho: " + note.getTitle(), true);
        if(mainFrame.getMouseEventDispatcher() != null) mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
        dialog.setSize(350, 350);
        dialog.setResizable(false);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Alarm currentAlarm = note.getAlarm();
        long existingAlarmId = (currentAlarm != null && currentAlarm.getId() > 0) ? currentAlarm.getId() : 0L;
        LocalDateTime initialDateTimeToShow = (currentAlarm != null && currentAlarm.getAlarmTime() != null) ?
                currentAlarm.getAlarmTime() :
                LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0);
        String initialTypeStr = (currentAlarm != null && currentAlarm.getRecurrencePattern() != null) ? currentAlarm.getRecurrencePattern().toUpperCase() : "ONCE";
        if(currentAlarm != null && !currentAlarm.isRecurring()) initialTypeStr = "ONCE";


        JLabel currentInfoLabel = new JLabel("Hiện tại: " + (currentAlarm != null ? currentAlarm.toString() : "Chưa đặt báo thức."));
        currentInfoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        dialog.add(currentInfoLabel, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        dialog.add(new JLabel("Loại báo thức:"), gbc);
        String[] alarmTypes = {"ONCE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        JComboBox<String> typeComboBox = new JComboBox<>(alarmTypes);
        typeComboBox.setSelectedItem(initialTypeStr);
        gbc.gridx = 1;
        dialog.add(typeComboBox, gbc);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel dateLabelComponent = new JLabel("Ngày (yyyy-MM-dd):");
        JTextField dateField = new JTextField(10);
        dateField.setText(initialDateTimeToShow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        datePanel.add(dateLabelComponent);
        datePanel.add(dateField);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        dialog.add(datePanel, gbc);

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
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
        };
        typeComboBox.addActionListener(e -> updatePanelsVisibility.run());
        updatePanelsVisibility.run();

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
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
        dialog.add(buttonsPanel, gbc);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(350, dialog.getHeight()));
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }


    private JPanel createNoteControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addNoteButton = new JButton(ADD_NOTE_LABEL);
        addNoteButton.addActionListener(e -> mainFrame.showAddNoteScreen());
        panel.add(addNoteButton);

        JButton addDrawPanelButton = new JButton(ADD_DRAW_PANEL_LABEL);
        addDrawPanelButton.addActionListener(e -> mainFrame.showNewDrawScreen());
        panel.add(addDrawPanelButton);

        try {
            ImageIcon scannerIcon = new ImageIcon(getClass().getResource("/images/Clara.jpg"));
            Image scaledIcon = scannerIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            JButton scannerButton = new JButton(new ImageIcon(scaledIcon));
            scannerButton.setToolTipText("Mở công cụ Scanner");
            scannerButton.addActionListener(e -> FloatingScannerTray.getInstance().setVisible(true));
            panel.add(scannerButton);
        } catch (Exception ex) {
            System.err.println("Không thể tải icon scanner: " + ex.getMessage());
        }

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
        panel.add(titleSearchField);

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
        panel.add(tagSearchField);

        JButton statsButton = new JButton(SHOW_STATS_LABEL);
        statsButton.addActionListener(e -> mainFrame.openCanvasPanel());
        panel.add(statsButton);

        JButton refreshButton = new JButton(REFRESH_LABEL);
        refreshButton.addActionListener(e -> refresh());
        panel.add(refreshButton);

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

        String titleQuery = (titleSearchField != null && !titleSearchField.getText().equals(TITLE_SEARCH_PLACEHOLDER)) ?
                titleSearchField.getText().trim().toLowerCase() : "";
        String tagQuery = (tagSearchField != null && !tagSearchField.getText().equals(TAG_SEARCH_PLACEHOLDER)) ?
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

        JMenuItem alarmItem = new JMenuItem("Đặt Báo thức");
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
                    .filter(f -> !"Root".equalsIgnoreCase(f.getName()) || (finalCurrentNoteFolder != null && !"Root".equalsIgnoreCase(finalCurrentNoteFolder.getName())))
                    .collect(Collectors.toList());

            if (finalCurrentNoteFolder != null && !"Root".equalsIgnoreCase(finalCurrentNoteFolder.getName())) {
                Folder rootF = controller.getFolderByName("Root").orElse(null);
                if (rootF != null && !targetFolders.stream().anyMatch(tf -> tf.getId() == rootF.getId())) {
                    targetFolders.add(0, rootF);
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
        refreshFolderPanel(); // Làm mới folder trước
        populateNoteTableModel(); // Sau đó làm mới note dựa trên folder đã chọn (hoặc mặc định)
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

