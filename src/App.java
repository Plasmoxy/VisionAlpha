import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;

import java.io.IOException;

public class App extends Application
{
    
    public FXMLLoader loader;
    public BorderPane root;
    public Controller contr;
    public Scene sc;
    public Stage st;
    
    @Override
    public void start(Stage s) throws IOException
    {

        loader = new FXMLLoader(getClass().getResource("gui.fxml"));
        root = loader.load();
        contr = loader.getController();
        contr.link(this);
        st = s;
        
        sc = new Scene(root);
        
        
        
        s.setScene(sc);
        s.setTitle("Plasmoxy::ThunderLord - VisionAlpha | by Sebo Petrík");
        s.setResizable(true);
        s.sizeToScene();
        s.show();
        s.setMinHeight(s.getHeight());
        s.setMinWidth(s.getWidth());

        s.setOnCloseRequest((new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we)
            {
                System.out.println("CLOSING");
                contr.setClosed();
            }
        }));
        
    }

    public static void main(String[] args)
    {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch(args);
        System.out.println("EXIT");
    }
    
}
