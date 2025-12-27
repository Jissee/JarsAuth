package me.jissee.jarsauth.gui.render;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class LicenseInstanceTableRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {

        super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column
        );

        if (isSelected) {
            setForeground(table.getSelectionForeground());
        } else if (value instanceof String text && "N/A".equals(text)) {
            setForeground(Color.RED);
        } else {
            setForeground(Color.BLACK);
        }

        return this;
    }
}
