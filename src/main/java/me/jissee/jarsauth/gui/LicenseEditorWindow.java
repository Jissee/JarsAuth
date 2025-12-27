package me.jissee.jarsauth.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import me.jissee.jarsauth.data.model.PeriodType;
import me.jissee.jarsauth.data.model.ServerLicense;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static me.jissee.jarsauth.data.TimeUtil.*;

public class LicenseEditorWindow extends AbstractModWindow {
    private final String title;
    private final ServerLicense oldLicense;
    private final Consumer<ServerLicense> onConfirm;

    private JPanel panel1;
    private JTextField idText;
    private JTextField validFromText;
    private JTextField validUntilText;
    private JTextField allowanceText;
    private JButton cancelButton;
    private JButton confirmButton;
    private JButton nowButton;
    private JCheckBox mondayBox;
    private JCheckBox tuesdayBox;
    private JCheckBox wednesdayBox;
    private JCheckBox thursdayBox;
    private JCheckBox fridayBox;
    private JCheckBox saturdayBox;
    private JCheckBox sundayBox;
    private JButton everydayButton;
    private JButton singleUseButton;
    private JTextField resetTimeText;
    private JTextField clearTimeText;
    private JButton reverseButton;

    private final Map<JCheckBox, PeriodType> periodBoxMap;


