package me.jissee.jarsauth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public final class DisconnectionHandler {
    private static final Queue<Task> TASKS = new ConcurrentLinkedQueue<>();

    private static final class Task {
        private final ServerPlayer player;
        private final Component reason;
        private int remainingTicks;

        Task(ServerPlayer player, Component reason, int delayTicks) {
            this.player = player;
            this.reason = reason != null ? reason : Component.literal("");
            this.remainingTicks = delayTicks;
        }

        /**
         * @return true 表示任务已完成，需要从队列移除
         */
        boolean handleDisconnect() {
            // 玩家已无效，直接结束任务
            if (player == null || player.connection == null) {
                return true;
            }

            if (remainingTicks > 0) {
                remainingTicks--;
                return false;
            }

            player.connection.disconnect(reason);
            return true;
        }
    }

    public static void addPlayerToBeRemove(ServerPlayer player, Component reason, int delayTick) {
        if (player == null) return;
        TASKS.add(new Task(player, reason, delayTick));
    }

    public static void tick() {
        TASKS.removeIf(Task::handleDisconnect);
    }
}

