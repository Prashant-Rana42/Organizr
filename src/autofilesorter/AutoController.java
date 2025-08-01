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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class AutoController implements Initializable {
    @FXML private TextField path;
    @FXML private ImageView add;
    @FXML private ImageView remove;
    @FXML private Button back;
    @FXML private TableView<String> folders;
    @FXML private TableColumn<String, String> foldername;

    @FXML private TableView<FileMapping> fileMapping;
    @FXML private TableColumn<FileMapping, String> extension;
    @FXML private TableColumn<FileMapping, String> mappingpath;

    @FXML private TextField extensiontxt;
    @FXML private ImageView addextensionfolder;
    @FXML private ImageView removeextensionfolder;
    @FXML private TextField foldermappingpathtxt;

    private ObservableList<String> folderList = FXCollections.observableArrayList();
    private ObservableList<FileMapping> mappingList = FXCollections.observableArrayList();

    private final String folderSaveFile = "monitored_paths.txt";
    private final String mappingSaveFile = "file_mappings.txt";

    @Override
    public void initialize(URL u, ResourceBundle r) {
        // Folders table setup
        foldername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        folders.setItems(folderList);

        // FileMapping table setup
        fileMapping.setEditable(true);
        extension.setCellValueFactory(c -> c.getValue().extensionProperty());
        mappingpath.setCellValueFactory(c -> c.getValue().folderPathProperty());
        extension.setCellFactory(TextFieldTableCell.forTableColumn());
        mappingpath.setCellFactory(TextFieldTableCell.forTableColumn());

        // Inline edit commit handlers
        extension.setOnEditCommit(e -> {
            e.getRowValue().setExtension(e.getNewValue().trim());
            persistMappings();
        });
        mappingpath.setOnEditCommit(e -> {
            e.getRowValue().setFolderPath(e.getNewValue().trim());
            persistMappings();
        });

        // Bind list to table
        fileMapping.setItems(mappingList);
        mappingList.addListener((ListChangeListener<FileMapping>) change -> persistMappings());

        // Add new mapping via textfields
        addextensionfolder.setOnMouseClicked(e -> {
            String ext = extensiontxt.getText().trim();
            String fp  = foldermappingpathtxt.getText().trim();
            boolean exists = mappingList.stream()
                .anyMatch(m -> m.getExtension().equalsIgnoreCase(ext));
            if (!ext.isEmpty() && !fp.isEmpty() && !exists) {
                mappingList.add(new FileMapping(ext, fp));
                extensiontxt.clear();
                foldermappingpathtxt.clear();
            }
        });

        // Remove selected mapping
        removeextensionfolder.setOnMouseClicked(e -> {
            FileMapping sel = fileMapping.getSelectionModel().getSelectedItem();
            if (sel != null) {
                mappingList.remove(sel);
            }
        });

        // Load saved folders and mappings
        loadFoldersAndStart();
        loadMappings();

        // Apply mappings
        FileMonitor.getInstance().setExtensionMappings(
            mappingList.stream()
                .filter(m -> !m.getExtension().isEmpty() && !m.getFolderPath().isEmpty())
                .collect(Collectors.toMap(FileMapping::getExtension, FileMapping::getFolderPath))
        );

        // Folder add/remove handlers
        add.setOnMouseClicked(e -> {
            String ip = path.getText().trim();
            if (!ip.isEmpty() && !folderList.contains(ip)) {
                folderList.add(ip);
                saveFolders();
                process(ip);
                FileMonitor.getInstance().watchDirectory(Paths.get(ip));
            }
        });
        remove.setOnMouseClicked(e -> {
            String s = folders.getSelectionModel().getSelectedItem();
            if (s != null) {
                folderList.remove(s);
                saveFolders();
            }
        });

        back.setOnAction(e -> ((Stage) back.getScene().getWindow()).close());
    }

    public void loadFoldersAndStart() {
        File f = new File(folderSaveFile);
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                List<String> list = r.lines().collect(Collectors.toList());
                folderList.clear();
                folderList.addAll(list);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        folderList.forEach(this::process);
        folderList.forEach(p -> FileMonitor.getInstance().watchDirectory(Paths.get(p)));
    }

    private void saveFolders() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(folderSaveFile))) {
            for (String p : folderList) {
                w.write(p);
                w.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void persistMappings() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(mappingSaveFile))) {
            for (FileMapping m : mappingList) {
                String ext = m.getExtension().trim();
                String fp  = m.getFolderPath().trim();
                if (!ext.isEmpty() && !fp.isEmpty()) {
                    w.write(ext + "," + fp);
                    w.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileMonitor.getInstance().setExtensionMappings(
            mappingList.stream()
                .filter(m -> !m.getExtension().isEmpty() && !m.getFolderPath().isEmpty())
                .collect(Collectors.toMap(FileMapping::getExtension, FileMapping::getFolderPath))
        );
    }

    private void loadMappings() {
        File f = new File(mappingSaveFile);
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                mappingList.clear();
                String line;
                while ((line = r.readLine()) != null) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        String ext = parts[0].trim();
                        String pth = parts[1].trim();
                        if (!ext.isEmpty() && !pth.isEmpty()) {
                            mappingList.add(new FileMapping(ext, pth));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

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
}
