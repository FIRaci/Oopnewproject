import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NoteListScreen extends JFrame {
    private final List<Note> notes; // Danh sách ghi chú
    private final JPanel mainPanel; // Panel chính
    private final CardLayout cardLayout; // Layout chuyển đổi
    private JTextArea noteContent; // Khu vực chỉnh sửa nội dung ghi chú
    private Note currentlyEditedNote; // Ghi chú đang được chỉnh sửa

    public NoteListScreen(List<Note> notes) {
        this.notes = notes;

        // Cài đặt JFrame
        setTitle("My Notes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null);

        // Layout Card để chuyển đổi giữa các màn hình
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Tạo các giao diện
        JPanel noteListPanel = createNoteListPanel();
        JPanel noteEditorPanel = createNoteEditorPanel();

        // Thêm vào mainPanel
        mainPanel.add(noteListPanel, "NoteListScreen");
        mainPanel.add(noteEditorPanel, "NoteEditorScreen");

        add(mainPanel);
        setVisible(true);
    }

    private JPanel createNoteListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Danh sách ghi chú
        JPanel notesPanel = new JPanel();
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(notesPanel); // Panel cuộn
        panel.add(scrollPane, BorderLayout.CENTER);

        // Làm mới danh sách ghi chú
        refreshNoteList(notesPanel);

        // Tạo nút Add trông hiện đại hơn
        JButton addButton = new JButton("+ Add Note");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        addButton.setFocusPainted(false);
        addButton.setBackground(new Color(0, 122, 255));
        addButton.setForeground(Color.WHITE);
        addButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        addButton.addActionListener(e -> {
            Note newNote = new Note("Untitled Note", "", false);
            notes.add(newNote);
            refreshNoteList(notesPanel);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshNoteList(JPanel notesPanel) {
        notesPanel.removeAll();

        // Sắp xếp ghi chú: yêu thích ở đầu
        notes.sort((n1, n2) -> {
            if (n1.isFavorite() == n2.isFavorite()) {
                return n1.getCreationDate().compareTo(n2.getCreationDate());
            }
            return n1.isFavorite() ? -1 : 1;
        });

        for (Note note : notes) {
            JPanel notePanel = new JPanel();
            notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.Y_AXIS));
            notePanel.setBorder(BorderFactory.createTitledBorder(note.getTitle()));

            // Hiển thị ngày tạo
            JLabel dateLabel = new JLabel("Created on: " + note.getFormattedCreationDate());
            notePanel.add(dateLabel);

            // Tạo các nút action (Rename, Edit, Favorite, Delete)
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Nút Rename
            JButton renameButton = createStyledButton("Rename", null);
            renameButton.addActionListener(e -> {
                String newTitle = JOptionPane.showInputDialog(this, "Rename Note", note.getTitle());
                if (newTitle != null && !newTitle.isBlank()) {
                    note.setTitle(newTitle.trim());
                    refreshNoteList(notesPanel);
                }
            });

            // Nút Edit
            JButton editButton = createStyledButton("Edit", null);
            editButton.addActionListener(e -> {
                currentlyEditedNote = note;
                noteContent.setText(note.getContent());
                cardLayout.show(mainPanel, "NoteEditorScreen");
            });

            // Nút Favorite
            JButton favButton = createStyledButton(
                    note.isFavorite() ? "Unfavorite" : "Favorite",
                    null
            );
            favButton.addActionListener(e -> {
                note.setFavorite(!note.isFavorite());
                refreshNoteList(notesPanel);
            });

            // Nút Delete
            JButton deleteButton = createStyledButton("Delete", null);
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to delete this note?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    notes.remove(note);
                    refreshNoteList(notesPanel);
                }
            });

            buttonPanel.add(renameButton);
            buttonPanel.add(editButton);
            buttonPanel.add(favButton);
            buttonPanel.add(deleteButton);
            notePanel.add(buttonPanel);

            notesPanel.add(notePanel);
            notesPanel.add(Box.createVerticalStrut(10));
        }

        notesPanel.revalidate();
        notesPanel.repaint();
    }

    private JPanel createNoteEditorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Khu vực chỉnh sửa nội dung ghi chú
        noteContent = new JTextArea();
        panel.add(new JScrollPane(noteContent), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            if (currentlyEditedNote != null) {
                currentlyEditedNote.setContent(noteContent.getText());
                JOptionPane.showMessageDialog(this, "Note saved!");
                cardLayout.show(mainPanel, "NoteListScreen");
            }
        });

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "NoteListScreen"));

        buttonPanel.add(saveButton);
        buttonPanel.add(backButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createStyledButton(String text, Icon icon) {
        JButton button = new JButton(text, icon);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBackground(new Color(240, 240, 240));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setIconTextGap(8);
        return button;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        List<Note> sampleNotes = new ArrayList<>();
        sampleNotes.add(new Note("Sample Note 1", "Content of Note 1", false));
        sampleNotes.add(new Note("Sample Note 2", "Content of Note 2", true));

        SwingUtilities.invokeLater(() -> new NoteListScreen(sampleNotes));
    }
}
