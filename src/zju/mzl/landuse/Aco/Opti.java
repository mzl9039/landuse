package zju.mzl.landuse.Aco;

import java.io.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by mzl on 2016/11/17.
 */
public class Opti {
    public HashMap<Integer, String> lu8 = new HashMap<>();
    public HashMap<Integer, String> lu4 = new HashMap<>();
    public HashMap<Integer, String> ghgzq = new HashMap<>();
    public int row;
    public int col;
    public Problem instance;
    public InOut inOut;
    public Pheromone ph;
    public ArrayList<Ant> results;

    final int max_grids = 4000;
    final int max_ants = 30;
    final int max_loop = 80;
    final int max_try_times = max_loop * max_grids;
    final int bestAntsRatio = 6;

    // alpha 和 beta也可以采用自适应方式来调节，即开始时侧重全局搜索，
    // 后期增加 alpha 的值，以提高收敛速度
    double alpha = 0.1;
    double beta = 0.9;
    ArrayList<Ant> ants;

    public Opti() {
        initLu8();
        initLu4();
        row = 0;
        col = 0;
        results = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        String relativePath = System.getProperty("user.dir");
        File path = new File(relativePath + "\\opti");
        if (!path.exists()) {
            System.out.println("不存在路径" + path);
            System.exit(1);
        }

        int curTryTime = 0;
        Opti opti = new Opti();
        System.out.println("最大运行次数:" + opti.max_try_times);
        Timer timer = new Timer();
        timer.start_timers();
        opti.inOut = new InOut(path + "\\" + path.list((dir, name) -> name.endsWith(".csv"))[0]);
        opti.instance = opti.inOut.read_opti();
        opti.row = opti.col = opti.inOut.gridLength;
        opti.inOut.printGrids(opti.instance.getGrids(), opti.row, opti.col, opti.inOut.file_path + "\\input.txt");
        opti.inOut.inputImage(opti.instance.getGrids());
        opti.init();
        int loopTime = 0;
        while (curTryTime++ < opti.max_try_times) {
            if (opti.simulateAnts() == 0) {
                loopTime++;
                // 根据循环次数调节信息素挥发因子和启发因子
                opti.ph.adaptivePheromoneVolatileCoefficient(loopTime, 0.0);
                opti.adaptiveHeuristicFactor(loopTime);

                opti.updatePheros(loopTime);
                if (curTryTime != opti.max_try_times) {
                    opti.restartAnts();
                }
                System.out.println("尝试次数" + loopTime);
            }
        }
        System.out.println("ACO:最优Pareto解集中解的个数：" + opti.results.size());
        // 为本次的输出创建目录
        Format format = new SimpleDateFormat("yyyyMMdd_hhmmss");
        File outDir = new File(opti.inOut.output_dir + "\\" + format.format(new Date()));
        if (!outDir.exists()) {
            outDir.mkdir();
        }
        for (int i = 0; i < opti.results.size(); i++) {
            // 每个结果要输出为txt和jpg两种结果
            String filename = outDir.toString() + "\\" + i;
            opti.inOut.printGrids(opti.results.get(i).getTours(), opti.row, opti.col, filename + ".txt");
            opti.inOut.generateImageByValues(opti.results.get(i).getTours(), filename + ".jpg");
        }
        double timeSpend = timer.elapsed_time();
        System.out.println("程序运行用时：" + timeSpend);
    }

    public void init() throws IOException {
        // 初始化蚁群
        ants = new ArrayList<>(max_ants);
        for (int i = 0; i < max_ants; i++) {
            Ant a = new Ant(0, 0, new int[row][col], 1);
            a.setTours(this.instance.gridsClone());
            a.adjustAntByGrids();
            a.setCurrentGrid(randomSelectPatch(a));
            for (Map.Entry<String, Target> e : instance.getTargets().entrySet()) {
                a.target.put(e.getKey(), 0.0);
            }
            ants.add(a);
        }

        // 初始化信息素矩阵
        // TODO:检查 蚁群的tabu 矩阵是否被初始化
        ph = new Pheromone(row, col, lu8.size());
        ph.initPhero();

        ants.forEach(a -> {
            Position p = a.getCurrentGrid();
            a.getTabu()[p.x][p.y] = 1;
            a.updated++;
            updateGridType(a, chooseGridType(a));
            initPatchNeighbour(a);
        });
    }

