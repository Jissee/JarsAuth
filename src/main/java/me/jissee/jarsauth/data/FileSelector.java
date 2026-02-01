package me.jissee.jarsauth.data;

import me.jissee.jarsauth.Codec;
import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.model.FileList;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class FileSelector {

    private final Path root;
    private final List<Pattern> positiveFilters = new ArrayList<>();
    private final List<Pattern> negativeFilters = new ArrayList<>();
    private AcceptedDetail optionalDataSource = null; // 可选数据源

    public FileSelector(String rootPath) {
        this.root = Paths.get(rootPath);
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Root path must be an existing directory");
        }
    }

    /** 设置可选数据源 */
    public void setDataSource(AcceptedDetail dataSource) {
        this.optionalDataSource = dataSource;
    }

    public void addFilter(String filter) {
        boolean isNegative = false;
        String patternStr;

        if (filter.startsWith("!:")) {       // 负向正则
            isNegative = true;
            patternStr = filter.substring(2);
        } else if (filter.startsWith(":")) { // 正向正则
            patternStr = filter.substring(1);
        } else if (filter.startsWith("!")) { // 负向通配符
            isNegative = true;
            patternStr = wildcardToRegex(filter.substring(1));
        } else {                             // 正向通配符
            patternStr = wildcardToRegex(filter);
        }

        Pattern pattern = Pattern.compile("^" + patternStr + "$");
        if (isNegative) negativeFilters.add(pattern);
        else positiveFilters.add(pattern);
    }

    private String wildcardToRegex(String s) {
        s = s.replace(".", "\\.");
        s = s.replace("?", ".");
        s = s.replace("*", ".*");
        return s;
    }

    public FileList getFileList() throws IOException {
        if (optionalDataSource != null) {
            // 使用可选数据源
            return getFileListFromDataSource();
        } else {
            // 使用文件系统扫描
            return getFileListFromFileSystem();
        }
    }

    /** 从文件系统扫描 */
    private FileList getFileListFromFileSystem() throws IOException {
        List<String> candidateFolders = new ArrayList<>();
        List<String> candidateFiles = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String rel = root.relativize(dir).toString();
                if (!rel.isEmpty() && matchesAny(positiveFilters, rel)) {
                    candidateFolders.add(rel);
                }
                if (!rel.isEmpty() && matchesAny(negativeFilters, rel)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String rel = root.relativize(file).toString();
                if (matchesAny(positiveFilters, rel)) {
                    candidateFiles.add(rel);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        List<String> finalFolders = filterNegative(candidateFolders);
        List<String> finalFiles = filterNegative(candidateFiles);

        Collections.sort(finalFolders);
        Collections.sort(finalFiles);

        return new FileList(finalFolders, finalFiles);
    }

    /** 从 AcceptedDetail 数据源获取 */
    private FileList getFileListFromDataSource() {
        List<String> candidateFolders = new ArrayList<>();
        List<String> candidateFiles = new ArrayList<>();

        // 文件夹过滤
        for (String folder : optionalDataSource.folders()) {
            if (matchesAny(positiveFilters, folder) && !matchesAny(negativeFilters, folder)) {
                candidateFolders.add(folder);
            }
        }

        // 文件过滤
        for (String file : optionalDataSource.files().keySet()) {
            if (matchesAny(positiveFilters, file) && !matchesAny(negativeFilters, file)) {
                candidateFiles.add(file);
            }
        }

        Collections.sort(candidateFolders);
        Collections.sort(candidateFiles);

        return new FileList(candidateFolders, candidateFiles);
    }

    private List<String> filterNegative(List<String> candidates) {
        List<String> result = new ArrayList<>();
        for (String path : candidates) {
            if (!matchesAny(negativeFilters, path)) result.add(path);
        }
        return result;
    }

    private boolean matchesAny(List<Pattern> patterns, String path) {
        for (Pattern p : patterns) {
            if (p.matcher(path).matches()) return true;
        }
        return false;
    }

    public AcceptedDetail scan(String name) throws Exception {
        AcceptedDetail detail = new AcceptedDetail(name);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs){

                if (!dir.equals(root)) {
                    String rel = root.relativize(dir).toString();
                    detail.addFolder(rel);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs){
                if (attrs.isRegularFile()) {
                    String rel = root.relativize(file).toString();
                    String sha256 = Codec.getFSHA256(file.toFile());
                    detail.addFile(rel, sha256);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return detail;
    }


}
