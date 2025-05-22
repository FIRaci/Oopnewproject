// File: MissionDialog.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MissionDialog extends JDialog {
    private JTextArea missionArea;
    private String result;
    private boolean saved = false; // Flag to indicate if save was pressed

    public MissionDialog(Frame owner) {
        super(owner, "Chi Tiết Nhiệm Vụ", true); // Title can be more dynamic
        initializeUI();
    }

    // Optional: Constructor for editing, could take current mission text
    public MissionDialog(Frame owner, String currentMission) {
        super(owner, "Sửa Nhiệm Vụ", true);
        initializeUI();
        missionArea.setText(currentMission);
    }


    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding for the dialog
        // setSize(400, 300); // Let pack() determine size initially
        // setMinimumSize(new Dimension(350, 250));

        JLabel instructionLabel = new JLabel("Nhập nội dung nhiệm vụ:");
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instructionLabel.setBorder(new EmptyBorder(0,0,5,0)); // Bottom margin for label
        add(instructionLabel, BorderLayout.NORTH);


        missionArea = new JTextArea(5, 20); // Rows, Columns for preferred size hint
        missionArea.setLineWrap(true);
        missionArea.setWrapStyleWord(true);
        missionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        missionArea.setMargin(new Insets(5,5,5,5)); // Padding inside text area

        JScrollPane scrollPane = new JScrollPane(missionArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        // buttonPanel.setBorder(new EmptyBorder(10,0,0,0)); // Top padding for buttons
        JButton saveButton = new JButton("Lưu"); // Changed from icon to text for consistency
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JButton cancelButton = new JButton("Hủy"); // Changed from icon to text
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 13));

        saveButton.addActionListener(e -> {
            result = missionArea.getText().trim();
            saved = true; // Set flag
            dispose();
        });

        cancelButton.addActionListener(e -> {
            result = null; // Ensure result is null on cancel
            saved = false; // Set flag
            dispose();
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);

        pack(); // Pack after all components are added
        setMinimumSize(new Dimension(380, getHeight())); // Set minimum width, height from pack
        setLocationRelativeTo(getOwner()); // Center after packing
    }


    public void setMission(String mission) {
        missionArea.setText(mission);
        missionArea.setCaretPosition(0); // Move cursor to start
    }

    public String getResult() {
        return result;
    }

    public boolean isSaved() { // Expose the flag
        return saved;
    }
}