    public void restartAnts() {
        ants.forEach(a -> {
            a.restartAnt(1, 0, new int[row][col], 1);
            a.setTours(instance.gridsClone());
            a.adjustAntByGrids();
            a.setCurrentGrid(randomSelectPatch(a));
            for (Map.Entry<String, Target> e : instance.getTargets().entrySet()) {
                a.target.put(e.getKey(), 0.0);
            }

            Position p = a.getCurrentGrid();
            a.getTabu()[p.x][p.y] = 1;
            updateGridType(a, chooseGridType(a));
            initPatchNeighbour(a);
        });
    }

    //////////////////////////////////////////////////////////////

    // 计算格网 m,n 处的启发信息值，在选择格网类型时使用
    // 由于有多个目标函数，所以多个目标函数的局部目标值之间，用乘积结果转为单目标
    double heuristic(Ant a, Pheromone ph, int type) {
        Position p = a.getCurrentGrid();
        Grid grids[][] = a.getTours();
        if (a.canConvert(type)) {
            double res = 1.0;
            for (Map.Entry<String, Target> e : instance.getTargets().entrySet()) {
                res *= e.getValue().eta(p, type, grids);
            }
            return Math.pow(ph.phero[p.x][p.y][Utils.lu8toIdx(type)], alpha) * Math.pow(res, beta);
        } else {
            return 0;
        }
    }

    void adaptiveHeuristicFactor(int looptime) {
        if (looptime % 10 == 0 && alpha <= 0.5) {
            alpha = 0.1 + 0.1 * looptime / 10;
            beta = 1 - alpha;
        }
    }

