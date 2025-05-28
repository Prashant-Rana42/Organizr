package autofilesorter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class GuiController implements Initializable {
    @FXML private TextField path;
    @FXML private Button start;
    @FXML private ProgressIndicator loading;
    @FXML private Label success;
    @FXML private Button setting;

    private boolean isRunning = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    loading.setVisible(false);
    success.setVisible(false);

    start.setOnAction(e -> handleStart());
    setting.setOnAction(e -> openSettings());
}

    private void handleStart() {
    String dir = path.getText().trim();
    if (dir.isEmpty()) { showMessage("Empty Path", true); return; }
    File folder = new File(dir);
    if (!folder.exists() || !folder.isDirectory()) { showMessage("Invalid Path", true); return; }

    if (!isRunning) {
        isRunning = true;

        // Show progress indicator, hide success label
        Platform.runLater(() -> {
            loading.setVisible(true);
            success.setVisible(false);
        });

        new Thread(() -> {
            FileMonitor monitor = FileMonitor.getInstance();
            // Sort existing files
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder.toPath())) {
                for (Path p : ds) {
                    if (Files.isRegularFile(p)) {
                        monitor.sortFile(p, folder.toPath());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                showMessage("Error during sorting.", true);
                isRunning = false;
                return;
            }

            // Watch directory for new files
            monitor.watchDirectory(folder.toPath());

            // Done sorting — hide progress, show success message
            Platform.runLater(() -> {
                loading.setVisible(false);
                success.setText("Files sorted!");
                success.setStyle("-fx-text-fill:black;");
                success.setVisible(true);
            });

            isRunning = false;
        }).start();
    }
}

    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/autofilesorter/auto.fxml"));

            Parent root = loader.load();
            
            AutoController autoController = loader.getController();
            autoController.loadFoldersAndStart();
        
            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showMessage("Failed to load settings.", true);
        }
    }

    private void showMessage(String msg, boolean error) {
    Platform.runLater(() -> {
        // Don't hide loading here, only show message label
        success.setText(msg);
        success.setStyle(error ? "-fx-text-fill:black;" : "-fx-text-fill:black;");
        success.setVisible(true);
    });
}

}
