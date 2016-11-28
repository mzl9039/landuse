package zju.mzl.landuse.Aco;

import javafx.geometry.Pos;

/**
 * 适宜性评价相关的目标函数
 * Created by mzl on 2016/11/20.
 */
public class SETarget extends Target {
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
        return super.eta(p, type, grids);
    }

    @Override
    public double tau(Position p, int type, Pheromone ph) {
        return super.tau(p, type, ph);
    }

    @Override
    public double targetVal(Grid[][] olds, Ant a) {
        double res = 0.0;
        int row, col;
        row = col = olds.length;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (olds[i][j] != null && olds[i][j].dlbm8 != a.getTours()[i][j].dlbm8) {
                    res += eta(new Position(i, j), a.getTours()[i][j].dlbm8, a.getTours());
                }
            }
        }
        return res;
    }
}
