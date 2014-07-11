package com.cloud.agent.resource.virtualnetwork;

public class FileConfigItem extends ConfigItem {
    private String filePath;
    private String fileName;
    private String fileContents;

    public FileConfigItem(String filePath, String fileName, String fileContents) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileContents = fileContents;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContents() {
        return fileContents;
    }

    public void setFileContents(String fileContents) {
        this.fileContents = fileContents;
    }

    @Override
    public String getAggregateCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("<file>\n");
        sb.append(filePath);

        // Don't use File.pathSeparator here as the target is the unix based systemvm
        if (!filePath.endsWith("/")) {
            sb.append('/');
        }

        sb.append(fileName);
        sb.append('\n');
        sb.append(fileContents);
        sb.append("\n</file>\n");
        return sb.toString();
    }

}
