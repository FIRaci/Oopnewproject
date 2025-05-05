import javax.swing.*;
import java.awt.*;

public class MissionDialog extends JDialog {
    private String result;
    private JTextArea missionArea;

    public MissionDialog(Frame owner) {
        super(owner, "Edit Mission", true);
        setLayout(new BorderLayout(10, 10));
        setSize(300, 200);
        setLocationRelativeTo(owner);

        missionArea = new JTextArea();
        missionArea.setLineWrap(true);
        missionArea.setWrapStyleWord(true);
        add(new JScrollPane(missionArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            result = missionArea.getText().trim();
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
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