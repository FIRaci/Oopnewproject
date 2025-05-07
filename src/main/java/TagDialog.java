import javax.swing.*;
import java.awt.*;

public class TagDialog extends JDialog {
    private Tag result;
    private JTextField tagField;

    public TagDialog(Frame owner) {
        super(owner, "Add Tag", true);
        setLayout(new GridBagLayout());
        setSize(300, 150);
        setLocationRelativeTo(owner);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tag input
        JLabel tagLabel = new JLabel("Tag Name:");
        tagLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(tagLabel, gbc);

        tagField = new JTextField(15);
        tagField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(tagField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        okButton.setBackground(new Color(0, 122, 255));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.addActionListener(e -> {
            String tagName = tagField.getText().trim();
            if (!tagName.isEmpty()) {
                result = new Tag(tagName);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Tag name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);
    }

    public Tag getResult() {
        return result;
    }
}