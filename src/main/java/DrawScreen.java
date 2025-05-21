import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

    private final MainFrame mainFrame;
    private final NoteController controller;
    private Note currentDrawingNote; // Note hiện tại (có thể là mới hoặc đang chỉnh sửa)

    private DrawingPanel drawingPanel;
    private JTextField titleField;
    private JSlider strokeSlider;
    private JButton colorButton;
    private JButton eraserButton;
    private JButton pencilButton; // Để quay lại chế độ bút vẽ
    private JLabel currentStrokeLabel;

    private Color currentColor = Color.BLACK;
    private int currentStrokeSize = 3;
    private boolean eraserMode = false;

    public DrawScreen(MainFrame mainFrame, NoteController controller) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        // Ban đầu, currentDrawingNote là null, nghĩa là tạo bản vẽ mới
        // Nếu muốn mở bản vẽ cũ, cần một phương thức setNote(Note note)
        initializeUI();
        setDrawingNote(null); // Khởi tạo cho bản vẽ mới
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel tiêu đề và các nút điều khiển chính
        JPanel topControlPanel = new JPanel(new BorderLayout(10, 5));
        titleField = new JTextField("Bản vẽ không tên");
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        topControlPanel.add(new JLabel("Tiêu đề:"), BorderLayout.WEST);
        topControlPanel.add(titleField, BorderLayout.CENTER);

        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.addActionListener(e -> saveDrawing());
        JButton backButton = new JButton(BACK_LABEL);
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        mainButtonsPanel.add(saveButton);
        mainButtonsPanel.add(backButton);
        topControlPanel.add(mainButtonsPanel, BorderLayout.EAST);
        add(topControlPanel, BorderLayout.NORTH);

        // Panel công cụ vẽ
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS)); // Sắp xếp theo chiều dọc
        toolbarPanel.setBorder(BorderFactory.createTitledBorder("Công cụ"));
        toolbarPanel.setPreferredSize(new Dimension(150, 0)); // Độ rộng cố định cho toolbar

        // Chọn màu
        colorButton = new JButton("Chọn Màu");
        colorButton.setOpaque(true);
        colorButton.setBackground(currentColor);
        colorButton.setForeground(getContrastColor(currentColor));
        colorButton.addActionListener(e -> chooseColor());
        toolbarPanel.add(colorButton);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Kích thước bút
        toolbarPanel.add(new JLabel("Kích thước nét:"));
        strokeSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, currentStrokeSize);
        strokeSlider.setMajorTickSpacing(10);
        strokeSlider.setMinorTickSpacing(1);
        strokeSlider.setPaintTicks(true);
        // strokeSlider.setPaintLabels(true); // Không cần label số chi tiết
        currentStrokeLabel = new JLabel(String.valueOf(currentStrokeSize));
        strokeSlider.addChangeListener(e -> {
            currentStrokeSize = strokeSlider.getValue();
            currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
            drawingPanel.setCurrentStrokeSize(currentStrokeSize);
        });
        JPanel strokePanel = new JPanel(new BorderLayout(5,0));
        strokePanel.add(strokeSlider, BorderLayout.CENTER);
        strokePanel.add(currentStrokeLabel, BorderLayout.EAST);
        toolbarPanel.add(strokePanel);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Bút vẽ (mặc định)
        pencilButton = new JButton(PENCIL_LABEL);
        pencilButton.addActionListener(e -> setPencilMode());
        toolbarPanel.add(pencilButton);

        // Tẩy
        eraserButton = new JButton(ERASER_LABEL);
        eraserButton.addActionListener(e -> setEraserMode());
        toolbarPanel.add(eraserButton);
        toolbarPanel.add(Box.createRigidArea(new Dimension(0, 10)));


        // Xóa hết
        JButton clearButton = new JButton(CLEAR_LABEL);
        clearButton.addActionListener(e -> drawingPanel.clearDrawing());
        toolbarPanel.add(clearButton);

        add(toolbarPanel, BorderLayout.WEST);

        // Vùng vẽ chính
        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE);
        add(new JScrollPane(drawingPanel), BorderLayout.CENTER); // Cho phép scroll nếu cần

        updateToolStates();
    }

    private Color getContrastColor(Color color) {
        double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000.0;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    private void updateToolStates() {
        pencilButton.setEnabled(eraserMode); // Bật nếu đang ở chế độ tẩy
        eraserButton.setEnabled(!eraserMode); // Bật nếu đang ở chế độ bút
    }

    private void setPencilMode() {
        eraserMode = false;
        drawingPanel.setEraserMode(false);
        drawingPanel.setCurrentColor(currentColor); // Đặt lại màu hiện tại cho bút
        System.out.println("Chế độ: Bút vẽ, Màu: " + currentColor);
        updateToolStates();
    }

    private void setEraserMode() {
        eraserMode = true;
        drawingPanel.setEraserMode(true);
        // Màu của tẩy sẽ là màu nền của drawingPanel
        System.out.println("Chế độ: Tẩy");
        updateToolStates();
    }


    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(this, "Chọn màu vẽ", currentColor);
        if (newColor != null) {
            currentColor = newColor;
            colorButton.setBackground(currentColor);
            colorButton.setForeground(getContrastColor(currentColor));
            if (!eraserMode) { // Chỉ cập nhật màu vẽ nếu không ở chế độ tẩy
                drawingPanel.setCurrentColor(currentColor);
            }
        }
    }

    public void setDrawingNote(Note note) {
        if (note != null && note.getNoteType() == Note.NoteType.DRAWING) {
            this.currentDrawingNote = note;
            titleField.setText(note.getTitle());
            if (note.getDrawingData() != null && !note.getDrawingData().isEmpty()) {
                try {
                    drawingPanel.loadImageFromBase64(note.getDrawingData());
                } catch (IOException e) {
                    System.err.println("Lỗi khi tải dữ liệu bản vẽ: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu bản vẽ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    drawingPanel.clearDrawing(); // Xóa nếu không tải được
                }
            } else {
                drawingPanel.clearDrawing();
            }
        } else { // Tạo bản vẽ mới
            this.currentDrawingNote = new Note("Bản vẽ không tên " + System.currentTimeMillis()%10000, Note.NoteType.DRAWING, controller.getCurrentFolder());
            titleField.setText(this.currentDrawingNote.getTitle());
            drawingPanel.clearDrawing();
        }
        // Reset các công cụ về mặc định
        currentColor = Color.BLACK;
        colorButton.setBackground(currentColor);
        colorButton.setForeground(getContrastColor(currentColor));
        currentStrokeSize = 3;
        strokeSlider.setValue(currentStrokeSize);
        currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
        setPencilMode(); // Mặc định là bút vẽ
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
                JOptionPane.showMessageDialog(this, "Không có gì để lưu (bản vẽ trống).", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if (currentDrawingNote == null || currentDrawingNote.getId() == 0) { // Tạo mới
                // currentDrawingNote đã được khởi tạo trong setDrawingNote(null)
                // hoặc nếu mở để tạo mới.
                // Chỉ cần cập nhật title và data.
                currentDrawingNote.setTitle(title);
                currentDrawingNote.setDrawingData(base64Image);
                currentDrawingNote.setNoteType(Note.NoteType.DRAWING);
                currentDrawingNote.setContent(null); // Không có content text cho drawing

                // Gán folder hiện tại nếu chưa có
                if (currentDrawingNote.getFolder() == null || currentDrawingNote.getFolder().getId() == 0) {
                    currentDrawingNote.setFolder(controller.getCurrentFolder());
                    if (controller.getCurrentFolder() != null) {
                        currentDrawingNote.setFolderId(controller.getCurrentFolder().getId());
                    }
                }
                controller.addNote(currentDrawingNote); // Controller sẽ gọi service, service gọi manager
                JOptionPane.showMessageDialog(this, "Bản vẽ '" + title + "' đã được lưu!", "Lưu Thành Công", JOptionPane.INFORMATION_MESSAGE);

            } else { // Cập nhật bản vẽ đã có
                currentDrawingNote.setTitle(title);
                currentDrawingNote.setDrawingData(base64Image);
                currentDrawingNote.setNoteType(Note.NoteType.DRAWING);
                currentDrawingNote.setContent(null);
                controller.updateNote(currentDrawingNote, title, null); // Truyền content là null
                JOptionPane.showMessageDialog(this, "Bản vẽ '" + title + "' đã được cập nhật!", "Cập Nhật Thành Công", JOptionPane.INFORMATION_MESSAGE);
            }
            mainFrame.showMainMenuScreen(); // Quay về màn hình chính sau khi lưu

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu bản vẽ: " + e.getMessage(), "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lớp nội bộ cho vùng vẽ
    private static class DrawingPanel extends JPanel {
        private BufferedImage canvasImage;
        private Graphics2D g2dCanvas;
        private Path2D.Double currentPath;
        private Color currentColor = Color.BLACK;
        private int currentStrokeSize = 3;
        private boolean isEraserMode = false;

        // Lưu trữ các nét vẽ để có thể vẽ lại (quan trọng cho việc thay đổi kích thước cửa sổ, v.v.)
        // Hoặc đơn giản hơn là vẽ trực tiếp lên BufferedImage và chỉ vẽ lại BufferedImage đó.
        // Cách đơn giản (vẽ lên BufferedImage):
        private Point lastPoint;

        public DrawingPanel() {
            // Khởi tạo canvasImage với kích thước mặc định, sẽ được vẽ lại khi panel có kích thước
            // Hoặc đợi đến khi có kích thước thực sự trong componentResized
            setPreferredSize(new Dimension(600, 400)); // Kích thước mặc định ban đầu

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastPoint = e.getPoint();
                    // Nếu dùng Path2D:
                    // currentPath = new Path2D.Double();
                    // currentPath.moveTo(e.getX(), e.getY());
                    // paths.add(new DrawablePath(currentPath, currentColor, new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));

                    // Vẽ điểm đầu tiên nếu là click chuột (không kéo)
                    if (g2dCanvas != null) {
                        g2dCanvas.setColor(isEraserMode ? getBackground() : currentColor);
                        g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2dCanvas.fillOval(e.getX() - currentStrokeSize / 2, e.getY() - currentStrokeSize / 2, currentStrokeSize, currentStrokeSize);
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    lastPoint = null;
                    // if (currentPath != null) {
                    //     // currentPath đã được thêm vào list, không cần làm gì thêm
                    //     currentPath = null;
                    // }
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
                        repaint();
                    }
                    // Nếu dùng Path2D:
                    // if (currentPath != null) {
                    //     currentPath.lineTo(e.getX(), e.getY());
                    //     repaint();
                    // }
                }
            });

            // Xử lý khi kích thước panel thay đổi để tạo lại BufferedImage
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    if (getWidth() > 0 && getHeight() > 0) {
                        if (canvasImage == null || canvasImage.getWidth() != getWidth() || canvasImage.getHeight() != getHeight()) {
                            BufferedImage oldImage = canvasImage;
                            canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                            g2dCanvas = canvasImage.createGraphics();
                            g2dCanvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2dCanvas.setColor(getBackground()); // Đặt màu nền cho canvas mới
                            g2dCanvas.fillRect(0, 0, getWidth(), getHeight());
                            if (oldImage != null) { // Vẽ lại ảnh cũ lên canvas mới (giữ nội dung khi resize)
                                g2dCanvas.drawImage(oldImage, 0, 0, null);
                            }
                            System.out.println("DrawingPanel resized, canvas recreated.");
                        }
                    }
                    repaint();
                }
            });
        }

        public void setCurrentColor(Color color) {
            this.currentColor = color;
        }

        public void setCurrentStrokeSize(int size) {
            this.currentStrokeSize = Math.max(1, size); // Đảm bảo stroke > 0
        }

        public void setEraserMode(boolean isEraser) {
            this.isEraserMode = isEraser;
        }

        public void clearDrawing() {
            if (g2dCanvas != null) {
                g2dCanvas.setColor(getBackground()); // Tô màu nền
                g2dCanvas.fillRect(0, 0, getWidth(), getHeight());
                repaint();
            }
            // Nếu dùng List<Path2D>, thì paths.clear();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvasImage == null && getWidth() > 0 && getHeight() > 0) {
                // Khởi tạo lần đầu nếu chưa có khi panel đã có kích thước
                canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                g2dCanvas = canvasImage.createGraphics();
                g2dCanvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2dCanvas.setColor(getBackground());
                g2dCanvas.fillRect(0, 0, getWidth(), getHeight());
            }
            if (canvasImage != null) {
                g.drawImage(canvasImage, 0, 0, this);
            }
            // Nếu dùng List<Path2D>:
            // Graphics2D g2 = (Graphics2D) g;
            // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // for (DrawablePath dp : paths) {
            //     g2.setColor(dp.getColor());
            //     g2.setStroke(dp.getStroke());
            //     g2.draw(dp.getPath());
            // }
        }

        public String getImageAsBase64(String formatName) throws IOException {
            if (canvasImage == null) return null;
            // Kiểm tra xem canvas có trống không (ví dụ, toàn màu nền)
            // Cách đơn giản: nếu không có path nào được vẽ (nếu dùng List<Path2D>)
            // Hoặc một cách phức tạp hơn là quét pixel, nhưng có thể bỏ qua bước này ban đầu.

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvasImage, formatName, baos);
            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length == 0) return null; // Hoặc một ngưỡng kích thước nhỏ
            return Base64.getEncoder().encodeToString(imageBytes);
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
                // Tạo canvas mới với kích thước của panel hiện tại và vẽ ảnh đã load lên đó
                if (getWidth() > 0 && getHeight() > 0) {
                    canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                    g2dCanvas = canvasImage.createGraphics();
                    g2dCanvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Vẽ màu nền trước
                    g2dCanvas.setColor(getBackground());
                    g2dCanvas.fillRect(0, 0, getWidth(), getHeight());
                    // Sau đó vẽ ảnh đã load, có thể cần scale cho vừa
                    g2dCanvas.drawImage(loadedImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    // Nếu panel chưa có kích thước, tạm thời giữ ảnh gốc
                    // và chờ componentResized xử lý
                    canvasImage = loadedImage;
                    // g2dCanvas sẽ được tạo trong componentResized hoặc paintComponent
                }
                repaint();
            } else {
                throw new IOException("Không thể giải mã dữ liệu ảnh từ Base64.");
            }
        }
    }
}
