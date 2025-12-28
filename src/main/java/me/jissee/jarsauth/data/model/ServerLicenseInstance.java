package me.jissee.jarsauth.data.model;

public record ServerLicenseInstance(
        String licenseId,
        String player,
        String groupName,
        String groupChain,
        long remaining
) {
    public static ServerLicenseInstance createNewEmpty(String licenseId, String userName, String groupName, String groupChain){
        return new ServerLicenseInstance(licenseId, userName, groupName, groupChain, 0);
    }
    public ServerLicenseInstance withRemaining(long remaining){
        return new ServerLicenseInstance(licenseId(), player(), groupName(), groupChain(), remaining);
    }

    public String getTaggedId(){
        return nameTag(licenseId, groupChain);
    }

    public static String nameTag(String licenseId, String groupChain){
        if(licenseId.startsWith(":")){
            licenseId = licenseId.substring(1);
        }
        if(groupChain.startsWith("(")){
            groupChain = groupChain.substring(1);
        }
        if(groupChain.endsWith(")")){
            groupChain = groupChain.substring(0, groupChain.length() - 1);
        }
        return ":" + licenseId + " (" + groupChain + ")";
    }

}
