package org.mesutormanli.jtypewriter.config;

import javafx.scene.text.Font;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Font editorFont() {
        return Font.font("JetBrains Mono", 16);
    }
}
