# Organizr

Organizr is a Java and JavaFX desktop application that organizes files based on their type. With just a folder path, the app creates separate folders for file types like images, videos, documents, etc., and sorts everything automatically.

---

## 📌 Features

- **One-Time Runner Mode:**  
  Enter a folder path and click **"GO"** — the app instantly organizes all files into categorized subfolders.

- **Auto Sorter Mode (Background Mode):**  
  Continuously monitors the folder for new files and auto-sorts them into relevant folders.  
  - Runs in the background even after closing the window.  
  - Can be terminated manually via Task Manager.  
  - Automatically stops when the system shuts down.

---

## 🖼 Interface

- Built using **JavaFX** for a modern, responsive UI.
- Two main windows:
  - **Main Window** for one-time manual sorting.
  - **Auto Sorter Window** for background sorting functionality.

---

## 🛠 Technologies Used

- Java
- JavaFX
- Netbeans IDE
- To store path for Auto sorter i used a simple text file and read write operations in java.

---

## 🗂 How It Works

1. User inputs the path of the folder to be sorted.
2. The application scans all files and detects their types.
3. It creates subfolders such as:
   - `/Images`
   - `/Videos`
   - `/Documents`
   - `/Audio`
   - `/Archives`
4. Each file is moved to its respective folder.
5. In Auto Mode, the app runs silently in the background and keeps sorting new files in real-time.

---

## 🚀 Getting Started

1. Download the latest release.
2. Run Organizer-Installer.exe and extract content in desired location.
3. Open folder and run Organizr.exe
4. Enter path to the folder to be sorted and click "GO" or add folder path in Auto mode for continuous scanning

---

## 📋 Note

1. Auto sorter mode runs in background even if you close the window. You can close it from Task manager, it will appear as "OpenJDK Platform binary" in Task manage, click on it and then click on "End task" to stop auto Sorter or it wil also terminate on shutdown.
