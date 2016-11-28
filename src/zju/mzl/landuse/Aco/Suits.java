package zju.mzl.landuse.Aco;

import java.util.HashMap;

/**
 * Created by mzl on 2016/11/19.
 */
public class Suits {
    int x, y;
    HashMap<Integer, Double> valMap;

    public Suits() {
        valMap = new HashMap<>();
    }

    public Suits(int x, int y, HashMap<Integer, Double> valMap) {
        this.x = x;
        this.y = y;
        this.valMap = valMap;
    }
}
