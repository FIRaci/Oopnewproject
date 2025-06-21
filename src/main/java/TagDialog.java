import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TagDialog extends JDialog {
    private Tag result;
    private JTextField tagField;

    public TagDialog(Frame owner) {
        super(owner, "Thêm Tag Mới", true);
        initializeUI();
    }

    public TagDialog(Frame owner, Tag tagToEdit) {
        super(owner, "Sửa Tag", true);
        initializeUI();
        if (tagToEdit != null) {
            tagField.setText(tagToEdit.getName());
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10,10));
        getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel tagLabel = new JLabel("Tên Tag:");
        tagLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        contentPanel.add(tagLabel, gbc);

        tagField = new JTextField(20);
        tagField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        contentPanel.add(tagField, gbc);

        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JButton cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 13));

        okButton.addActionListener(e -> {
            String tagName = tagField.getText().trim();
            if (!tagName.isEmpty()) {
                 result = new Tag(tagName);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Tên tag không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);

        pack();
        setMinimumSize(new Dimension(300, getHeight()));
        setLocationRelativeTo(getOwner());
    }

    public Tag getResult() {
        return result;
    }
}
