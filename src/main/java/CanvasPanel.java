import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CanvasPanel extends JPanel {
    private final NoteController controller;
    private final MainFrame mainFrame;
    private JPanel chartPanel;

    public CanvasPanel(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        chartPanel = new JPanel(new GridLayout(1, 2));
        updateChart();

        JButton backButton = new JButton("🔙 Back");
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(backButton);

        add(chartPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void updateChart() {
        chartPanel.removeAll();

        // Example Data - Replace with actual data
        Map<String, Integer> folderCounts = new HashMap<>();
        folderCounts.put("Work", 5);
        folderCounts.put("Personal", 3);
        folderCounts.put("Important", 2);

        Map<String, Integer> tagCounts = new HashMap<>();
        tagCounts.put("Urgent", 4);
        tagCounts.put("Work", 3);
        tagCounts.put("Personal", 2);

        chartPanel.add(new PieChartPanel("Note Distribution by Folder", folderCounts));
        chartPanel.add(new PieChartPanel("Tag Usage Distribution", tagCounts));

        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private static class PieChartPanel extends JPanel {
        private final String title;
        private final Map<String, Integer> data;

        public PieChartPanel(String title, Map<String, Integer> data) {
            this.title = title;
            this.data = data;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = Math.min(width, height) / 3;

            double total = data.values().stream().mapToInt(Integer::intValue).sum();
            double startAngle = 0;

            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                double angle = (entry.getValue() / total) * 360;
                g2d.setColor(new Color((int) (Math.random() * 0x1000000)));
                g2d.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, (int) startAngle, (int) angle);

                startAngle += angle;
            }

            g2d.setColor(Color.BLACK);
            g2d.drawString(title, 10, 20);
        }
    }
}
