package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JTypeWriterAppTests {

    @Test
    void hasMainMethod() {
        var hasMain = Arrays.stream(JTypeWriterApp.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("main"));
        assertTrue(hasMain);
    }
}
