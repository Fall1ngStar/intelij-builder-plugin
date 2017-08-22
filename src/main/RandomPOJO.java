package main;

import java.util.List;
import java.util.UUID;
import java.io.Serializable;

public class RandomPOJO extends ParentPOJO implements Serializable{
    private static final long serialVersionUID = -51531L;
    public final Integer aInteger;
    public final String aString;
    public final Double aDouble;
    public final List<Float> aFloatList;

    public RandomPOJO(UUID uuid, Integer aInteger, String aString, Double aDouble, List<Float> aFloatList) {
        super(uuid);
        this.aInteger = aInteger;
        this.aString = aString;
        this.aDouble = aDouble;
        this.aFloatList = aFloatList;
    }

    
}
