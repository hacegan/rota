package dev.rota.solver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes solve reports to {@code solver/target/p03/<fixture>-report.txt} for scripts/verify_p03.ps1.
 */
final class Reports {

    private Reports() {
    }

    static Path write(String fixture, String report) {
        try {
            Path dir = Path.of("target", "p03");
            Files.createDirectories(dir);
            Path file = dir.resolve(fixture + "-report.txt");
            Files.writeString(file, report);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
