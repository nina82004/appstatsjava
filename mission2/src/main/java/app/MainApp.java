package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Bienvenue !");
        Button bouton = new Button("Clique ici");
        bouton.setOnAction(e -> label.setText("Bravo, tu as cliqué !"));

        VBox root = new VBox(10, label, bouton);
        Scene scene = new Scene(root, 300, 200);

        primaryStage.setTitle("Ma première interface JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
