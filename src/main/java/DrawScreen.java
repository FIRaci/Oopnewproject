// File: DrawScreen.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class DrawScreen extends JPanel {
    private static final String SAVE_LABEL = "Lưu Bản Vẽ";
    private static final String BACK_LABEL = "Quay Lại";
    private static final String CLEAR_LABEL = "Xóa Hết";
    private static final String ERASER_LABEL = "Tẩy";
    private static final String PENCIL_LABEL = "Bút Vẽ"; // Changed for clarity

    private final MainFrame mainFrame;
    private final NoteController controller;
    private Note currentDrawingNote;

    private DrawingPanel drawingPanel;
    private JTextField titleField;
    private JSlider strokeSlider;
    private JButton colorButton;
    private JButton eraserButton;
    private JButton pencilButton;
    private JLabel currentStrokeLabel;
    private JPanel toolbarInnerPanel; // Panel to hold tools for better alignment

    private Color currentColor = Color.BLACK;
    private int currentStrokeSize = 3;
    private boolean eraserMode = false;

    public DrawScreen(MainFrame mainFrame, NoteController controller) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        initializeUI();
        setDrawingNote(null);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(15, 15)); // Increased gaps
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Overall padding

        // Top Panel: Title and Main Buttons
        JPanel topControlPanel = new JPanel(new BorderLayout(10, 5));
        topControlPanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0)); // Bottom margin

        JPanel titlePanel = new JPanel(new BorderLayout(5,0));
        JLabel titleLabel = new JLabel("Tiêu đề:");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titleField = new JTextField("Bản vẽ không tên");
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titlePanel.add(titleField, BorderLayout.CENTER);
        topControlPanel.add(titlePanel, BorderLayout.CENTER);


        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.setToolTipText("Lưu bản vẽ hiện tại");
        saveButton.addActionListener(e -> saveDrawing());
        JButton backButton = new JButton(BACK_LABEL);
        backButton.setToolTipText("Quay lại màn hình chính (Esc)");
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        mainButtonsPanel.add(saveButton);
        mainButtonsPanel.add(backButton);
        topControlPanel.add(mainButtonsPanel, BorderLayout.EAST);
        add(topControlPanel, BorderLayout.NORTH);

        // Toolbar Panel (West)
        JPanel toolbarOuterPanel = new JPanel(new BorderLayout()); // Use BorderLayout for TitledBorder padding
        toolbarOuterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Công cụ vẽ"),
                new EmptyBorder(5, 8, 5, 8) // Padding inside TitledBorder
        ));
        toolbarOuterPanel.setPreferredSize(new Dimension(180, 0)); // Slightly wider toolbar

        toolbarInnerPanel = new JPanel();
        toolbarInnerPanel.setLayout(new BoxLayout(toolbarInnerPanel, BoxLayout.Y_AXIS));

        // Color Chooser Button
        colorButton = new JButton("Chọn Màu");
        colorButton.setOpaque(true); // Necessary for background to show
        colorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        setButtonColor(currentColor);
        colorButton.addActionListener(e -> chooseColor());
        toolbarInnerPanel.add(colorButton);
        toolbarInnerPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Stroke Size Slider
        JLabel strokeTitleLabel = new JLabel("Kích thước nét:");
        strokeTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolbarInnerPanel.add(strokeTitleLabel);

        JPanel strokePanel = new JPanel(new BorderLayout(5,0));
        strokePanel.setOpaque(false); // Make transparent if toolbar has own bg
        strokeSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, currentStrokeSize);
        strokeSlider.setMajorTickSpacing(10);
        strokeSlider.setMinorTickSpacing(1);
        strokeSlider.setPaintTicks(true);
        currentStrokeLabel = new JLabel(String.valueOf(currentStrokeSize), SwingConstants.CENTER);
        currentStrokeLabel.setPreferredSize(new Dimension(25, currentStrokeLabel.getPreferredSize().height)); // Fixed width for label
        strokeSlider.addChangeListener(e -> {
            currentStrokeSize = strokeSlider.getValue();
            currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
            drawingPanel.setCurrentStrokeSize(currentStrokeSize);
        });
        strokePanel.add(strokeSlider, BorderLayout.CENTER);
        strokePanel.add(currentStrokeLabel, BorderLayout.EAST);
        strokePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolbarInnerPanel.add(strokePanel);
        toolbarInnerPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Pencil Button
        pencilButton = new JButton(PENCIL_LABEL);
        pencilButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        pencilButton.addActionListener(e -> setPencilMode());
        toolbarInnerPanel.add(pencilButton);
        toolbarInnerPanel.add(Box.createRigidArea(new Dimension(0, 5)));


        // Eraser Button
        eraserButton = new JButton(ERASER_LABEL);
        eraserButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        eraserButton.addActionListener(e -> setEraserMode());
        toolbarInnerPanel.add(eraserButton);
        toolbarInnerPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Clear Button
        JButton clearButton = new JButton(CLEAR_LABEL);
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearButton.addActionListener(e -> drawingPanel.clearDrawing());
        toolbarInnerPanel.add(clearButton);

        toolbarInnerPanel.add(Box.createVerticalGlue()); // Pushes tools to the top if space available

        toolbarOuterPanel.add(toolbarInnerPanel, BorderLayout.NORTH); // Add inner panel to outer
        add(toolbarOuterPanel, BorderLayout.WEST);

        // Main Drawing Area
        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE); // Default background
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Border for drawing area
        add(new JScrollPane(drawingPanel) {{
            setBorder(BorderFactory.createEmptyBorder()); // Remove scroll pane's own border
        }}, BorderLayout.CENTER);

        updateToolStates();
    }

    private void setButtonColor(Color color) {
        colorButton.setBackground(color);
        colorButton.setForeground(getContrastColor(color));
    }

    private Color getContrastColor(Color color) {
        double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000.0;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    private void updateToolStates() {
        pencilButton.setEnabled(eraserMode);
        eraserButton.setEnabled(!eraserMode);
        // Visually indicate active tool (e.g., thicker border or different background)
        pencilButton.setBorder(eraserMode ? UIManager.getBorder("Button.border") : BorderFactory.createLineBorder(Color.BLUE, 2));
        eraserButton.setBorder(!eraserMode ? UIManager.getBorder("Button.border") : BorderFactory.createLineBorder(Color.BLUE, 2));

    }

    private void setPencilMode() {
        eraserMode = false;
        drawingPanel.setEraserMode(false);
        drawingPanel.setCurrentColor(currentColor);
        updateToolStates();
    }

    private void setEraserMode() {
        eraserMode = true;
        drawingPanel.setEraserMode(true);
        updateToolStates();
    }


    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(this, "Chọn màu vẽ", currentColor);
        if (newColor != null) {
            currentColor = newColor;
            setButtonColor(currentColor);
            if (!eraserMode) {
                drawingPanel.setCurrentColor(currentColor);
            }
        }
    }

    public void setDrawingNote(Note note) {
        this.currentDrawingNote = (note != null && note.getNoteType() == Note.NoteType.DRAWING) ?
                note :
                new Note("Bản vẽ " + System.currentTimeMillis()%10000, Note.NoteType.DRAWING, controller.getCurrentFolder());

        titleField.setText(this.currentDrawingNote.getTitle());
        if (this.currentDrawingNote.getDrawingData() != null && !this.currentDrawingNote.getDrawingData().isEmpty()) {
            try {
                drawingPanel.loadImageFromBase64(this.currentDrawingNote.getDrawingData());
            } catch (IOException e) {
                System.err.println("Lỗi khi tải dữ liệu bản vẽ: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu bản vẽ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                drawingPanel.clearDrawing();
            }
        } else {
            drawingPanel.clearDrawing();
        }

        currentColor = Color.BLACK;
        setButtonColor(currentColor);
        currentStrokeSize = 3;
        strokeSlider.setValue(currentStrokeSize);
        currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
        setPencilMode();
        drawingPanel.setCurrentColor(currentColor);
        drawingPanel.setCurrentStrokeSize(currentStrokeSize);
    }

    private void saveDrawing() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tiêu đề không được để trống.", "Lỗi Lưu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String base64Image = drawingPanel.getImageAsBase64("png");
            if (base64Image == null || base64Image.isEmpty()) {
                // Check if the panel is truly empty or just appears so due to background color
                if (drawingPanel.isEffectivelyEmpty()) {
                    JOptionPane.showMessageDialog(this, "Không có gì để lưu (bản vẽ trống).", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                // If not effectively empty, it means something was drawn (even if it's white on white)
                // Still, it's unusual, so maybe a warning or proceed. For now, let's proceed.
            }


            currentDrawingNote.setTitle(title);
            currentDrawingNote.setDrawingData(base64Image); // base64Image can be null if panel is empty
            currentDrawingNote.setNoteType(Note.NoteType.DRAWING);
            currentDrawingNote.setContent(null); // No text content for drawing

            if (currentDrawingNote.getId() == 0) { // New drawing
                if (currentDrawingNote.getFolder() == null || currentDrawingNote.getFolder().getId() == 0) {
                    currentDrawingNote.setFolder(controller.getCurrentFolder());
                }
                controller.addNote(currentDrawingNote);
                JOptionPane.showMessageDialog(this, "Bản vẽ '" + title + "' đã được lưu!", "Lưu Thành Công", JOptionPane.INFORMATION_MESSAGE);
            } else { // Update existing drawing
                controller.updateNote(currentDrawingNote, title, null);
                JOptionPane.showMessageDialog(this, "Bản vẽ '" + title + "' đã được cập nhật!", "Cập Nhật Thành Công", JOptionPane.INFORMATION_MESSAGE);
            }
            mainFrame.showMainMenuScreen();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu bản vẽ: " + e.getMessage(), "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class DrawingPanel extends JPanel {
        private BufferedImage canvasImage;
        private Graphics2D g2dCanvas;
        private Color currentColor = Color.BLACK;
        private int currentStrokeSize = 3;
        private boolean isEraserMode = false;
        private Point lastPoint;
        private boolean hasDrawingContent = false; // Track if anything has been drawn

        public DrawingPanel() {
            setPreferredSize(new Dimension(800, 600)); // Default preferred size
            setBackground(Color.WHITE); // Explicitly set background

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastPoint = e.getPoint();
                    if (g2dCanvas != null) {
                        g2dCanvas.setColor(isEraserMode ? getBackground() : currentColor);
                        g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        // Draw a small circle for a single click
                        g2dCanvas.fillOval(e.getX() - currentStrokeSize / 2, e.getY() - currentStrokeSize / 2, currentStrokeSize, currentStrokeSize);
                        hasDrawingContent = true;
                        repaint();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    lastPoint = null;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (g2dCanvas != null && lastPoint != null) {
                        g2dCanvas.setColor(isEraserMode ? getBackground() : currentColor);
                        g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2dCanvas.drawLine(lastPoint.x, lastPoint.y, e.getX(), e.getY());
                        lastPoint = e.getPoint();
                        hasDrawingContent = true;
                        repaint();
                    }
                }
            });

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    if (getWidth() > 0 && getHeight() > 0) {
                        BufferedImage oldImage = canvasImage;
                        canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                        g2dCanvas = canvasImage.createGraphics();
                        setupGraphics2D(g2dCanvas);
                        if (oldImage != null) {
                            g2dCanvas.drawImage(oldImage, 0, 0, null);
                        }
                        repaint();
                    }
                }
            });
            // Ensure canvas is initialized on first paint if not resized yet
            SwingUtilities.invokeLater(this::initializeCanvas);
        }

        private void initializeCanvas() {
            if (getWidth() > 0 && getHeight() > 0 && canvasImage == null) {
                canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                g2dCanvas = canvasImage.createGraphics();
                setupGraphics2D(g2dCanvas);
                repaint();
            }
        }

        private void setupGraphics2D(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }


        public void setCurrentColor(Color color) { this.currentColor = color; }
        public void setCurrentStrokeSize(int size) { this.currentStrokeSize = Math.max(1, size); }
        public void setEraserMode(boolean isEraser) { this.isEraserMode = isEraser; }

        public void clearDrawing() {
            if (g2dCanvas != null && getWidth() > 0 && getHeight() > 0) {
                g2dCanvas.setColor(getBackground());
                g2dCanvas.fillRect(0, 0, getWidth(), getHeight());
                hasDrawingContent = false;
                repaint();
            } else if (getWidth() > 0 && getHeight() > 0) { // If g2dCanvas was null but panel has size
                initializeCanvas(); // Initialize and clear
                hasDrawingContent = false;
                repaint();
            }
        }

        public boolean isEffectivelyEmpty() {
            return !hasDrawingContent;
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvasImage == null && getWidth() > 0 && getHeight() > 0) {
                initializeCanvas();
            }
            if (canvasImage != null) {
                g.drawImage(canvasImage, 0, 0, this);
            }
        }

        public String getImageAsBase64(String formatName) throws IOException {
            if (canvasImage == null || !hasDrawingContent) return null; // Return null if nothing drawn

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvasImage, formatName, baos);
            byte[] imageBytes = baos.toByteArray();
            return (imageBytes.length == 0) ? null : Base64.getEncoder().encodeToString(imageBytes);
        }

        public void loadImageFromBase64(String base64Image) throws IOException {
            if (base64Image == null || base64Image.isEmpty()) {
                clearDrawing();
                return;
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage loadedImage = ImageIO.read(bais);

            if (loadedImage != null) {
                if (getWidth() <= 0 || getHeight() <= 0) { // Panel not yet sized
                    // Temporarily store and wait for resize, or use image's own size
                    setPreferredSize(new Dimension(loadedImage.getWidth(), loadedImage.getHeight()));
                    canvasImage = loadedImage; // This will be redrawn correctly on resize or first paint
                    // No g2dCanvas to draw on yet, will be created in componentResized or paintComponent
                } else {
                    // Panel is sized, create/recreate canvasImage and draw loadedImage onto it
                    canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                    g2dCanvas = canvasImage.createGraphics();
                    setupGraphics2D(g2dCanvas); // Clears with background
                    g2dCanvas.drawImage(loadedImage, 0, 0, getWidth(), getHeight(), null); // Scale to fit
                }
                hasDrawingContent = true; // Assume loaded image has content
                repaint();
            } else {
                throw new IOException("Không thể giải mã dữ liệu ảnh từ Base64.");
            }
        }
    }
}
