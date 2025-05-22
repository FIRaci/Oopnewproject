import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D; // Thêm import này
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

public class DrawScreen extends JPanel {
    private static final String SAVE_LABEL = "Lưu Bản Vẽ";
    private static final String BACK_LABEL = "Quay Lại";
    private static final String CLEAR_LABEL = "Xóa Hết";
    private static final String ERASER_LABEL = "Tẩy";
    private static final String PENCIL_LABEL = "Bút";
    private static final String IMPORT_IMAGE_LABEL = "Chèn Ảnh";
    private static final String HAND_TOOL_LABEL = "Bàn Tay (Pan)";
    private static final String ZOOM_IN_LABEL = "Phóng To";
    private static final String ZOOM_OUT_LABEL = "Thu Nhỏ";
    private static final String ZOOM_RESET_LABEL = "Reset Zoom";


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
    private JButton importImageButton;
    private JButton handToolButton;
    private JButton zoomInButton, zoomOutButton, zoomResetButton;

    private Color currentColor = Color.BLACK;
    private int currentStrokeSize = 3;
    private boolean eraserMode = false;
    private boolean handMode = false;

    public DrawScreen(MainFrame mainFrame, NoteController controller) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        initializeUI();
        setDrawingNote(null);

        this.setFocusable(true);
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    if (!handMode) {
                        drawingPanel.setTemporaryPanMode(true);
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    if (drawingPanel.isTemporaryPanMode()) {
                        drawingPanel.setTemporaryPanMode(false);
                        if (eraserMode) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        } else {
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            }
        });
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topControlPanel = new JPanel(new BorderLayout(10, 5));
        titleField = new JTextField("Bản vẽ không tên");
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        topControlPanel.add(new JLabel("Tiêu đề:"), BorderLayout.WEST);
        topControlPanel.add(titleField, BorderLayout.CENTER);

        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.addActionListener(e -> saveDrawing());
        JButton backButton = new JButton(BACK_LABEL);
        backButton.addActionListener(e -> {
            setHandMode(false);
            drawingPanel.setTemporaryPanMode(false);
            setCursor(Cursor.getDefaultCursor());
            mainFrame.showMainMenuScreen();
        });
        mainButtonsPanel.add(saveButton);
        mainButtonsPanel.add(backButton);
        topControlPanel.add(mainButtonsPanel, BorderLayout.EAST);
        add(topControlPanel, BorderLayout.NORTH);

        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));
        toolbarPanel.setBorder(BorderFactory.createTitledBorder("Công cụ"));
        toolbarPanel.setPreferredSize(new Dimension(180, 0));

        colorButton = new JButton("Chọn Màu");
        colorButton.setOpaque(true);
        colorButton.setBackground(currentColor);
        colorButton.setForeground(getContrastColor(currentColor));
        colorButton.addActionListener(e -> chooseColor());
        setFullWidth(colorButton, toolbarPanel);
        toolbarPanel.add(colorButton);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        toolbarPanel.add(new JLabel("Kích thước nét:"));
        strokeSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, currentStrokeSize);
        strokeSlider.setMajorTickSpacing(20);
        strokeSlider.setMinorTickSpacing(5);
        strokeSlider.setPaintTicks(true);
        currentStrokeLabel = new JLabel(String.valueOf(currentStrokeSize));
        strokeSlider.addChangeListener(e -> {
            currentStrokeSize = strokeSlider.getValue();
            currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
            drawingPanel.setCurrentStrokeSize(currentStrokeSize);
        });
        JPanel strokePanel = new JPanel(new BorderLayout(5,0));
        strokePanel.add(strokeSlider, BorderLayout.CENTER);
        strokePanel.add(currentStrokeLabel, BorderLayout.EAST);
        setFullWidth(strokePanel, toolbarPanel);
        toolbarPanel.add(strokePanel);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        pencilButton = new JButton(PENCIL_LABEL);
        pencilButton.addActionListener(e -> setPencilMode());
        setFullWidth(pencilButton, toolbarPanel);
        toolbarPanel.add(pencilButton);

        eraserButton = new JButton(ERASER_LABEL);
        eraserButton.addActionListener(e -> setEraserMode());
        setFullWidth(eraserButton, toolbarPanel);
        toolbarPanel.add(eraserButton);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        handToolButton = new JButton(HAND_TOOL_LABEL);
        handToolButton.addActionListener(e -> toggleHandMode());
        setFullWidth(handToolButton, toolbarPanel);
        toolbarPanel.add(handToolButton);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        importImageButton = new JButton(IMPORT_IMAGE_LABEL);
        importImageButton.addActionListener(e -> importImage());
        setFullWidth(importImageButton, toolbarPanel);
        toolbarPanel.add(importImageButton);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        zoomInButton = new JButton(ZOOM_IN_LABEL);
        zoomInButton.addActionListener(e -> drawingPanel.zoomIn());
        setFullWidth(zoomInButton, toolbarPanel);
        toolbarPanel.add(zoomInButton);

        zoomOutButton = new JButton(ZOOM_OUT_LABEL);
        zoomOutButton.addActionListener(e -> drawingPanel.zoomOut());
        setFullWidth(zoomOutButton, toolbarPanel);
        toolbarPanel.add(zoomOutButton);

        zoomResetButton = new JButton(ZOOM_RESET_LABEL);
        zoomResetButton.addActionListener(e -> drawingPanel.resetZoomAndPan());
        setFullWidth(zoomResetButton, toolbarPanel);
        toolbarPanel.add(zoomResetButton);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton clearButton = new JButton(CLEAR_LABEL);
        clearButton.addActionListener(e -> drawingPanel.clearDrawing());
        setFullWidth(clearButton, toolbarPanel);
        toolbarPanel.add(clearButton);

        toolbarPanel.add(Box.createVerticalGlue());

        add(toolbarPanel, BorderLayout.WEST);

        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(drawingPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setWheelScrollingEnabled(true);
        add(scrollPane, BorderLayout.CENTER);

        updateToolStates();
    }

    private void setFullWidth(JComponent component, Container parent) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private Color getContrastColor(Color color) {
        double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000.0;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    private void updateToolStates() {
        pencilButton.setEnabled(eraserMode || handMode);
        eraserButton.setEnabled(!eraserMode || handMode);
        handToolButton.setEnabled(!handMode);

        if (!handMode && !drawingPanel.isTemporaryPanMode()) {
            if (eraserMode) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        } else if (handMode || drawingPanel.isTemporaryPanMode()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    private void setPencilMode() {
        handMode = false;
        eraserMode = false;
        drawingPanel.setPanMode(false);
        drawingPanel.setEraserMode(false);
        drawingPanel.setCurrentColor(currentColor);
        System.out.println("Chế độ: Bút vẽ, Màu: " + currentColor);
        updateToolStates();
    }

    private void setEraserMode() {
        handMode = false;
        eraserMode = true;
        drawingPanel.setPanMode(false);
        drawingPanel.setEraserMode(true);
        System.out.println("Chế độ: Tẩy");
        updateToolStates();
    }

    private void toggleHandMode() {
        setHandMode(!this.handMode);
    }

    private void setHandMode(boolean enable) {
        this.handMode = enable;
        drawingPanel.setPanMode(this.handMode);
        if (this.handMode) {
            this.eraserMode = false;
            drawingPanel.setEraserMode(false);
            System.out.println("Chế độ: Bàn Tay (Pan)");
        } else {
            setPencilMode();
        }
        updateToolStates();
    }

    private void chooseColor() {
        setHandMode(false);
        Color newColor = JColorChooser.showDialog(this, "Chọn màu vẽ", currentColor);
        if (newColor != null) {
            currentColor = newColor;
            colorButton.setBackground(currentColor);
            colorButton.setForeground(getContrastColor(currentColor));
            if (!eraserMode) {
                drawingPanel.setCurrentColor(currentColor);
            }
        }
    }

    private void importImage() {
        setHandMode(false);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file ảnh để chèn");
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Ảnh JPG & PNG", "jpg", "jpeg", "png");
        fileChooser.addChoosableFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(mainFrame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage importedImage = ImageIO.read(selectedFile);
                if (importedImage != null) {
                    drawingPanel.importImageAndResizeCanvas(importedImage);
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể đọc file ảnh đã chọn.", "Lỗi Chèn Ảnh", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khi đọc file ảnh: " + ex.getMessage(), "Lỗi Chèn Ảnh", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setDrawingNote(Note note) {
        setHandMode(false);
        drawingPanel.resetZoomAndPan();

        if (note != null && note.getNoteType() == Note.NoteType.DRAWING) {
            this.currentDrawingNote = note;
            titleField.setText(note.getTitle());
            if (note.getDrawingData() != null && !note.getDrawingData().isEmpty()) {
                try {
                    drawingPanel.loadImageFromBase64AndResizeCanvas(note.getDrawingData());
                } catch (IOException e) {
                    System.err.println("Lỗi khi tải dữ liệu bản vẽ: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu bản vẽ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    drawingPanel.clearDrawingAndResizeCanvas(800, 600);
                }
            } else {
                drawingPanel.clearDrawingAndResizeCanvas(800, 600);
            }
        } else {
            this.currentDrawingNote = new Note("Bản vẽ " + System.currentTimeMillis()%10000, Note.NoteType.DRAWING, controller.getCurrentFolder());
            titleField.setText(this.currentDrawingNote.getTitle());
            drawingPanel.clearDrawingAndResizeCanvas(800, 600);
        }
        currentColor = Color.BLACK;
        colorButton.setBackground(currentColor);
        colorButton.setForeground(getContrastColor(currentColor));
        currentStrokeSize = 3;
        strokeSlider.setValue(currentStrokeSize);
        currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
        setPencilMode();
        drawingPanel.setCurrentColor(currentColor);
        drawingPanel.setCurrentStrokeSize(currentStrokeSize);
    }

    private void saveDrawing() {
        setHandMode(false);
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tiêu đề không được để trống.", "Lỗi Lưu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String base64Image = drawingPanel.getImageAsBase64("png");
            if (base64Image == null || base64Image.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có gì để lưu (bản vẽ trống).", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if (currentDrawingNote == null || currentDrawingNote.getId() == 0) {
                currentDrawingNote.setTitle(title);
                currentDrawingNote.setDrawingData(base64Image);
                currentDrawingNote.setNoteType(Note.NoteType.DRAWING);
                currentDrawingNote.setContent(null);

                if (currentDrawingNote.getFolder() == null || currentDrawingNote.getFolder().getId() == 0) {
                    currentDrawingNote.setFolder(controller.getCurrentFolder());
                    if (controller.getCurrentFolder() != null) {
                        currentDrawingNote.setFolderId(controller.getCurrentFolder().getId());
                    }
                }
                controller.addNote(currentDrawingNote);
                JOptionPane.showMessageDialog(this, "Bản vẽ '" + title + "' đã được lưu!", "Lưu Thành Công", JOptionPane.INFORMATION_MESSAGE);

            } else {
                currentDrawingNote.setTitle(title);
                currentDrawingNote.setDrawingData(base64Image);
                currentDrawingNote.setNoteType(Note.NoteType.DRAWING);
                currentDrawingNote.setContent(null);
                controller.updateNote(currentDrawingNote); // Sử dụng phương thức updateNote(Note)
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
        private boolean isPanMode = false;
        private boolean isTemporaryPanMode = false;

        private Point lastMousePoint;
        private Point panStartPoint;

        private double scaleFactor = 1.0;
        private double offsetX = 0;
        private double offsetY = 0;
        private static final double ZOOM_INCREMENT = 0.1;
        private static final double MIN_ZOOM = 0.2;
        private static final double MAX_ZOOM = 5.0;

        public DrawingPanel() {
            setBackground(Color.GRAY);
            resizeCanvas(800, 600, false);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isPanModeActive()) {
                        panStartPoint = e.getPoint();
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    } else {
                        lastMousePoint = transformScreenPointToCanvas(e.getPoint());
                        if (g2dCanvas != null) {
                            // SỬA MÀU TẨY: Luôn dùng Color.WHITE (màu nền thực sự của canvasImage)
                            g2dCanvas.setColor(isEraserMode ? Color.WHITE : currentColor);
                            g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            g2dCanvas.fillOval(lastMousePoint.x - currentStrokeSize / 2, lastMousePoint.y - currentStrokeSize / 2, currentStrokeSize, currentStrokeSize);
                            repaint();
                        }
                    }
                    DrawScreen parentDrawScreen = (DrawScreen) SwingUtilities.getAncestorOfClass(DrawScreen.class, DrawingPanel.this);
                    if(parentDrawScreen != null) parentDrawScreen.requestFocusInWindow();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isPanModeActive()) {
                        panStartPoint = null;
                        if (!isPanMode) {
                            setCursorBasedOnTool();
                        }
                    } else {
                        lastMousePoint = null;
                    }
                }
            };
            addMouseListener(mouseAdapter);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isPanModeActive() && panStartPoint != null) {
                        int dx = e.getX() - panStartPoint.x;
                        int dy = e.getY() - panStartPoint.y;
                        offsetX += dx;
                        offsetY += dy;
                        panStartPoint = e.getPoint();
                        repaint();
                    } else if (!isPanModeActive() && lastMousePoint != null && g2dCanvas != null) {
                        Point currentCanvasPoint = transformScreenPointToCanvas(e.getPoint());
                        // SỬA MÀU TẨY: Luôn dùng Color.WHITE
                        g2dCanvas.setColor(isEraserMode ? Color.WHITE : currentColor);
                        g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2dCanvas.drawLine(lastMousePoint.x, lastMousePoint.y, currentCanvasPoint.x, currentCanvasPoint.y);
                        lastMousePoint = currentCanvasPoint;
                        repaint();
                    }
                }
            });

            addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (e.isControlDown()) {
                        Point mousePosOnPanel = e.getPoint();
                        int rotation = e.getWheelRotation();
                        double oldScale = scaleFactor;

                        if (rotation < 0) {
                            scaleFactor = Math.min(MAX_ZOOM, scaleFactor * (1 + ZOOM_INCREMENT * Math.abs(rotation)));
                        } else {
                            scaleFactor = Math.max(MIN_ZOOM, scaleFactor / (1 + ZOOM_INCREMENT * Math.abs(rotation)));
                        }

                        Point2D.Double pCanvasOld = transformScreenPointToCanvasDouble(mousePosOnPanel, oldScale, offsetX, offsetY);
                        offsetX = mousePosOnPanel.x - pCanvasOld.x * scaleFactor;
                        offsetY = mousePosOnPanel.y - pCanvasOld.y * scaleFactor;

                        System.out.println("Zoom: " + scaleFactor + ", OffsetX: " + offsetX + ", OffsetY: " + offsetY);
                        repaint();
                        e.consume();
                    } else {
                        getParent().dispatchEvent(e);
                    }
                }
            });
        }

        private void setCursorBasedOnTool() {
            DrawScreen parentDrawScreen = (DrawScreen) SwingUtilities.getAncestorOfClass(DrawScreen.class, this);
            if (parentDrawScreen != null) {
                if (parentDrawScreen.handMode) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (parentDrawScreen.eraserMode) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        }

        public boolean isPanModeActive() { return isPanMode || isTemporaryPanMode; }
        public boolean isTemporaryPanMode() { return isTemporaryPanMode; }

        public void setPanMode(boolean panMode) {
            this.isPanMode = panMode;
            if (panMode) this.isTemporaryPanMode = false;
            setCursorBasedOnTool();
        }

        public void setTemporaryPanMode(boolean temporaryPanMode) {
            if (!isPanMode) {
                this.isTemporaryPanMode = temporaryPanMode;
                setCursorBasedOnTool();
            } else if (!temporaryPanMode && isPanMode) {
            } else {
                this.isTemporaryPanMode = temporaryPanMode;
                setCursorBasedOnTool();
            }
        }

        private Point transformScreenPointToCanvas(Point screenPoint) {
            int canvasX = (int) ((screenPoint.x - offsetX) / scaleFactor);
            int canvasY = (int) ((screenPoint.y - offsetY) / scaleFactor);
            return new Point(canvasX, canvasY);
        }

        private Point2D.Double transformScreenPointToCanvasDouble(Point screenPoint, double scale, double offX, double offY) {
            double canvasX = (screenPoint.x - offX) / scale;
            double canvasY = (screenPoint.y - offY) / scale;
            return new Point2D.Double(canvasX, canvasY);
        }

        public void setCurrentColor(Color color) { this.currentColor = color; }
        public void setCurrentStrokeSize(int size) { this.currentStrokeSize = Math.max(1, size); }
        public void setEraserMode(boolean isEraser) { this.isEraserMode = isEraser; }

        private void resizeCanvas(int newWidth, int newHeight, boolean copyOldContent) {
            if (newWidth <= 0 || newHeight <= 0) {
                System.err.println("Cảnh báo: Kích thước canvas không hợp lệ: " + newWidth + "x" + newHeight);
                return;
            }

            BufferedImage newCanvas = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D newG2d = newCanvas.createGraphics();
            newG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            newG2d.setColor(Color.WHITE);
            newG2d.fillRect(0, 0, newWidth, newHeight);

            if (copyOldContent && canvasImage != null) {
                newG2d.drawImage(canvasImage, 0, 0, null);
            }

            canvasImage = newCanvas;
            g2dCanvas = newG2d;
            setPreferredSize(new Dimension(newWidth, newHeight));
            revalidate();
            repaint();
            System.out.println("Canvas resized to: " + newWidth + "x" + newHeight);
        }

        public void clearDrawingAndResizeCanvas(int width, int height) {
            resizeCanvas(width, height, false);
        }

        public void importImageAndResizeCanvas(BufferedImage importedImage) {
            if (importedImage == null) return;
            resizeCanvas(importedImage.getWidth(), importedImage.getHeight(), false);
            if (g2dCanvas != null) {
                g2dCanvas.drawImage(importedImage, 0, 0, null);
                repaint();
            }
        }

        public void loadImageFromBase64AndResizeCanvas(String base64Image) throws IOException {
            if (base64Image == null || base64Image.isEmpty()) {
                clearDrawingAndResizeCanvas(800, 600);
                return;
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage loadedImage = ImageIO.read(bais);

            if (loadedImage != null) {
                importImageAndResizeCanvas(loadedImage);
            } else {
                throw new IOException("Không thể giải mã dữ liệu ảnh từ Base64.");
            }
        }

        public void clearDrawing() {
            if (g2dCanvas != null && canvasImage != null) {
                g2dCanvas.setColor(Color.WHITE); // Luôn tô màu trắng khi clear
                g2dCanvas.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
                repaint();
            }
        }

        public void importImage(BufferedImage importedImage) {
            if (g2dCanvas == null && canvasImage == null && getWidth() > 0 && getHeight() > 0) {
                importImageAndResizeCanvas(importedImage);
                return;
            }
            if (g2dCanvas == null) {
                System.err.println("Không thể chèn ảnh: canvas chưa sẵn sàng (g2dCanvas is null).");
                return;
            }
            Point canvasOrigin = transformScreenPointToCanvas(new Point(0,0));
            // Vẽ ảnh với kích thước đã được scale theo scaleFactor hiện tại của view
            // Điều này đảm bảo ảnh chèn vào có kích thước trực quan tương ứng với mức zoom
            int scaledWidth = (int)(importedImage.getWidth() / scaleFactor);
            int scaledHeight = (int)(importedImage.getHeight() / scaleFactor);
            g2dCanvas.drawImage(importedImage, canvasOrigin.x, canvasOrigin.y, scaledWidth, scaledHeight, null);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            AffineTransform at = new AffineTransform();
            at.translate(offsetX, offsetY);
            at.scale(scaleFactor, scaleFactor);
            g2.transform(at);

            if (canvasImage != null) {
                g2.drawImage(canvasImage, 0, 0, this);
            } else if (getWidth() > 0 && getHeight() > 0) {
                resizeCanvas(getWidth(), getHeight(), false); // Nên là kích thước mặc định hơn là getWidth()
                if (canvasImage != null) g2.drawImage(canvasImage, 0, 0, this);
            }
            g2.dispose();
        }

        public String getImageAsBase64(String formatName) throws IOException {
            if (canvasImage == null) return null;
            boolean isEmpty = true;
            if (canvasImage.getWidth() > 0 && canvasImage.getHeight() > 0) {
                // Kiểm tra kỹ hơn: so sánh với màu nền thực sự của canvasImage (là WHITE)
                int whiteRgb = Color.WHITE.getRGB();
                for(int x = 0; x < canvasImage.getWidth(); x+= Math.max(1, canvasImage.getWidth()/10)) {
                    for(int y = 0; y < canvasImage.getHeight(); y+= Math.max(1, canvasImage.getHeight()/10)) {
                        if (canvasImage.getRGB(x, y) != whiteRgb) {
                            isEmpty = false;
                            break;
                        }
                    }
                    if (!isEmpty) break;
                }
            }
            if (isEmpty) {
                System.out.println("Bản vẽ được coi là trống (toàn màu trắng), không lưu.");
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvasImage, formatName, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        }

        public void zoomIn() {
            Point center = new Point(getWidth() / 2, getHeight() / 2);
            applyZoom(1 + ZOOM_INCREMENT * 2, center);
        }

        public void zoomOut() {
            Point center = new Point(getWidth() / 2, getHeight() / 2);
            applyZoom(1 / (1 + ZOOM_INCREMENT * 2), center);
        }

        private void applyZoom(double factor, Point anchor) {
            double oldScale = scaleFactor;
            scaleFactor = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, scaleFactor * factor));

            Point2D.Double pCanvasOld = transformScreenPointToCanvasDouble(anchor, oldScale, offsetX, offsetY);
            offsetX = anchor.x - pCanvasOld.x * scaleFactor;
            offsetY = anchor.y - pCanvasOld.y * scaleFactor;

            System.out.println("Applied Zoom: " + scaleFactor + ", OffsetX: " + offsetX + ", OffsetY: " + offsetY);
            repaint();
        }

        public void resetZoomAndPan() {
            scaleFactor = 1.0;
            if (canvasImage != null) {
                // Căn giữa canvasImage trong vùng nhìn của panel
                offsetX = (getWidth() - canvasImage.getWidth() * scaleFactor) / 2.0;
                offsetY = (getHeight() - canvasImage.getHeight() * scaleFactor) / 2.0;
            } else {
                offsetX = 0;
                offsetY = 0;
            }
            System.out.println("Zoom/Pan Reset. Scale: " + scaleFactor + ", OffsetX: " + offsetX + ", OffsetY: " + offsetY);
            repaint();
        }
    }
}
