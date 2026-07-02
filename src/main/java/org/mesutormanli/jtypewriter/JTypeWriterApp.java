package org.mesutormanli.jtypewriter;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "org.mesutormanli.jtypewriter")
public class JTypeWriterApp {

    public static void main(String[] args) {
        Application.launch(SpringBootFxApplication.class, args);
    }
}
