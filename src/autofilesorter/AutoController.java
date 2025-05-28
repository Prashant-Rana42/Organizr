package autofilesorter;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class AutoController implements Initializable {
    @FXML private TextField path;
    @FXML private ImageView add;
    @FXML private ImageView remove;
    @FXML private Button back;
    @FXML private TableView<String> folders;
    @FXML private TableColumn<String, String> foldername;

    private ObservableList<String> folderList = FXCollections.observableArrayList();
    private final String saveFile = "monitored_paths.txt";

    @Override
    public void initialize(URL u, ResourceBundle r) {
        foldername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        folders.setItems(folderList);

        // Removed load() here to avoid loading multiple times; 
        // loading and starting monitoring will be done from GuiController on app start

        add.setOnMouseClicked(e -> {
            String ip = path.getText().trim();
            if (!ip.isEmpty() && !folderList.contains(ip)) {
                folderList.add(ip);
                save();
                process(ip);
                FileMonitor.getInstance().watchDirectory(Paths.get(ip));
            }
        });

        remove.setOnMouseClicked(e -> {
            String s = folders.getSelectionModel().getSelectedItem();
            if (s != null) {
                folderList.remove(s);
                save();
                // TODO: Stop watching directory? Implement if needed in FileMonitor
            }
        });

        back.setOnAction(e -> {
            try {
               Parent root = FXMLLoader.load(getClass().getResource("/autofilesorter/gui.fxml"));
                Stage st = (Stage) back.getScene().getWindow();
                st.setScene(new Scene(root));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        back.setOnAction(e -> {
    Stage stage = (Stage) back.getScene().getWindow();
    stage.close();
});

    }

    /**
     * Called by GuiController after loading to load saved folders
     * and start monitoring existing files and directory watchers.
     */
    public void loadFoldersAndStart() {
        load();
        folderList.forEach(this::process);
        folderList.forEach(p -> FileMonitor.getInstance().watchDirectory(Paths.get(p)));
    }

    /**
     * Process all files currently in directory by sorting them.
     * @param dir directory path as string
     */
    void process(String dir) {
        Path p = Paths.get(dir);
        if (Files.isDirectory(p)) {
            try {
                Files.list(p)
                     .filter(Files::isRegularFile)
                     .forEach(f -> FileMonitor.getInstance().sortFile(f, p));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads monitored folders from a saved file.
     */
    void load() {
        File f = new File(saveFile);
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                List<String> l = r.lines().collect(Collectors.toList());
                folderList.clear();
                folderList.addAll(l);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves monitored folders to a file.
     */
    void save() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(saveFile))) {
            for (String p : folderList) {
                w.write(p);
                w.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
