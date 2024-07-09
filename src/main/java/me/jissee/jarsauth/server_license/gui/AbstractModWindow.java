/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_license.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public abstract class AbstractModWindow extends JFrame {
    protected static final Dimension WINDOW_SIZE_SMALL = new Dimension(400, 300);
    protected static final Dimension WINDOW_SIZE_WIDE = new Dimension(600, 500);
    protected static final Dimension BUTTON_SIZE = new Dimension(80, 20);
    protected static final Dimension TEXT_SIZE = new Dimension(160, 20);
    protected static final Dimension LIST_SIZE = new Dimension(560, 380);

    protected static final int COL_1 = 20;
    protected static final int COL_2 = 220;
    protected static final int COL_3 = 420;
    protected static final int COL_4 = 620;

    protected static final int ROW_1 = 20;
    protected static final int ROW_2 = 50;
    protected static final int ROW_3 = 80;
    protected static final int ROW_4 = 110;
    protected static final int ROW_5 = 140;
    protected static final int ROW_6 = 170;
    protected static final int ROW_7 = 200;
    protected static final int ROW_8 = 230;
    protected static final int ROW_9 = 260;

    protected static Point posText(int col, int row){
        return new Point(col * 200 -180, row * 30 - 10);
    }

    protected static Point posButton(int col, int row){
        return new Point(col * 200 -140, row * 30 - 10);
    }

    protected static void showError(String message, String title){
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    protected static void showInfo(String message, String title){
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
    }

    protected static boolean showConfirmation(String message, String title){
        int result = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
        return result == JOptionPane.YES_OPTION;
    }

    abstract void change2cn(ActionEvent event);
    abstract void change2en(ActionEvent event);
}
