package zju.mzl.landuse.Aco;

/**
 * Created by mzl on 2016/12/10.
 */
public class LUTarget extends Target {
    // 计算当前目标下土地利用强度
    @Override
    public double luComp(Position p, int type, Grid grids[][]) {
        return super.luComp(p, type, grids);
    }

    // 计算当前目标在格网 m,n 位置处的 局部目标函数值，
    // 注意局部目标函数值是一个邮权重suit 和 comp ，以及当前适宜度和土地利用强度共同计算的结果
    // 如果不需要计算土地利用强度，则suit和comp就不需要了，将suit设为1也可以
    @Override
    public double eta(Position p, int type, Grid grids[][]) {
        return 0;
    }

    @Override
    public double tau(Position p, int type, Pheromone ph) {
        return 0;
    }

    @Override
    public double targetVal(Grid[][] olds, Ant a) {
        int row, col;
        row = col = olds.length;
        double sum = 0;
        Grid[][] grids = a.getTours();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (grids[i][j] != null) {
                    sum += this.luComp(new Position(i, j), grids[i][j].dlbm4, grids);
                }
            }
        }
        return sum;
    }

    @Override
    public double targetVal2(Grid[][] olds, Ant a) {
        return targetVal(olds, a);
    }
}
