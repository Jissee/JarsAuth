package me.jissee.jarsauth.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.TimeUtil;
import me.jissee.jarsauth.data.model.*;
import me.jissee.jarsauth.data.service.*;
import me.jissee.jarsauth.gui.render.LicenseInstanceTableRenderer;
import me.jissee.jarsauth.gui.render.LicenseTableRenderer;
import me.jissee.jarsauth.gui.render.RuleListRenderer;
import me.jissee.jarsauth.manip.ObfConfigBuilder;
import me.jissee.jarsauth.manip.Pipeline;
import me.jissee.jarsauth.manip.RandomStringGenerator;
import me.jissee.jarsauth.manip.Tuple;
import me.jissee.jarsauth.verification.Verification;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static me.jissee.jarsauth.data.TimeUtil.formatDuration;
import static me.jissee.jarsauth.data.model.ServerLicenseInstance.nameTag;
import static me.jissee.jarsauth.gui.render.LocalDateTimeWrap.*;

public class MainWindow extends AbstractModWindow {
    public JPanel panel1;
    private JTabbedPane tabbedPane1;
    private JList<String> accGroupList;
    private JList<String> accRuleList;
    private JTextField serverIdText;
    private JTable userIdTable;
    private JButton reset;
    private JTable licenseTable;
    private JCheckBox isFCEnabledBox;
    private JCheckBox isCAEnabledBox;
    private JCheckBox isSLEnabledBox;
    private JTextField fcIntervalText;
    private JTextField caIntervalText;
    private JTextField slIntervalText;
    private JTextField fcTimeoutText;
    private JTextField caTimeoutText;
    private JComboBox<String> languageComboBox;
    private JButton exportButton;
    private JLabel signedLabel;
    private JLabel environmentLabel;
    private JLabel signableLabel;
    private JButton createObfDictButton;
    private JButton createObfConfig;
    private JTextField signedExpandedPathText;
    private JTextField dictPathText;
    private JTextField obfConfigPathText;
    private JList<String> licenseGroupList;
    private JScrollPane licenseScrollPane;
    private JList<String> licenseRuleList;
    private JTable licenseInstanceTable;
    private JScrollPane licenseInstancePane;
    private JTable rowHeaderTable;
    private JScrollPane licenseInstanceHeaderPane;

    private boolean isConfigInitialized = false;
    private boolean isChangingCheckBox = false;
    private Set<String> validLicenseIds;

    private void createUIComponents() {
        // 这里初始化被标记为 Custom Create 的组件
        rowHeaderTable = new JTableWithTooltip(); // 或者其他自定义组件
        // 可在这里做初始化操作，例如设置列宽、Tooltip 等
    }

