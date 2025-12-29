package me.jissee.jarsauth.gui.render;

import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.model.ServerLicense;
import me.jissee.jarsauth.data.service.ServerLicenseService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDateTime;

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
            String taggedName = table.getColumnName(column);
            int idx = taggedName.lastIndexOf('(');
            String name = taggedName.substring(0, idx - 1).substring(1);
            ServerLicenseService sls = DataManager.getServerInstance().getService(ServerLicenseService.class);
            ServerLicense license = sls.getLicense(name);
            if (license == null || !license.isValid(LocalDateTime.now())) {
                setForeground(Color.RED);
            }else {
                setForeground(Color.BLACK);
            }
        }

        return this;
    }
}
