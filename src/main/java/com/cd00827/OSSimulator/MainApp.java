package com.cd00827.OSSimulator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class MainApp extends Application {

    /**
     * Called when the JavaFX application starts
     * @param stage JavaFX stage
     * @throws Exception Any exception thrown by the app
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("scene.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("bootstrap3.css").toExternalForm());

        Kernel kernel = loader.getController();
        kernel.setStage(stage);

        stage.setOnCloseRequest(e -> kernel.shutdown());
        stage.setTitle("OSSimulator");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Main entry point for the application
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
