package zju.mzl.landuse.Aco;

/**
 * Created by mzl on 2016/11/20.
 */
public abstract class Target implements ITarget {

    @Override
    public double luComp(Position p, int type, Grid[][] grids) {
        int row, col;
        row = col = grids.length;
        double sametype = 0, windowNum = 0;
        for (int i = p.x-1; i <= p.x+1; i++) {
            for (int j = p.y-1; j <= p.y+1; j++) {
                if (i < 0 || j < 0 || i >= row || j >= col || (i == p.x && j == p.y) || grids[i][j] == null) {
                    continue;
                } else {
                    windowNum += 1;
                    if (this.getLuType() == 8) {
                        if (grids[i][j].dlbm8 == type) {
                            sametype += 1;
                        }
                    } else if (this.getLuType() == 4) {
                        if (grids[i][j].dlbm4 == type) {
                            sametype += 1;
                        }
                    }
                }
            }
        }
        return sametype / windowNum;
    }

    public double EtaNotConsiderLuComp(Position p, int type, Grid grids[][]) {
        return 0;
    }

    public double EtaConsiderLuComp(Position p, int type, Grid grids[][]) {
        return 0;
    }

    @Override
    public double eta(Position p, int type, Grid[][] grids) {
        if(this.luType == 4) {
            type = Utils.lu8tolu4(grids[p.x][p.y].dlbm8);
        }
        return this.isConsiderLuComp() == true
                ? EtaConsiderLuComp(p, type, grids)
                : EtaNotConsiderLuComp(p, type, grids);
    }

    // 返回当前日记p位置的信息素
    @Override
    public double tau(Position p, int type, Pheromone ph) {
        return ph.phero[p.x][p.y][type];
    }

    @Override
    public double targetVal(Grid olds[][], Ant a) {
        double res = 0.0;
        int row, col;
        row = col = olds.length;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (olds[i][j] != null) {
                    double t = eta(new Position(i, j), a.getTours()[i][j].dlbm8, a.getTours());
                    res = res + t;
                }
            }
        }
        return res;
    }

    public double targetVal2(Grid olds[][], Ant a) {
        double res = 0.0;
        int row, col;
        row = col = olds.length;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (olds[i][j] != null) {
                    double t = eta(new Position(i, j), a.getTours()[i][j].dlbm8, a.getTours());
                    res = res + t;
                }
            }
        }
        return res;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLuType() {
        return luType;
    }

    public void setLuType(int luType) {
        if (luType == 4 || luType == 8) {
            this.luType = luType;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getSuit() {
        return this.suit;
    }

    public void setSuit(double suit) {
        this.suit = suit;
        this.comp = 1 - suit;
    }

    public double getComp() {
        return this.comp;
    }

    public boolean isConsiderLuComp() {
        return considerLuComp;
    }

    public double getTargetVal() {
        return targetVal;
    }

    public void setTargetVal(double targetVal) {
        this.targetVal = targetVal;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    private String name;        // 目标名称
    private String description; // 对目标的描述
    private int luType;           // 对应的土地利用类型，是4大类还是8大类，只能取 4 或 8
    private double suit, comp;  // 本适宜性的权重和土地利用强度的权重，考虑 suit + comp = 1
    private String type;           // 目标类型
    public double targetVal;    // 目标函数的最终目标函数值
    protected boolean considerLuComp = true;    // 计算时考虑土地利用强度
}
