package me.jissee.jarsauth.gui.render;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDateTime;
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
        if(value instanceof LocalDateTimeWrap date){
            try{
                LocalDateTime now = LocalDateTime.now();
                if(date instanceof LocalDateTimeWrap.From){
                    if (date.toLocalDateTime().isAfter(now)) {
                        setForeground(Color.GREEN);
                    } else {
                        setForeground(Color.RED);
                    }
                }else{
                    if (date.toLocalDateTime().isBefore(now)) {
                        setForeground(Color.RED);
                    } else {
                        setForeground(Color.GREEN);
                    }
                }


                return this;
            }catch (DateTimeParseException e){

            }
        }

        if(value instanceof LocalTime time){
            try{
                LocalTime now = LocalTime.now();
                if (time.isBefore(now)) {
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