    public void updatePheros(int loopTime) {
        ants.forEach(a -> {
            // TODO：评价当前的解是不是非支配解，若是，则加入解集中去
            // 求出 蚂蚁 a 的所有目标的目标函数值
            for (Map.Entry<String, Target> e : instance.getTargets().entrySet()) {
                a.target.replace(e.getKey(), e.getValue().targetVal(instance.getGrids(), a));
            }
            // 求出所有目标函数值的乘积
            for (Map.Entry<String, Double> e : a.target.entrySet()) {
                a.f *= e.getValue();
            }
        });
        // a.f按从大到小排列
        ants.sort((a, b) -> (int)(b.f - a.f));
        System.out.println("aaa");
        for (int i = 0; i < ants.size()/bestAntsRatio; i++) {
            if (results.size() == 0) {
                results.add(ants.get(i).clone());
            } else {
                // 当前蚂蚁要和所有的优秀结果集做比较，如果比其中的一个要优秀，则替换；
                // 如果和其中一个相等，则跳过；否则，加入解集
                int compare2 = -1;
                for (int j = 0; j < results.size(); j++) {
                    int compare = Ant.targetCompareTo(ants.get(i), results.get(j));
                    if (compare > 0) {
                        results.set(j, ants.get(i).clone());
                        compare2 = 1;
                        break;
                    } else if (compare == 0) {
                        compare2 = 0;
                        break;
                    }
                }
                if (compare2 == -1) {
                    results.add(ants.get(i).clone());
                }
            }
        }

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                // n 只蚂蚁，只取最优的 1/6 只蚂蚁的解决方案更新信息素
                for (int k = 0; k < ants.size()/bestAntsRatio; k++) {
                    Ant a = ants.get(k);
                    for (int l = 0; l < lu8.size(); l++) {
                        ph.updatePheros(new Position(i, j), l, a, loopTime);
                    }
                }
            }
        }
    }
    //////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////
    /* 选择斑块，确定斑块邻居，确定格网类型 */

    public Position randomSelectPatch(Ant a) {
        int x = Utils.random(1, row) - 1, y = Utils.random(1, col) - 1, m = x, n = y;
        while ((x<row && y<col) && a.getTabu()[x][y]==1) {
            y++;
            if (y == col) {
                y = 0;
                x++;
            }
        }
        if (x != row) {
            return new Position(x, y);
        }

        x = m; y = n;
        while ((x>=0 && y>=0) && a.getTabu()[x][y]==1) {
            y--;
            if (y < 0) {
                y = col - 1;
                x--;
            }
        }
        return (x != -1) ? new Position(x, y) : new Position(-1, -1);
    }

    // 更新格网的类型信息，type 为要变成的类型
    public void updateGridType(Ant a, int type) {
        Position p = a.getCurrentGrid();
        if (type != a.getTours()[p.x][p.y].dlbm8 && a.canConvert(type)) {
            Grid gd = a.getTours()[p.x][p.y];
            // 如果类型是耕地或建设用地，则调整蚂蚁内保存的现有耕地面积和建设用地面积
            a.adjustArea(gd.dlbm8, type);
            gd.dlbm8 = type;
            gd.dlbm4 = Utils.lu8tolu4(type);
        }
    }

    // 更新格网信息
    public void updateGrid(Ant a, Position p, int type) {
        a.setCurrentGrid(p);
        a.updated++;
        updateGridType(a, type);
        a.getTabu()[p.x][p.y] = 1;          // 访问过了
    }

    // 根据最大概率求格网类型，若最大概率的类型与当前格网类型不一致，
    // 则采用轮盘赌的形式确定一个类型，只在初始化斑块的第一个格网时用到
    public int chooseGridType(Ant a) {
        int m = a.getCurrentGrid().x, n = a.getCurrentGrid().y;
        // 若当前位置处为建设用地，由于建设用地不能转为其它用地，所以直接返回原用地类型
        if (Utils.lu8tolu4(a.getTours()[m][n].dlbm8) == 3) {
            return a.getTours()[m][n].dlbm8;
        }
        double prob[] = new double[lu8.size()], t[] = new double[lu8.size()];
        double maxProbi = 0.0;
        int maxtype = 0, index = 0;
        for (int type : lu8.keySet()) {
            prob[index] = heuristic(a, ph, type);
            t[index] = index == 0 ? prob[index] : t[index-1] + prob[index];
            if (prob[index] > maxProbi) {
                maxProbi = prob[index];
                maxtype = type;
            }
            index++;
        }

        // 如果概率最大的土地利用类型与原来的土地利用类型相同，
        // 则当前处理的土地利用类型就是当前要处理的土地利用类型，
        // 否则采用轮盘赌的形式决定
        if (maxtype == instance.getGrids()[m][n].dlbm8) {
            return maxtype;
        } else {
            int type = 20;
            for (int i = 0; i < t.length; i++) {
                t[i] /= t[t.length-1];
            }
            Random r = new Random();
            double d = r.nextDouble();
            for (int i = 0; i < t.length; i++) {
                if (d < t[i]) {
                    return Utils.idxtolu8(i);
                }
            }
            return type;
        }
    }

    // 根据最大概率求格网的类型，不考虑概率类型与原来的格网是否一致
    public int chooseGridTypeByMaxProbility(Ant a) {
        Position p = a.getCurrentGrid();
        // 若当前位置处为建设用地，由于建设用地不能转为其它用地，所以直接返回原用地类型
        if (Utils.lu8tolu4(a.getTours()[p.x][p.y].dlbm8) == 3) {
            return a.getTours()[p.x][p.y].dlbm8;
        }
        double prob[] = new double[lu8.size()];
        double maxProbi = 0.0;
        int maxtype = 0, index = 0;
        for (int type : lu8.keySet()) {
            prob[index] = heuristic(a, ph, type);
            if (prob[index] > maxProbi) {
                maxProbi = prob[index];
                maxtype = type;
            }
            index++;
        }
        return maxtype;
    }

    // 添加到斑块的邻居列表里
    public void neighbours(Ant a) {
        Position p = a.getCurrentGrid();
        int type = a.getTours()[p.x][p.y].dlbm8;
        for (int i = p.x-1; i <= p.x+1 ; i++) {
            for (int j = p.y-1; j <= p.y+1 ; j++) {
                if (i<0 || j<0 || i>=row || j>=col || (i==p.x && j==p.y) || a.getTabu()[i][j]!=0) {
                    continue;
                } else {
                    String str = Utils.pos2str(new Position(i, j));
                    if (!a.getNeighbours().containsKey(str)) {
                        a.getNeighbours().put(str, heuristic(a, ph, type));
                    }
                }
            }
        }
    }

    // 初始化斑块的邻居信息
    public void initPatchNeighbour(Ant a) {
        a.getNeighbours().clear();
        neighbours(a);
    }
    //////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////
    /* 蚂蚁在行进的相关函数，如果邻居为空，则走下一个斑块，否则走下一个邻居 */
    public int nextPatch(Ant a) {
        Position p = randomSelectPatch(a);
        if (p.x == -1 || p.y == -1) {
            System.out.println("第" + ants.indexOf(a) + "只蚂蚁走完了全部格网，退出");
            a.setStop(1);
            return 1;
        }
        // 选择中心格网要更新为成的土地利用类型
        int type = chooseGridType(a);
        updateGrid(a, p, type);
        neighbours(a);
        return 1;
    }

    public int nextGrid(Ant a, int type) {
        int res = 0;
        if (a.getNeighbours().size() == 0) {
            res = nextPatch(a);
        } else {
            Map.Entry<String, Double> e = a.getNeighbours().entrySet().stream()
                    .max((m1, m2) -> m1.getValue().compareTo(m2.getValue()))
                    .get();
            Position p = Utils.str2pos(e.getKey());
            if (next(a, p, type)) {
                neighbours(a);
                res = 1;
            }
            a.getNeighbours().remove(e.getKey());
        }
        if (res == 1) {
            return res;
        } else {
            while (res != 1) {
                res = nextGrid(a, type);
            }
            return res;
        }
    }

    // 决定蚂蚁a下一步是否走位置p(判断标准是位置p的type是否是概率最大的)
    public boolean next(Ant a, Position p, int type) {
        if (type == chooseGridTypeByMaxProbility(a)) {
            updateGrid(a, p, type);
            return true;
        }
        return false;
    }

    // 蚁群中的蚂蚁每次走一步
    public int simulateAnts() {
        int moving = 0;
        Position p;
        for (Ant a : ants) {
            if (a.getStop() != 1) {
                p = a.getCurrentGrid();
                nextGrid(a, a.getTours()[p.x][p.y].dlbm8);
                moving++;
            }
        }
        return moving;
    }
    //////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////
    /* 这部分是构造函数中调用的，用来初始化土地利用和规划的基本地类信息，以及lu8->lu4的转换 */
    public void initLu8() {
        lu8.put(1, "耕地");
        lu8.put(2, "园地");
        lu8.put(3, "林地");
        lu8.put(4, "草地");
        lu8.put(10, "交通运输用地");
        lu8.put(11, "水域及水利设施用地");
        lu8.put(12, "未利用地");
        lu8.put(20, "建设用地");
    }



    public void initLu4() {
        lu4.put(1, "农用地");
        lu4.put(2, "绿地");
        lu4.put(3, "建设用地");
        lu4.put(4, "森林");
    }


    //////////////////////////////////////////////////////////////
}
