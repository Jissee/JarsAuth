package me.jissee.jarsauth.gui;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

public class ImmutableTableModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
