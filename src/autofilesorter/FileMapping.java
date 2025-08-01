/*
 * File: FileMapping.java
 * Model class for mapping file extensions to folder paths.
 */
package autofilesorter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FileMapping {
    private final StringProperty extension;
    private final StringProperty folderPath;

    public FileMapping(String extension, String folderPath) {
        this.extension = new SimpleStringProperty(extension);
        this.folderPath = new SimpleStringProperty(folderPath);
    }

    public String getExtension() {
        return extension.get();
    }
    public void setExtension(String ext) {
        extension.set(ext);
    }
    public StringProperty extensionProperty() {
        return extension;
    }

    public String getFolderPath() {
        return folderPath.get();
    }
    public void setFolderPath(String path) {
        folderPath.set(path);
    }
    public StringProperty folderPathProperty() {
        return folderPath;
    }
}
