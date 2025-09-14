public class BoundingBox {
    public Vec2 min, max;

    public BoundingBox(float min_x, float min_y, float max_x, float max_y) {
        min = new Vec2(min_x, min_y);
        max = new Vec2(max_x, max_y);
    }

    public Vec2 OverlapSize(BoundingBox b) {
        float ox = Math.min(max.x, b.max.x) - Math.max(min.x, b.min.x);
        float oy = Math.min(max.y, b.max.y) - Math.max(min.y, b.min.y);
        return new Vec2(ox, oy);
    }

    public boolean intersect(BoundingBox b) {
        return (min.x <= b.max.x) && (max.x >= b.min.x) && (min.y <= b.max.y) && (max.y >= b.min.y);
    }
}