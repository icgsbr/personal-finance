package com.finance;

import com.finance.config.JPAUtil;
import com.finance.ui.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainController mainController = new MainController();
        BorderPane root = mainController.getView();

        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Finanças Pessoais");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            JPAUtil.close();
            Platform.exit();
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
