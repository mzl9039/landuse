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
    public int bestAntSameLoopTime = 0;
    public Ant bestAnt = null;
    public int row;
    public int col;
    public Problem instance;
    public InOut inOut;
    public Pheromone ph;
    public ArrayList<Ant> results;
    public ArrayList<Double> lastEanlistAnts;
    public ArrayList<ArrayList<HashMap<String, Double>>> targets = new ArrayList<>();
    public Grid[][] speGrids;

    final int max_grids = 20000;
    final int max_ants = 20;
    final int max_loop = 80;
    final int max_try_times = max_loop * max_grids;

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
        ArrayList<ArrayList<Double>> params = new ArrayList<>();
        // rho
        params.add(new ArrayList<Double>(){ {
            add(0.1);   add(0.2);   add(0.3);   add(0.4);   add(0.5);   add(0.6);   add(0.7);
        } });
        // alpha
        params.add(new ArrayList<Double>() {{
            add(0.1);   add(0.2);   add(0.3);   add(0.4);   add(0.5);   add(0.6);   add(0.7);
        }});
        // beta
        params.add(new ArrayList<Double>() {{
            add(0.3);   add(0.5);   add(0.7);   add(0.9);   add(1.0);   add(3.0);   add(5.0);
        }});
        Problem p = null;
        int gridLength = 0;
        double distance = 0;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                int curTryTime = 0;
                Opti opti = new Opti();
                System.out.println("最大运行次数:" + opti.max_try_times);
                Timer timer = new Timer();
                timer.start_timers();
                Format format = new SimpleDateFormat("yyyyMMdd_hhmmss");
                Date date = new Date();
                opti.inOut = new InOut(path + "\\" + path.list((dir, name) -> name.equals("grids.csv"))[0]);
                // 因为instance不会改变，因而只读取一次，不再每次读取浪费时间
                if (p == null) {
                    p = opti.inOut.read_opti();
                    gridLength = opti.inOut.gridLength;
                    distance = opti.inOut.distance;
                } else {
                    opti.instance = p;
                    opti.row = opti.col = gridLength;
                    opti.inOut.distance = distance;
                    opti.inOut.gridLength = gridLength;
                    opti.inOut.distance = distance;
                }

                opti.init();
                String name = "";
                if (j == 0) {
                    name = "rho";
                    opti.ph.rho = params.get(j).get(i);
                } else if (j == 1) {
                    name = "alpha";
                    opti.alpha = params.get(j).get(i);
                } else {
                    name = "beta";
                    opti.beta = params.get(j).get(i);
                }

                int loopTime = 0;
                while (curTryTime++ < opti.max_try_times || opti.bestAntSameLoopTime < 25) {
                    if (opti.simulateAnts() == 0) {
                        loopTime++;

                        opti.updatePheros(loopTime);
                        if (curTryTime != opti.max_try_times) {
                            opti.restartAnts(loopTime);
                        }
                    }
                }
                // 为本次的输出创建目录
                File outDir = new File(opti.inOut.output_dir + "\\result\\" + name + "\\" + format.format(date));
                if (!outDir.exists()) {
                    outDir.mkdir();
                }
                // 将蚂蚁的最终配置方案单独输出
                Grid[][] grids = opti.bestAnt.getTours();
                opti.inOut.printAntGrids(grids, outDir.toString() + "\\ant_grids.json");
                // 将蚂蚁的地类转换矩阵单独输出
                int[][] statGrids = Grid.statTransform(opti.instance.getGrids(), opti.bestAnt.getTours(), opti.row, opti.col);
                opti.inOut.printChangedGrids(statGrids, outDir.toString() + "\\ant_chg.json");
                opti.inOut.printTargets(opti.targets, outDir.toString() + "\\targets.json");
                opti.inOut.printAntGridsImage(opti.bestAnt.getTours(), outDir.toString() + "\\ant.jpg");
                opti.inOut.printChangedGridsImg(opti.instance.getGrids(), opti.bestAnt.getTours(), outDir.toString() + "\\ant_chg.jpg");
                opti.bestAnt.setTours(null);
                opti.inOut.printAnt(opti.bestAnt, outDir.toString() + "\\ant.json");
                double timeSpend = timer.elapsed_time();
                System.out.println("程序运行用时：" + timeSpend);
            }

        }
    }

    public Ant initAnt(int looptime) {
        Ant a = new Ant(0, 0, new int[row][col], 1);
        a.setTours(this.instance.gridsClone());
        a.adjustAntByGrids();
        a.setCurrentPos(randomSelectPatch(a));
        a.patchCenter = a.getCurrentPos();
        a.initTarget(instance.getTargets());
        a.looptime = looptime;
        return a;
    }

    public void initAnts(int looptime) {
        ants.clear();
        for (int i = 0; i < max_ants; i++) {
            ants.add(initAnt(looptime));
        }

        ants.forEach(a -> {
            Position p = a.getCurrentPos();
            a.getTabu()[p.x][p.y] = 1;
            a.updated++;
            updateGridType(a, chooseGridType(a, p));
            initPatchNeighbour(a);
        });
    }

    public void init() throws IOException {
        // 初始化信息素矩阵
        ph = new Pheromone(row, col, lu8.size());
        ph.initPhero();

        // 初始化蚁群
        ants = new ArrayList<>(max_ants);
        initAnts(0);
    }

    public void restartAnts(int looptime) {
        initAnts(looptime);
    }
    //////////////////////////////////////////////////////////////

    // 计算启发信息值
    // 多目标转为单目标计算局部最优值
    double expInfo(Ant a, int type, Position p) {
        Grid grids[][] = a.getTours();
        if (a.canConvert(p, type)) {
            double res = 1.0;
            for (Map.Entry<String, Target> e : instance.getTargets().entrySet()) {
                // 如果是土地利用强度，则跳过，因为计算其它目标时会考虑
                if (e.getKey() == "LU") continue;
                double t = e.getValue().eta(p, type, grids);
                res = res * t;
            }
            // 记录下配置方案在信息p处类型type的启发信息值
            grids[p.x][p.y].exp.replace(type, res);
            return res;
        } else {
            return 0;
        }
    }

    // 计算概率转移函数
    double heuristic(Ant a, Pheromone ph, int type, Position p) {
        double exp = expInfo(a, type, p);
        return Math.pow(ph.phero[p.x][p.y][Utils.lu8toIdx(type)], alpha) * Math.pow(exp, beta);
    }

    public void updatePheros(int loopTime) {
        System.out.println("第" + loopTime + "次更新信息素");
        ants.forEach(a -> {
            // 当前的蚂蚁是第几轮迭代时的蚂蚁
            a.looptime = loopTime;
            // 求出 蚂蚁 a 的所有目标的目标函数值
            for (Map.Entry<String, Target> e : instance.getTargets().entrySet()) {
                a.target.replace(e.getKey(), e.getValue().targetVal(instance.getGrids(), a));
            }
            for (Map.Entry<String, Double> e : a.target.entrySet()) {
                a.f *= e.getValue() / a.updated;
            }
            a.StatTours();
        });
        // a.f按从大到小排列
        ants.sort((a, b) -> (int)(b.f - a.f));
        // 选择最优蚂蚁
        if (bestAnt == null || bestAnt.f < ants.get(0).f) {
            bestAntSameLoopTime = 0;
            bestAnt = ants.get(0);
            Grid.statTransform(bestAnt.getTours(), instance.getGrids(), row, col);
        } else {
            bestAntSameLoopTime++;
        }
        // 将本次运行结果与上一次做比较
        ArrayList<Boolean> com = CompareAntsWithLast();
        //记录最优秀的5只蚂蚁的各个目标函数值
        targets.add(RecordEalistAnts());
        System.out.println("尝试次数" + loopTime + ";本轮最优蚂蚁目标函数值为: " + ants.get(0).f
                +";最优蚂蚁目标函数值为：" + bestAnt.f);

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                for (int k = 0; k < ants.size(); k++) {
                    Ant a = ants.get(k);
                    for (int l = 0; l < lu8.size(); l++) {
                        ph.updatePheros(new Position(i, j), l, a, loopTime, com.get(l));
                    }
                }
            }
        }
        // 将本次蚁群搜索的结果保存下来
        GetLastEalistAnts();
    }

    public ArrayList<Boolean> CompareAntsWithLast() {
        ArrayList<Boolean> com = new ArrayList<>();
        for (int i = 0; i < ants.size(); i++) {
            if (lastEanlistAnts == null || ants.get(i).f >= lastEanlistAnts.get(i)) {
                com.add(true);
            } else {
                com.add(false);
            }
        }
        return com;
    }

    public void GetLastEalistAnts() {
        if (lastEanlistAnts == null) {
            lastEanlistAnts = new ArrayList<>();
        }
        for (int i = 0; i < ants.size(); i++) {
            lastEanlistAnts.add(ants.get(i).f);
        }
    }

    public ArrayList<HashMap<String, Double>> RecordEalistAnts() {
        ArrayList<HashMap<String, Double>> targ = new ArrayList<>();
        for (int i = 0; i < ants.size() / 4; i++) {
            targ.add(ants.get(i).target);
        }
        return targ;
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
        Position p = a.getCurrentPos();
        if (type != a.getTours()[p.x][p.y].dlbm8 && a.canConvert(p, type)) {
            Grid gd = a.getTours()[p.x][p.y];
            // 如果类型是耕地或建设用地，则调整蚂蚁内保存的现有耕地面积和建设用地面积
            if (a.adjustArea(gd.dlbm8, type, p)) {
                gd.dlbm8 = type;
                gd.dlbm4 = Utils.lu8tolu4(type);
            }
        }
    }

    // 更新格网信息
    public void updateGrid(Ant a, Position p, int type) {
        a.setCurrentPos(p);
        a.updated++;
        updateGridType(a, type);
        a.getTabu()[p.x][p.y] = 1;          // 访问过了
    }

    // 根据最大概率求格网类型，若最大概率的类型与当前格网类型不一致，
    // 则采用轮盘赌的形式确定一个类型，只在初始化斑块的第一个格网时用到
    public int chooseGridType(Ant a, Position p) {
        // 当前格网是建设用地，且不是未利用地时，建设用地的格网类型不发生变化
        // 原来为 ||，这是个明显的bug
        if (a.getGrid(p).dlbm4 == 3 && a.getGrid(p).dlbm8 != 12) {
            return a.getGrid(p).dlbm8;
        }
        double prob[] = new double[lu8.size()], t[] = new double[lu4.size()];
        double maxProbi = 0.0;
        int maxtype = 0, index = 0;
        for (int type : lu8.keySet()) {
            if (type == 2 || type == 10 || type == 11 || type == 12) {
                index++;
                continue;
            }
            prob[index] = heuristic(a, ph, type, p);
            if (prob[index] > maxProbi) {
                maxProbi = prob[index];
                maxtype = type;
            }
            index++;
        }
        // TODO 不考虑类型选择，看永远选择概率最大的是什么效果
        //return maxtype == 0 ? a.getCurGrid().dlbm8 : maxtype;
        // 如果概率最大的土地利用类型与原来的土地利用类型(大类相同即可)相同，
        // 则当前处理的土地利用类型就是当前要处理的土地利用类型，
        // 否则采用轮盘赌的形式决定
        if (Utils.lu8tolu4(maxtype) == instance.getGrids()[p.x][p.y].dlbm4) {
            return maxtype;
        } else {
            t[0] = prob[0] + prob[1];
            t[1] = prob[3];
            t[2] = prob[4] + prob[5] + prob[6] + prob[7];
            t[3] = prob[2];
            for (int i = 1; i < t.length; i++) {
                t[i] += t[i-1];
            }

            for (int i = 0; i < t.length; i++) {
                t[i] /= t[t.length-1];
            }
            Random r = new Random();
            double d = r.nextDouble();
            for (int i = 0; i < t.length; i++) {
                if (d < t[i]) {
                    return Utils.lu4tolu8(Utils.Idxtolu4(i));
                }
            }
            return a.getGrid(p).dlbm8;
        }
    }

    // 根据最大概率求格网的类型，不考虑概率类型与原来的格网是否一致
    public int chooseGridTypeByMaxProbility(Ant a, Position p) {
        // 若当前位置处为建设用地，由于建设用地不能转为其它用地，所以直接返回原用地类型
        if (Utils.lu8tolu4(a.getTours()[p.x][p.y].dlbm8) == 3) {
            return a.getTours()[p.x][p.y].dlbm8;
        }
        double prob[] = new double[lu8.size()];
        double maxProbi = 0.0;
        int maxtype = 0, index = 0;
        for (int type : lu8.keySet()) {
            if (type == 2 || type == 10 || type == 11 || type == 12) {
                index++;
                continue;
            }
            prob[index] = heuristic(a, ph, type, p);
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
        Position p = a.getCurrentPos();
        int type = a.getTours()[p.x][p.y].dlbm8;
        for (int i = p.x-2; i <= p.x+2 ; i++) {
            for (int j = p.y-2; j <= p.y+2 ; j++) {
                if (i<0 || j<0 || i>=row || j>=col || (i==p.x && j==p.y) || a.getTabu()[i][j]!=0) {
                    continue;
                } else {
                    String str = Utils.pos2str(new Position(i, j));
                    // 只检查斑块领域放大一些，距离不超过2的都算
                    if (!a.getNeighbours().containsKey(str)
                            && Math.abs(i+j - a.patchCenter.x-a.patchCenter.y) <=2) {
                        a.getNeighbours().put(str, heuristic(a, ph, type, new Position(i, j)));
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
        a.patchCenter = p;
        a.setCurrentPos(p);
        if (p.x == -1 || p.y == -1) {
            a.setStop(1);
            return 1;
        }
        // 选择中心格网要更新为成的土地利用类型
        int type = chooseGridType(a, p);
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
            a.setCurrentPos(p);
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
        if (type == chooseGridTypeByMaxProbility(a, p)) {
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
                p = a.getCurrentPos();
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
        lu8.put(20, "城镇村及工矿用地");
    }



    public void initLu4() {
        lu4.put(1, "农用地");
        lu4.put(2, "绿地");
        lu4.put(3, "建设用地");
        lu4.put(4, "森林");
    }


    //////////////////////////////////////////////////////////////
}
