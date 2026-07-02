package org.mesutormanli.jtypewriter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.ui.WelcomeDialog;
import org.mesutormanli.jtypewriter.ui.theme.Theme;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringBootFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        applicationContext = SpringApplication.run(JTypeWriterApp.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var resource = getClass().getResource("/fxml/main.fxml");
        if (resource == null) {
            throw new IllegalStateException("main.fxml not found");
        }
        var loader = new FXMLLoader(resource);
        loader.setControllerFactory(applicationContext::getBean);

        Parent root = loader.load();

        var scene = new Scene(root);
        primaryStage.setTitle("JTypeWriter");
        primaryStage.setScene(scene);

        var themeManager = applicationContext.getBean(ThemeManager.class);
        themeManager.applyTheme(scene, Theme.DARK);

        var mainController = applicationContext.getBean("mainController");
        var getStageMethod = mainController.getClass().getMethod("setStage", Stage.class);
        getStageMethod.invoke(mainController, primaryStage);

        var mainStage = applicationContext.getBean("mainStage");
        var configureMethod = mainStage.getClass().getMethod("configure", Stage.class, Scene.class);
        configureMethod.invoke(mainStage, primaryStage, scene);

        primaryStage.show();

        var welcomeDialog = applicationContext.getBean(WelcomeDialog.class);
        welcomeDialog.show(primaryStage);
    }

    @Override
    public void stop() {
        applicationContext.close();
    }
}
