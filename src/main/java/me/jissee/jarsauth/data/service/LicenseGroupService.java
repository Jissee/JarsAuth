package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.LicenseGroupDAO;

import java.util.Set;

public class LicenseGroupService implements Service {
    private final LicenseGroupDAO dao;
    public LicenseGroupService(ConnectionProvider provider) {
        this.dao = new LicenseGroupDAO(provider);
    }
    public void ensureDefaultGroup(){
        Set<String> names = dao.getAllGroupNames();
        if(names.contains("default")){
            return;
        }
        dao.insertGroup("default");
    }
    public Set<String> getAllGroupNames(){
        return dao.getAllGroupNames();
    }

    public void addGroup(String group){
        dao.insertGroup(group);
    }

    public void removeGroup(String group) {
        dao.deleteGroup(group);
    }

    public void renameGroup(String oldName, String newName) {
        dao.renameGroup(oldName, newName);
    }

    @Override
    public void initTable() {
        dao.initTable();
    }
}
