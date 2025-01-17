package at.ac.fhcampuswien.barcode_scanner;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class CameraHandler {

    private VideoCapture camera;
    private ImageView videoFeedView;
    private volatile boolean cameraPaused = false;
    private UIController uiController;
    private int currentCameraIndex;

    public CameraHandler(ImageView videoFeedView, UIController uiController) {
        this.videoFeedView = videoFeedView;
        this.uiController = uiController;
        currentCameraIndex = 0;
        initializeCamera();
    }

    private void initializeCamera() {
        camera = new VideoCapture(currentCameraIndex);
        startCameraFeed();
    }

    private void startCameraFeed() {
        new Thread(() -> {
            Mat frame = new Mat();
            while (camera.isOpened()) {
                if (camera.read(frame) && !cameraPaused) {
                    // Notify the callback with the captured frame
                    if (uiController != null) {
                        uiController.onFrameCaptured(frame);
                    }

                    // Convert Mat to JavaFX Image and update the video feed
                    Image image = matToImage(frame);
                    Platform.runLater(() -> videoFeedView.setImage(image));
                }
            }
        }).start();
    }

    public void pauseCamera() {
        cameraPaused = true;
    }

    public void resumeCamera() {
        cameraPaused = false;
    }

    public static boolean checkSecondCamera() {
        VideoCapture cap = new VideoCapture(1);
        if (cap.isOpened()) {
            cap.release();
            System.out.println("Second camera found");
            return true;
        }
        System.out.println("No second camera found");
        return false;
    }

    public void changeCamera() {
        currentCameraIndex = currentCameraIndex == 0 ? 1 : 0;
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
        initializeCamera();
    }

    private Image matToImage(Mat mat) {
        if (mat.channels() != 4) {
            Mat convertedMat = new Mat();
            Imgproc.cvtColor(mat, convertedMat, Imgproc.COLOR_BGR2BGRA);
            mat = convertedMat;
        }

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();
        byte[] buffer = new byte[width * height * channels];

        mat.get(0, 0, buffer);

        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(width, height);
        javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();
        pixelWriter.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getByteBgraPreInstance(), buffer, 0, width * channels);

        return writableImage;
    }
}