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
import java.util.UUID;

public class LicenseEditorWindow extends AbstractModWindow {
    private final Point RADIO_1 = new Point(COL_2, 40);
    private final Point RADIO_2 = new Point(COL_2, 60);
    private final Point RADIO_3 = new Point(COL_2, 80);

    private final Button button_save = setupButton_Save();
    private final Button button_cancel = setupButton_Cancel();

    private final TextField text_name = setupText_Name();
    private final Label label_name = setupLabel_Name();
    private final Label label_type = setupLabel_Type();
    private final ButtonGroup group_type = new ButtonGroup();
    private final JRadioButton radio_byMinutes = setupRadio_ByMinutes();
    private final JRadioButton radio_byConnections = setupRadio_ByConnections();
    private final JRadioButton radio_infinite = setupRadio_Infinite();

    private final Label label_startTime = setupLabel_StartTime();
    private final Label label_expireTime = setupLabel_ExpireTime();
    private final TextField text_startTime = setupText_StartTime();
    private final TextField text_expireTime = setupText_ExpireTime();

    private final Label label_allowance = setupLabel_Allowance();
    private final TextField text_allowance = setupText_Allowance();

    private final Label label_dateFormat = setupLabel_DateFormat();

    private int buttonSelection = ServerLicense.BY_MIN;

    private ServerLicense holdedLicense = null;
    private boolean isNew;

    private String EMPTY_NAME = "";
    private String ILLEGAL_TIME = "";
    private String ILLEGAL_ALLOWANCE = "";
    private String SAVED = "";
    private String SUCCESS = "";
    private String ERROR = "";

    public LicenseEditorWindow(){
        initialize();
    }
    private void initialize(){
        setSize(WINDOW_SIZE_SMALL);
        setLayout(null);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);

    }

    public void setLicense(ServerLicense license){
        holdedLicense = license;
        if(license != null){
            text_name.setText(license.getPlayerName());
            text_startTime.setText(DateParser.format(license.getStartTime()));
            text_expireTime.setText(DateParser.format(license.getExpireTime()));
            text_allowance.setText(String.valueOf(license.getAllowance()));
            switch (license.getType()){
                case ServerLicense.BY_MIN -> {
                    radio_byMinutes.setSelected(true);
                    text_allowance.setEnabled(true);
                }
                case ServerLicense.BY_CONN -> {
                    radio_byConnections.setSelected(true);
                    text_allowance.setEnabled(true);
                }
                case ServerLicense.INFINITE -> {
                    radio_infinite.setSelected(true);
                    text_allowance.setEnabled(false);
                }
            }
        }else{
            text_name.setText("");
            text_startTime.setText("");
            text_expireTime.setText("");
            text_allowance.setText("");
            text_allowance.setEnabled(true);
            radio_byMinutes.setSelected(true);
        }
    }

    public ServerLicense getLicense(){
        return holdedLicense;
    }

    private Button setupButton_Save(){
        Button button = new Button();
        button.setSize(BUTTON_SIZE);
        button.setLocation(posButton(2, 8));
        button.addActionListener((event) ->{
            String str = text_name.getText();
            try {
                if(holdedLicense == null || isNew){
                    holdedLicense = makeNewLicense();
                }else {
                    holdedLicense = makeLicense(holdedLicense.getUuid());
                }
                holdedLicense.write("");

                ManageWindow mw = WindowManager.manager.getWindow(ManageWindow.class);
                mw.updateData();

                showInfo(SAVED + holdedLicense, SAVED);
                holdedLicense = null;

                if(!isNew){
                    this.dispose();
                }

            } catch (Exception e) {
                showError(String.valueOf(e), ERROR);
            }
        });
        add(button);
        return button;
    }

    private Button setupButton_Cancel() {
        Button button = new Button();
        button.setSize(BUTTON_SIZE);
        button.setLocation(posButton(1, 8));
        button.addActionListener(e -> dispose());
        add(button);
        return button;
    }
