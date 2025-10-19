package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.dao.ServerLicenseDAO;
import me.jissee.jarsauth.data.model.ServerLicense;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

public class ServerLicenseService implements Service{
    private final ServerLicenseDAO dao;

    public ServerLicenseService(Connection connection) {
        this.dao = new ServerLicenseDAO(connection);
    }

    public List<ServerLicense> getAllLicenses() {
        return dao.getAllLicenses();
    }

    public List<ServerLicense> getLicensesForUser(String userName) {
        return dao.getLicensesForUser(userName);
    }

    public ServerLicense getLicense(UUID uuid) {
        return dao.getLicense(uuid);
    }

    public void saveLicense(ServerLicense license) {
        dao.saveLicense(license);
    }

    public void removeLicense(UUID uuid) {
        dao.removeLicense(uuid);
    }

    public void updateAllowance(UUID uuid, int delta) {
        ServerLicense license = dao.getLicense(uuid);
        if (license == null) {
            return;
        }
        if (!isTimeValid(license)) {
            return;
        }
        if (license.allowance().get() + delta < 0) {
            return;
        }
        dao.updateAllowance(uuid, delta);
    }

    public void updateLicense(ServerLicense license) {
        dao.updateLicense(license);
    }

    public void setAllowance(UUID uuid, int allowance) {
        dao.setAllowance(uuid, allowance);
    }

    public void removeExpiredLicenses() {
        dao.removeExpiredLicenses();
    }

    private boolean isTimeValid(ServerLicense license) {
        long now = System.currentTimeMillis() / 1000;
        return now >= license.validFrom() && now <= license.validUntil();
    }

    public boolean isLicenseValid(ServerLicense license) {
        return isTimeValid(license) && license.allowance().get() > 0;
    }

    @Override
    public void initTable() {
        dao.initTable();
    }
}
