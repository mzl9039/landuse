package zju.mzl.landuse.Aco;

/**
 * 规划成本目标 Planning Costs
 * Created by mzl on 2016/11/20.
 */
public class PCTarget extends Target {
    @Override
    // 计算当前目标下土地利用强度
    public double luComp(Position p, int type, Grid grids[][]) {
        return super.luComp(p, type, grids);
    }

    @Override
    public double EtaNotConsiderLuComp(Position p, int type, Grid grids[][]) {
        if (grids[p.x][p.y] != null) {
            return (1 / this.getSuits()[p.x][p.y].valMap.get(type));
        } else {
            return 0;
        }
    }

    @Override
    public double EtaConsiderLuComp(Position p, int type, Grid grids[][]) {
        if (grids[p.x][p.y] != null) {
            return  1 / (this.getSuit() * this.getSuits()[p.x][p.y].valMap.get(type)
                    + this.getComp() * luComp(p, type, grids));
        } else {
            return 0;
        }
    }

    // 成本函数一般是越小越优,这里调用了super，要看super里会不会再调用这个对象里的
    // EtaConsiderLuComp 和 EtaNotConsiderLuComp
    @Override
    public double eta(Position p, int type, Grid grids[][]) {
        return super.eta(p, type, grids);
    }

    @Override
    public double targetVal(Grid[][] olds, Ant a) {
        double res = 0.0;
        int row, col;
        row = col = olds.length;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (olds[i][j].dlbm8 != a.getTours()[i][j].dlbm8) {
                    res += eta(new Position(i, j), a.getTours()[i][j].dlbm8, a.getTours());
                }
            }
        }
        return 1 / res;
    }
}
