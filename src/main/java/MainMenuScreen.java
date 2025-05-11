import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
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
    private static final String TITLE_SEARCH_PLACEHOLDER = "Search by title...";
    private static final String TAG_SEARCH_PLACEHOLDER = "Search by tag (e.g., work)";

    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel folderPanel;
    private JTable noteTable;
    private JTextField titleSearchField;
    private JTextField tagSearchField;
    private JList<Folder> folderList;
    private List<Note> filteredNotes;

    public MainMenuScreen(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        initializeUI();
        setupShortcuts();
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

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    int column = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && column == 2) {
                        Note selectedNote = filteredNotes.get(row);
                        MissionDialog dialog = new MissionDialog(mainFrame);
                        dialog.setMission(selectedNote.getMissionContent());
                        dialog.setVisible(true);
                        String result = dialog.getResult();
                        if (result != null) {
                            selectedNote.setMissionContent(result);
                            selectedNote.setMission(!result.isEmpty());
                            populateNoteTableModel();
                        }
                    } else if (row >= 0) {
                        handleNoteDoubleClick(table);
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
            public void insertUpdate(javax.swing.event.DocumentEvent e) { populateNoteTableModel(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { populateNoteTableModel(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { populateNoteTableModel(); }
        });
    }

    private void populateNoteTableModel() {
        if (noteTable == null) return;
        DefaultTableModel model = (DefaultTableModel) noteTable.getModel();
        model.setRowCount(0);

        String titleQuery = titleSearchField.getText().equals(TITLE_SEARCH_PLACEHOLDER) ? "" : titleSearchField.getText().trim();
        String tagQuery = tagSearchField.getText().equals(TAG_SEARCH_PLACEHOLDER) ? "" : tagSearchField.getText().trim();

        filteredNotes = filterNotes(controller.getSortedNotes(), titleQuery, tagQuery);
        for (Note note : filteredNotes) {
            model.addRow(new Object[]{
                    note.getTitle(),
                    note.isFavorite() ? "★" : "",
                    note.getMissionContent().isEmpty() ? "" : "✔ " + note.getMissionContent(),
                    note.getAlarm() != null ? "⏰" : "",
                    note.getFormattedModificationDate()
            });
        }
    }

    private List<Note> filterNotes(List<Note> notes, String titleQuery, String tagQuery) {
        List<Note> filtered = notes;

        if (!titleQuery.isEmpty() && !tagQuery.isEmpty()) {
            Tag tag = new Tag(tagQuery);
            filtered = controller.getNoteManager().searchNotesByTag(tag).stream()
                    .filter(note -> note.getTitle().toLowerCase().contains(titleQuery.toLowerCase()))
                    .collect(Collectors.toList());
        } else if (!titleQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(note -> note.getTitle().toLowerCase().contains(titleQuery.toLowerCase()))
                    .collect(Collectors.toList());
        } else if (!tagQuery.isEmpty()) {
            Tag tag = new Tag(tagQuery);
            filtered = controller.getNoteManager().searchNotesByTag(tag);
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
                note.setMissionContent(result);
                note.setMission(!result.isEmpty());
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
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
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
                populateNoteTableModel();
                refreshFolderPanel();
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