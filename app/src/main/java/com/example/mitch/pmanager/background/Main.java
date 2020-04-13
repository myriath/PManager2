//package com.example.mitch.pmanager;
//
//import javax.crypto.NoSuchPaddingException;
//import javax.swing.*;
//import javax.swing.border.TitledBorder;
//import javax.swing.table.DefaultTableModel;
//import java.awt.*;
//import java.awt.datatransfer.Clipboard;
//import java.awt.datatransfer.StringSelection;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.File;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class Main extends JFrame {
//
//    private JTextField textFieldFilename, textFieldSort, textFieldDelete, textFieldTitleAdd, textFieldArtistAdd, textFieldRatingAdd, textFieldPassword;
//    private String filename, password;
//    private DefaultTableModel model;
//    private List<PasswordEntry> fileData = new ArrayList<>();
//
//    private Main() {
//        createView();
//        setTitle("Password Manager");
//        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        setSize(new Dimension(614, 660));
//        setLocationRelativeTo(null);
//        setResizable(false);
//    }
//
//    private void clearTable() {
//        while (model.getRowCount() > 0) {
//            model.removeRow(0);
//        }
//    }
//
//    private void createView() {
//        Container contentPane = getContentPane();
//        contentPane.setLayout(null);
//        contentPane.add(constructPanel());
//        contentPane.add(constructFilterPanel());
//        contentPane.add(constructAddPanel());
//        contentPane.add(constructExtraPanel());
//    }
//
//    private JPanel constructExtraPanel() {
//        TitledBorder borderExtra = new TitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1), "Copy and Delete");
//
//        JPanel extraPanel = new JPanel();
//        extraPanel.setLayout(null);
//        extraPanel.setBounds(4, 555, 600, 63);
//        extraPanel.setBorder(borderExtra);
//        extraPanel.setVisible(true);
//
//        JLabel labelIndex = new JLabel();
//        labelIndex.setText("Index:");
//        labelIndex.setBounds(10, 20, 100, 30);
//        extraPanel.add(labelIndex);
//
//        JButton buttonSave = new JButton("Save");
//        buttonSave.setBounds(485, 20, 105, 29);
//        buttonSave.addActionListener(new ButtonSaveListen());
//        buttonSave.setToolTipText("This button saves any changes to the file.");
//        extraPanel.add(buttonSave);
//
//        JButton buttonDelete = new JButton("Delete");
//        buttonDelete.setBounds(360, 20, 125, 29);
//        buttonDelete.addActionListener(new ButtonDeleteListen());
//        buttonDelete.setToolTipText("This button deletes the entry with the index of the number to the left.");
//        extraPanel.add(buttonDelete);
//
//        JButton buttonCopy = new JButton("Copy Password");
//        buttonCopy.setBounds(235, 20, 125, 29);
//        buttonCopy.addActionListener(new CopyButtonListener());
//        buttonCopy.setToolTipText("This button copies the password of the entry to the clipboard.");
//        extraPanel.add(buttonCopy);
//
//        JButton buttonCopyUser = new JButton("Copy Username");
//        buttonCopyUser.setBounds(110, 20, 125, 29);
//        buttonCopyUser.addActionListener(new CopyUserButtonListener());
//        buttonCopyUser.setToolTipText("This button copies the password of the entry to the clipboard.");
//        extraPanel.add(buttonCopyUser);
//
//        textFieldDelete = new JTextField();
//        textFieldDelete.setBounds(60, 20, 40, 30);
//        textFieldDelete.setToolTipText("Here goes the index (Number inside [ ] next to the entry) of the entry you want to delete.");
//        extraPanel.add(textFieldDelete);
//
//        return extraPanel;
//    }
//
//    private JPanel constructAddPanel() {
//        TitledBorder borderAdd = new TitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1), "Add Entry");
//
//        JPanel addPanel = new JPanel();
//        addPanel.setLayout(null);
//        addPanel.setBounds(4, 430, 600, 120);
//        addPanel.setBorder(borderAdd);
//        addPanel.setVisible(true);
//
//        JLabel labelTitle = new JLabel();
//        labelTitle.setText("Domain :");
//        labelTitle.setBounds(10, 20, 100, 30);
//        addPanel.add(labelTitle);
//
//        JLabel labelArtist = new JLabel();
//        labelArtist.setText("Username:");
//        labelArtist.setBounds(10, 48, 100, 30);
//        addPanel.add(labelArtist);
//
//        JLabel labelRating = new JLabel();
//        labelRating.setText("Password:");
//        labelRating.setBounds(10, 76, 100, 30);
//        addPanel.add(labelRating);
//
//        JButton buttonAdd = new JButton("+");
//        buttonAdd.setBounds(505, 20, 85, 85);
//        buttonAdd.setFont(new Font("Arial", Font.BOLD, 40));
//        buttonAdd.addActionListener(new ButtonAddListen());
//        buttonAdd.setToolTipText("This button adds the entry with the data defined above to the file.");
//        addPanel.add(buttonAdd);
//
//        textFieldTitleAdd = new JTextField();
//        textFieldTitleAdd.setBounds(80, 20, 425, 30);
//        textFieldTitleAdd.setToolTipText("Here goes the domain of the entry you want to add.");
//        addPanel.add(textFieldTitleAdd);
//
//        textFieldArtistAdd = new JTextField();
//        textFieldArtistAdd.setBounds(80, 48, 425, 30);
//        textFieldArtistAdd.setToolTipText("Here goes the username of the entry you want to add.");
//        addPanel.add(textFieldArtistAdd);
//
//        textFieldRatingAdd = new JTextField();
//        textFieldRatingAdd.setBounds(80, 76, 425, 30);
//        textFieldRatingAdd.setToolTipText("Here goes the password of the entry you want to add.");
//        addPanel.add(textFieldRatingAdd);
//
//        return addPanel;
//    }
//
//    private JPanel constructFilterPanel() {
//        TitledBorder borderFilter = new TitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1), "Filter");
//
//        JPanel filterPanel = new JPanel();
//        filterPanel.setLayout(null);
//        filterPanel.setBounds(4, 373, 600, 54);
//        filterPanel.setBorder(borderFilter);
//        filterPanel.setVisible(true);
//
//        JButton buttonReset = new JButton("Clear");
//        buttonReset.setBounds(523, 15, 67, 29);
//        buttonReset.addActionListener(new ButtonResetListener());
//        buttonReset.setToolTipText("This button shows all entries in the file again.");
//        filterPanel.add(buttonReset);
//
//        JButton buttonSortTitle = new JButton("Domain");
//        buttonSortTitle.setBounds(243, 15, 80, 29);
//        buttonSortTitle.addActionListener(new ButtonSortDomainListen());
//        buttonSortTitle.setToolTipText("This button sorts the file by the domain above.");
//        filterPanel.add(buttonSortTitle);
//
//        JButton buttonSortArtist = new JButton("Username");
//        buttonSortArtist.setBounds(323, 15, 100, 29);
//        buttonSortArtist.addActionListener(new ButtonSortUsernameListen());
//        buttonSortArtist.setToolTipText("This button sorts the file by the username above.");
//        filterPanel.add(buttonSortArtist);
//
//        JButton buttonLessThan = new JButton("Password");
//        buttonLessThan.setBounds(423, 15, 100, 29);
//        buttonLessThan.addActionListener(new ButtonPasswordSortListen());
//        buttonLessThan.setToolTipText("This button sorts the file by the password above.");
//        filterPanel.add(buttonLessThan);
//
//        textFieldSort = new JTextField();
//        textFieldSort.setBounds(11, 15, 232, 30);
//        textFieldSort.setToolTipText("This is either the domain, username, or password you want to filter by.");
//        filterPanel.add(textFieldSort);
//
//        TextPrompt sorttp = new TextPrompt("Filter...", textFieldSort, TextPrompt.Show.FOCUS_LOST);
//        sorttp.changeAlpha(200);
//        sorttp.setShowPromptOnce(true);
//
//        return filterPanel;
//    }
//
//    private JPanel constructPanel() {
//        JPanel panel = new JPanel();
//        panel.setLayout(null);
//        panel.setBounds(3, 0, 608, 375);
//        panel.setVisible(true);
//
//        model = new DefaultTableModel(new Object[]{"Index", "Domain", "Username", "Password"}, 0);
//        JTable readData = new JTable(model);
//        readData.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
//        readData.getColumnModel().getColumn(0).setPreferredWidth(1);
//        readData.getColumnModel().getColumn(1).setPreferredWidth(1);
//        readData.setDefaultEditor(Object.class, null);
//        readData.setAutoCreateRowSorter(true);
//        JScrollPane readDataScroll = new JScrollPane(readData);
//        readDataScroll.setBounds(10, 70, 583, 300);
//        readDataScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//        readDataScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        readDataScroll.setVisible(false);
//        readDataScroll.setWheelScrollingEnabled(true);
//        readData.setVisible(true);
//        readDataScroll.setVisible(true);
//        readData.setToolTipText("This is where the data from the file is shown.");
//        panel.add(readDataScroll);
//
//        JLabel labelReadData = new JLabel();
//        labelReadData.setText("File Data:");
//        labelReadData.setBounds(10, 45, 100, 25);
//        panel.add(labelReadData);
//
//        JButton buttonFilename = new JButton("Open File");
//        buttonFilename.addActionListener(new ButtonFilenameListener());
//        buttonFilename.setToolTipText("Opens file with the name and password of the fields to the right.");
//        buttonFilename.setBounds(494, 10, 100, 29);
//        panel.add(buttonFilename);
//
//        JButton button = new JButton();
//        button.setBounds(10, 40, 5, 5);
//        button.addActionListener(new ButtonListener());
////        panel.add(button);
//
//        textFieldPassword = new JTextField();
//        textFieldPassword.setBounds(252, 10, 232, 30);
//        panel.add(textFieldPassword);
//
//        textFieldFilename = new JTextField();
//        textFieldFilename.setBounds(10, 10, 232, 30);
//        panel.add(textFieldFilename);
//
//        TextPrompt filenametp = new TextPrompt("Filename", textFieldFilename, TextPrompt.Show.FOCUS_GAINED);
//        filenametp.changeAlpha(200);
//        filenametp.setShowPromptOnce(true);
//
//        TextPrompt passwordtp = new TextPrompt("Password", textFieldPassword, TextPrompt.Show.FOCUS_LOST);
//        passwordtp.changeAlpha(200);
//        passwordtp.setShowPromptOnce(true);
//
//        return panel;
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
//    }
//
//    private class ButtonFilenameListener implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            filename = textFieldFilename.getText() + ".jpweds";
//            password = textFieldPassword.getText();
//            File tmp = new File(System.getenv("APPDATA") + "/PManager/" + filename);
//            boolean exists = tmp.exists();
//            try {
//                if (exists) {
//                    textFieldPassword.setText("Opened!");
//                    LibraryFile f = new LibraryFile(filename);
//                    AES decrypt = new AES(AES.pad(password));
//                    String decrypted = decrypt.decrypt(filename);
//                    String[] splitFile = decrypted.split(System.lineSeparator());
//                    fileData = f.read(password);
//                    clearTable();
//                    if (splitFile[0].equals(filename)) {
//                        for (PasswordEntry entry : fileData) {
//                            model.addRow(new Object[]{entry.index, entry.domain, entry.username, entry.password});
//                        }
//                    } else {
//                        textFieldPassword.setText("Wrong Password!");
//                    }
//                } else {
//                    AES newFile = new AES(AES.pad(password));
//                    newFile.encryptString(filename, filename);
//                    textFieldPassword.setText("Created new file, reenter pass to open");
//                }
//            } catch (Exception e1) {
//                textFieldPassword.setText("Wrong Password!");
//                e1.printStackTrace();
//            }
//        }
//    }
//
//    private class ButtonAddListen implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            String domain = textFieldTitleAdd.getText();
//            String username = textFieldArtistAdd.getText();
//            String password = textFieldRatingAdd.getText();
//            PasswordEntry entry = new PasswordEntry(domain, username, password, fileData.size() + 1);
//            fileData.add(entry);
//            try {
//                model.addRow(new Object[]{entry.index, entry.domain, entry.username, entry.password});
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//        }
//    }
//
//    private class ButtonSaveListen implements ActionListener {
//
//        private void save() {
//            List<String> dat = new ArrayList<>();
//            dat.add(filename);
//            for (PasswordEntry entry : fileData) {
//                dat.add(entry.domain);
//                dat.add(entry.username);
//                dat.add(entry.password);
//            }
//            StringBuilder sb = new StringBuilder();
//            for (String str : dat) {
//                sb.append(str);
//                sb.append('\0');
//            }
//            AES file;
//            try {
//                file = new AES(AES.pad(password));
//                file.encryptString(sb.toString(), filename);
//                if (!file.decrypt(filename).split(System.lineSeparator())[0].equals(filename)) {
//                    save();
//                }
//            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
//                e1.printStackTrace();
//            }
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            save();
//        }
//    }
//
//    private class ButtonSortDomainListen implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            String sortTitle = textFieldSort.getText();
//            clearTable();
//            for (PasswordEntry entry : fileData) {
//                if (entry.domain.equals(sortTitle)) {
//                    model.addRow(new Object[]{entry.index, entry.domain, entry.username, entry.password});
//                }
//            }
//        }
//    }
//
//    private class ButtonSortUsernameListen implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            String sortUsername = textFieldSort.getText();
//            clearTable();
//            for (PasswordEntry entry : fileData) {
//                if (entry.username.equals(sortUsername)) {
//                    model.addRow(new Object[]{entry.index, entry.domain, entry.username, entry.password});
//                }
//            }
//        }
//    }
//
//    private class ButtonPasswordSortListen implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            String sortPassword = textFieldSort.getText();
//            clearTable();
//            for (PasswordEntry entry : fileData) {
//                if (entry.password.equals(sortPassword)) {
//                    model.addRow(new Object[]{entry.index, entry.domain, entry.username, entry.password});
//                }
//            }
//        }
//    }
//
//    private class ButtonResetListener implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            clearTable();
//            for (PasswordEntry entry : fileData) {
//                model.addRow(new Object[]{entry.index, entry.domain, entry.username, entry.password});
//            }
//        }
//    }
//
//    private class ButtonDeleteListen implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            List<PasswordEntry> temp = new ArrayList<>();
//            clearTable();
//            int i = Integer.parseInt(textFieldDelete.getText());
//            for (PasswordEntry entry : fileData) {
//                if (entry.index != i) {
//                    if (entry.index > i) {
//                        entry.index--;
//                    }
//                    model.addRow(new Object[]{entry.index, entry.domain, entry.username, entry.password});
//                    temp.add(entry);
//                }
//            }
//            fileData = temp;
//        }
//    }
//
//    private class CopyButtonListener implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            StringSelection data = new StringSelection(fileData.get(Integer.parseInt(textFieldDelete.getText()) - 1).password);
//            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
//            cb.setContents(data, data);
//        }
//    }
//
//    private class CopyUserButtonListener implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            StringSelection data = new StringSelection(fileData.get(Integer.parseInt(textFieldDelete.getText()) - 1).username);
//            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
//            cb.setContents(data, data);
//        }
//    }
//
//    private class ButtonListener implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            System.out.println(getSize());
//        }
//    }
//}
