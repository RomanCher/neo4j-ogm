package org.neo4j.ogm.mapper;

import org.neo4j.ogm.entityaccess.FieldAccess;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ObjectMemo {

    private final Map<Object, Long> objectHash = new HashMap<>();

    // objects with no properties will always hash to this value.
    private static final long seed = 0xDEADBEEF / (11 * 257);

    /**
     * constructs a 64-bit hash of this object's node properties
     * and maps the object to that hash. The object must not be null
     * @param object the object whose persistable properties we want to hash
     * @param classInfo metadata about the object
     */
    public void remember(Object object, ClassInfo classInfo) {
        objectHash.put(object, hash(object, classInfo));
    }

    /**
     * determines whether the specified has already
     * been memorised. The object must not be null. An object
     * is regarded as memorised if its hash value in the memo hash
     * is identical to a recalculation of its hash value.
     *
     * @param object the object whose persistable properties we want to check
     * @param classInfo metadata about the object
     * @return true if the object hasn't changed since it was remembered, false otherwise
     */
    public boolean remembered(Object object, ClassInfo classInfo) {
        return objectHash.containsKey(object) && hash(object, classInfo) == objectHash.get(object);
    }

    public void clear() {
        objectHash.clear();
    }

    public boolean contains(Object o) {
        return objectHash.containsKey(o);
    }


    private static long hash(Object object, ClassInfo classInfo) {
        long hash = seed;
        for (FieldInfo fieldInfo : classInfo.propertyFields()) {
            Field field = classInfo.getField(fieldInfo);
            Object value = FieldAccess.read(field, object);
            if (value != null) {
                hash = hash * 31L + hash(value.toString());
            }
        }
        return hash;
    }

    private static long hash(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31*h + string.charAt(i);
        }
        return h;
    }
}
