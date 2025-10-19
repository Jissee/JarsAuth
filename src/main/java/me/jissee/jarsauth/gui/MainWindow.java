package me.jissee.jarsauth.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.model.AuthProfile;
import me.jissee.jarsauth.data.model.LicenseType;
import me.jissee.jarsauth.data.model.ServerLicense;
import me.jissee.jarsauth.data.service.*;
import me.jissee.jarsauth.manip.ObfConfigBuilder;
import me.jissee.jarsauth.manip.Pipeline;
import me.jissee.jarsauth.manip.RandomStringGeneratorToFile;
import me.jissee.jarsauth.manip.Tuple;
import me.jissee.jarsauth.verification.Verification;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainWindow extends AbstractModWindow {
    public JPanel panel1;
    private JTabbedPane tabbedPane1;
    private JList<String> accGroupList;
    private JList<String> ruleList;
    private JList<String> ruleExpandedList;
    private JTextField serverIdText;
    private JTable userIdTable;
    private JButton reset;
    private JTable serverLicenseTable;
    private JCheckBox isFCEnabledBox;
    private JCheckBox isCAEnabledBox;
    private JCheckBox isSLEnabledBox;
    private JTextField fcIntervalText;
    private JTextField caIntervalText;
    private JTextField slIntervalText;
    private JTextField fcTimeoutText;
    private JTextField caTimeoutText;
    private JCheckBox slAutoRemove;
    private JComboBox<String> languageComboBox;
    private JCheckBox limitTimeBox;
    private JCheckBox limitWalkBox;
    private JCheckBox limitDigBox;
    private JCheckBox limitPickBox;
    private JButton exportButton;
    private JLabel signedLabel;
    private JLabel environmentLabel;
    private JLabel signableLabel;
    private JButton createObfDictButton;
    private JButton createObfConfig;
    private JTextField signedExpandedPathText;
    private JTextField dictPathText;
    private JTextField obfConfigPathText;

    private boolean isConfigInitialized = false;
    private boolean isChangingCheckBox = false;


    public MainWindow() {
        frame.setSize(800, 600);

        tabbedPane1.addChangeListener(this::onChangeTab);

        accGroupList.addListSelectionListener(this::onAccGroupListChangeSelection);

        accGroupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onAccGroupListRenameSelection();
                }
            }

        });

        accGroupList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                    onAccGroupListDeleteSelection();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_INSERT || e.getKeyCode() == KeyEvent.VK_HELP) {
                    onAccGroupListAddNew();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onAccGroupListRenameSelection();
                    return;
                }
            }
        });

        ruleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (e.isShiftDown()) {
                        onRuleListEditBatch();
                    } else {
                        onRuleListRenameSelection();
                    }
                }
            }

        });

        ruleList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                    onRuleListDeleteSelection();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_INSERT || e.getKeyCode() == KeyEvent.VK_HELP) {
                    if (e.isShiftDown()) {
                        onRuleListEditBatch();
                    } else {
                        onRuleListAddNew();
                    }
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        onRuleListEditBatch();
                    } else {
                        onRuleListRenameSelection();
                    }
                    return;
                }
            }
        });

        reset.addActionListener(this::onResetServerId);

        userIdTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    onUserIdTableDelete();
                    return;
                }
            }
        });

        isFCEnabledBox.addActionListener(e -> {
            isChangingCheckBox = true;
            onChangeConfig(true);
        });
        isCAEnabledBox.addActionListener(e -> {
            isChangingCheckBox = true;
            onChangeConfig(true);
        });
        isSLEnabledBox.addActionListener(e -> {
            isChangingCheckBox = true;
            onChangeConfig(true);
        });

        languageComboBox.addActionListener(e -> onChangeConfig(false));
        fcIntervalText.getDocument().addDocumentListener(new DocumentListenerImpl());
        fcTimeoutText.getDocument().addDocumentListener(new DocumentListenerImpl());
        caIntervalText.getDocument().addDocumentListener(new DocumentListenerImpl());
        caTimeoutText.getDocument().addDocumentListener(new DocumentListenerImpl());
        slIntervalText.getDocument().addDocumentListener(new DocumentListenerImpl());
        slAutoRemove.addActionListener(e -> onChangeConfig(false));
        limitTimeBox.addActionListener(e -> onChangeConfig(false));
        limitWalkBox.addActionListener(e -> onChangeConfig(false));
        limitPickBox.addActionListener(e -> onChangeConfig(false));
        limitDigBox.addActionListener(e -> onChangeConfig(false));

        serverLicenseTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                    onLicenseTableDeleteSelection();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_INSERT || e.getKeyCode() == KeyEvent.VK_HELP) {
                    onLicenseTableAddNew();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onLicenseTableEdit();
                    return;
                }
            }
        });

        serverLicenseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onLicenseTableEdit();
                }
            }

        });

        userIdTable.getActionMap().put("selectNextRowCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        serverLicenseTable.getActionMap().put("selectNextRowCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        exportButton.addActionListener(this::onExport);
        createObfDictButton.addActionListener(this::onCreateObfDict);
        createObfConfig.addActionListener(this::onCreateObfConfig);

    }


    public void onShow() {
        updateAccGroupList();
        updateServerId();
        updateClientId();
        updateServerLicense();
        updateSignatureStatus();
        readConfig();
    }


    @Override
    public String getTitle() {
        return Locales.getString("title.window.main");
    }

    private void onAccGroupListRenameSelection() {
        String nameWithTag = accGroupList.getSelectedValue();
        String oldName = removeTag(nameWithTag);

        TextEditorWindow textEditorWindow = new TextEditorWindow(Locales.getString("title.window.edit"), oldName, (newName) -> {
            AcceptedDetailService service = DataManager.getServerInstance().getService(AcceptedDetailService.class);
            try {
                service.renameGroup(oldName, newName);
            } catch (Exception ex) {
                showError(Locales.getString("info.rename.failed"), "");
            }
            updateAccGroupList();
        });
        textEditorWindow.show();
    }

    private void onAccGroupListChangeSelection(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            String nameWithTag = accGroupList.getSelectedValue();
            if (nameWithTag == null) return;
            String name = removeTag(nameWithTag);
            updateRuleList(name);
        }
    }

    private void onAccGroupListDeleteSelection() {
        String nameWithTag = accGroupList.getSelectedValue();
        String groupName = removeTag(nameWithTag);
        AcceptedDetailService service = DataManager.getServerInstance().getService(AcceptedDetailService.class);
        try {
            if (showConfirmation(Locales.getString("info.delete.confirm") + " " + groupName, "")) {
                service.removeGroup(groupName);
            }
        } catch (Exception ex) {
            showError(Locales.getString("info.delete.failed"), "");
        }
        updateAccGroupList();
    }

    private void onAccGroupListAddNew() {
        TextEditorWindow window = new TextEditorWindow(Locales.getString("title.window.edit"), "", (newName) -> {
            AcceptedDetailService accService = DataManager.getServerInstance().getService(AcceptedDetailService.class);
            accService.saveAcceptedDetail(new AcceptedDetail(newName));
            updateAccGroupList();
        });
        window.show();
    }

    private void onRuleListRenameSelection() {
        String nameWithTag = accGroupList.getSelectedValue();
        String groupName = removeTag(nameWithTag);
        String oldName = ruleList.getSelectedValue();

        TextEditorWindow textEditorWindow = new TextEditorWindow(Locales.getString("title.window.edit") + " (" + groupName + ")", oldName, (newName) -> {
            AuthProfileService service = DataManager.getServerInstance().getService(AuthProfileService.class);
            try {
                service.changeRule(groupName, oldName, newName);
            } catch (Exception ex) {
                showError(Locales.getString("info.rename.failed"), "");
            }
            updateAccGroupList();
            updateRuleList(groupName);
        });
        textEditorWindow.show();
    }


    private void onRuleListDeleteSelection() {
        String nameWithTag = accGroupList.getSelectedValue();
        String groupName = removeTag(nameWithTag);
        String rule = ruleList.getSelectedValue();

        AuthProfileService service = DataManager.getServerInstance().getService(AuthProfileService.class);
        try {
            if (showConfirmation(Locales.getString("info.delete.confirm") + rule + " (" + groupName + ")", "")) {
                service.removeRule(groupName, rule);
                updateRuleList(groupName);
            }
        } catch (Exception ex) {
            showError(Locales.getString("info.delete.failed"), "");
        }

    }

    private void onRuleListAddNew() {
        String nameWithTag = accGroupList.getSelectedValue();
        String groupName = removeTag(nameWithTag);

        TextEditorWindow window = new TextEditorWindow(Locales.getString("title.window.edit") + " (" + groupName + ")", "", (newName) -> {
            AuthProfileService service = DataManager.getServerInstance().getService(AuthProfileService.class);

            AuthProfile profile = new AuthProfile(groupName, List.of(newName));

            service.saveProfile(profile);

            updateAccGroupList();
            updateRuleList(groupName);
        });
        window.show();
    }

    private void onRuleListEditBatch() {
        String nameWithTag = accGroupList.getSelectedValue();
        String groupName = removeTag(nameWithTag);
        AuthProfileService service = DataManager.getServerInstance().getService(AuthProfileService.class);
        AuthProfile profile = service.getUnflattenedProfile(groupName);
        StringBuilder builder = new StringBuilder();
        for (String str : profile.rules()) {
            builder.append(str).append("\n");
        }
        if (!builder.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }

        TextAreaEditorWindow window = new TextAreaEditorWindow(Locales.getString("title.window.edit") + " (" + groupName + ")", builder.toString(), (newName) -> {
            AuthProfileService service1 = DataManager.getServerInstance().getService(AuthProfileService.class);
            service1.removeGroup(groupName);


            AuthProfile profile1 = new AuthProfile(groupName, Arrays.stream(newName.split("\n"))
                    .map(String::trim)           // 去除前后空白
                    .filter(s -> !s.isEmpty())   // 过滤空字符串
                    .collect(Collectors.toList()));

            service1.saveProfile(profile1);

            updateAccGroupList();
            updateRuleList(groupName);
        });
        window.show();
    }

    private void onChangeTab(ChangeEvent e) {
        Component component = tabbedPane1.getSelectedComponent();
        if (component instanceof JPanel panel) {
            onShow();
        }
    }


    private void onResetServerId(ActionEvent actionEvent) {
        if (showConfirmation(Locales.getString("info.server.id.reset"), "")) {
            ServerIdService service = DataManager.getServerInstance().getService(ServerIdService.class);
            service.resetServerId();
            updateServerId();
        }
    }

    private void onUserIdTableDelete() {
        int row = userIdTable.getSelectedRow();
        String player = userIdTable.getValueAt(row, 0).toString();
        String uuid = userIdTable.getValueAt(row, 1).toString();
        UserIdService service = DataManager.getServerInstance().getService(UserIdService.class);
        if (showConfirmation(Locales.getString("info.delete.confirm") + "(" + player + ", " + uuid + ")", "")) {
            service.removeUserId(player);
            updateClientId();
        }
    }


    private void onChangeConfig(boolean reloadConfig) {
        if (isConfigInitialized && !isChangingCheckBox) {
            saveConfig();
        }
        if (reloadConfig) {
            readConfig();
        }
    }


    private static final String TAG_CLIENT_GROUP = "[" + Locales.getString("tag.group.client") + "] ";
    private static final String TAG_CUSTOM_GROUP = "[" + Locales.getString("tag.group.custom") + "] ";

    private static String removeTag(String taggedName) {
        int tagPos = taggedName.lastIndexOf(TAG_CLIENT_GROUP);
        if (tagPos != -1) {
            return taggedName.substring(tagPos + TAG_CLIENT_GROUP.length());
        } else {
            tagPos = taggedName.lastIndexOf(TAG_CUSTOM_GROUP);
            return taggedName.substring(tagPos + TAG_CUSTOM_GROUP.length());
        }
    }

    private void updateAccGroupList() {
        int selected = accGroupList.getSelectedIndex();
        DataManager manager = DataManager.getServerInstance();
        AcceptedDetailService service = manager.getService(AcceptedDetailService.class);

        List<String> allGroupNames = service.getAllGroupNames();
        List<String> registeredAccGroupNames = service.getRegisteredAccGroupNames();

        Set<String> groupNames = new TreeSet<>(registeredAccGroupNames);
        groupNames.addAll(allGroupNames);

        DefaultListModel<String> model = new DefaultListModel<>();

        Set<String> newGroupNames = new TreeSet<>();
        for (String name : groupNames) {
            if (registeredAccGroupNames.contains(name)) {
                newGroupNames.add(TAG_CLIENT_GROUP + name);
            } else {
                newGroupNames.add(TAG_CUSTOM_GROUP + name);
            }
        }
        newGroupNames.stream().sorted().forEach(model::addElement);
        accGroupList.setModel(model);
        accGroupList.setSelectedIndex(selected);
    }

    private void updateRuleList(String groupName) {
        AuthProfileService service = DataManager.getServerInstance().getService(AuthProfileService.class);
        AuthProfile profile = service.getUnflattenedProfile(groupName);

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addAll(profile.rules());
        ruleList.setModel(model);

        AuthProfile profileExpanded = service.getTaggedFlattenProfile(groupName);

        DefaultListModel<String> expandedModel = new DefaultListModel<>();
        expandedModel.addAll(profileExpanded.rules());
        ruleExpandedList.setModel(expandedModel);
    }

    private void updateServerId() {
        ServerIdService service = DataManager.getServerInstance().getService(ServerIdService.class);
        UUID uuid = service.getOrCreateServerId();
        serverIdText.setText(uuid.toString());
    }

    private void updateClientId() {
        DefaultTableModel model = new ImmutableTableModel();

        model.addColumn(Locales.getString("label.client.id.player"));
        model.addColumn(Locales.getString("label.client.id"));

        UserIdService service = DataManager.getServerInstance().getService(UserIdService.class);
        Map<String, UUID> ids = service.getUserIds();
        for (Map.Entry<String, UUID> entry : ids.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }


        userIdTable.setModel(model);

        DefaultTableColumnModel columnModel = (DefaultTableColumnModel) userIdTable.getColumnModel();
        columnModel.getColumn(0).setMaxWidth(120);
        columnModel.getColumn(1).setMinWidth(360);
    }

    private void updateServerLicense() {
        DefaultTableModel model = new ImmutableTableModel();

        model.addColumn("UUID");
        model.addColumn(Locales.getString("label.server.license.player"));
        model.addColumn(Locales.getString("label.server.license.valid.from"));
        model.addColumn(Locales.getString("label.server.license.valid.until"));
        model.addColumn(Locales.getString("label.server.license.type"));
        model.addColumn(Locales.getString("label.server.license.allowance"));
        model.addColumn(Locales.getString("label.server.license.period"));


        ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
        List<ServerLicense> licenses = service.getAllLicenses();

        for (ServerLicense license : licenses) {
            LicenseType type = LicenseType.fromCode(license.type().getCode());
            String name = Locales.getString(type.getNameKey());
            model.addRow(new Object[]{
                    license.uuid(),
                    license.userName(),
                    ServerLicense.formatDate(license.validFrom()),
                    ServerLicense.formatDate(license.validUntil()),
                    name,
                    license.allowance(),
                    ServerLicense.formatPeriodTime(license.period().get())
            });
        }


        serverLicenseTable.setModel(model);

        DefaultTableColumnModel columnModel = (DefaultTableColumnModel) serverLicenseTable.getColumnModel();
        columnModel.removeColumn(columnModel.getColumn(0));

        columnModel.getColumn(0).setMaxWidth(120);
    }

    private ServerLicense getLicenseInRow(int row) {
        DefaultTableModel model = (DefaultTableModel) serverLicenseTable.getModel();
        UUID uuid = UUID.fromString(model.getValueAt(row, 0).toString());
        ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);

        return service.getLicense(uuid);
    }

    private void onLicenseTableDeleteSelection() {
        int row = serverLicenseTable.getSelectedRow();
        ServerLicense oldLicense = getLicenseInRow(row);

        ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
        if (showConfirmation(Locales.getString("info.delete.confirm") + oldLicense.toStringFormattedWithoutUUID(), "")) {
            service.removeLicense(oldLicense.uuid());
            updateServerLicense();
        }
    }

    private void onLicenseTableAddNew() {
        ServerLicense oldLicense = new ServerLicense(
                UUID.randomUUID(),
                "",
                System.currentTimeMillis() / 1000L,
                System.currentTimeMillis() / 1000L,
                LicenseType.TIME,
                0,
                -1
        );

        LicenseEditorWindow window = new LicenseEditorWindow(Locales.getString("title.window.edit"), oldLicense, (newLicense) -> {
            ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
            service.saveLicense(newLicense);
            updateServerLicense();
        });
        window.show();
    }

    private void onLicenseTableEdit() {
        int row = serverLicenseTable.getSelectedRow();
        ServerLicense oldLicense = getLicenseInRow(row);

        LicenseEditorWindow window = new LicenseEditorWindow(Locales.getString("title.window.edit"), oldLicense, (newLicense) -> {
            ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
            service.updateLicense(newLicense);
            updateServerLicense();
        });
        window.show();
    }

    private void updateSignatureStatus() {
        environmentLabel.setText(String.format(Locales.getString("info.status.environment"), isEnvironmentOK() ? "✅" : "❌"));
        signedLabel.setText(String.format(Locales.getString("info.status.signed"), Verification.signedFlag() == 0 ? "✅" : "❌"));
        signableLabel.setText(String.format(Locales.getString("info.status.signable"), Verification.expansionFlag() == 1 ? "✅" : "❌"));
    }

    private void onExport(ActionEvent event) {
        if (!isEnvironmentOK()) {
            showError(Locales.getString("info.class.not.found"), "");
            return;
        }

        try {
            Tuple<String, File> result = Pipeline.run();
            showInfo(result.a(), "");
            if (result.b() != null) {
                String exp = result.b().getAbsolutePath();
                signedExpandedPathText.setText(exp);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onCreateObfDict(ActionEvent actionEvent) {
        File outFile = RandomStringGeneratorToFile.generate();
        showInfo(String.format(Locales.getString("info.exported.obf.dict"), outFile.getAbsoluteFile()), "");
        dictPathText.setText(outFile.getAbsolutePath());
    }

    private void onCreateObfConfig(ActionEvent actionEvent) {
        String input = signedExpandedPathText.getText();
        String output = input.replace(".jar", "-obf.jar");
        String dict = dictPathText.getText();
        String mapping = dictPathText.getText().replace("dict.txt", "mapping.txt");
        if(input.isEmpty()) {
            showError(Locales.getString("info.invalid.target"), "");
            return;
        }
        if(dict.isEmpty()) {
            showError(Locales.getString("info.invalid.dict"), "");
            return;
        }
        File config = new File("config.pro");
        ObfConfigBuilder builder = new ObfConfigBuilder(input, output, dict, mapping);
        builder.export(config);
        showInfo(String.format(Locales.getString("info.exported.obf.config"), config.getAbsolutePath()), "");
        obfConfigPathText.setText(config.getAbsolutePath());
    }


    private void readConfig() {
        ConfigService service = DataManager.getServerInstance().getService(ConfigService.class);
        boolean isFCEnabled = service.getValue(ConfigKey.FILE_CHECKSUM_ENABLED) != 0;
        boolean isCAEnabled = service.getValue(ConfigKey.CLIENT_AUTH_ENABLED) != 0;
        boolean isSLEnabled = service.getValue(ConfigKey.SERVER_LICENSE_ENABLED) != 0;
        if (isConfigInitialized) {
            isFCEnabled = isFCEnabledBox.isSelected();
            isCAEnabled = isCAEnabledBox.isSelected();
            isSLEnabled = isSLEnabledBox.isSelected();
        }
        if (isFCEnabled) {
            isFCEnabledBox.setSelected(true);
            fcIntervalText.setEnabled(true);
            fcTimeoutText.setEnabled(true);
            fcIntervalText.setText(String.valueOf(service.getValue(ConfigKey.FILE_CHECKSUM_INTERVAL)));
            fcTimeoutText.setText(String.valueOf(service.getValue(ConfigKey.FILE_CHECKSUM_TIMEOUT)));
        } else {
            fcIntervalText.setEnabled(false);
            fcTimeoutText.setEnabled(false);
            fcIntervalText.setText("");
            fcTimeoutText.setText("");
        }

        if (isCAEnabled) {
            isCAEnabledBox.setSelected(true);
            caIntervalText.setEnabled(true);
            caTimeoutText.setEnabled(true);
            caIntervalText.setText(String.valueOf(service.getValue(ConfigKey.CLIENT_AUTH_INTERVAL)));
            caTimeoutText.setText(String.valueOf(service.getValue(ConfigKey.CLIENT_AUTH_TIMEOUT)));
        } else {
            caIntervalText.setEnabled(false);
            caTimeoutText.setEnabled(false);
            caTimeoutText.setText("");
            caIntervalText.setText("");
        }

        if (isSLEnabled) {
            isSLEnabledBox.setSelected(true);
            slIntervalText.setEnabled(true);
            slAutoRemove.setEnabled(true);
            slIntervalText.setText(String.valueOf(service.getValue(ConfigKey.SERVER_LICENSE_INTERVAL)));
            slAutoRemove.setSelected(service.getValue(ConfigKey.SERVER_LICENSE_AUTO_REMOVE) != 0);

            limitDigBox.setEnabled(true);
            limitWalkBox.setEnabled(true);
            limitPickBox.setEnabled(true);
            limitTimeBox.setEnabled(true);


            limitTimeBox.setSelected(service.getValue(ConfigKey.SERVER_LICENSE_LIMIT_TIME) != 0);
            limitDigBox.setSelected(service.getValue(ConfigKey.SERVER_LICENSE_LIMIT_DIG) != 0);
            limitPickBox.setSelected(service.getValue(ConfigKey.SERVER_LICENSE_LIMIT_PICK) != 0);
            limitWalkBox.setSelected(service.getValue(ConfigKey.SERVER_LICENSE_LIMIT_WALK) != 0);

        } else {
            isSLEnabledBox.setSelected(false);
            slIntervalText.setEnabled(false);
            slAutoRemove.setEnabled(false);
            slIntervalText.setText("");
            slAutoRemove.setSelected(false);

            limitTimeBox.setSelected(false);
            limitTimeBox.setEnabled(false);
            limitDigBox.setSelected(false);
            limitDigBox.setEnabled(false);
            limitPickBox.setSelected(false);
            limitPickBox.setEnabled(false);
            limitWalkBox.setSelected(false);
            limitWalkBox.setEnabled(false);
        }

        isChangingCheckBox = false;

        if (!isConfigInitialized) {
            languageComboBox.removeAllItems();
            for (String locale : Locales.getLocales()) {
                languageComboBox.addItem(locale);
            }
            isConfigInitialized = true;
        }

        long lang = service.getValue(ConfigKey.UI_LANGUAGE);
        languageComboBox.setSelectedIndex((int) lang);
    }

    private void saveConfig() {
        ConfigService service = DataManager.getServerInstance().getService(ConfigService.class);

        service.setCheckedValue(ConfigKey.FILE_CHECKSUM_ENABLED, parseLong(isFCEnabledBox.isSelected() ? "1" : "0"));
        if (isFCEnabledBox.isSelected()) {
            service.setCheckedValue(ConfigKey.FILE_CHECKSUM_INTERVAL, parseLong(fcIntervalText.getText()));
            service.setCheckedValue(ConfigKey.FILE_CHECKSUM_TIMEOUT, parseLong(fcTimeoutText.getText()));
        }

        service.setCheckedValue(ConfigKey.CLIENT_AUTH_ENABLED, parseLong(isCAEnabledBox.isSelected() ? "1" : "0"));
        if (isCAEnabledBox.isSelected()) {
            service.setCheckedValue(ConfigKey.CLIENT_AUTH_INTERVAL, parseLong(caIntervalText.getText()));
            service.setCheckedValue(ConfigKey.CLIENT_AUTH_TIMEOUT, parseLong(caTimeoutText.getText()));
        }

        service.setCheckedValue(ConfigKey.SERVER_LICENSE_ENABLED, parseLong(isSLEnabledBox.isSelected() ? "1" : "0"));
        if (isSLEnabledBox.isSelected()) {
            service.setCheckedValue(ConfigKey.SERVER_LICENSE_INTERVAL, parseLong(slIntervalText.getText()));
            service.setCheckedValue(ConfigKey.SERVER_LICENSE_AUTO_REMOVE, parseLong(slAutoRemove.isSelected() ? "1" : "0"));

            service.setCheckedValue(ConfigKey.SERVER_LICENSE_LIMIT_DIG, parseLong(limitDigBox.isSelected() ? "1" : "0"));
            service.setCheckedValue(ConfigKey.SERVER_LICENSE_LIMIT_PICK, parseLong(limitPickBox.isSelected() ? "1" : "0"));
            service.setCheckedValue(ConfigKey.SERVER_LICENSE_LIMIT_TIME, parseLong(limitTimeBox.isSelected() ? "1" : "0"));
            service.setCheckedValue(ConfigKey.SERVER_LICENSE_LIMIT_WALK, parseLong(limitWalkBox.isSelected() ? "1" : "0"));

        }

        int lang = languageComboBox.getSelectedIndex();
        if (lang >= 0) {
            service.setCheckedValue(ConfigKey.UI_LANGUAGE, lang);
        }
    }

    public static long parseLong(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0; // 或返回默认值，例如 0L
        }

        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    public JPanel getPanel() {
        return panel1;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        panel1.setToolTipText("");
        tabbedPane1 = new JTabbedPane();
        tabbedPane1.setTabLayoutPolicy(1);
        tabbedPane1.setToolTipText("");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(tabbedPane1, gbc);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.file.checksum"), null, panel2, this.$$$getMessageFromBundle$$$("ui", "tab.file.checksum.tip"));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        panel2.add(panel3, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("ui", "label.profile.rule"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel3.add(label1, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setOpaque(false);
        scrollPane1.setPreferredSize(new Dimension(99999, 99999));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 3.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 3;
        panel3.add(scrollPane1, gbc);
        ruleList = new JList();
        scrollPane1.setViewportView(ruleList);
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("ui", "label.profile.rule.expanded"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel3.add(label2, gbc);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setPreferredSize(new Dimension(99999, 99999));
        scrollPane2.setRequestFocusEnabled(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel3.add(scrollPane2, gbc);
        ruleExpandedList = new JList();
        scrollPane2.setViewportView(ruleExpandedList);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        panel2.add(panel4, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("ui", "label.profile.name"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel4.add(label3, gbc);
        final JScrollPane scrollPane3 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(scrollPane3, gbc);
        accGroupList = new JList();
        scrollPane3.setViewportView(accGroupList);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.client.auth"), null, panel5, this.$$$getMessageFromBundle$$$("ui", "tab.client.auth.tip"));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("ui", "label.client.id"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel5.add(label4, gbc);
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("ui", "label.server.id"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel5.add(label5, gbc);
        serverIdText = new JTextField();
        serverIdText.setEditable(false);
        serverIdText.setEnabled(true);
        serverIdText.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel5.add(serverIdText, gbc);
        final JScrollPane scrollPane4 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel5.add(scrollPane4, gbc);
        userIdTable = new JTable();
        userIdTable.setAutoResizeMode(0);
        scrollPane4.setViewportView(userIdTable);
        reset = new JButton();
        this.$$$loadButtonText$$$(reset, this.$$$getMessageFromBundle$$$("ui", "label.server.id.reset"));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel5.add(reset, gbc);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.server.license"), null, panel6, this.$$$getMessageFromBundle$$$("ui", "tab.server.license.tip"));
        final JScrollPane scrollPane5 = new JScrollPane();
        panel6.add(scrollPane5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        serverLicenseTable = new JTable();
        scrollPane5.setViewportView(serverLicenseTable);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.signature"), panel7);
        environmentLabel = new JLabel();
        environmentLabel.setText("Label");
        panel7.add(environmentLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signableLabel = new JLabel();
        signableLabel.setText("Label");
        panel7.add(signableLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signedLabel = new JLabel();
        signedLabel.setText("Label");
        panel7.add(signedLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exportButton = new JButton();
        this.$$$loadButtonText$$$(exportButton, this.$$$getMessageFromBundle$$$("ui", "info.export.signed"));
        exportButton.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "info.export.signed.tip"));
        panel7.add(exportButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createObfDictButton = new JButton();
        this.$$$loadButtonText$$$(createObfDictButton, this.$$$getMessageFromBundle$$$("ui", "info.generate.obf.dict"));
        createObfDictButton.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "info.generate.obf.dict.tip"));
        panel7.add(createObfDictButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createObfConfig = new JButton();
        this.$$$loadButtonText$$$(createObfConfig, this.$$$getMessageFromBundle$$$("ui", "info.generate.obf.config"));
        panel7.add(createObfConfig, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel7.add(spacer1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel7.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        signedExpandedPathText = new JTextField();
        panel7.add(signedExpandedPathText, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        dictPathText = new JTextField();
        panel7.add(dictPathText, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        obfConfigPathText = new JTextField();
        panel7.add(obfConfigPathText, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(7, 6, new Insets(0, 0, 500, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.config"), null, panel8, this.$$$getMessageFromBundle$$$("ui", "tab.config.tip"));
        isFCEnabledBox = new JCheckBox();
        isFCEnabledBox.setText("");
        panel8.add(isFCEnabledBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("ui", "config.fc.enabled"));
        panel8.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, this.$$$getMessageFromBundle$$$("ui", "config.ca.enabled"));
        panel8.add(label7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, this.$$$getMessageFromBundle$$$("ui", "config.sl.enabled"));
        panel8.add(label8, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        isCAEnabledBox = new JCheckBox();
        isCAEnabledBox.setText("");
        panel8.add(isCAEnabledBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        isSLEnabledBox = new JCheckBox();
        isSLEnabledBox.setText("");
        panel8.add(isSLEnabledBox, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, this.$$$getMessageFromBundle$$$("ui", "config.fc.interval"));
        label9.setToolTipText("");
        panel8.add(label9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        this.$$$loadLabelText$$$(label10, this.$$$getMessageFromBundle$$$("ui", "config.ca.interval"));
        label10.setToolTipText("");
        panel8.add(label10, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        this.$$$loadLabelText$$$(label11, this.$$$getMessageFromBundle$$$("ui", "config.sl.interval"));
        label11.setToolTipText("");
        panel8.add(label11, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fcIntervalText = new JTextField();
        fcIntervalText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel8.add(fcIntervalText, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        caIntervalText = new JTextField();
        caIntervalText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel8.add(caIntervalText, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        slIntervalText = new JTextField();
        slIntervalText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel8.add(slIntervalText, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label12 = new JLabel();
        this.$$$loadLabelText$$$(label12, this.$$$getMessageFromBundle$$$("ui", "config.fc.timeout"));
        label12.setToolTipText("");
        panel8.add(label12, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        this.$$$loadLabelText$$$(label13, this.$$$getMessageFromBundle$$$("ui", "config.ca.timeout"));
        label13.setToolTipText("");
        panel8.add(label13, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        this.$$$loadLabelText$$$(label14, this.$$$getMessageFromBundle$$$("ui", "config.sl.auto-remove"));
        panel8.add(label14, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fcTimeoutText = new JTextField();
        fcTimeoutText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel8.add(fcTimeoutText, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        caTimeoutText = new JTextField();
        caTimeoutText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel8.add(caTimeoutText, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        slAutoRemove = new JCheckBox();
        slAutoRemove.setText("");
        panel8.add(slAutoRemove, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        this.$$$loadLabelText$$$(label15, this.$$$getMessageFromBundle$$$("ui", "config.ui.language"));
        label15.setToolTipText("");
        panel8.add(label15, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        languageComboBox = new JComboBox();
        languageComboBox.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.ui.language.tip"));
        panel8.add(languageComboBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        this.$$$loadLabelText$$$(label16, this.$$$getMessageFromBundle$$$("ui", "config.sl.limit.time"));
        panel8.add(label16, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        this.$$$loadLabelText$$$(label17, this.$$$getMessageFromBundle$$$("ui", "config.sl.limit.walk"));
        panel8.add(label17, new GridConstraints(4, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label18 = new JLabel();
        this.$$$loadLabelText$$$(label18, this.$$$getMessageFromBundle$$$("ui", "config.sl.limit.dig"));
        panel8.add(label18, new GridConstraints(5, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        this.$$$loadLabelText$$$(label19, this.$$$getMessageFromBundle$$$("ui", "config.sl.limit.pick"));
        panel8.add(label19, new GridConstraints(6, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        limitTimeBox = new JCheckBox();
        limitTimeBox.setSelected(false);
        limitTimeBox.setText("");
        panel8.add(limitTimeBox, new GridConstraints(3, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        limitWalkBox = new JCheckBox();
        limitWalkBox.setText("");
        panel8.add(limitWalkBox, new GridConstraints(4, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        limitDigBox = new JCheckBox();
        limitDigBox.setText("");
        panel8.add(limitDigBox, new GridConstraints(5, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        limitPickBox = new JCheckBox();
        limitPickBox.setEnabled(true);
        limitPickBox.setText("");
        panel8.add(limitPickBox, new GridConstraints(6, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private class DocumentListenerImpl implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            onChangeConfig(false);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onChangeConfig(false);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onChangeConfig(false);
        }
    }

    private boolean isEnvironmentOK() {
        try {
            Class.forName("org.objectweb.asm.MethodVisitor");
            Class.forName("org.objectweb.asm.ClassWriter");
            Class.forName("org.objectweb.asm.ClassReader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
