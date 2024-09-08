import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.text.*;

public class ImageCombiner extends JFrame {

    private JTextPane dropArea;

    public ImageCombiner() {
        setTitle("Image Combiner");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        dropArea = new JTextPane();
        dropArea.setEditable(false);
        dropArea.setText("PNG 혹은 GIF 를 드래그 앤 드롭해주세요");
        centerText(dropArea);

        dropArea.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(evt.getDropAction());
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    handleFiles(droppedFiles);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        add(new JScrollPane(dropArea), BorderLayout.CENTER);
    }

    public static void centerText(JTextPane textPane) {
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
    }

    public static BufferedImage combineImagesVertically(List<BufferedImage> images) {
        int totalHeight = 0;
        int maxWidth = 0;

        for (BufferedImage img : images) {
            totalHeight += img.getHeight();
            maxWidth = Math.max(maxWidth, img.getWidth());
        }

        BufferedImage combinedImage = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        int currentY = 0;

        for (BufferedImage img : images) {
            combinedImage.createGraphics().drawImage(img, 0, currentY, null);
            currentY += img.getHeight();
        }

        return combinedImage;
    }

    private void handleFiles(List<File> files) {
        List<File> imageFiles = new ArrayList<>();

        for (File file : files) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                imageFiles.add(file);
            }
        }

        Collections.sort(imageFiles, Comparator.comparingInt(f -> extractNumber(f.getName())));

        if (imageFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "PNG 또는 GIF 파일이 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<BufferedImage> images = new ArrayList<>();
        for (File file : imageFiles) {
            try {
                if (file.getName().toLowerCase().endsWith(".gif")) {
                    images.addAll(readGif(file));
                } else {
                    images.add(ImageIO.read(file));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BufferedImage combinedImage = combineImagesVertically(images);

        try {
            String outputFileName = imageFiles.get(0).getName();
            File outputFile = new File(imageFiles.get(0).getParent(), outputFileName);
            ImageIO.write(combinedImage, "PNG", outputFile);
            createMcMetaFile(outputFile.getPath());

            JOptionPane.showMessageDialog(this, "이미지가 성공적으로 결합되었습니다");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<BufferedImage> readGif(File gifFile) throws IOException {
        List<BufferedImage> frames = new ArrayList<>();
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream stream = ImageIO.createImageInputStream(gifFile)) {
            reader.setInput(stream, false);

            int numFrames = reader.getNumImages(true);
            for (int i = 0; i < numFrames; i++) {
                frames.add(reader.read(i));
            }
        } finally {
            reader.dispose();
        }
        return frames;
    }

    private int extractNumber(String fileName) {
        String number = fileName.replaceAll("\\D+", "");
        return number.isEmpty() ? 0 : Integer.parseInt(number);
    }

    public static void createMcMetaFile(String filePath) {
        String mcmetaFilePath = filePath + ".mcmeta";
        String mcmetaContent = "{\n  \"animation\": {}\n}";

        try (FileWriter fileWriter = new FileWriter(mcmetaFilePath)) {
            fileWriter.write(mcmetaContent);
            System.out.println(".mcmeta 파일 생성: " + mcmetaFilePath);
        } catch (IOException e) {
            System.out.println(".mcmeta 파일 생성 오류");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImageCombiner frame = new ImageCombiner();
            frame.setVisible(true);
        });
    }
}
