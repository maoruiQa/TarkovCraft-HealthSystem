package tnt.tarkovcraft.medsystem.client;

public final class ClientServerConfigState {
    private static boolean synced;
    private static float swordLightBleed;
    private static float swordHeavyBleed;
    private static float axeLightBleed;
    private static float axeHeavyBleed;
    private static float axeFracture;
    private static float bluntFracture;
    private static float bluntLightBleed;

    private ClientServerConfigState() {}

    public static void update(float sL, float sH, float aL, float aH, float aFx, float bFx, float bL) {
        swordLightBleed = sL;
        swordHeavyBleed = sH;
        axeLightBleed = aL;
        axeHeavyBleed = aH;
        axeFracture = aFx;
        bluntFracture = bFx;
        bluntLightBleed = bL;
        synced = true;
    }

    public static boolean isSynced() { return synced; }

    public static float getSwordLightBleed() { return swordLightBleed; }
    public static float getSwordHeavyBleed() { return swordHeavyBleed; }
    public static float getAxeLightBleed() { return axeLightBleed; }
    public static float getAxeHeavyBleed() { return axeHeavyBleed; }
    public static float getAxeFracture() { return axeFracture; }
    public static float getBluntFracture() { return bluntFracture; }
    public static float getBluntLightBleed() { return bluntLightBleed; }
}
