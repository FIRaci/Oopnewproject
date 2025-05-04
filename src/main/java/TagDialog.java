import javax.swing.*;
import java.awt.*;

public class TagDialog extends JDialog {
    private Tag result;
    private JTextField tagField;

    public TagDialog(Frame owner) {
        super(owner, "Add Tag", true);
        setLayout(new BorderLayout(10, 10));
        setSize(300, 150);
        setLocationRelativeTo(owner);

        tagField = new JTextField(20);
        add(tagField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            String tagName = tagField.getText().trim();
            if (!tagName.isEmpty()) {
                result = new Tag(tagName);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Tag cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public Tag getResult() {
        return result;
    }
}