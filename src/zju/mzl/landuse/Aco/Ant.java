package zju.mzl.landuse.Aco;

import javafx.geometry.Pos;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mzl on 2016/11/17.
 */
public class Ant {

    public Grid getGrid(Position p) {
        if (this.tours != null && p != null) {
            return this.tours[p.x][p.y];
        } else {
            return null;
        }
    }

    public Position getCurrentPos() {
        return currentPos;
    }

    public void setCurrentPos(Position currentPos) {
        this.currentPos = currentPos;
    }

    public Grid getCurGrid() {
        return getGrid(this.currentPos);
    }

    public int getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }

    public int[][] getTabu() {
        return tabu;
    }

    public void setTabu(int[][] tabu) {
        this.tabu = tabu;
    }

    public Grid[][] getTours() {
        return tours;
    }

    public void setTours(Grid[][] tours) {
        this.tours = tours;
    }

    public Map<String, Double> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(Map<String, Double> neighbours) {
        this.neighbours = neighbours;
    }

    public void adjustAntByGrids() {
        int row, col;
        if (this.tours != null) {
            row = col = this.tours.length;
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    if (this.tours[i][j] == null) {
                        this.tabu[i][j] = 1;
                    } else {
                        int type = Utils.lu8tolu4(this.tours[i][j].dlbm8);
                        if (type == 1)  this.farmArea += 1;
                        else if (type == 3 && tours[i][j].dlbm8 != 11 && tours[i][j].dlbm8 != 12) this.consLandArea += 1;
                        else if (type == 2) this.grassArea += 1;
                    }
                }
            }
        }
    }

    public void initTarget(Map<String, Target> targets) {
        this.target.clear();
        for (Map.Entry<String, Target> e : targets.entrySet()) {
            this.target.put(e.getKey(), 0.0);
        }
    }

    // 参数为要转为的类型
    public boolean canConvert(Position p, int to) {
        if (Utils.canConvert(this.getTours()[p.x][p.y].dlbm8, to, this.getTours()[p.x][p.y])
                && farmLandCanConvert(to) && consLandCanConvertTo(to) && grassCanBeConvertedTo(to)) {
            return true;
        }
        return false;
    }

    // 可以从农用地转为其它用地
    private boolean farmLandCanConvert(int to) {
        Position p = this.currentPos;
        if (this.getTours()[p.x][p.y].dlbm4 == 1) {
            if ((Utils.lu8toIdx(to) != 1 && this.farmArea - 1 >= Utils.minFarmArea)
                    || Utils.lu8toIdx(to) == 1) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    // 可以从其它用地转为建设用地
    private boolean consLandCanConvertTo(int to) {
        Position p = this.currentPos;
        if (Utils.lu8tolu4(to) == 3) {
            if ((this.getTours()[p.x][p.y].dlbm4 != 3 && this.consLandArea + 1 <= Utils.maxConsArea)
                    || (this.getTours()[p.x][p.y].dlbm4 == 3 && this.getTours()[p.x][p.y].dlbm8 != 11)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean grassCanBeConvertedTo(int to) {
        Position p = this.currentPos;
        if (Utils.lu8tolu4(to) == 4) {
            if((this.getTours()[p.x][p.y].dlbm4 != 4 && this.grassArea + 1 <= Utils.maxGrassArea)
                    || this.getTours()[p.x][p.y].dlbm4 == 4) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean adjustArea(int from, int to, Position p) {
        // 除草地外，其它地类，邻域内至少有一个这种地类，才能进行地类类型改变
        int dlbm4 = Utils.lu8tolu4(to);
        boolean result = true;
        int neighbourToTypeNum = 0;     // p邻域有几个和to相同类型的格网
        for (int i = p.x-1; i <= p.x + 1; i++) {
            for (int j = p.y-1; j <= p.y+1; j++) {
                if ( !(i==p.x && j==p.y) && ((dlbm4 != 2) && this.getTours()[i][j] != null)) {
                    if (this.getTours()[i][j].dlbm4 == dlbm4) {
                        neighbourToTypeNum++;
                    }
                }
            }
        }
        result = neighbourToTypeNum >= 2 ? true : false;
        return result && farmConvert(from, to) && convertToCons(from, to) && convertToGrass(from, to);
    }

    private boolean farmConvert(int from, int to) {
        if (Utils.lu8tolu4(from) == 1 && Utils.lu8toIdx(to) != 1) {
            if (this.farmArea-1 >= Utils.minFarmArea) {
                this.farmArea -= 1;
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean convertToCons(int from, int to) {
        if (Utils.lu8toIdx(from) != 3 && Utils.lu8tolu4(to) == 3) {
            if (this.consLandArea+1 <= Utils.maxConsArea) {
                this.consLandArea += 1;
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean convertToGrass(int from, int to) {
        if (Utils.lu8toIdx(from) != 4 && Utils.lu8tolu4(to) == 4) {
            if (this.grassArea + 1 <= Utils.maxGrassArea) {
                this.grassArea += 1;
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public Ant() { }

    public Ant(int updated, int stop, int tabu[][], double f) {
        this.updated = updated;
        this.stop = stop;
        this.tabu = tabu;
        this.neighbours = new HashMap();
        this.f = f;
        this.target = new HashMap<>();
    }

    public Ant clone() {
        Ant a = new Ant();
        int row, col;
        row = col = this.tours.length;
        a.setTours(Grid.gridsClone(this.getTours(), row, col));
        a.target = this.target;
        Double d1 = new Double(this.consLandArea);
        a.consLandArea = d1.doubleValue();
        Double d2 = new Double(this.farmArea);
        a.farmArea = d2.doubleValue();
        a.currentPos = this.currentPos;
        Double d3 = new Double(this.f);
        a.f = d3.doubleValue();
        a.looptime = looptime;
        return a;
    }

    // 比较当前的蚂蚁、结果中的蚂蚁的目标优劣情况，返回值情况有：
    // 返回 1：优于结果中的蚂蚁，进行过替换
    // 返回 0：与结果中的某只蚂蚁情况相同，未进行替换
    // 返回 -1：比结果中的所有蚂蚁都要差
    // 原理采用Pareto最优解进化原理
    public static int targetCompareTo(Ant a, Ant aInResults) {
        // 对每个目标，若a与aInResults的该目标相等，则res.0加1，若小于，则res.-1加1；若大于，则res.1加1
        HashMap<Integer, Integer> res = new HashMap<Integer, Integer>() {
            {   put(1,0);   put(0,0);   put(-1,0); }
        };
        for (Map.Entry<String, Double> e : a.target.entrySet()) {
            if (e.getValue() > aInResults.target.get(e.getKey())) {
                res.replace(1, res.get(1) + 1);
            } else if (e.getValue() < aInResults.target.get(e.getKey())) {
                res.replace(-1, res.get(-1) + 1);
            } else {
                res.replace(0, res.get(0) + 1);
            }
        }
        if (res.get(-1) != 0) {
            return -1;
        } else {
            if (res.get(0) == a.target.size()) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    public void StatTours() {
        this.statGrids = Grid.statGrids(this.tours, this.tours.length, this.tours.length);
        //System.out.println("蚂蚁的目标函数值f:" + this.f);
        //statGrids.forEach((k, v) -> System.out.print("k:" + k + "; v:" + v + "\t\t"));
    }

    private Position currentPos;
    public Position patchCenter;
    public int updated;                     // 用来记录更新过的格网的数目
    private int stop;                       // 用来标记是否所有的格网都访问过了
    private int tabu[][];                   // 禁忌表，即已经访问过的格网
    private Grid tours[][];                 // 尝试只记录相应位置的type
    private Map<String, Double> neighbours; // 当前 patch 的所有邻居
    public double f;
    public HashMap<String, Double> target;
    public double farmArea, consLandArea;       // 农用地面积，建设用地面积，注意农用地有最小值，建设用地有最大值
    public double grassArea;                    // 加草地限制
    public HashMap<Integer, Integer> statGrids;
    public int statTransform[][];
    public int looptime;
}
