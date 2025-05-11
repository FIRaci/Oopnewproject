import javax.swing.*;
import java.awt.*;

public class MissionDialog extends JDialog {
    private JTextArea missionArea;
    private String result;

    public MissionDialog(Frame owner) {
        super(owner, "Mission", true);
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(400, 300);
        setLocationRelativeTo(getOwner());

        missionArea = new JTextArea();
        missionArea.setLineWrap(true);
        missionArea.setWrapStyleWord(true);
        add(new JScrollPane(missionArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            result = missionArea.getText().trim();
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setMission(String mission) {
        missionArea.setText(mission);
    }

    public String getResult() {
        return result;
    }
}