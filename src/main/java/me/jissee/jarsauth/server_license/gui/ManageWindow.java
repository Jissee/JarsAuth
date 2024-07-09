/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_license.gui;

import me.jissee.jarsauth.server_license.ServerLicense;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ManageWindow extends AbstractModWindow {
    private final Vector<String> data = new Vector<>();
    private static final String LICENSE_FOLDER = "licenses" + File.separator;
    private static final Dimension TEXT_FILTER_SIZE = new Dimension(380, 20);

    private final Label label_name = setupLabel_Name();
    private final TextField text_name = setupText_Name();
    private final JList<String> list_licenses = setupList_Licenses();
    private final JScrollPane scrollPane = setupScrollPane();

    private final JMenuBar menuBar = setupMenuBar();
    private final JMenu menu_file = setupMenu_File();
    private final JMenuItem item_new = setupItem_New();
    private final JMenuItem item_view = setupItem_View();
    private final JMenuItem item_delete = setupItem_Delete();

    private final JMenu menu_language = setupMenu_Language();
    private final JMenuItem item_cn = setupItem_CN();
    private final JMenuItem item_en = setupItem_EN();

    private FilenameFilter filter = null;

    private String NO_SELECTED = "";
    private String ERROR = "";
    private String CONFIRM = "";
    private Map<Integer, Runnable> keyOperations = new HashMap<>();

    public ManageWindow() {
        initialize();
    }

    private void initialize() {
        setSize(WINDOW_SIZE_WIDE);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        updateData();

        keyOperations.put(KeyEvent.VK_DELETE, this::removeSelectedFile);
        keyOperations.put(KeyEvent.VK_INSERT, () -> edit(true));
        keyOperations.put(KeyEvent.VK_HELP, () -> edit(true));
    }

    private JMenuBar setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        return menuBar;
    }

    private JMenu setupMenu_File() {
        JMenu menu = new JMenu();
        menuBar.add(menu);
        return menu;
    }

    private JMenuItem setupItem_New() {
        JMenuItem item = new JMenuItem();
        item.addActionListener(e -> edit(true));
        menu_file.add(item);
        return item;
    }

    private JMenuItem setupItem_View() {
        JMenuItem item = new JMenuItem();
        item.addActionListener(e -> edit(false));
        menu_file.add(item);
        return item;
    }

    private JMenuItem setupItem_Delete() {
        JMenuItem item = new JMenuItem();
        item.addActionListener(e -> removeSelectedFile());
        menu_file.add(item);
        return item;
    }

    private JMenu setupMenu_Language() {
        JMenu menu = new JMenu();
        menuBar.add(menu);
        return menu;
    }

    private JMenuItem setupItem_CN() {
        JMenuItem item = new JMenuItem();
        item.addActionListener(WindowManager.manager::changeAllWindows2cn);
        menu_language.add(item);
        return item;
    }

    private JMenuItem setupItem_EN() {
        JMenuItem item = new JMenuItem();
        item.addActionListener(WindowManager.manager::changeAllWindows2en);
        menu_language.add(item);
        return item;
    }


    public void updateData() {
        data.clear();
        File folder = new File(LICENSE_FOLDER);

        File[] files;
        if (filter != null) {
            files = folder.listFiles(filter);
        } else {
            files = folder.listFiles();
        }

        if (files != null) {
            for (File file : files) {
                data.add(file.getName());
            }
        }
        data.sort(String::compareTo);
        list_licenses.updateUI();
    }

    private Label setupLabel_Name() {
        Label label = new Label();
        label.setSize(TEXT_SIZE);
        label.setLocation(posText(3, 1));
        add(label);
        return label;
    }

    private TextField setupText_Name() {
        TextField textField = new TextField();

        textField.setSize(TEXT_FILTER_SIZE);
        textField.setLocation(posText(1, 1));
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String nameCondition = text_name.getText();
                if (!nameCondition.isEmpty()) {
                    filter = (dir, name) -> {
                        String playerName = name.split("\\.")[0];
                        return playerName.contains(nameCondition);
                    };
                } else {
                    filter = null;
                }
                updateData();
            }
        });
        add(textField);
        return textField;
    }

    private JList<String> setupList_Licenses() {
        JList<String> list = new JList<>(data);
        list.setSize(LIST_SIZE);
        list.addMouseListener(new MouseAdapter());
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                Runnable op = keyOperations.get(key);
                if(op != null){
                    keyOperations.get(key).run();
                }
            }
        });
        return list;
    }

    private JScrollPane setupScrollPane() {
        JScrollPane scrollPane = new JScrollPane(list_licenses);
        scrollPane.setSize(LIST_SIZE);
        scrollPane.setLocation(posText(1, 2));
        add(scrollPane);
        return scrollPane;
    }

    @Override
    void change2cn(ActionEvent event) {
        setTitle("许可证管理器");
        label_name.setText("玩家名称");
        menu_file.setText("文件");
        item_new.setText("新建");
        item_view.setText("查看");
        item_delete.setText("删除");

        menu_language.setText("语言");
        item_en.setText("English");
        item_cn.setText("中文");

        NO_SELECTED = "没有选中的文件";
        ERROR = "错误";
        CONFIRM = "你确定要删除以下文件吗？";
    }

    @Override
    void change2en(ActionEvent event) {
        setTitle("License Manager");
        label_name.setText("Player name");

        menu_file.setText("File");
        item_new.setText("New");
        item_view.setText("View");
        item_delete.setText("Delete");

        menu_language.setText("Language");
        item_en.setText("English");
        item_cn.setText("中文");

        NO_SELECTED = "No selected file";
        ERROR = "Error";
        CONFIRM = "Are you sure you want to delete this file?";
    }

    private void edit(boolean isNew) {
        LicenseEditorWindow ldw = WindowManager.manager.getWindow(LicenseEditorWindow.class);
        ldw.setNew(isNew);
        if (isNew) {
            ldw.setLicense(null);
        } else {
            String selected = list_licenses.getSelectedValue();
            if(selected == null || selected.isEmpty()){
                showError(NO_SELECTED, ERROR);
                return;
            }
            ServerLicense license = ServerLicense.readFile(selected);
            ldw.setLicense(license);
        }
        WindowManager.manager.showWindow(LicenseEditorWindow.class);
    }

    private void removeSelectedFile(){
        String fileName = list_licenses.getSelectedValue();
        if(showConfirmation(CONFIRM + "\n" + fileName, "")){
            ServerLicense.removeFile(fileName, "");
            updateData();
        }
    }



    private class MouseAdapter extends java.awt.event.MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                edit(false);
            }
        }
    }
}