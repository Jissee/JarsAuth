package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;

import java.util.List;
import java.util.function.Function;

public class JarCustomTask implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    private final Runnable action;

    /**
     * 构造函数
     *
     * @param action 需要执行的自定义任务
     */
    public JarCustomTask(Runnable action) {
        this.action = action;
    }

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> input) {
        action.run(); // 执行自定义逻辑
        return input; // 不改变输入，原样返回
    }
}