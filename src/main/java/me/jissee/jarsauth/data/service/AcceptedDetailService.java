package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.dao.AccGroupDAO;
import me.jissee.jarsauth.data.dao.AccInfoDAO;
import me.jissee.jarsauth.data.model.AccInfoEntry;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;

public class AcceptedDetailService implements Service {
    private final AccGroupDAO groupDAO;
    private final AccInfoDAO infoDAO;

    private AcceptedDetail buffer;
    private final Object bufferLock = new Object();

    public void initTable(){
        groupDAO.initTable();
        infoDAO.initTable();
    }

    public AcceptedDetailService(Connection connection) {
        this.groupDAO = new AccGroupDAO(connection);
        this.infoDAO = new AccInfoDAO(connection);
    }

    public AcceptedDetail createNewDetail() {
        List<String> existing = groupDAO.getAllGroupNames();
        int index = 0;
        while (existing.contains("acc" + index)) index++;
        return new AcceptedDetail("acc" + index);
    }

    public List<String> getAllGroupNames() {
        return groupDAO.getAllGroupNames();
    }

    public List<String> getRegisteredAccGroupNames() {
        return infoDAO.getRegisteredAccGroupNames();
    }

    public AcceptedDetail getGroup(String group) {
        if (!groupDAO.getAllGroupNames().contains(group)) {
            return new AcceptedDetail(group);
        }
        List<AccInfoEntry> entries = infoDAO.getAllEntries(group);
        AcceptedDetail detail = new AcceptedDetail(group);
        for (AccInfoEntry entry : entries) {
            if ("folder".equals(entry.value())) {
                detail.addFolder(entry.key());
            } else {
                detail.addFile(entry.key(), entry.value());
            }
        }
        return detail;
    }

    public AcceptedDetail getAllFromPath(String group, String path) {
        List<AccInfoEntry> exactEntries = infoDAO.getEntriesByKey(group, path);
        if (!exactEntries.isEmpty()) {
            AccInfoEntry first = exactEntries.get(0);
            if (first.value().length() == 64) {
                throw new IllegalArgumentException("Cannot use * with a file.");
            }
        }

        List<AccInfoEntry> prefixEntries = infoDAO.getEntriesByPrefix(group, path);
        AcceptedDetail result = new AcceptedDetail(group);
        for (AccInfoEntry e : prefixEntries) {
            if (e.value().length() == 64) {
                result.addFile(e.key(), e.value());
            } else {
                result.addFolder(e.key());
            }
        }
        return result;
    }

    public AcceptedDetail getExactFromPath(String group, String path) {
        List<AccInfoEntry> exactEntries = infoDAO.getEntriesByKey(group, path);
        if (exactEntries.isEmpty()) {
            return new AcceptedDetail(group);
        }

        AccInfoEntry first = exactEntries.get(0);
        if (first.value().length() == 64) {
            AcceptedDetail detail = new AcceptedDetail(group);
            detail.addFile(first.key(), first.value());
            return detail;
        }

        int slashCount = (int) path.chars().filter(ch -> ch == '/').count();

        List<AccInfoEntry> entries = infoDAO.getEntriesByPrefixWithDepth(group, path, slashCount + 1);
        AcceptedDetail result = new AcceptedDetail(group);
        for (AccInfoEntry e : entries) {
            if (e.value().length() == 64) {
                result.addFile(e.key(), e.value());
            } else {
                result.addFolder(e.key());
            }
        }
        return result;
    }

    public void saveAcceptedDetail(AcceptedDetail detail) {
        String group = detail.groupName();
        if (groupDAO.getAllGroupNames().contains(group)) {
            groupDAO.deleteGroup(group);
        }
        groupDAO.insertGroup(group);
        infoDAO.insertEntries(group, detail.files(), detail.folders());
    }

    public boolean buffer(String key, String value, int totalCount){
        if(buffer == null){
            buffer = createNewDetail();
        }
        synchronized (bufferLock){
            if("folder".equals(value)){
                buffer.addFolder(key);
            }else{
                buffer.addFile(key, value);
            }
            LoggerFactory.getLogger("Authinfo").info("[{}/{}] {} --> {}",buffer.getTotalCount(), totalCount, key, value);
            if(totalCount == buffer.getTotalCount()){
                saveAcceptedDetail(buffer);
                buffer = null;
                return true;
            }
            return false;
        }
    }

    public void removeGroup(String group) {
        groupDAO.deleteGroup(group);
    }

    public void renameGroup(String oldName, String newName) {
        groupDAO.renameGroup(oldName, newName);
    }
}
