package org.apache.cloudstack.utils.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.FileUtil;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveUtilTest {

    private final List<Path> cleanupPaths = new ArrayList<>();

    @Before
    public void setup() {
        cleanupPaths.clear();
    }

    @After
    public void cleanup() {
        for (int i = cleanupPaths.size() - 1; i >= 0; i--) {
            Path path = cleanupPaths.get(i);
            try {
                if (Files.exists(path)) {
                    if (Files.isDirectory(path)) {
                        FileUtil.deleteRecursively(path);
                    } else {
                        Files.delete(path);
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void runPackPathTest(ArchiveUtil.ArchiveFormat archiveFormat, boolean shouldSucceed) {
        Path sourcePath = Path.of("source-path");
        Path archivePath = Path.of("test-archive." + archiveFormat.name().toLowerCase());
        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            scriptMock.when(() -> Script.getExecutableAbsolutePath(archiveFormat.getPackToolName()))
                    .thenReturn("/bin/" + archiveFormat.getPackToolName());
            scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), Mockito.any(String[].class)))
                    .thenReturn(shouldSucceed ? 0 : 1);
            boolean result = ArchiveUtil.packPath(archiveFormat, sourcePath, archivePath, 60);
            assertEquals(shouldSucceed, result);
        }
    }

    @Test
    public void packFilesAsTgzReturnsTrue() {
        runPackPathTest(ArchiveUtil.ArchiveFormat.TGZ, true);
    }

    @Test
    public void packFilesAsTgzReturnsFalse() {
        runPackPathTest(ArchiveUtil.ArchiveFormat.TGZ, false);
    }

    private void runExtractArchiveTest(ArchiveUtil.ArchiveFormat archiveFormat, boolean shouldSucceed) {
        Path archivePath = Path.of("test-archive." + archiveFormat.name().toLowerCase());
        Path destPath = Path.of("dest-path");
        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            scriptMock.when(() -> Script.getExecutableAbsolutePath(archiveFormat.getExtractToolName()))
                    .thenReturn("/bin/" + archiveFormat.getExtractToolName());
            scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), Mockito.any(String[].class)))
                    .thenReturn(shouldSucceed ? 0 : 1);
            boolean result = ArchiveUtil.extractToPath(archiveFormat, archivePath, destPath, 60);
            assertEquals(shouldSucceed, result);
        }
    }

    @Test
    public void extractTgzExtractsArchiveSuccessfully() {
        runExtractArchiveTest(ArchiveUtil.ArchiveFormat.TGZ, true);
    }

    @Test
    public void extractTgzReturnsFalseWhenExtractionFails() {
        runExtractArchiveTest(ArchiveUtil.ArchiveFormat.TGZ, false);
    }

    @Test
    public void extractZipExtractsArchiveSuccessfully() {
        runExtractArchiveTest(ArchiveUtil.ArchiveFormat.ZIP, true);
    }

    @Test
    public void extractZipReturnsFalseWhenExtractionFails() {
        runExtractArchiveTest(ArchiveUtil.ArchiveFormat.ZIP, false);
    }

    private void runSimpleArchivePackExtractTest(ArchiveUtil.ArchiveFormat archiveFormat) {
        if (archiveFormat.getPackToolName().equals(Script.getExecutableAbsolutePath(archiveFormat.getPackToolName())) ||
                (archiveFormat.getPackToolName().equals(archiveFormat.getExtractToolName()) &&
                        archiveFormat.getExtractToolName().equals(Script.getExecutableAbsolutePath(archiveFormat.getExtractToolName())))) {
            System.out.println("Skipping test as " + archiveFormat + " commands is not available");
            return;
        }
        Path sourcePath = null;
        String fileName = "testfile.txt";
        String randomFileContent = UUID.randomUUID().toString();
        try {
            sourcePath = Files.createTempDirectory(getClass().getSimpleName().toLowerCase());
            cleanupPaths.add(sourcePath);
            Path sourceFilePath = sourcePath.resolve(fileName);
            Files.createFile(sourceFilePath);
            Files.writeString(sourceFilePath, randomFileContent);
        } catch (IOException e) {
            fail("Failed to create temp directory with file for tests");
        }
        Path archivePath = Paths.get(System.getProperty("java.io.tmpdir"), "testarchive-" + UUID.randomUUID() +
                "." + archiveFormat.name().toLowerCase());
        boolean result = ArchiveUtil.packPath(archiveFormat, sourcePath, archivePath, 60);
        assertTrue(result);
        Files.exists(archivePath);
        cleanupPaths.add(archivePath);
        try {
            Path destinationPath = Files.createTempDirectory(getClass().getSimpleName().toLowerCase() + "-dest");
            cleanupPaths.add(destinationPath);
            assertTrue(ArchiveUtil.extractToPath(archiveFormat, archivePath, destinationPath, 60));
            Path extractedFilePath = destinationPath.resolve(fileName);
            assertTrue(Files.exists(extractedFilePath));
            String extractedFileContent = Files.readString(extractedFilePath);
            assertEquals(randomFileContent, extractedFileContent);
        } catch (IOException e) {
            fail("Failed to create temp directory with file for tests");
        }
    }

    @Test
    public void packExtractTgzForValidInputs() {
        runSimpleArchivePackExtractTest(ArchiveUtil.ArchiveFormat.TGZ);
    }

    @Test
    public void packExtractZipForValidInputs() {
        runSimpleArchivePackExtractTest(ArchiveUtil.ArchiveFormat.ZIP);
    }
}
