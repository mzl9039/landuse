package zju.mzl.landuse.Aco;

import java.util.HashMap;

/**
 * Created by mzl on 2016/11/17.
 */
public class Problem {

    public int[] getNum() {
        return num;
    }

    public void setNum(int[] num) {
        this.num = num;
    }

    public Grid[][] getGrids() {
        return grids;
    }

    public void setGrids(Grid[][] grids) {
        this.grids = grids;
    }

    public HashMap<String, Target> getTargets() {
        return targets;
    }

    public void setTargets(HashMap<String, Target> targets) {
        this.targets = targets;
    }

    private int num[];				// 格网的行和列，num[0]为行数，num[1]为列数
    private Grid grids[][];
    private HashMap<String, Target> targets;

    public Problem() {
        targets = new HashMap<>();
    }

    public Problem(int num[], Grid grids[][]) {
        this.num = num;
        this.grids = grids;
    }

    public Grid[][] gridsClone() {
        int row = this.num[0], col = this.num[1];
        Grid grids[][] = Grid.gridsClone(this.getGrids(), row, col);
        return grids;
    }


}
