// File: TagDialog.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TagDialog extends JDialog {
    private Tag result;
    private JTextField tagField;

    // Removed static UIManager block, theme should be handled by ThemeManager

    public TagDialog(Frame owner) {
        super(owner, "Thêm Tag Mới", true); // Title can be more dynamic if used for editing too
        initializeUI();
    }

    // Optional: Constructor for editing an existing tag
    public TagDialog(Frame owner, Tag tagToEdit) {
        super(owner, "Sửa Tag", true);
        initializeUI();
        if (tagToEdit != null) {
            tagField.setText(tagToEdit.getName());
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10,10)); // Main layout with gaps
        getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding for the dialog
        // setSize(350, 180); // Let pack() determine size initially
        // setMinimumSize(new Dimension(300, 160)); // Set minimum after packing

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding around components
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel tagLabel = new JLabel("Tên Tag:");
        tagLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Consistent font
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0; // Label doesn't expand
        contentPanel.add(tagLabel, gbc);

        tagField = new JTextField(20); // Increased preferred columns
        tagField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1; // Field expands
        contentPanel.add(tagField, gbc);

        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        // buttonPanel.setBorder(new EmptyBorder(10,0,0,0)); // Top padding for buttons
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 13)); // Consistent font
        JButton cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 13));

        okButton.addActionListener(e -> {
            String tagName = tagField.getText().trim();
            if (!tagName.isEmpty()) {
                // If editing, result should be the existing tag with updated name
                // For simplicity, this dialog currently only creates new Tag objects.
                // If editing is needed, the logic to update an existing Tag object would be here.
                result = new Tag(tagName);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Tên tag không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            result = null; // Ensure result is null on cancel
            dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);

        pack(); // Pack after all components are added
        setMinimumSize(new Dimension(300, getHeight())); // Set minimum width, height from pack
        setLocationRelativeTo(getOwner()); // Center after packing
    }

    public Tag getResult() {
        return result;
    }
}
