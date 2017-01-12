package zju.mzl.landuse.Aco;

/**
 * Created by mzl on 2016/11/21.
 */
public class Pheromone {
    final double min_phero = 0.5;
    final double max_phero = 12;
    final double init_phero = 1;
    double rho = 0.1;   // 信息素挥发系数，刚开始时挥发系数较小，随着时间推移，挥发系数越来越大，加快收敛
    int row = 0;
    int col = 0;
    int num = 0;
    public double phero[][][];
    // 信息素自适用调节系数阈值
    final double T1 = 10, T2 = 25, T3 = 40;
    final double Q1 = 0.1, Q2 = 0.3, Q3 = 0.6, Q4 = 0.9;

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
                    phero[i][j][k] = init_phero;
                }
            }
        }
    }

    public void pheroVolatilize(Position p, int type) {
        phero[p.x][p.y][type] = phero[p.x][p.y][type] <= min_phero ? min_phero : (1 - rho) * phero[p.x][p.y][type];
    }

    // 这里要定义一个自适应信息素函数，并依此调整信息素矩阵在位置 p 处的信息素值
    public void updatePheros(Position p, int type, Ant a, int loopTime) {
        // 蚂蚁释放的信息素，改为由信息素强度和启发信息值的乘积决定，而不是单纯由信息素强度决定
        // 由于目标函数值太小，故调整目标函数值，使其保持在1<f<10的范围内
        double t = a.f;
        while (t < 1) {
            t *= 10;
        }
        // 改为当前蚂蚁的全局目标函数值
        phero[p.x][p.y][type] += adaptivePheromoneAdjustmentCoefficient(loopTime) * t;
        checkPheromoneLeft(p, type);
    }

    // loopTime：循环次数
    // return: 调节系数
    public double adaptivePheromoneAdjustmentCoefficient(int loopTime) {
        double res = 0;
        if (loopTime < this.T1) {
            res = this.Q1;
        } else if (loopTime >= this.T1 && loopTime < this.T2) {
            res = this.Q2;
        } else if (loopTime >= this.T2 && loopTime < this.T3) {
            res = this.Q3;
        } else {
            res = this.Q4;
        }
        return res;
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
    // 初始时，挥发系数较大，信息素遗留因子较小，便于快速，循环次数比较多时，为增加全局探索能力，
    // 后期调整小信息素挥发系数
    // 最优解评价指数，如多次循环，最优解仍然没有变化，则减小信息素挥发系统，加快收敛
    // 如果需要跳出局部最估解，则需要再次增大挥发系统，这里没有处理
    // 由于现在最优解评价指数无法确定，暂时只考虑循环次数，挥发因子随循环次数增加而增加
    public void adaptivePheromoneVolatileCoefficient(int loopTime, double bestSolutionEvaluation) {
        if (rho <= 0.5 && loopTime%5 == 0) {
            rho += 0.1 * loopTime / 5;
        }
    }
}