    public MainWindow() {
        $$$setupUI$$$();
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
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    onViewClientFiles();
                    return;
                }
            }
        });

        accRuleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onAccRuleListEditBatch();
                }
            }

        });

        accRuleList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                    onAccRuleListDeleteSelection();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_INSERT || e.getKeyCode() == KeyEvent.VK_HELP || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onAccRuleListEditBatch();
                    return;
                }
            }
        });


        licenseGroupList.addListSelectionListener(this::onLicenseGroupListChangeSelection);

        licenseGroupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onLicenseGroupListRenameSelection();
                }
            }

        });

        licenseGroupList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                    onLicenseGroupListDeleteSelection();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_INSERT || e.getKeyCode() == KeyEvent.VK_HELP) {
                    onLicenseGroupListAddNew();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onLicenseGroupListRenameSelection();
                    return;
                }
            }
        });

        licenseRuleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onLicenseRuleListEditBatch();
                }
            }

        });

        licenseRuleList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                    onLicenseRuleListDeleteSelection();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_INSERT || e.getKeyCode() == KeyEvent.VK_HELP || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onLicenseRuleListEditBatch();
                    return;
                }
            }
        });

        licenseRuleList.setCellRenderer(new RuleListRenderer(() -> validLicenseIds));

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

        licenseTable.addKeyListener(new KeyAdapter() {
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

        licenseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onLicenseTableEdit();
                }
            }

        });

        licenseTable.setDefaultRenderer(Object.class, new LicenseTableRenderer());

        userIdTable.getActionMap().put("selectNextRowCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        licenseTable.getActionMap().put("selectNextRowCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        licenseInstanceTable.getActionMap().put("selectNextRowCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        licenseInstanceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        //rowHeadTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        rowHeaderTable.setSelectionModel(licenseInstanceTable.getSelectionModel());

        exportButton.addActionListener(this::onExport);
        createObfDictButton.addActionListener(this::onCreateObfDict);
        createObfConfig.addActionListener(this::onCreateObfConfig);

        JViewport licenseViewport = licenseScrollPane.getViewport();
        licenseViewport.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                licenseTable.requestFocus();
            }
        });

        JViewport licenseInstanceViewport = licenseInstancePane.getViewport();
        licenseInstanceViewport.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                licenseInstanceTable.requestFocus();
            }
        });
        licenseInstanceTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    System.out.println("refresh");
                    ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
                    if (e.isShiftDown()) {
                        boolean result = service.updateAndVerifyForPlayer("Dev");
                        System.out.println(result);
                        //service.updateInstances(new ArrayList<>(), false);
                    } else {
                        //service.updateInstances(new ArrayList<>(), true);
                    }
                    String name = licenseGroupList.getSelectedValue();
                    updateServerLicenseInstanceTable(name);
                }
            }
        });

        licenseInstanceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onLicenseInstanceTableEdit();
                }
            }
        });

        licenseInstanceTable.setDefaultRenderer(Object.class, new LicenseInstanceTableRenderer());


        licenseInstanceHeaderPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        licenseInstanceHeaderPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        licenseInstancePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

    }


    public void onShow() {
        updateAccGroupList();
        updateServerId();
        updateClientIdTable();
        updateServerLicensePage();
        updateSignatureStatus();
        readConfig();
    }


    @Override
    public String getTitle() {
        return Locales.getString("title.window.main");
    }

    private void onViewClientFiles() {
        String nameWithTag = accGroupList.getSelectedValue();
        String groupName = removeAccGroupTag(nameWithTag);
        AccProfileService service = DataManager.getServerInstance().getService(AccProfileService.class);
        List<String> list = service.getAllFileNames(groupName);
        FileListWindow window = new FileListWindow(Locales.getString("title.window.file"), list);
        window.show();
    }

    private void onAccGroupListRenameSelection() {
        String nameWithTag = accGroupList.getSelectedValue();
        if (nameWithTag == null) return;
        String oldName = removeAccGroupTag(nameWithTag);

        TextEditorWindow textEditorWindow = new TextEditorWindow(Locales.getString("title.window.edit"), oldName, (newName) -> {
            AccProfileService service = DataManager.getServerInstance().getService(AccProfileService.class);
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
            String name = removeAccGroupTag(nameWithTag);
            updateAccRuleList(name);
        }
    }

    private void onAccGroupListDeleteSelection() {
        String nameWithTag = accGroupList.getSelectedValue();
        String groupName = removeAccGroupTag(nameWithTag);
        AccProfileService service = DataManager.getServerInstance().getService(AccProfileService.class);
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
        TextEditorWindow window = new TextEditorWindow(Locales.getString("title.window.add"), "", (newName) -> {
            AccProfileService accService = DataManager.getServerInstance().getService(AccProfileService.class);
            accService.saveAcceptedDetail(new AcceptedDetail(newName));
            updateAccGroupList();
        });
        window.show();
    }

    private void onAccRuleListDeleteSelection() {
        String nameWithTag = accGroupList.getSelectedValue();
        String groupName = removeAccGroupTag(nameWithTag);
        String rule = accRuleList.getSelectedValue();

        AuthRuleService service = DataManager.getServerInstance().getService(AuthRuleService.class);
        try {
            if (showConfirmation(Locales.getString("info.delete.confirm") + rule + " (" + groupName + ")", "")) {
                service.removeRule(groupName, rule);
                updateAccRuleList(groupName);
            }
        } catch (Exception ex) {
            showError(Locales.getString("info.delete.failed"), "");
        }

    }

    private void onAccRuleListEditBatch() {
        String nameWithTag = accGroupList.getSelectedValue();
        if (nameWithTag == null) return;
        String groupName = removeAccGroupTag(nameWithTag);
        AuthRuleService service = DataManager.getServerInstance().getService(AuthRuleService.class);
        AuthRuleEntry profile = service.getUnflattenedRuleEntry(groupName);
        StringBuilder builder = new StringBuilder();
        for (String str : profile.rules()) {
            builder.append(str).append("\n");
        }
        if (!builder.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }

        TextAreaEditorWindow window = new TextAreaEditorWindow(Locales.getString("title.window.edit") + " (" + groupName + ")", builder.toString(), (newName) -> {
            AuthRuleService service1 = DataManager.getServerInstance().getService(AuthRuleService.class);
            service1.removeGroup(groupName);


            AuthRuleEntry profile1 = new AuthRuleEntry(groupName, Arrays.stream(newName.split("\n"))
                    .map(String::trim)           // 去除前后空白
                    .filter(s -> !s.isEmpty())   // 过滤空字符串
                    .collect(Collectors.toList()));

            service1.saveRuleEntry(profile1);

            updateAccGroupList();
            updateAccRuleList(groupName);
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
            updateClientIdTable();
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

    private static String removeAccGroupTag(String taggedName) {
        int tagPos = taggedName.lastIndexOf(TAG_CLIENT_GROUP);
        if (tagPos != -1) {
            return taggedName.substring(tagPos + TAG_CLIENT_GROUP.length());
        } else {
            tagPos = taggedName.lastIndexOf(TAG_CUSTOM_GROUP);
            return taggedName.substring(tagPos + TAG_CUSTOM_GROUP.length());
        }
    }

    private static String removeLicenseRuleTag(String taggedName) {
        int tagPos = taggedName.lastIndexOf("(");
        String removedBrackets = taggedName;
        if (tagPos != -1) {
            removedBrackets = taggedName.substring(0, tagPos - 1);
        }
        return removedBrackets;
    }

    private void updateAccGroupList() {
        int selected = accGroupList.getSelectedIndex();
        DataManager manager = DataManager.getServerInstance();
        AccProfileService service = manager.getService(AccProfileService.class);

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

    private void updateAccRuleList(String groupName) {
        AuthRuleService service = DataManager.getServerInstance().getService(AuthRuleService.class);
        AuthRuleEntry profileExpanded = service.getTaggedFlattenRuleEntry(groupName);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addAll(profileExpanded.rules());
        accRuleList.setModel(listModel);
    }

    private void updateServerId() {
        ServerIdService service = DataManager.getServerInstance().getService(ServerIdService.class);
        UUID uuid = service.getOrCreateServerId();
        serverIdText.setText(uuid.toString());
    }

    private void updateClientIdTable() {
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

    private void updateServerLicensePage() {
        updateServerLicenseTable();
        updateServerLicenseGroupList();
    }

    private void updateServerLicenseTable() {
        DefaultTableModel model = new ImmutableTableModel();

        model.addColumn(Locales.getString("label.server.license.id"));
        model.addColumn(Locales.getString("label.server.license.valid.from"));
        model.addColumn(Locales.getString("label.server.license.valid.until"));
        model.addColumn(Locales.getString("label.server.license.time.reset"));
        model.addColumn(Locales.getString("label.server.license.time.clear"));
        model.addColumn(Locales.getString("label.server.license.type"));
        model.addColumn(Locales.getString("label.server.license.allowance"));


        ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
        List<ServerLicense> licenses = service.getAllLicenses();
        this.validLicenseIds = service.getAllLicensesIds();


        for (ServerLicense license : licenses) {
            int type = license.type();
            String typeTag = PeriodType.parse(type, Locales::getString);
            if (type == 0) {
                model.addRow(new Object[]{
                        ":" + license.id(),
                        from(license.validFrom(), license.resetTime()),
                        until(license.validUntil(), license.clearTime()),
                        "",
                        "",
                        typeTag,
                        formatDuration(license.allowance())
                });
            } else {
                model.addRow(new Object[]{
                        ":" + license.id(),
                        from(license.validFrom(), null),
                        until(license.validUntil(), null),
                        license.resetTime(),
                        license.clearTime(),
                        typeTag,
                        formatDuration(license.allowance())
                });
            }

        }


        licenseTable.setModel(model);

        DefaultTableColumnModel columnModel = (DefaultTableColumnModel) licenseTable.getColumnModel();

        //columnModel.getColumn(0).setMaxWidth(120);
        columnModel.getColumn(1).setMinWidth(120);
        columnModel.getColumn(2).setMinWidth(120);

    }

    private void updateServerLicenseInstanceTable(String groupName) {

        LicenseGroupRuleService lgrs = DataManager.getServerInstance().getService(LicenseGroupRuleService.class);
        LicenseGroupRuleEntry ruleEntry = lgrs.getTaggedFlattenRuleEntry(groupName);
        ServerLicenseService sls = DataManager.getServerInstance().getService(ServerLicenseService.class);
        Set<String> validIdSet = sls.getAllLicensesIds();

        List<String> players = new ArrayList<>();
        List<String> validIds = new ArrayList<>();
        List<String> validChains = new ArrayList<>();
        List<String> invalidIds = new ArrayList<>();
        List<String> invalidChains = new ArrayList<>();


        for (String rule : ruleEntry.rules()) {
            if (rule.startsWith(":")) {
                int idx = rule.lastIndexOf('(');
                int endx = rule.lastIndexOf(')');
                String id = rule.substring(1, idx - 1);
                String groupChain = rule.substring(idx + 1, endx);
                if (validIdSet.contains(id)) {
                    validIds.add(id);
                    validChains.add(groupChain);
                } else {
                    invalidIds.add(id);
                    invalidChains.add(groupChain);
                }
            } else {
                int idx = rule.lastIndexOf('(');
                players.add(rule.substring(0, idx - 1));
            }
        }

        var combinedValid = sls.combineLicenseIdsAndGroupChain(validIds, validChains, true);
        DefaultTableModel licenseTableModel = new ImmutableTableModel();
        for (var pair : combinedValid) {
            licenseTableModel.addColumn(nameTag(pair.getA(), pair.getB()));
        }

        var combinedInvalid = sls.combineLicenseIdsAndGroupChain(invalidIds, invalidChains, false);
        for (var pair : combinedInvalid) {
            licenseTableModel.addColumn(nameTag(pair.getA(), pair.getB()));
        }
        DefaultTableModel headerModel = new ImmutableTableModel();
        headerModel.addColumn(Locales.getString("label.server.license.instance.player.name"));

        var resultMap = sls.getRemainingMatrixForGroup(groupName);

        for (String playerName : resultMap.keySet()) {
            headerModel.addRow(new Object[]{playerName});
            Object[] row = new Object[validIds.size() + invalidIds.size()];
            int col = 0;
            for (var validId : combinedValid) {
                String key = nameTag(validId.getA(), validId.getB());
                long remaining = resultMap.get(playerName).getOrDefault(key, -1L);
                String remainingStr;
                if (remaining > -1) {
                    remainingStr = TimeUtil.formatDuration(remaining);
                } else {
                    remainingStr = "-1";
                }
                row[col] = remainingStr;
                col++;
            }
            for (var invalidId : invalidIds) {
                row[col] = "N/A";
                col++;
            }
            licenseTableModel.addRow(row);
        }


        rowHeaderTable.setModel(headerModel);
        licenseInstanceTable.setModel(licenseTableModel);
        JTableUtil.adjustColumnWidths(licenseInstanceTable);
        licenseInstanceHeaderPane.getVerticalScrollBar()
                .setModel(licenseInstancePane.getVerticalScrollBar().getModel());
    }

    private void updateServerLicenseGroupList() {
        int selected = licenseGroupList.getSelectedIndex();
        DataManager manager = DataManager.getServerInstance();
        LicenseGroupService service = manager.getService(LicenseGroupService.class);
        service.ensureDefaultGroup();

        Set<String> allGroupNames = service.getAllGroupNames();

        Set<String> groupNames = new TreeSet<>(allGroupNames);

        DefaultListModel<String> model = new DefaultListModel<>();

        groupNames.stream().sorted().forEach(model::addElement);
        licenseGroupList.setModel(model);
        licenseGroupList.setSelectedIndex(selected);
    }

    private void updateServerLicenseRuleList(String groupName) {
        LicenseGroupRuleService service = DataManager.getServerInstance().getService(LicenseGroupRuleService.class);
        LicenseGroupRuleEntry profile = service.getTaggedFlattenRuleEntry(groupName);

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addAll(profile.rules());
        licenseRuleList.setModel(model);
    }

    private ServerLicense getLicenseInRow(int row) {
        DefaultTableModel model = (DefaultTableModel) licenseTable.getModel();
        String id = model.getValueAt(row, 0).toString();
        if (id.startsWith(":")) {
            id = id.substring(1);
        }
        ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);

        return service.getLicense(id);
    }


    private void onLicenseGroupListRenameSelection() {
        String oldName = licenseGroupList.getSelectedValue();

        TextEditorWindow textEditorWindow = new TextEditorWindow(Locales.getString("title.window.edit"), oldName, (newName) -> {
            LicenseGroupService service = DataManager.getServerInstance().getService(LicenseGroupService.class);
            try {
                service.renameGroup(oldName, newName);
            } catch (Exception ex) {
                showError(Locales.getString("info.rename.failed"), "");
            }
            updateServerLicenseGroupList();
        });
        textEditorWindow.show();
    }

    private void onLicenseGroupListChangeSelection(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            String name = licenseGroupList.getSelectedValue();
            updateServerLicenseRuleList(name);
            updateServerLicenseInstanceTable(name);
        }
    }

    private void onLicenseGroupListDeleteSelection() {
        String groupName = licenseGroupList.getSelectedValue();
        LicenseGroupService service = DataManager.getServerInstance().getService(LicenseGroupService.class);
        try {
            if (showConfirmation(Locales.getString("info.delete.confirm") + " " + groupName, "")) {
                service.removeGroup(groupName);
            }
        } catch (Exception ex) {
            showError(Locales.getString("info.delete.failed"), "");
        }
        updateServerLicenseGroupList();
    }

    private void onLicenseGroupListAddNew() {
        TextEditorWindow window = new TextEditorWindow(Locales.getString("title.window.edit"), "", (newName) -> {
            LicenseGroupService service = DataManager.getServerInstance().getService(LicenseGroupService.class);
            service.addGroup(newName);
            updateServerLicenseGroupList();
        });
        window.show();
    }

    private void onLicenseRuleListDeleteSelection() {
        String groupName = licenseGroupList.getSelectedValue();
        String taggedName = licenseRuleList.getSelectedValue();
        String rule = removeLicenseRuleTag(taggedName);

        LicenseGroupRuleService service = DataManager.getServerInstance().getService(LicenseGroupRuleService.class);
        try {
            if (showConfirmation(Locales.getString("info.delete.confirm") + rule + " (" + groupName + ")", "")) {
                service.removeRule(groupName, rule);
                updateServerLicenseRuleList(groupName);
            }
        } catch (Exception ex) {
            showError(Locales.getString("info.delete.failed"), "");
        }

    }

    private void onLicenseRuleListEditBatch() {
        String groupName = licenseGroupList.getSelectedValue();
        LicenseGroupRuleService service = DataManager.getServerInstance().getService(LicenseGroupRuleService.class);
        LicenseGroupRuleEntry profile = service.getUnflattenedRuleEntry(groupName);
        StringBuilder builder = new StringBuilder();
        for (String str : profile.rules()) {
            builder.append(str).append("\n");
        }
        if (!builder.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }

        TextAreaEditorWindow window = new TextAreaEditorWindow(Locales.getString("title.window.edit") + " (" + groupName + ")", builder.toString(), (newName) -> {
            LicenseGroupRuleService service1 = DataManager.getServerInstance().getService(LicenseGroupRuleService.class);
            service1.removeGroup(groupName);


            LicenseGroupRuleEntry profile1 = new LicenseGroupRuleEntry(groupName, Arrays.stream(newName.split("\n"))
                    .map(String::trim)           // 去除前后空白
                    .filter(s -> !s.isEmpty())   // 过滤空字符串
                    .collect(Collectors.toSet())
            );

            service1.saveRuleEntry(profile1);

            updateServerLicenseGroupList();
            updateServerLicenseRuleList(groupName);
        });
        window.show();
    }


    private void onLicenseTableDeleteSelection() {
        int row = licenseTable.getSelectedRow();
        ServerLicense oldLicense = getLicenseInRow(row);

        ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
        if (showConfirmation(Locales.getString("info.delete.confirm") + oldLicense.toStringFormatted(), "")) {
            service.removeLicense(oldLicense.id());
            updateServerLicensePage();
        }
    }

    private void onLicenseTableAddNew() {
        ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
        ServerLicense oldLicense = ServerLicense.getDefault(service);

        LicenseEditorWindow window = new LicenseEditorWindow(Locales.getString("title.window.edit"), oldLicense, (newLicense) -> {
            service.saveLicense(newLicense);
            updateServerLicensePage();
        });
        window.show();
    }

    private void onLicenseTableEdit() {
        int row = licenseTable.getSelectedRow();
        ServerLicense oldLicense = getLicenseInRow(row);
        final String oldId = oldLicense.id();
        LicenseEditorWindow window = new LicenseEditorWindow(Locales.getString("title.window.edit"), oldLicense, (newLicense) -> {
            ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
            service.updateLicense(oldId, newLicense);
            updateServerLicensePage();
        });
        window.show();
    }

    private void onLicenseInstanceTableEdit() {
        int row = licenseInstanceTable.getSelectedRow();
        int column = licenseInstanceTable.getSelectedColumn();
        String player = rowHeaderTable.getValueAt(row, 0).toString();
        String groupName = licenseGroupList.getSelectedValue();

        String rule = licenseInstanceTable.getColumnName(column);
        rule = rule.substring(1);
        int index = rule.lastIndexOf('(');
        int endIndex = rule.lastIndexOf(')');
        String licenseId = rule.substring(0, index - 1);
        String groupChain = rule.substring(index + 1, endIndex);
        String value = licenseInstanceTable.getValueAt(row, column).toString();

        ServerLicenseService service = DataManager.getServerInstance().getService(ServerLicenseService.class);
        ServerLicenseInstance instance = service.getLicenseInstance(licenseId, player, groupName, groupChain);
        ServerLicense license = service.getLicense(licenseId);


        if (value.equals("N/A")) {
            showInfo(Locales.getString("info.no.license"), "");
        } else if (instance == null) {
            showInfo(Locales.getString("info.no.instance"), "");
        } else {
            if (!license.isValid(LocalDateTime.now())) {
                showInfo(Locales.getString("info.license.expired"), "");
            }
            LicenseInstanceWindow window = new LicenseInstanceWindow(Locales.getString("title.window.edit"), instance, (newLicense) -> {
                long newRemaining = newLicense.remaining();
                ServerLicense license1 = service.getLicense(newLicense.licenseId());
                long limit = license1.allowance();
                if (newRemaining > limit) {
                    showInfo(Locales.getString("info.instance.allowance.exceed"), "");
                    newLicense = newLicense.withRemaining(limit);
                }
                service.saveInstances(List.of(newLicense), true);
                updateServerLicenseInstanceTable(groupName);
            });
            window.show();
        }

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
        RandomStringGenerator generator = new RandomStringGenerator(1000, 10);
        File outFile = generator.toFile();
        showInfo(String.format(Locales.getString("info.exported.obf.dict"), outFile.getAbsoluteFile()), "");
        dictPathText.setText(outFile.getAbsolutePath());
    }

    private void onCreateObfConfig(ActionEvent actionEvent) {
        String input = signedExpandedPathText.getText();
        String output = input.replace(".jar", "-obf.jar");
        String dict = dictPathText.getText();
        String mapping = dictPathText.getText().replace("dict.txt", "mapping.txt");
        if (input.isEmpty()) {
            showError(Locales.getString("info.invalid.target"), "");
            return;
        }
        if (dict.isEmpty()) {
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
            slIntervalText.setText(String.valueOf(service.getValue(ConfigKey.SERVER_LICENSE_INTERVAL)));

        } else {
            isSLEnabledBox.setSelected(false);
            slIntervalText.setEnabled(false);
            slIntervalText.setText("");
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

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
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
        accRuleList = new JList();
        accRuleList.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.profile.rule.rip"));
        scrollPane1.setViewportView(accRuleList);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        panel2.add(panel4, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("ui", "label.profile.name"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel4.add(label2, gbc);
        final JScrollPane scrollPane2 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(scrollPane2, gbc);
        accGroupList = new JList();
        accGroupList.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.profile.name.tip"));
        scrollPane2.setViewportView(accGroupList);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.client.auth"), null, panel5, this.$$$getMessageFromBundle$$$("ui", "tab.client.auth.tip"));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("ui", "label.client.id"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel5.add(label3, gbc);
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("ui", "label.server.id"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel5.add(label4, gbc);
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
        final JScrollPane scrollPane3 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel5.add(scrollPane3, gbc);
        userIdTable = new JTable();
        userIdTable.setAutoResizeMode(0);
        scrollPane3.setViewportView(userIdTable);
        reset = new JButton();
        this.$$$loadButtonText$$$(reset, this.$$$getMessageFromBundle$$$("ui", "label.server.id.reset"));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel5.add(reset, gbc);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.setRequestFocusEnabled(true);
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.server.license"), null, panel6, this.$$$getMessageFromBundle$$$("ui", "tab.server.license.tip"));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        licenseScrollPane = new JScrollPane();
        panel7.add(licenseScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        licenseTable = new JTable();
        licenseScrollPane.setViewportView(licenseTable);
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("ui", "label.server.license.license"));
        panel7.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridBagLayout());
        panel6.add(panel8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("ui", "label.server.license.group"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        panel8.add(label6, gbc);
        final JScrollPane scrollPane4 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel8.add(scrollPane4, gbc);
        licenseGroupList = new JList();
        licenseGroupList.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.group.tip"));
        scrollPane4.setViewportView(licenseGroupList);
        final JScrollPane scrollPane5 = new JScrollPane();
        scrollPane5.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.rule.tip"));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel8.add(scrollPane5, gbc);
        licenseRuleList = new JList();
        licenseRuleList.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.rule.tip"));
        scrollPane5.setViewportView(licenseRuleList);
        licenseInstancePane = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 2.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel8.add(licenseInstancePane, gbc);
        licenseInstanceTable = new JTable();
        licenseInstancePane.setViewportView(licenseInstanceTable);
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, this.$$$getMessageFromBundle$$$("ui", "label.server.license.rule"));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel8.add(label7, gbc);
        final JLabel label8 = new JLabel();
        label8.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel8.add(label8, gbc);
        licenseInstanceHeaderPane = new JScrollPane();
        licenseInstanceHeaderPane.setMaximumSize(new Dimension(10, 10));
        licenseInstanceHeaderPane.setMinimumSize(new Dimension(1, 1));
        licenseInstanceHeaderPane.setVerticalScrollBarPolicy(22);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel8.add(licenseInstanceHeaderPane, gbc);
        licenseInstanceHeaderPane.setViewportView(rowHeaderTable);
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, this.$$$getMessageFromBundle$$$("ui", "label.server.license.instance"));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        panel8.add(label9, gbc);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.signature"), panel9);
        environmentLabel = new JLabel();
        environmentLabel.setText("Label");
        panel9.add(environmentLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signableLabel = new JLabel();
        signableLabel.setText("Label");
        panel9.add(signableLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signedLabel = new JLabel();
        signedLabel.setText("Label");
        panel9.add(signedLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exportButton = new JButton();
        this.$$$loadButtonText$$$(exportButton, this.$$$getMessageFromBundle$$$("ui", "info.export.signed"));
        exportButton.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "info.export.signed.tip"));
        panel9.add(exportButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createObfDictButton = new JButton();
        this.$$$loadButtonText$$$(createObfDictButton, this.$$$getMessageFromBundle$$$("ui", "info.generate.obf.dict"));
        createObfDictButton.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "info.generate.obf.dict.tip"));
        panel9.add(createObfDictButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createObfConfig = new JButton();
        this.$$$loadButtonText$$$(createObfConfig, this.$$$getMessageFromBundle$$$("ui", "info.generate.obf.config"));
        panel9.add(createObfConfig, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel9.add(spacer1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel9.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        signedExpandedPathText = new JTextField();
        panel9.add(signedExpandedPathText, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        dictPathText = new JTextField();
        panel9.add(dictPathText, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        obfConfigPathText = new JTextField();
        panel9.add(obfConfigPathText, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(4, 6, new Insets(0, 0, 500, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("ui", "tab.config"), null, panel10, this.$$$getMessageFromBundle$$$("ui", "tab.config.tip"));
        isFCEnabledBox = new JCheckBox();
        isFCEnabledBox.setText("");
        panel10.add(isFCEnabledBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        this.$$$loadLabelText$$$(label10, this.$$$getMessageFromBundle$$$("ui", "config.fc.enabled"));
        panel10.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        this.$$$loadLabelText$$$(label11, this.$$$getMessageFromBundle$$$("ui", "config.ca.enabled"));
        panel10.add(label11, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        this.$$$loadLabelText$$$(label12, this.$$$getMessageFromBundle$$$("ui", "config.sl.enabled"));
        panel10.add(label12, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        isCAEnabledBox = new JCheckBox();
        isCAEnabledBox.setText("");
        panel10.add(isCAEnabledBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        isSLEnabledBox = new JCheckBox();
        isSLEnabledBox.setText("");
        panel10.add(isSLEnabledBox, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        this.$$$loadLabelText$$$(label13, this.$$$getMessageFromBundle$$$("ui", "config.fc.interval"));
        label13.setToolTipText("");
        panel10.add(label13, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        this.$$$loadLabelText$$$(label14, this.$$$getMessageFromBundle$$$("ui", "config.ca.interval"));
        label14.setToolTipText("");
        panel10.add(label14, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        this.$$$loadLabelText$$$(label15, this.$$$getMessageFromBundle$$$("ui", "config.sl.interval"));
        label15.setToolTipText("");
        panel10.add(label15, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fcIntervalText = new JTextField();
        fcIntervalText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel10.add(fcIntervalText, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        caIntervalText = new JTextField();
        caIntervalText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel10.add(caIntervalText, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        slIntervalText = new JTextField();
        slIntervalText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel10.add(slIntervalText, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label16 = new JLabel();
        this.$$$loadLabelText$$$(label16, this.$$$getMessageFromBundle$$$("ui", "config.fc.timeout"));
        label16.setToolTipText("");
        panel10.add(label16, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        this.$$$loadLabelText$$$(label17, this.$$$getMessageFromBundle$$$("ui", "config.ca.timeout"));
        label17.setToolTipText("");
        panel10.add(label17, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fcTimeoutText = new JTextField();
        fcTimeoutText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel10.add(fcTimeoutText, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        caTimeoutText = new JTextField();
        caTimeoutText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.unit.tip"));
        panel10.add(caTimeoutText, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        languageComboBox = new JComboBox();
        languageComboBox.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "config.ui.language.tip"));
        panel10.add(languageComboBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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


}
