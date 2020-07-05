package entities.xmiReference;


import java.util.*;

/**
 * Represents entities associated with a given location
 */
public class AnchoredEntities {
    /**
     * key1: text or note
     * key2: location
     * key3: entity type (PER,LOC,etc)
     * key4: entity text (type)
     * val:     token count
     */
    HashMap<String,HashMap<String,HashMap<String,HashMap<String,Integer>>>> entities;

    public AnchoredEntities() {
        entities = new HashMap<>();
        entities.put("text", new HashMap<>());

    }

    public void updateTextInstance(String location, String neType, String entity) {
        updateLocation(entities.get("text"), location, neType, entity);
    }

    private void updateLocation(HashMap<String,HashMap<String,HashMap<String,Integer>>> map, String location, String neType, String entity) {

        entity = entity.replaceAll("\n", "");
        location = formatLocation(location);
        if (map.containsKey(location))
            updateNEType(map.get(location), neType, entity);
        else {
            HashMap<String,HashMap<String,Integer>> map1 = new HashMap<>();
            map1.put(neType, initMap(entity));
            map.put(location, map1);
        }
    }

    private String formatLocation(String location) {
        String[] parts = location.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p: parts) {
            sb.append(p.substring(0, 1).toUpperCase());
            sb.append(p.substring(1).toLowerCase());
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private void updateNEType(HashMap<String,HashMap<String,Integer>> map, String neType, String entity) {
        if (map.containsKey(neType))
            updateEntity(map.get(neType), entity);
        else {
            map.put(neType, initMap(entity));
        }

    }

    private void updateEntity(HashMap<String,Integer> map, String entity) {
        if (map.containsKey(entity))
            map.put(entity, map.get(entity) + 1);
        else
            map.put(entity, 1);
    }

    private HashMap<String, Integer> initMap(String entity) {
        HashMap<String,Integer> map = new HashMap<>();
        map.put(entity, 1);
        return map;
    }

    public void initNoteMap() {
        entities.put("notes", new HashMap<>());
    }

    public void updateNoteInstance(String location, String neType, String entity) {

        updateLocation(entities.get("notes"), location, neType, entity);
    }

    public List<AnchoredEntity> values() {
        List<AnchoredEntity> list = new LinkedList<>();
        list.addAll(locationValues("text", entities.get("text")));
        if (entities.containsKey("notes"))
            list.addAll(locationValues("notes", entities.get("notes")));
        return list;
    }

    private List<AnchoredEntity> locationValues(String source, HashMap<String, HashMap<String, HashMap<String, Integer>>> map) {
        List<AnchoredEntity> list = new LinkedList<>();
        map.entrySet().forEach(e -> list.addAll(neTypeValues(source, e.getKey(), e.getValue())));
        return list;
    }

    private Collection<AnchoredEntity> neTypeValues(String source, String location, HashMap<String,HashMap<String,Integer>> map) {
        List<AnchoredEntity> list = new LinkedList<>();
        map.entrySet().forEach(e -> list.addAll(entityValues(source, location, e.getKey(), e.getValue())));
        return list;
    }

    private Collection<? extends AnchoredEntity> entityValues(String source, String location, String neType, HashMap<String,Integer> map) {
        List<AnchoredEntity> list = new LinkedList<>();
        map.entrySet().forEach(e -> list.add(new AnchoredEntity(source, location, neType, e.getKey(), e.getValue())));
        return list;
    }
}
