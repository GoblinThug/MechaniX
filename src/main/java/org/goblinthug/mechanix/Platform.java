package org.goblinthug.mechanix;

public final class Platform {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    private Platform() {}

    public static boolean isFolia() {
        return FOLIA;
    }
}