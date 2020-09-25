package deprecated.xmiReference;

@Deprecated
public class AnchoredEntity {

    String source;
    String location;
    String neType;
    String entity;
    int count;

    public AnchoredEntity(String source, String location, String neType, String entity, int count) {
        this.source = source;
        this.location = location;
        this.neType = neType;
        this.entity = entity;
        this.count = count;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return source + "," + location + "," + neType + "," + entity + "," + count;
    }
}
