package me.jissee.jarsauth.gui.render;

import me.jissee.jarsauth.data.model.ServerLicense;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class LicenseTableRenderer extends DefaultTableCellRenderer {
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
        if(value instanceof LocalDate date){
            try{
                LocalDate now = LocalDate.now();
                if (date.isBefore(now)) {
                    setForeground(Color.RED);
                } else {
                    setForeground(Color.GREEN);
                }
                return this;
            }catch (DateTimeParseException e){

            }
        }

        if(value instanceof LocalTime date){
            try{
                LocalTime now = LocalTime.now();
                if (date.isBefore(now)) {
                    setForeground(Color.RED);
                } else  {
                    setForeground(Color.GREEN);
                }
                return this;
            }catch (DateTimeParseException e){

            }
        }
        if(isSelected){
            setForeground(table.getSelectionForeground());
        }else {
            setForeground(Color.BLACK);
        }
        return this;
    }
}
