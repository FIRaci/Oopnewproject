import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class MainMenuScreen extends JPanel {
    private static final String[] NOTE_COLUMNS = {"Title", "Favorite", "Mission", "Alarm", "Modified"};
    private static final String FOLDERS_TITLE = "Folders";
    private static final String NOTES_TITLE = "Notes";
    private static final String ADD_FOLDER_LABEL = "Add Folder";
    private static final String ADD_NOTE_LABEL = "Add Note";
    private static final String SHOW_STATS_LABEL = "Show Stats";

    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel folderPanel;
    private JTable noteTable;
    private JTextField searchField;
    private JList<Folder> folderList;

    public MainMenuScreen(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        initializeUI();
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
        folderPanel.add(new JLabel(FOLDERS_TITLE));

        folderList = new JList<>(new DefaultListModel<>());
        folderList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Folder) {
                    setText(((Folder) value).getName() + (isSelected ? " (Selected)" : ""));
                }
                return c;
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
        notesPanel.setBorder(BorderFactory.createTitledBorder(NOTES_TITLE));

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
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Title
        table.getColumnModel().getColumn(1).setMaxWidth(60); // Favorite
        table.getColumnModel().getColumn(2).setMaxWidth(60); // Mission
        table.getColumnModel().getColumn(3).setMaxWidth(60); // Alarm
        table.getColumnModel().getColumn(4).setPreferredWidth(180); // Modified

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) handleNoteDoubleClick(table);
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

        searchField = new JTextField(15);
        searchField.setToolTipText("Search by title or #tagname");
        addSearchFieldListener();
        panel.add(searchField);

        JButton statsButton = new JButton(SHOW_STATS_LABEL);
        statsButton.addActionListener(e -> mainFrame.openCanvasPanel());
        panel.add(statsButton);

        return panel;
    }

    private void addSearchFieldListener() {
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

        List<Note> notes = filterNotes(controller.getSortedNotes(), searchField.getText().trim());
        for (Note note : notes) {
            model.addRow(new Object[]{
                    note.getTitle(),
                    note.isFavorite() ? "★" : "",
                    note.isMission() ? "✔" : "",
                    note.getAlarm() != null ? "⏰" : "",
                    note.getFormattedModificationDate()
            });
        }
    }

    private List<Note> filterNotes(List<Note> notes, String query) {
        if (query.isEmpty()) return notes;

        if (query.startsWith("#")) {
            Tag tag = new Tag(query.substring(1));
            return controller.getNoteManager().searchNotesByTag(tag);
        }
        return controller.searchNotes(query);
    }

    private void handleNoteDoubleClick(JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0) {
            Note selectedNote = controller.getSortedNotes().get(row);
            mainFrame.showNoteDetailScreen(selectedNote);
        }
    }

    private void showPopupMenu(MouseEvent e) {
        int row = noteTable.rowAtPoint(e.getPoint());
        if (row >= 0 && row < noteTable.getRowCount()) {
            noteTable.setRowSelectionInterval(row, row);
            showNotePopup(e, controller.getSortedNotes().get(row));
        }
    }

    private void showNotePopup(MouseEvent e, Note note) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(ev -> handleNoteDeletion(note));
        popup.add(deleteItem);

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
            controller.setNoteMission(note, !note.isMission());
            populateNoteTableModel();
        });
        popup.add(missionItem);

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

    private void refreshFolderPanel() {
        DefaultListModel<Folder> model = (DefaultListModel<Folder>) folderList.getModel();
        model.clear();
        for (Folder folder : controller.getFolders()) {
            model.addElement(folder);
        }
        folderList.repaint();
    }
}