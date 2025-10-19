package me.jissee.jarsauth.gui;

import javax.swing.*;

public abstract class AbstractModWindow {
    protected JFrame frame;

    public AbstractModWindow() {
        String title = getTitle();
        if (title == null) title = "title";
        frame = new JFrame(title);
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void show(){
        frame.setContentPane(getPanel()); // panel1 是 public 的
        frame.setVisible(true); // 显示窗口
        //frame.pack(); // 自动调整大小
        frame.setLocationRelativeTo(null); // 居中显示
        onShow();
    }

    public void dispose(){
        frame.setVisible(false);
        frame.dispose();
    }

    protected abstract void onShow();

    public abstract String getTitle();

    public abstract JPanel getPanel();
    protected static void showError(String message, String title){
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    protected static void showInfo(String message, String title){
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
    }

    protected static boolean showConfirmation(String message, String title){
        int result = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
        return result == JOptionPane.YES_OPTION;
    }
}
