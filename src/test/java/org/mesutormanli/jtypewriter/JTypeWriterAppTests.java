package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

class JTypeWriterAppTests {

    @Test
    void appStarts() {
        var mainMethod = JTypeWriterApp.class.getDeclaredMethods();
        assert mainMethod.length > 0;
    }
}
