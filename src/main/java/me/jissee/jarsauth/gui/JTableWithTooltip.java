package me.jissee.jarsauth.gui;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class JTableWithTooltip extends JTable {

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        int col = columnAtPoint(e.getPoint());

        if (row < 0 || col < 0) {
            return null;
        }

        Object value = getValueAt(row, col);
        return value == null ? null : value.toString();
    }
}