    private class KeyAdapter extends java.awt.event.KeyAdapter {
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                onConfirm(null);
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                dispose();
            }
        }
    }

    public LicenseEditorWindow(String title, ServerLicense oldLicense, Consumer<ServerLicense> onConfirm) {
        frame.setSize(500, 300);
        this.title = title;
        this.oldLicense = oldLicense;
        this.onConfirm = onConfirm;
        frame.setTitle(getTitle());
        cancelButton.addActionListener(this::onCancel);
        confirmButton.addActionListener(this::onConfirm);


        idText.setText(oldLicense.id());
        validFromText.setText(oldLicense.validFrom().toString());
        validUntilText.setText(oldLicense.validUntil().toString());
        resetTimeText.setText(oldLicense.resetTime().toString());
        clearTimeText.setText(oldLicense.clearTime().toString());
        allowanceText.setText(String.valueOf(oldLicense.allowance()));

        idText.addKeyListener(new KeyAdapter());
        validFromText.addKeyListener(new KeyAdapter());
        validUntilText.addKeyListener(new KeyAdapter());
        clearTimeText.addKeyListener(new KeyAdapter());
        resetTimeText.addKeyListener(new KeyAdapter());
        allowanceText.addKeyListener(new KeyAdapter());
        nowButton.addActionListener(this::today);

        periodBoxMap = Map.of(
                mondayBox, PeriodType.MONDAY,
                tuesdayBox, PeriodType.TUESDAY,
                wednesdayBox, PeriodType.WEDNESDAY,
                thursdayBox, PeriodType.THURSDAY,
                fridayBox, PeriodType.FRIDAY,
                saturdayBox, PeriodType.SATURDAY,
                sundayBox, PeriodType.SUNDAY
        );

        int type = oldLicense.type();
        for (Map.Entry<JCheckBox, PeriodType> entry : periodBoxMap.entrySet()) {
            JCheckBox checkBox = entry.getKey();
            PeriodType periodType = entry.getValue();
            checkBox.setSelected((type & periodType.getCode()) != 0);
        }

        everydayButton.addActionListener(e -> {
            for (Map.Entry<JCheckBox, PeriodType> entry : periodBoxMap.entrySet()) {
                JCheckBox checkBox = entry.getKey();
                PeriodType periodType = entry.getValue();
                if (periodType.getCode() <= PeriodType.SUNDAY.getCode()) {
                    checkBox.setSelected(true);
                } else {
                    checkBox.setSelected(false);
                }
            }
        });

        singleUseButton.addActionListener(e -> {
            for (Map.Entry<JCheckBox, PeriodType> entry : periodBoxMap.entrySet()) {
                JCheckBox checkBox = entry.getKey();
                checkBox.setSelected(false);
            }
        });

        reverseButton.addActionListener(e -> {
            for (Map.Entry<JCheckBox, PeriodType> entry : periodBoxMap.entrySet()) {
                JCheckBox checkBox = entry.getKey();
                checkBox.setSelected(!checkBox.isSelected());
            }
        });


    }

    private void today(ActionEvent e) {
        validFromText.setText(LocalDate.now().toString());
    }

    private void onCancel(ActionEvent e) {
        dispose();
    }

    private void onConfirm(ActionEvent e) {
        try {
            ServerLicense newLicense = new ServerLicense(
                    idText.getText(),
                    LocalDate.parse(validFromText.getText()),
                    LocalDate.parse(validUntilText.getText()),
                    parseType(),
                    LocalTime.parse(resetTimeText.getText()),
                    LocalTime.parse(clearTimeText.getText()),
                    Long.parseLong(allowanceText.getText())
            );
            onConfirm.accept(newLicense);
            dispose();
        } catch (Exception ex) {
            showError(ex.toString(), "");
        }
    }

    private int parseType() {
        int result = 0;
        for (Map.Entry<JCheckBox, PeriodType> entry : periodBoxMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                result |= entry.getValue().getCode();
            }
        }
        return result;
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
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(8, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        mondayBox = new JCheckBox();
        this.$$$loadButtonText$$$(mondayBox, this.$$$getMessageFromBundle$$$("ui", "label.server.license.monday"));
        panel2.add(mondayBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tuesdayBox = new JCheckBox();
        this.$$$loadButtonText$$$(tuesdayBox, this.$$$getMessageFromBundle$$$("ui", "label.server.license.tuesday"));
        panel2.add(tuesdayBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wednesdayBox = new JCheckBox();
        this.$$$loadButtonText$$$(wednesdayBox, this.$$$getMessageFromBundle$$$("ui", "label.server.license.wednesday"));
        panel2.add(wednesdayBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thursdayBox = new JCheckBox();
        this.$$$loadButtonText$$$(thursdayBox, this.$$$getMessageFromBundle$$$("ui", "label.server.license.thursday"));
        panel2.add(thursdayBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fridayBox = new JCheckBox();
        this.$$$loadButtonText$$$(fridayBox, this.$$$getMessageFromBundle$$$("ui", "label.server.license.friday"));
        panel2.add(fridayBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saturdayBox = new JCheckBox();
        this.$$$loadButtonText$$$(saturdayBox, this.$$$getMessageFromBundle$$$("ui", "label.server.license.saturday"));
        panel2.add(saturdayBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sundayBox = new JCheckBox();
        this.$$$loadButtonText$$$(sundayBox, this.$$$getMessageFromBundle$$$("ui", "label.server.license.sunday"));
        panel2.add(sundayBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        everydayButton = new JButton();
        this.$$$loadButtonText$$$(everydayButton, this.$$$getMessageFromBundle$$$("ui", "label.server.license.everyday"));
        panel2.add(everydayButton, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        singleUseButton = new JButton();
        this.$$$loadButtonText$$$(singleUseButton, this.$$$getMessageFromBundle$$$("ui", "label.server.license.singleuse"));
        singleUseButton.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.singleuse.tip"));
        panel2.add(singleUseButton, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("ui", "label.server.license.time.reset"));
        label1.setToolTipText("");
        panel2.add(label1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resetTimeText = new JTextField();
        resetTimeText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.time.reset.tip"));
        panel2.add(resetTimeText, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        clearTimeText = new JTextField();
        clearTimeText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.time.clear.tip"));
        panel2.add(clearTimeText, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("ui", "label.server.license.time.clear"));
        panel2.add(label2, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("ui", "label.server.license.type"));
        panel2.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reverseButton = new JButton();
        this.$$$loadButtonText$$$(reverseButton, this.$$$getMessageFromBundle$$$("ui", "label.server.license.reverse"));
        panel2.add(reverseButton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(8, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(292, 269), null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("ui", "label.server.license.id"));
        panel3.add(label4, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        idText = new JTextField();
        panel3.add(idText, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("ui", "label.server.license.valid.from"));
        panel3.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nowButton = new JButton();
        nowButton.setOpaque(false);
        this.$$$loadButtonText$$$(nowButton, this.$$$getMessageFromBundle$$$("ui", "label.server.license.now"));
        panel3.add(nowButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validFromText = new JTextField();
        validFromText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.valid.tip"));
        panel3.add(validFromText, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("ui", "label.server.license.valid.until"));
        panel3.add(label6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validUntilText = new JTextField();
        validUntilText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.valid.tip"));
        panel3.add(validUntilText, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, this.$$$getMessageFromBundle$$$("ui", "label.server.license.allowance"));
        panel3.add(label7, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        allowanceText = new JTextField();
        allowanceText.setToolTipText(this.$$$getMessageFromBundle$$$("ui", "label.server.license.allowance.tip"));
        panel3.add(allowanceText, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cancelButton = new JButton();
        this.$$$loadButtonText$$$(cancelButton, this.$$$getMessageFromBundle$$$("ui", "info.cancel"));
        panel4.add(cancelButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        confirmButton = new JButton();
        this.$$$loadButtonText$$$(confirmButton, this.$$$getMessageFromBundle$$$("ui", "info.confirm"));
        panel4.add(confirmButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    @Override
    protected void onShow() {

    }

    @Override
    public String getTitle() {
        if (title == null) return null;
        return title;
    }

    @Override
    public JPanel getPanel() {
        return panel1;
    }
}
