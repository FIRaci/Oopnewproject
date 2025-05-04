import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NoteEditorScreen extends JFrame {
    private Note note;
    private JTextField titleField;
    private JTextArea contentArea;
    private JCheckBox favoriteCheck;

    public NoteEditorScreen(Note note) {
        this.note = note;
        setTitle("Edit Note - " + note.getTitle());
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initUI();
        setVisible(true);
    }

    private void initUI() {
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(10, 10));

        // Title
        titleField = new JTextField(note.getTitle());
        titleField.setFont(new Font("Arial", Font.BOLD, 18));
        cp.add(titleField, BorderLayout.NORTH);

        // Content
        contentArea = new JTextArea(note.getContent());
        contentArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(contentArea);
        cp.add(scroll, BorderLayout.CENTER);

        // South panel: Favorite + Save
        JPanel southPanel = new JPanel(new BorderLayout());

        favoriteCheck = new JCheckBox("Favorite", note.isFavorite());
        southPanel.add(favoriteCheck, BorderLayout.WEST);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveNote());
        southPanel.add(saveButton, BorderLayout.EAST);

        cp.add(southPanel, BorderLayout.SOUTH);
    }

    private void saveNote() {
        note.setTitle(titleField.getText().trim());
        note.setContent(contentArea.getText().trim());
        note.setFavorite(favoriteCheck.isSelected());
        JOptionPane.showMessageDialog(this, "Note saved successfully.");
        this.dispose(); // đóng cửa sổ sau khi lưu
    }
}
