package me.jissee.jarsauth.gui.render;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.function.Supplier;

public class RuleListRenderer extends DefaultListCellRenderer {
    private final Supplier<Set<String>> provider;

    public RuleListRenderer(Supplier<Set<String>> validLicenseIdsProvider) {
        this.provider = validLicenseIdsProvider;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        Component c = super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        if (value instanceof String text) {
            if (text.startsWith(":")) {
                Set<String> set = provider.get();
                int idx = text.lastIndexOf("(");
                text = text.substring(0, idx - 1);
                if (set.contains(text.substring(1))) {
                    c.setForeground(Color.BLUE);
                } else {
                    c.setForeground(Color.RED);
                }
            } else {
                c.setForeground(Color.BLACK);
            }
        }

        return c;
    }
}
