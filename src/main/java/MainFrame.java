import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class MainFrame extends JFrame {
    private final NoteController controller;
    private MainMenuScreen mainMenuScreen;
    private MissionScreen missionScreen;
    private NoteEditorScreen noteEditorScreen;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private boolean isDarkTheme = false;

    public MainFrame(NoteController controller) {
        this.controller = controller;
        initializeUI();
        setupShortcuts();
    }

    private void initializeUI() {
        setTitle("XiNoClo - Note App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);

        JPanel tabPanel = new JPanel(new BorderLayout());

        JButton notesButton = new JButton("📝 Notes");
        notesButton.setFocusPainted(false);
        notesButton.addActionListener(e -> showScreenWithTransition("Notes"));

        JButton missionsButton = new JButton("🎯 Missions");
        missionsButton.setFocusPainted(false);
        missionsButton.addActionListener(e -> showScreenWithTransition("Missions"));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(notesButton);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(missionsButton);

        tabPanel.add(leftPanel, BorderLayout.WEST);
        tabPanel.add(rightPanel, BorderLayout.EAST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        mainMenuScreen = new MainMenuScreen(controller, this);
        missionScreen = new MissionScreen(controller, this);

        contentPanel.add(mainMenuScreen, "Notes");
        contentPanel.add(missionScreen, "Missions");

        add(tabPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            JOptionPane.showMessageDialog(this, "Failed to set default theme!", "Error", JOptionPane.ERROR_MESSAGE);
        }

        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/5048557ff0fc302e6cc0d419e2fc0219.jpg")));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load icon: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showScreenWithTransition(String screenName) {
        float[] opacity = {0f};
        Timer timer = new Timer(15, e -> {
            opacity[0] += 0.05f;
            if (opacity[0] >= 1f) {
                opacity[0] = 1f;
                ((Timer) e.getSource()).stop();
            }
            contentPanel.setOpaque(false);
            contentPanel.repaint();
        });
        cardLayout.show(contentPanel, screenName);
        timer.start();
    }

    public void showAddNoteScreen() {
        noteEditorScreen = new NoteEditorScreen(this, controller, null);
        contentPanel.add(noteEditorScreen, "NoteEditor");
        showScreenWithTransition("NoteEditor");
    }

    public void showNoteDetailScreen(Note note) {
        noteEditorScreen = new NoteEditorScreen(this, controller, note);
        noteEditorScreen.setNote(note);
        contentPanel.add(noteEditorScreen, "NoteEditor");
        showScreenWithTransition("NoteEditor");
    }

    public void showMainMenuScreen() {
        showScreenWithTransition("Notes");
        mainMenuScreen.refresh();
    }

    public void openCanvasPanel() {
        // Tạo dữ liệu cho biểu đồ: số lượng note theo folder
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Folder folder : controller.getFolders()) {
            long noteCount = controller.getSortedNotes().stream()
                    .filter(note -> note.getFolder() != null && note.getFolder().getName().equals(folder.getName()))
                    .count();
            dataset.addValue(noteCount, "Notes", folder.getName());
        }

        // Tạo biểu đồ cột
        JFreeChart chart = ChartFactory.createBarChart(
                "Note Count by Folder",
                "Folder",
                "Number of Notes",
                dataset
        );

        // Tạo panel cho biểu đồ
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(600, 400));

        // Hiển thị trong dialog
        JDialog dialog = new JDialog(this, "Statistics", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(chartPanel, BorderLayout.CENTER);
        dialog.setSize(650, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void setupShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK), "exit");
        actionMap.put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Are you sure you want to exit?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), "toggleTheme");
        actionMap.put("toggleTheme", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    if (isDarkTheme) {
                        UIManager.setLookAndFeel(new FlatLightLaf());
                    } else {
                        UIManager.setLookAndFeel(new FlatDarkLaf());
                    }
                    isDarkTheme = !isDarkTheme;
                    SwingUtilities.updateComponentTreeUI(MainFrame.this);
                } catch (UnsupportedLookAndFeelException ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Failed to change theme!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK), "showNotes");
        actionMap.put("showNotes", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showScreenWithTransition("Notes");
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_DOWN_MASK), "showMissions");
        actionMap.put("showMissions", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showScreenWithTransition("Missions");
            }
        });
    }
}