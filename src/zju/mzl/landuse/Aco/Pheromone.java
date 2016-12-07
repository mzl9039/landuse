package zju.mzl.landuse.Aco;

/**
 * Created by mzl on 2016/11/21.
 */
public class Pheromone {
    final double min_phero = 0.5;
    final double max_phero = 12;
    double rho = 0.1;   // 信息素挥发系数，刚开始时挥发系数较小，随着时间推移，挥发系数越来越大，加快收敛
    int row = 0;
    int col = 0;
    int num = 0;
    public double phero[][][];
    // 信息素自适用调节系数阈值
    final double T1 = 10, T2 = 30, T3 = 50;
    final double Q1 = 0.5, Q2 = 1.5, Q3 = 3, Q4 = 4.5;

    public Pheromone(int row, int col, int luTypeNum) {
        this.row = row;
        this.col = col;
        this.num = luTypeNum;       // 有多少种土地利用类型
        phero = new double[row][col][luTypeNum];
    }

    public void initPhero() {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                for (int k = 0; k < num; k++) {
                    phero[i][j][k] = min_phero;
                }
            }
        }
    }

    // TODO:这里要定义一个自适应信息素函数，并依此调整信息素矩阵在位置 p 处的信息素值
    public void updatePheros(Position p, int type, Ant a, int loopTime) {
        phero[p.x][p.y][type] = phero[p.x][p.y][type] <= min_phero ? min_phero : (1 - rho) * phero[p.x][p.y][type];
        phero[p.x][p.y][type] += adaptivePheromoneAdjustmentCoefficient(loopTime) * a.f / 100000;
        checkPheromoneLeft(p, type);
    }

    // loopTime：循环次数
    // return: 调节系数
    public double adaptivePheromoneAdjustmentCoefficient(int loopTime) {
        if (loopTime < this.T1) {
            return this.Q1;
        } else if (loopTime >= this.T1 && loopTime < this.T2) {
            return this.Q2;
        } else if (loopTime >= this.T2 && loopTime < this.T3) {
            return this.Q3;
        } else {
            return this.Q4;
        }
    }

    // 保证位置 p 处的信息素保持在一个最大最小值之间，不会超出阈值
    public void checkPheromoneLeft(Position p, int type) {
        if (this.phero[p.x][p.y][type] < min_phero) {
            this.phero[p.x][p.y][type] = min_phero;
        } else if (this.phero[p.x][p.y][type] > max_phero) {
            this.phero[p.x][p.y][type] = max_phero;
        }
    }

    // 自适应调整信息素挥发系数
    // 参数：循环次数，最优解评价指数
    // 循环次数，是初始时，挥发系数较大，便于快速，循环次数比较多时，为增加全局探索能力，调整小信息素挥发系数
    // 最优解评价指数，如多次循环，最优解仍然没有变化，则减小信息素挥发系统，便于跳出局部最优解
    // 由于现在最优解评价指数无法确定，暂时只考虑循环次数，挥发因子随循环次数增加而增加
    public void adaptivePheromoneVolatileCoefficient(int loopTime, double bestSolutionEvaluation) {
        if (rho <= 0.45 && loopTime%10 == 0) {
            rho += 0.05 * loopTime / 10;
        }
    }
}
