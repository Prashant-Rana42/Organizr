package autofilesorter;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;

public class FileMonitor {
    private static FileMonitor instance;
    private final WatchService watchService;
    private final Map<WatchKey, Path> keyPathMap = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Mappings will be set at runtime by AutoController
    private final Map<String, String> extensionToFolder = new HashMap<>();

    private FileMonitor() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        startWatcherThread();
    }

    public static synchronized FileMonitor getInstance() {
        if (instance == null) {
            try {
                instance = new FileMonitor();
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize FileMonitor", e);
            }
        }
        return instance;
    }

    /** Replace current mappings with new ones. */
    public void setExtensionMappings(Map<String, String> map) {
        extensionToFolder.clear();
        extensionToFolder.putAll(map);
    }

    public void watchDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            System.err.println("Not a directory: " + dir);
            return;
        }
        try {
            WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            keyPathMap.put(key, dir);
            System.out.println("Started watching: " + dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startWatcherThread() {
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take();
                    Path dir = keyPathMap.get(key);
                    if (dir == null) {
                        key.reset();
                        continue;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fullPath = dir.resolve(ev.context());
                        if (Files.isRegularFile(fullPath)) {
                            sortFile(fullPath, dir);
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException | ClosedWatchServiceException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void sortFile(Path filePath, Path baseDir) {
        String fileName = filePath.getFileName().toString();
        String ext = getExtension(fileName);
        if (ext == null) return;

        // Wait until file is fully written
        try {
            for (int i = 0; i < 10; i++) {
                if (Files.exists(filePath)) {
                    try { Files.newInputStream(filePath).close(); break; }
                    catch (IOException e) { Thread.sleep(200); }
                } else { Thread.sleep(200); }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); return;
        }

        Path targetDir;
        if (extensionToFolder.containsKey(ext)) {
            targetDir = Paths.get(extensionToFolder.get(ext));
        } else {
            targetDir = baseDir.resolve(ext);
        }

        try {
            if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
            Files.move(filePath, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            Platform.runLater(() -> { /* update UI if needed */ });
            System.out.println("Moved: " + fileName + " -> " + targetDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot < 0 || dot == fileName.length() - 1) ? null
            : fileName.substring(dot + 1).toLowerCase();
    }

    public void shutdown() {
        try { watchService.close(); } catch (IOException e) { e.printStackTrace(); }
        executor.shutdownNow();
    }
}
