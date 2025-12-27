package me.jissee.jarsauth.gui;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;

public class JTableUtil {
    public static void adjustColumnWidths(JTable table) {
        FontMetrics fm = table.getFontMetrics(table.getFont());
        JTableHeader header = table.getTableHeader();
        FontMetrics headerFm = header.getFontMetrics(header.getFont());

        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);

            int maxWidth = 0;

            // 1. 表头宽度
            Object headerValue = column.getHeaderValue();
            if (headerValue != null) {
                maxWidth = headerFm.stringWidth(headerValue.toString());
            }

            // 2. 单元格内容宽度
            for (int row = 0; row < table.getRowCount(); row++) {
                Object cellValue = table.getValueAt(row, col);
                if (cellValue != null) {
                    int cellWidth = fm.stringWidth(cellValue.toString());
                    maxWidth = Math.max(maxWidth, cellWidth);
                }
            }

            // 3. 增加边距（避免贴边）
            maxWidth += 16;

            column.setPreferredWidth(maxWidth);
        }
    }

}
