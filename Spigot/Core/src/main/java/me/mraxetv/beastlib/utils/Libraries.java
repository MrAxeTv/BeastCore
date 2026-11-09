package me.mraxetv.beastlib.utils;


import com.alessiodp.libby.Library;

public enum Libraries implements LibraryObject {


    MYSQL("com.mysql", "mysql-connector-j", "8.2.0", "mysql_connector_library", "com{}mysql", "me{}mraxetv{}beastlib{}lib{}mysql");


    private final String groupID;
    private final String artifactID;
    private final String version;
    private final String id;
    private final String oldRelocation;
    private final String newRelocation;

    Libraries(String groupID, String artifactID, String version, String id, String oldRelocation, String newRelocation) {
        this.groupID = groupID;
        this.artifactID = artifactID;
        this.version = version;
        this.id = id;
        this.oldRelocation = oldRelocation;
        this.newRelocation = newRelocation;
    }


    @Override
    public Library createRelocatedLibrary() {
        return Library.builder().groupId(groupID).artifactId(artifactID).version(version).relocate(oldRelocation,newRelocation).build();
    }

    @Override
    public Library createLibrary() {
        return Library.builder().groupId(groupID).artifactId(artifactID).version(version).build();
    }

    @Override
    public boolean hasRelocations() {
        return oldRelocation != null;
    }

}
