package main;

import java.util.List;

/**
 * Created by thierry on 19/08/17.
 */
public class RandomPOJO {
    public final Integer aInteger;
    public final String aString;
    public final Double aDouble;
    public final List<Float> aFloatList;

    public RandomPOJO(Integer aInteger, String aString, Double aDouble, List<Float> aFloatList) {
        this.aInteger = aInteger;
        this.aString = aString;
        this.aDouble = aDouble;
        this.aFloatList = aFloatList;
    }


}
