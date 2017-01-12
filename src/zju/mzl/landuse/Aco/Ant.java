package zju.mzl.landuse.Aco;

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

    // 原理采用Pareto最优解进化原理比较两个目标，
    // 若对每个目标都有newTar不小于oldTar,则newTar是oldTar的非支配解，返回1
    // 若对每个目标都有oldTar不小于newTar，则oldTar是newTar的非支配解，返回-1
    // 若对不同的目标，newTar和oldTar的大小不一致，则无法确定两者的支配关系
    // 默认是非支配解，只要newTar有一个目标小于oldTar，则newTar不是非支配解，返回false，否则newTar是非支配解，返回true
    public static int CompareTargets(HashMap<String, Double> newTar, HashMap<String, Double> oldTar) {
        // 程序中，
        int res = 1;
        int com[] = new int[newTar.size()], index = -1;
        for (Map.Entry<String, Double> e : newTar.entrySet()) {
            index++;
            if (!valPass(e.getValue(), oldTar.get(e.getKey()))) {
                com[index] = -1;
            } else {
                com[index] = 1;
            }
        }
        res = com[0];
        for (int i = 1; i < com.length; i++) {
            if (res != com[i]) {
                res = 0;        // 可以直接break了
            }
        }
        return res;
    }

    // newVal在域值范围内不小于oldVar，则返回true，否则返回false
    public static boolean valPass(double newVal, double oldVal) {
        // 加上0.1的域值，否则Pareto解集太多了
        if (newVal + 0.1 < oldVal) {
            return false;
        } else {
            return true;
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
