package org.mesutormanli.jtypewriter.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Rotating file backups protecting against corrupt overwrites: before a target
 * file is replaced, its previous content is copied to {@code <file>.bak}, the
 * older {@code <file>.bak.<n>} generations shift up and the oldest is dropped.
 * Free of UI types so it can be unit tested headlessly.
 */
public final class BackupKeeper {

    private static final int GENERATIONS = 3;

    /** Copies {@code target} to {@code <target>.bak}; no-op when it does not exist. */
    public void backup(Path target) throws IOException {
        if (!Files.isRegularFile(target)) {
            return;
        }
        for (int i = GENERATIONS - 1; i >= 1; i--) {
            var older = backupPath(target, i);
            var newer = backupPath(target, i - 1);
            if (Files.isRegularFile(newer)) {
                Files.move(newer, older, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.copy(target, backupPath(target, 0), StandardCopyOption.REPLACE_EXISTING);
    }

    /** @param generation 0 is the newest backup ({@code .bak}), 1 the previous one, ... */
    public static Path backupPath(Path target, int generation) {
        var name = target.getFileName().toString();
        return target.resolveSibling(generation == 0 ? name + ".bak" : name + ".bak." + generation);
    }
}