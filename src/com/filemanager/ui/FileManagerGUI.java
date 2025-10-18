// basic funtion file management simulator
package com.filemanager.ui;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Stack;

public class FileManagerGUI extends JFrame {
    private JList<File> fileList;
    private DefaultListModel<File> listModel;
    private File currentDirectory;
    private JTextField searchField;
    private JTextArea propertiesArea;

    private Stack<File> backStack = new Stack<>();
    private Stack<File> forwardStack = new Stack<>();

    public FileManagerGUI() {
        setTitle("Mini File Management simulator");
        setSize(900, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        currentDirectory = new File(System.getProperty("user.home"));

        // File list
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setCellRenderer(new FileListRenderer());

        // 🔹 Properties panel
        propertiesArea = new JTextArea();
        propertiesArea.setEditable(false);
        propertiesArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane propertiesScroll = new JScrollPane(propertiesArea);
        propertiesScroll.setPreferredSize(new Dimension(300, 0));

        // Split pane
        JScrollPane scrollPane = new JScrollPane(fileList);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, propertiesScroll);
        splitPane.setDividerLocation(600);

        loadFiles(currentDirectory);

        // Double-click open
        fileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    openSelectedFile();
                }
            }
        });

        // Single-click → show properties
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showProperties(fileList.getSelectedValue());
            }
        });

        // Top bar: Back + Next + Search
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton backBtn = new JButton("← Back");
        JButton nextBtn = new JButton("→ Next");
        navPanel.add(backBtn);
        navPanel.add(nextBtn);
        topPanel.add(navPanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField();
        JButton searchBtn = new JButton("Search");
        JButton resetBtn = new JButton("Reset");

        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel();
        searchButtons.add(searchBtn);
        searchButtons.add(resetBtn);
        searchPanel.add(searchButtons, BorderLayout.EAST);

        topPanel.add(searchPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // Bottom panel buttons
        JPanel bottomPanel = new JPanel();
        JButton openBtn = new JButton("Open");
        JButton createBtn = new JButton("New File");
        JButton deleteBtn = new JButton("Delete");
        JButton renameBtn = new JButton("Rename");
        JButton refreshBtn = new JButton("Refresh");

        bottomPanel.add(openBtn);
        bottomPanel.add(createBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(renameBtn);
        bottomPanel.add(refreshBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        // Action Listeners
        backBtn.addActionListener(e -> goBack());
        nextBtn.addActionListener(e -> goForward());
        openBtn.addActionListener(e -> openSelectedFile());
        createBtn.addActionListener(e -> createNewFile());
        deleteBtn.addActionListener(e -> deleteSelectedFile());
        renameBtn.addActionListener(e -> renameSelectedFile());
        refreshBtn.addActionListener(e -> loadFiles(currentDirectory));

        searchBtn.addActionListener(e -> searchFiles());
        resetBtn.addActionListener(e -> loadFiles(currentDirectory));
    }

    private void loadFiles(File dir) {
        listModel.clear();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                listModel.addElement(f);
            }
        }
        propertiesArea.setText("");
    }

    private void goBack() {
        if (!backStack.isEmpty()) {
            forwardStack.push(currentDirectory);
            currentDirectory = backStack.pop();
            loadFiles(currentDirectory);
        }
    }

    private void goForward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(currentDirectory);
            currentDirectory = forwardStack.pop();
            loadFiles(currentDirectory);
        }
    }

    private File checkSelection() {
        File file = fileList.getSelectedValue();
        if (file == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Please select a file first!", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
        return file;
    }

    private void openSelectedFile() {
        File file = checkSelection();
        if (file != null) {
            if (file.isDirectory()) {
                backStack.push(currentDirectory);
                currentDirectory = file;
                forwardStack.clear(); // clear forward history on new navigation
                loadFiles(file);
            } else {
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Cannot open file.");
                }
            }
        }
    }

    private void createNewFile() {
        String name = JOptionPane.showInputDialog(this, "Enter new file name:");
        if (name != null && !name.trim().isEmpty()) {
            try {
                File newFile = new File(currentDirectory, name);
                if (newFile.createNewFile()) {
                    loadFiles(currentDirectory);
                } else {
                    JOptionPane.showMessageDialog(this, "File creation failed.");
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error creating file.");
            }
        }
    }

    private void deleteSelectedFile() {
        File file = checkSelection();
        if (file != null) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete: " + file.getName() + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                if (file.delete()) {
                    loadFiles(currentDirectory);
                } else {
                    JOptionPane.showMessageDialog(this, "Delete failed.");
                }
            }
        }
    }

    private void renameSelectedFile() {
        File file = checkSelection();
        if (file != null) {
            String newName = JOptionPane.showInputDialog(this, "Enter new name:", file.getName());
            if (newName != null && !newName.trim().isEmpty()) {
                File newFile = new File(currentDirectory, newName);
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Rename " + file.getName() + " to " + newName + "?",
                        "Confirm Rename",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    if (file.renameTo(newFile)) {
                        loadFiles(currentDirectory);
                    } else {
                        JOptionPane.showMessageDialog(this, "Rename failed.");
                    }
                }
            }
        }
    }

    private void searchFiles() {
        String query = searchField.getText().trim().toLowerCase();
        if (!query.isEmpty()) {
            listModel.clear();
            File[] files = currentDirectory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().toLowerCase().contains(query)) {
                        listModel.addElement(f);
                    }
                }
            }
        }
    }

    private void showProperties(File file) {
        if (file == null) {
            propertiesArea.setText("");
            return;
        }

        StringBuilder props = new StringBuilder();
        props.append("Name: ").append(file.getName()).append("\n");
        props.append("Path: ").append(file.getAbsolutePath()).append("\n");
        props.append("Type: ").append(file.isDirectory() ? "Folder" : "File").append("\n");
        props.append("Size: ").append(file.isDirectory() ? "-" : file.length() + " bytes").append("\n");
        props.append("Last Modified: ").append(
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(file.lastModified())
        ).append("\n");
        props.append("Readable: ").append(file.canRead()).append("\n");
        props.append("Writable: ").append(file.canWrite()).append("\n");
        props.append("Executable: ").append(file.canExecute()).append("\n");

        propertiesArea.setText(props.toString());
    }

    private static class FileListRenderer extends DefaultListCellRenderer {
        private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File) {
                File file = (File) value;
                setText(file.getName());
                setIcon(fileSystemView.getSystemIcon(file));
            }
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileManagerGUI().setVisible(true));
    }
}