import javax.swing.*;
import java.awt.*;

public class MissionDialog extends JDialog {
    private JTextArea missionArea;
    private String result;

    public MissionDialog(Frame owner) {
        super(owner, "Mission", true);
        setLayout(new BorderLayout(10, 10));
        setSize(400, 250);
        setLocationRelativeTo(owner);

        missionArea = new JTextArea();
        missionArea.setLineWrap(true);
        missionArea.setWrapStyleWord(true);
        missionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        add(new JScrollPane(missionArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("✅ Save");
        JButton cancelButton = new JButton("❌ Cancel");

        saveButton.addActionListener(e -> {
            result = missionArea.getText().trim();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

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

    public boolean isSaved() {
        return result != null;
    }
}
