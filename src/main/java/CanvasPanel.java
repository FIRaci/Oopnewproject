import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CanvasPanel extends JPanel {
    private final NoteController controller;
    private final MainFrame mainFrame;
    private Map<String, Integer> folderCounts;
    private Map<String, Integer> tagCounts;
    private JPanel chartPanel;
    private JPanel buttonPanel;

    public CanvasPanel(NoteController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        chartPanel = new JPanel(new GridLayout(2, 1));
        add(chartPanel, BorderLayout.CENTER);

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        buttonPanel.add(backButton);

        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> updateChart());
        buttonPanel.add(runButton);

        add(buttonPanel, BorderLayout.SOUTH);

        updateChart();
    }

    public void updateChart() {
        // Tính số note theo folder (loại trừ Root nếu không có note)
        folderCounts = new HashMap<>();
        Map<String, Integer> rawCounts = controller.getNoteManager().getFolderNoteCounts();
        for (Map.Entry<String, Integer> entry : rawCounts.entrySet()) {
            if (entry.getValue() > 0 && !entry.getKey().equals("Root")) {
                folderCounts.put(entry.getKey(), entry.getValue());
            }
        }

        // Tính số note theo tag
        tagCounts = new HashMap<>();
        for (Note note : controller.getNotes()) {
            for (Tag tag : note.getTags()) {
                tagCounts.put(tag.getName(), tagCounts.getOrDefault(tag.getName(), 0) + 1);
            }
        }

        // Vẽ lại biểu đồ
        chartPanel.removeAll();
        chartPanel.add(new PieChartPanel("Note Distribution by Folder", folderCounts));
        chartPanel.add(new PieChartPanel("Tag Usage Distribution", tagCounts));
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private class PieChartPanel extends JPanel {
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

            if (data.isEmpty()) {
                g2d.setColor(Color.BLACK);
                g2d.drawString("No data to display", 10, 20);
                return;
            }

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = Math.min(width, height) / 4;

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(0, 0, width, height);

            double total = data.values().stream().mapToInt(Integer::intValue).sum();
            double startAngle = 0;
            int colorIndex = 0;
            Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA};

            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                if (entry.getValue() > 0) {
                    double angle = (entry.getValue() / total) * 360;
                    g2d.setColor(colors[colorIndex % colors.length]);
                    g2d.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, (int) startAngle, (int) angle);

                    double labelAngle = Math.toRadians(startAngle + angle / 2);
                    int labelX = centerX + (int) (radius * 1.3 * Math.cos(labelAngle));
                    int labelY = centerY - (int) (radius * 1.3 * Math.sin(labelAngle));
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(entry.getKey() + " (" + entry.getValue() + ")", labelX, labelY);

                    startAngle += angle;
                    colorIndex++;
                }
            }

            g2d.setColor(Color.BLACK);
            g2d.drawString(title, 10, 20);
        }
    }
}