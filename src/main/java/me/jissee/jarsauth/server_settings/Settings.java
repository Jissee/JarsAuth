/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_settings;


import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class Settings {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static ServerID serverID;
    private static SettingFileChecksum settingFileChecksum;
    private static SettingClientAuth settingClientAuth;
    private static SettingServerLicense settingServerLicense;

    public synchronized static void loadAllSettings(String serverSaveDir){
        serverID = ServerID.load(serverSaveDir);
        settingFileChecksum = SettingFileChecksum.load(serverSaveDir);
        settingClientAuth = SettingClientAuth.load(serverSaveDir);
        settingServerLicense = SettingServerLicense.load(serverSaveDir);
    }
    public synchronized static ServerID getServerID(){
        return serverID;
    }
    public synchronized static SettingFileChecksum getFileChecksumSetting(){
        return settingFileChecksum;
    }
    public synchronized static SettingClientAuth getClientAuthSetting(){
        return settingClientAuth;
    }
    public synchronized static SettingServerLicense getServerLicenseSetting(){
        return settingServerLicense;
    }
    public static void printAll(){
        LOGGER.info("---FC---");
        LOGGER.info("enabled = {}", settingFileChecksum.isEnabled());
        LOGGER.info("timeout = {}", settingFileChecksum.getTimeout());
        LOGGER.info("interval = {}", settingFileChecksum.getInterval());
        LOGGER.info("inclusion = {}", settingFileChecksum.getInclusion());
        LOGGER.info("---CA---");
        LOGGER.info("enabled = {}", settingClientAuth.isEnabled());
        LOGGER.info("interval = {}", settingClientAuth.getInterval());
        LOGGER.info("timeout = {}", settingClientAuth.getTimeout());
        LOGGER.info("---SL---");
        LOGGER.info("enabled = {}", settingServerLicense.isEnabled());
        LOGGER.info("autoRemove = {}", settingServerLicense.isAutoRemove());
        LOGGER.info("authPerMinute = {}", settingServerLicense.getAuthPerMinute());
        LOGGER.info("generalAllowance = {}", settingServerLicense.getGeneralAllowance());
    }
}
