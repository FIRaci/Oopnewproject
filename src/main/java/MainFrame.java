import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private MainMenuScreen mainMenuScreen;
    private NoteEditorScreen noteEditorScreen;
    private CanvasPanel canvasPanel;
    private final NoteController noteController;
    private final AlarmController alarmController;

    public MainFrame(NoteController noteController) {
        this.noteController = noteController;
        this.alarmController = new AlarmController(noteController.getNoteManager(), this);

        // Apply theme
        applyTheme(noteController.getCurrentTheme());

        // Setup JFrame
        setTitle("My Notes");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Window close listener
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                alarmController.stop();
                System.exit(0);
            }
        });

        // Initialize CardLayout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Initialize screens
        mainMenuScreen = new MainMenuScreen(noteController, this);
        noteEditorScreen = new NoteEditorScreen(this, noteController, new Note("New Note", "", false));
        canvasPanel = new CanvasPanel(noteController, this);

        // Add screens to CardLayout
        mainPanel.add(mainMenuScreen, "MainMenu");
        mainPanel.add(noteEditorScreen, "NoteEditor");
        mainPanel.add(canvasPanel, "CanvasPanel");

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem switchThemeItem = new JMenuItem("Switch Theme");
        switchThemeItem.addActionListener(e -> {
            String newTheme = noteController.getCurrentTheme().equalsIgnoreCase("Dark") ? "Light" : "Dark";
            noteController.changeTheme(newTheme);
            applyTheme(newTheme);
        });
        settingsMenu.add(switchThemeItem);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);

        // Add mainPanel to frame
        add(mainPanel);
        cardLayout.show(mainPanel, "MainMenu");
    }

    private void applyTheme(String newTheme) {
        try {
            if ("Dark".equalsIgnoreCase(newTheme)) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to apply theme: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showNoteDetailScreen(Note selectedNote) {
        if (selectedNote == null) {
            JOptionPane.showMessageDialog(this, "No note selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        noteEditorScreen.setNote(selectedNote);
        cardLayout.show(mainPanel, "NoteEditor");
    }

    public void showAddNoteScreen() {
        noteEditorScreen.setNote(new Note("New Note", "", false));
        cardLayout.show(mainPanel, "NoteEditor");
    }

    public void openCanvasPanel() {
        cardLayout.show(mainPanel, "CanvasPanel");
    }

    public void showMainMenuScreen() {
        cardLayout.show(mainPanel, "MainMenu");
    }
}