package cn.oneachina.captureSpawn.item;

public record BallData(boolean captured, String entityType, String entityNbt) {
    public static BallData empty() {
        return new BallData(false, null, null);
    }
}
