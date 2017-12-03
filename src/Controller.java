import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Controller implements Initializable
{
    
    @FXML private Button cambtn;
    @FXML private ImageView currentFrame;
    @FXML private BorderPane root;
    @FXML private Button colorButton;
    @FXML private JFXTextField colorText;
    @FXML private Canvas colorShow;
    @FXML private JFXToggleButton weirdToggle;
    @FXML private Text cameraIDText;
    
    private App app;

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that realizes the video capture
    private VideoCapture capture = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive = false;
    // the id of the camera to be used
    private static int cameraId = 0;
    
    private double[] midPoint;
    
    private boolean weirdRenderActive;
    private boolean flipActive;

    BooleanProperty isImgVisible = new SimpleBooleanProperty(true);
    
    @FXML
    protected void updateColor(ActionEvent ev)
    {
        try {
            String hex =  String.format("#%02x%02x%02x", (int) midPoint[0], (int)midPoint[1], (int)midPoint[2]);
            colorText.setText(hex);
            colorButton.setStyle("-fx-background-color: " + hex + ";");
        } catch(Exception ex) {ex.printStackTrace();}
        
    }
    
    @FXML
    protected void weirdToggleAction(ActionEvent ev)
    {
        weirdRenderActive = weirdToggle.isSelected();
    }
    
    @FXML
    protected void flipToggleAction(ActionEvent ev)
    {
        flipActive = ((JFXToggleButton)ev.getSource()).isSelected();
    }
    
    @FXML
    protected void increaseCameraID(ActionEvent ev)
    {
        cameraId++;
        cameraIDText.setText(Integer.toString(cameraId));
    }
    
    @FXML
    protected void decreaseCameraID(ActionEvent ev)
    {
        if (cameraId>0) cameraId--;
        cameraIDText.setText(Integer.toString(cameraId));
    }

    
    @FXML
    protected void startCamera(ActionEvent event)
    {
        
        colorButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        colorText.setFont(new Font("Symbol", 25.0));
        colorText.setText("");
        
        if (!this.cameraActive)
        {
            // start the video capture
            this.capture.open(cameraId);

            // is the video stream available?
            if (this.capture.isOpened())
            {
                this.cameraActive = true;

                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {

                    @Override
                    public void run()
                    {
                        // effectively grab and process a single frame
                        Mat frame = grabFrame();
                        // convert and show the frame
                        Image imageToShow = Utils.mat2Image(frame);
                        updateImageView(currentFrame, imageToShow);
                        
                        GraphicsContext g = colorShow.getGraphicsContext2D();
                        
                        g.setFill(Color.rgb((int) midPoint[0], (int)midPoint[1], (int)midPoint[2]));
                        g.fillRect(0, 0, colorShow.getWidth(), colorShow.getHeight());
                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                // update the button content
                this.cambtn.setText("Stop Camera");
            }
            else
            {
                // log the error
                System.err.println("Impossible to open the camera connection...");
                cambtn.setText("ERROR");
            }
        }
        else
        {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.cambtn.setText("Start Camera");

            // stop the timer
            this.stopAcquisition();
        }
    }
    
    private Mat grabFrame()
    {
        // init everything
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened())
        {
            try
            {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty())
                {
                    process(frame);
                }

            }
            catch (Exception e)
            {
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }

        return frame;
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition()
    {
        if (this.timer!=null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened())
        {
            // release the camera
            this.capture.release();
        }
    }
    
    private void updateImageView(ImageView view, Image image)
    {
        Utils.onFXThread(view.imageProperty(), image);
    }
    
    protected void setClosed()
    {
        this.stopAcquisition();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle bundle)
    {
        currentFrame.visibleProperty().bind(isImgVisible);
    }
    
    public void link(App a)
    {
        app = a;
    }
    
    
    private void process(Mat f)
    {
        Mat gen = new Mat();
        
        if (flipActive) Core.flip(f, f, 1);
        
        Imgproc.cvtColor(f, gen, Imgproc.COLOR_BGR2RGB);
        
        if (weirdRenderActive) Imgproc.cvtColor(gen, gen, Imgproc.COLOR_HSV2RGB);
        
        Point mid = new Point(gen.size().width/2, gen.size().height/2);
        
        midPoint = gen.get((int)mid.y, (int)mid.x);
        
        Imgproc.circle(gen, mid, 5, new Scalar(255-(int)(midPoint[0]), 255-(int)(midPoint[1]), 255-(int)(midPoint[2])), 2);

        Imgproc.cvtColor(gen, f, Imgproc.COLOR_RGB2BGR);
        
    }

}