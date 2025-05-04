import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class CanvasPanel extends JPanel {
    private final NoteController controller;
    private final MainFrame mainFrame;
    private final JTextArea scriptArea;

    public CanvasPanel(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        scriptArea = new JTextArea();
        scriptArea.setText(getDefaultScript());
        add(new JScrollPane(scriptArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        buttonPanel.add(backButton);

        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> updateChart());
        buttonPanel.add(runButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private String getDefaultScript() {
        return "function drawChart() {\n" +
                "  const canvas = document.createElement('canvas');\n" +
                "  document.body.appendChild(canvas);\n" +
                "  const ctx = canvas.getContext('2d');\n" +
                "  canvas.width = 300;\n" +
                "  canvas.height = 200;\n" +
                "\n" +
                "  const folders = ['Work', 'Personal', 'Important', 'Root'];\n" +
                "  const counts = " + getFolderCounts() + ";\n" +
                "  const total = counts.reduce((a, b) => a + b, 0);\n" +
                "  let startAngle = 0;\n" +
                "\n" +
                "  ctx.fillStyle = 'lightgray';\n" +
                "  ctx.fillRect(0, 0, canvas.width, canvas.height);\n" +
                "\n" +
                "  for (let i = 0; i < folders.length; i++) {\n" +
                "    if (counts[i] > 0) {\n" +
                "      const angle = (counts[i] / total) * 2 * Math.PI;\n" +
                "      ctx.beginPath();\n" +
                "      ctx.moveTo(150, 100);\n" +
                "      ctx.arc(150, 100, 80, startAngle, startAngle + angle);\n" +
                "      ctx.fillStyle = `hsl(${i * 90}, 70%, 50%)`;\n" +
                "      ctx.fill();\n" +
                "      startAngle += angle;\n" +
                "\n" +
                "      const labelX = 150 + 100 * Math.cos(startAngle - angle / 2);\n" +
                "      const labelY = 100 + 100 * Math.sin(startAngle - angle / 2);\n" +
                "      ctx.fillStyle = 'black';\n" +
                "      ctx.font = '12px Arial';\n" +
                "      ctx.fillText(folders[i] + ' (' + counts[i] + ')', labelX, labelY);\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "drawChart();";
    }

    private String getFolderCounts() {
        Map<String, Integer> counts = controller.getNoteManager().getFolderNoteCounts();
        String[] folders = {"Work", "Personal", "Important", "Root"};
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < folders.length; i++) {
            sb.append(counts.getOrDefault(folders[i], 0));
            if (i < folders.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public void updateChart() {
        scriptArea.setText(getDefaultScript());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Note distribution by folder (click Run to update)", 10, 20);
    }
}