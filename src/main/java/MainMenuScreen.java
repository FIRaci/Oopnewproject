import javax.swing.*;
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
    private ImageIcon[] hourIcons;

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
                    System.err.println("Cannot find resource: /icons/hour_" + i + ".jpg");
                    hourIcons[i] = createDefaultIcon("H" + i);
                }
            } catch (Exception e) {
                System.err.println("Could not load icon hour_" + i + ".jpg: " + e.getMessage());
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

        folderList = new JList<>(new DefaultListModel<>());
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

        folderList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Folder selectedFolder = folderList.getSelectedValue();
                controller.selectFolder(selectedFolder);
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
        if (index < 0) return;

        folderList.setSelectedIndex(index);
        Folder folder = folderList.getSelectedValue();

        if (folder != null && !"Root".equalsIgnoreCase(folder.getName())) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem renameItem = new JMenuItem("Rename");
            renameItem.addActionListener(ev -> {
                String newName = JOptionPane.showInputDialog(mainFrame, "Enter new folder name:", folder.getName());
                if (newName != null && !newName.trim().isEmpty()) {
                    controller.renameFolder(folder, newName.trim());
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
                controller.deleteFolder(folder);
                refreshFolderPanel();
                populateNoteTableModel();
            });
            popup.add(deleteItem);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private JButton createAddFolderButton() {
        JButton addFolderButton = new JButton(ADD_FOLDER_LABEL);
        addFolderButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(mainFrame, "Enter folder name:");
            if (name != null && !name.trim().isEmpty()) {
                controller.addNewFolder(name.trim());
                refreshFolderPanel();
            }
        });
        return addFolderButton;
    }

    private JPanel buildNotesPanel() {
        JPanel notesPanel = new JPanel(new BorderLayout(5, 5));
        notesPanel.setBorder(BorderFactory.createTitledBorder("Notes"));
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

        DefaultTableCellRenderer centerRendererAll = new DefaultTableCellRenderer();
        centerRendererAll.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < noteTable.getColumnModel().getColumnCount(); i++) {
            noteTable.getColumnModel().getColumn(i).setCellRenderer(centerRendererAll);
        }
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        noteTable.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);

        noteTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                label.setText("");
                label.setIcon(null);
                if (value instanceof Integer) {
                    int hour = (Integer) value;
                    if (hour >= 0 && hour < 24 && hourIcons[hour] != null) {
                        label.setIcon(hourIcons[hour]);
                        label.setHorizontalAlignment(JLabel.CENTER);
                    } else {
                        label.setText("-");
                        label.setHorizontalAlignment(JLabel.CENTER);
                    }
                } else {
                    label.setText("-");
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                return label;
            }
        });

        noteTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = noteTable.rowAtPoint(e.getPoint());
                int col = noteTable.columnAtPoint(e.getPoint());

                if (row < 0 || filteredNotes == null || row >= filteredNotes.size()) return;
                Note selectedNote = filteredNotes.get(row);

                if (e.getClickCount() == 2) {
                    if (col == 2) {
                        MissionDialog dialog = new MissionDialog(mainFrame);
                        mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
                        dialog.setMission(selectedNote.getMissionContent());
                        dialog.setVisible(true);
                        String result = dialog.getResult();
                        if (result != null) {
                            controller.updateMission(selectedNote, result);
                            populateNoteTableModel();
                        }
                    } else if (col == 3) {
                        showAlarmDialog(selectedNote);
                    } else {
                        handleNoteDoubleClick(noteTable);
                    }
                }
            }

            private void handleRightClick(MouseEvent e) {
                int row = noteTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < noteTable.getRowCount()) {
                    noteTable.setRowSelectionInterval(row, row);
                    if (filteredNotes != null && row < filteredNotes.size()) {
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
        if (note == null) return;

        JDialog dialog = new JDialog(mainFrame, "Alarm Details for: " + note.getTitle(), true);
        mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
        dialog.setSize(300, 320);
        dialog.setResizable(false);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Alarm currentAlarm = note.getAlarm();
        long existingAlarmId = (currentAlarm != null && currentAlarm.getId() > 0) ? currentAlarm.getId() : 0L;

        LocalDateTime initialDateTimeToShow = (currentAlarm != null) ? currentAlarm.getAlarmTime() : LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0);
        String initialTypeStr = (currentAlarm != null) ? currentAlarm.getFrequency().toUpperCase() : "ONCE";

        JLabel currentInfoLabel = new JLabel("Current: " + (currentAlarm != null ? currentAlarm.toString() : "No alarm set."));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        dialog.add(currentInfoLabel, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        dialog.add(new JLabel("Alarm Type:"), gbc);
        String[] alarmTypes = {"ONCE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        JComboBox<String> typeComboBox = new JComboBox<>(alarmTypes);
        typeComboBox.setSelectedItem(initialTypeStr);
        gbc.gridx = 1;
        dialog.add(typeComboBox, gbc);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel dateLabelComponent = new JLabel("Date (yyyy-MM-dd):");
        JTextField dateField = new JTextField(10);
        dateField.setText(initialDateTimeToShow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        datePanel.add(dateLabelComponent);
        datePanel.add(dateField);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        dialog.add(datePanel, gbc);

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        timePanel.add(new JLabel("Time (HH:mm):"));
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
            dialog.setSize(Math.max(dialog.getWidth(), 300), Math.max(dialog.getHeight(), 320));
        };
        typeComboBox.addActionListener(e -> updatePanelsVisibility.run());
        updatePanelsVisibility.run();

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton updateButton = new JButton(existingAlarmId > 0 ? "Update Alarm" : "Set Alarm");
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
                        JOptionPane.showMessageDialog(dialog, "Alarm time for 'ONCE' type must be in the future.", "Warning", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } else {
                    LocalDate baseDateForRecurring = (currentAlarm != null && currentAlarm.isRecurring()) ?
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
                JOptionPane.showMessageDialog(dialog, "Invalid date format! Please use yyyy-MM-dd.", "Date Format Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        JButton deleteButton = new JButton("Delete Alarm");
        deleteButton.setEnabled(existingAlarmId > 0);
        deleteButton.addActionListener(e -> {
            if (existingAlarmId > 0) {
                controller.setAlarm(note, null);
                populateNoteTableModel();
            }
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonsPanel.add(updateButton);
        if (existingAlarmId > 0) buttonsPanel.add(deleteButton);
        buttonsPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(buttonsPanel, gbc);

        dialog.pack();
        dialog.setSize(Math.max(dialog.getWidth(), 300), Math.max(dialog.getHeight(), 320));
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    private JPanel createNoteControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

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
        if (noteTable == null) return;
        DefaultTableModel model = (DefaultTableModel) noteTable.getModel();
        model.setRowCount(0);

        String titleQuery = titleSearchField.getText().equals(TITLE_SEARCH_PLACEHOLDER) ? "" : titleSearchField.getText().trim().toLowerCase();
        String tagQuery = tagSearchField.getText().equals(TAG_SEARCH_PLACEHOLDER) ? "" : tagSearchField.getText().trim().toLowerCase();

        List<Note> notesToDisplay;
        Folder currentSelectedFolder = controller.getCurrentFolder();

        if (currentSelectedFolder != null && "Root".equalsIgnoreCase(currentSelectedFolder.getName())) {
            notesToDisplay = controller.getNotes();
        } else {
            notesToDisplay = controller.getSortedNotes();
        }

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
                        missionDisplay += " (Done)";
                    }
                }
                model.addRow(new Object[]{
                        note.getTitle(),
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
            mainFrame.showNoteDetailScreen(selectedNote);
        }
    }

    private void showNotePopup(MouseEvent e, Note note) {
        if (note == null) return;
        JPopupMenu popup = new JPopupMenu();

        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(ev -> {
            String newTitle = JOptionPane.showInputDialog(mainFrame, "Enter new title:", note.getTitle());
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                controller.renameNote(note, newTitle.trim());
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

        JCheckBoxMenuItem missionItemOriginal = new JCheckBoxMenuItem("Mission", note.isMission());
        missionItemOriginal.addActionListener(ev -> {
            MissionDialog dialog = new MissionDialog(mainFrame);
            mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(dialog);
            dialog.setMission(note.getMissionContent());
            dialog.setVisible(true);
            String result = dialog.getResult();
            if (result != null) {
                controller.updateMission(note, result.trim());
            }
            populateNoteTableModel();
        });
        popup.add(missionItemOriginal);

        JMenuItem alarmItem = new JMenuItem("Set Alarm");
        alarmItem.addActionListener(ev -> {
            AlarmDialog externalAlarmDialog = new AlarmDialog(mainFrame, note.getAlarm());
            mainFrame.getMouseEventDispatcher().addMouseMotionListenerToWindow(externalAlarmDialog);
            externalAlarmDialog.setVisible(true);
            if (externalAlarmDialog.isOkPressed()) {
                Alarm resultAlarm = externalAlarmDialog.getResult();
                controller.setAlarm(note, resultAlarm);
                populateNoteTableModel();
            }
        });
        popup.add(alarmItem);

        JMenuItem moveItem = new JMenuItem("Move");
        moveItem.addActionListener(ev -> {
            List<Folder> allFolders = controller.getFolders();
            Folder currentNoteFolder = null;
            if (note.getFolderId() > 0) {
                for (Folder f : allFolders) if (f.getId() == note.getFolderId()) currentNoteFolder = f;
            }
            final Folder finalCurrentNoteFolder = currentNoteFolder;
            List<Folder> targetFolders = allFolders.stream()
                    .filter(f -> finalCurrentNoteFolder == null || f.getId() != finalCurrentNoteFolder.getId())
                    .collect(Collectors.toList());
            if (targetFolders.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "No other folders to move to.", "Move Note", JOptionPane.INFORMATION_MESSAGE);
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
            int result = JOptionPane.showConfirmDialog(mainFrame, folderCombo, "Move to Folder", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                Folder selectedFolder = (Folder) folderCombo.getSelectedItem();
                if (selectedFolder != null) {
                    controller.moveNoteToFolder(note, selectedFolder);
                    populateNoteTableModel();
                }
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
        if (folderList == null) return;
        DefaultListModel<Folder> model = (DefaultListModel<Folder>) folderList.getModel();
        Folder previouslySelectedFolder = folderList.getSelectedValue();

        model.clear();
        List<Folder> folders = controller.getFolders();

        Folder rootFolder = folders.stream()
                .filter(f -> "Root".equalsIgnoreCase(f.getName()))
                .findFirst().orElse(null);
        if (rootFolder != null) {
            model.addElement(rootFolder);
        }
        List<Folder> otherFolders = folders.stream()
                .filter(f -> !"Root".equalsIgnoreCase(f.getName()))
                .sorted(Comparator.comparing(Folder::isFavorite, Comparator.reverseOrder())
                        .thenComparing(Folder::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        for (Folder folder : otherFolders) {
            model.addElement(folder);
        }

        if (previouslySelectedFolder != null) {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).getId() == previouslySelectedFolder.getId()) {
                    folderList.setSelectedIndex(i);
                    break;
                }
            }
        } else if (controller.getCurrentFolder() != null && !model.isEmpty()) {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).getId() == controller.getCurrentFolder().getId()) {
                    folderList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "addNote");
        actionMap.put("addNote", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                mainFrame.showAddNoteScreen();
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
                String name = JOptionPane.showInputDialog(mainFrame, "Enter folder name:");
                if (name != null && !name.trim().isEmpty()) {
                    controller.addNewFolder(name.trim());
                    refreshFolderPanel();
                }
            }
        });
    }
}