/*
    private Button setupButton_Manage() {
        Button button = new Button();
        button.setSize(BUTTON_SIZE);
        button.setLocation(posButton(2, 6));
        button.addActionListener((event) -> WindowManager.manager.showWindow(ManageWindow.class));
        add(button);
        return button;
    }
*/
    private Label setupLabel_Name(){
        Label label = new Label();
        label.setSize(TEXT_SIZE);
        label.setLocation(posText(1, 1));
        add(label);
        return label;
    }

    private TextField setupText_Name(){
        TextField text = new TextField();
        text.setSize(TEXT_SIZE);
        text.setLocation(posText(1, 2));
        add(text);
        return text;
    }

    private Label setupLabel_Type() {
        Label label = new Label();
        label.setSize(TEXT_SIZE);
        label.setLocation(posText(2, 1));
        add(label);
        return label;
    }
    private JRadioButton setupRadio_ByMinutes() {
        JRadioButton radio = new JRadioButton();
        radio.setSize(TEXT_SIZE);
        radio.setLocation(RADIO_1);
        group_type.add(radio);
        radio.addActionListener(e-> {
            buttonSelection = ServerLicense.BY_MIN;
            text_allowance.setEnabled(true);
        });
        add(radio);
        radio.setSelected(true);
        return radio;
    }

    private JRadioButton setupRadio_ByConnections() {
        JRadioButton radio = new JRadioButton();
        radio.setSize(TEXT_SIZE);
        radio.setLocation(RADIO_2);
        group_type.add(radio);
        radio.addActionListener(e-> {
            buttonSelection = ServerLicense.BY_CONN;
            text_allowance.setEnabled(true);
        });
        add(radio);
        return radio;
    }
    private JRadioButton setupRadio_Infinite() {
        JRadioButton radio = new JRadioButton();
        radio.setSize(TEXT_SIZE);
        radio.setLocation(RADIO_3);
        group_type.add(radio);
        radio.addActionListener(e-> {
            buttonSelection = ServerLicense.INFINITE;
            text_allowance.setEnabled(false);
        });
        add(radio);
        return radio;
    }

    private Label setupLabel_StartTime() {
        Label label = new Label();
        label.setSize(TEXT_SIZE);
        label.setLocation(posText(1, 3));
        add(label);
        return label;
    }
    private TextField setupText_StartTime() {
        TextField textField = new TextField();
        textField.setSize(TEXT_SIZE);
        textField.setLocation(posText(1, 4));
        add(textField);
        return textField;
    }
    private Label setupLabel_ExpireTime() {
        Label label = new Label();
        label.setSize(TEXT_SIZE);
        label.setLocation(posText(1, 5));
        add(label);
        return label;
    }

    private TextField setupText_ExpireTime() {
        TextField textField = new TextField();
        textField.setSize(TEXT_SIZE);
        textField.setLocation(posText(1, 6));
        add(textField);
        return textField;
    }

    private Label setupLabel_Allowance() {
        Label label = new Label();
        label.setSize(TEXT_SIZE);
        label.setLocation(posText(2, 4));
        add(label);
        return label;
    }

    private TextField setupText_Allowance() {
        TextField textField = new TextField();
        textField.setSize(TEXT_SIZE);
        textField.setLocation(posText(2, 5));
        add(textField);
        return textField;
    }
    private Label setupLabel_DateFormat() {
        Label label = new Label();
        label.setSize(300, 20);
        label.setLocation(posText(1, 7));
        add(label);
        return label;
    }

    private ServerLicense makeNewLicense() throws Exception {
        return makeLicense(UUID.randomUUID());
    }

    private ServerLicense makeLicense(UUID uuid) throws Exception {
        ServerLicense.Builder builder = new ServerLicense.Builder();
        builder.uuid(uuid);

        String name = text_name.getText();
        if (name.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_NAME);
        }
        builder.playerName(name);

        String timeStr = "";
        long dateTime;
        try {
            timeStr = text_startTime.getText();
            dateTime = DateParser.parse(timeStr);
            builder.startTime(dateTime);
        } catch (Exception e) {
            throw new IllegalArgumentException(ILLEGAL_TIME + ": " + timeStr);
        }


        try {
            timeStr = text_expireTime.getText();
            dateTime = DateParser.parse(timeStr);
            builder.expireTime(dateTime);
        } catch (Exception e) {
            throw new IllegalArgumentException(ILLEGAL_TIME + ": " + timeStr);
        }


        builder.type(buttonSelection);

        if (buttonSelection != ServerLicense.INFINITE) {
            String allowanceStr = text_allowance.getText();
            try{
                int allowanceInt = Integer.parseInt(allowanceStr);
                builder.allowance(allowanceInt);
            }catch (Exception e){
                throw new IllegalArgumentException(ILLEGAL_ALLOWANCE + ": " + allowanceStr);
            }
        }else{
            builder.allowance(ServerLicense.INFINITE);
        }
        return builder.end();
    }

    @Override
    public void change2cn(ActionEvent event) {
        setTitle("编辑");
        button_save.setLabel("保存");
        button_cancel.setLabel("取消");

        label_name.setText("玩家名称");
        label_type.setText("类型");
        radio_byMinutes.setText("时间 (分钟)");
        radio_byConnections.setText("连接次数");
        radio_infinite.setText("无限制连接");

        label_startTime.setText("有效期开始时间");
        label_expireTime.setText("有效期结束时间");
        label_allowance.setText("限额 (分钟/连接次数)");
        label_dateFormat.setText("时间格式：\"yyyyMMddHHmmss\"，无空格");

        ILLEGAL_TIME = "无效的时间字符串";
        EMPTY_NAME = "无效的玩家名称";
        ILLEGAL_ALLOWANCE = "无效的限额";
        SAVED = "许可证已保存\n";
        SUCCESS = "成功";
        ERROR = "错误";
    }

    @Override
    public void change2en(ActionEvent event) {
        setTitle("Edit");
        button_save.setLabel("Save");
        button_cancel.setLabel("Cancel");

        label_name.setText("Player name");
        label_type.setText("Type");
        radio_byMinutes.setText("Time (Minutes)");
        radio_byConnections.setText("Num of connections");
        radio_infinite.setText("No restriction");

        label_startTime.setText("Available from");
        label_expireTime.setText("Available until");
        label_allowance.setText("Allowance (Min/Conn)");
        label_dateFormat.setText("Time format: \"yyyyMMddHHmmss\", no spaces");

        ILLEGAL_TIME = "Invalid time string";
        EMPTY_NAME = "Invalid player name";
        ILLEGAL_ALLOWANCE = "Invalid allowance";
        SAVED = "License saved\n";
        SUCCESS = "Success";
        ERROR = "Error";
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }
}
