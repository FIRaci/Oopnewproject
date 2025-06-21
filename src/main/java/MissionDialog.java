import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MissionDialog extends JDialog {
    private JTextArea missionArea;
    private String result;
    private boolean saved = false;

    public MissionDialog(Frame owner) {
        super(owner, "Chi Tiết Nhiệm Vụ", true);
        initializeUI();
    }


    public MissionDialog(Frame owner, String currentMission) {
        super(owner, "Sửa Nhiệm Vụ", true);
        initializeUI();
        missionArea.setText(currentMission);
    }


    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel instructionLabel = new JLabel("Nhập nội dung nhiệm vụ:");
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instructionLabel.setBorder(new EmptyBorder(0,0,5,0));
        add(instructionLabel, BorderLayout.NORTH);


        missionArea = new JTextArea(5, 20);
        missionArea.setLineWrap(true);
        missionArea.setWrapStyleWord(true);
        missionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        missionArea.setMargin(new Insets(5,5,5,5));

        JScrollPane scrollPane = new JScrollPane(missionArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton saveButton = new JButton("Lưu");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JButton cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 13));

        saveButton.addActionListener(e -> {
            result = missionArea.getText().trim();
            saved = true;
            dispose();
        });

        cancelButton.addActionListener(e -> {
            result = null;
            saved = false;
            dispose();
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);

        pack();
        setMinimumSize(new Dimension(380, getHeight()));
        setLocationRelativeTo(getOwner());
    }


    public void setMission(String mission) {
        missionArea.setText(mission);
        missionArea.setCaretPosition(0);
    }

    public String getResult() {
        return result;
    }

    public boolean isSaved() { // Expose the flag
        return saved;
    }
}
