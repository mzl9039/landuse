package zju.mzl.landuse.Aco;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public ArrayList<ArrayList<HashMap<String, Double>>> targets = new ArrayList<>();
    public HashMap<Integer, ArrayList<HashMap<String, Double>>> tars = new HashMap<>();
    public Grid[][] speGrids;

    int max_ants = 30;
    final int max_try_times = 80;

    // alpha 和 beta也可以采用自适应方式来调节，即开始时侧重全局搜索，
    // 后期增加 alpha 的值，以提高收敛速度
    double alpha = 0.7;
    double beta = 0.3;
    ArrayList<Ant> ants;

    public Opti() {
        initLu8();
        initLu4();
        row = 0;
        col = 0;
    }

    public static void main(String[] args) throws IOException {
        String relativePath = System.getProperty("user.dir");
        File path = new File(relativePath + "\\opti");
        if (!path.exists()) {
            System.out.println("no path" + path);
            System.exit(1);
        }

        Date date = new Date();
        String targetPath = path + "\\targets";
        File carbin = new File(targetPath + "\\tmp\\PC_CARB.json");
        File carbout = new File(targetPath + "\\PC_CARB.json");
        File esvin = new File(targetPath + "\\tmp\\VL_ESV.json");
        File esvout = new File(targetPath + "\\VL_ESV.json");
        String type = "ALL";
        if (type == "PC") {
            if (!carbout.exists()) {
                Utils.copyFile(carbin.toString(), carbout.toString());
            }
            if (esvout.exists()) {
                esvout.delete();
            }
        } else if (type == "Basic") {
            if (carbout.exists()) {
                carbout.delete();
            }
            if (esvout.exists()) {
                esvout.delete();
            }
        } else if (type == "VL") {
            if (!esvout.exists()) {
                Utils.copyFile(esvin.toString(), esvout.toString());
            }
            if (carbout.exists()) {
                carbout.delete();
            }
        } else {
            type = "ALL";
            if (!carbout.exists()) {
                Utils.copyFile(carbin.toString(), carbout.toString());
            }
            if (!esvout.exists()) {
                Utils.copyFile(esvin.toString(), esvout.toString());
            }
        }
        // 每种多目标体系10次模拟
        for (int k = 0; k < 1; k++) {
            int curTryTime = 0;
            Opti opti = new Opti();
            System.out.println("biggest run time:" + opti.max_try_times);
            Timer timer = new Timer();
            timer.start_timers();
            Format format = new SimpleDateFormat("yyyyMMdd_hhmmss");
            opti.inOut = new InOut(path + "\\" + path.list((dir, name) -> name.equals("grids.csv"))[0]);
            opti.instance = opti.inOut.read_opti();
            opti.row = opti.col = opti.inOut.gridLength;

            opti.init();

            // 为本次的输出创建目录,name表示是哪个参数，j_k表示哪个值的第几次模拟
            File outDir = new File(opti.inOut.output_dir + "\\result\\" + format.format(date) + type + "\\" + k + "_time");
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            int loopTime = 0;
            while (loopTime < opti.max_try_times || opti.bestAntSameLoopTime < 20) {
                if (opti.simulateAnts() == 0) {
                    loopTime++;

                    opti.updatePheros(loopTime);
                    printLoopBestAnt(opti, outDir.toString(), loopTime);
                    opti.adaptiveHeuristicFactor(loopTime);
                    // 如果最优蚂蚁在10次迭代中没有改变，则增加蚁群规模增加10只
                    //opti.adaptiveAntNums(opti.bestAntSameLoopTime);
                    if (curTryTime != opti.max_try_times) {
                        opti.restartAnts(loopTime);
                    }
                }
            }
            // 将蚂蚁的最终配置方案单独输出
            Grid[][] grids = opti.bestAnt.getTours();
            opti.inOut.printAntGrids(grids, outDir.toString() + "\\ant_grids.json");
            // 将蚂蚁的地类转换矩阵单独输出
            int[][] statGrids = Grid.statTransform(opti.instance.getGrids(), opti.bestAnt.getTours(), opti.row, opti.col);
            opti.inOut.printChangedGrids(statGrids, outDir.toString() + "\\ant_chg.json");
            opti.inOut.printTargets(opti.tars, outDir.toString() + "\\targets.json");
            opti.inOut.printAntGridsImage(opti.bestAnt.getTours(), outDir.toString() + "\\ant.jpg");
            opti.inOut.printChangedGridsImg(opti.instance.getGrids(), opti.bestAnt.getTours(), outDir.toString() + "\\ant_chg.jpg");
            opti.bestAnt.setTours(null);
            opti.inOut.printAnt(opti.bestAnt, outDir.toString() + "\\ant.json");
            double timeSpend = timer.elapsed_time();
            System.out.println("use time: " + timeSpend);
        }
    }

    public static void printLoopBestAnt(Opti opti, String path, int loopTime) throws IOException {
        File outDir = new File(path + "\\" + loopTime + "_loop");
        outDir.mkdirs();
        // 将蚂蚁的地类转换矩阵单独输出
        opti.inOut.printTargets(opti.tars, outDir.toString() + "\\targets.json");
        opti.inOut.printAntTargets(opti.bestAnt.target, outDir.toString() + "\\antTarget.json");
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

    void adaptiveHeuristicFactor(int looptime) {
        if (looptime % 5 == 0 && alpha >= 0.3) {
            alpha -= 0.01 * looptime / 5;
            beta = 1 - alpha;
        }
    }

    void adaptiveAntNums(int bestAntSameLoopTime) {
        if (bestAntSameLoopTime > 10) {
            max_ants += 5;
        }
    }

    // 计算蚂蚁各个目标的目标函数值
    public void caculateAntTargets(int loopTime) {
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
    }

    // 选择出是非支配解的蚂蚁
    public void selectNonDominatedSolution(int loopTime) {
        Iterator<Ant> antIter = ants.iterator();
        while (antIter.hasNext()) {
            Ant a = antIter.next();
            if (isAntDominatedByOthers(a)) {
                if (!tars.containsKey(loopTime)) {
                    tars.put(loopTime, new ArrayList<>());
                }
                tars.get(loopTime).add(a.target);
            } else {
                // 当前蚂蚁被支配，删除
                antIter.remove();
            }
        }
    }

    public void updatePheros(int loopTime) {
        System.out.println("The " + loopTime + " time update phere");
        caculateAntTargets(loopTime);
        selectNonDominatedSolution(loopTime);
        // a.f按从大到小排列
        ants.sort((a, b) -> (int)(a.f - b.f));

        if (ants.size() > 0) {
            // 选择最优蚂蚁
            int betterAntIdx = getBetterAnt();
            if (bestAnt == null || betterAntIdx != -1) {
                bestAntSameLoopTime = 0;
                bestAnt = ants.get(betterAntIdx);
                Grid.statTransform(bestAnt.getTours(), instance.getGrids(), row, col);
            } else {
                bestAntSameLoopTime++;
            }
            if (betterAntIdx >= 0) {
                System.out.println("try " + loopTime + " time; this time best ant target func value is:" + ants.get(betterAntIdx).f
                        + "; Global best ant target func value is:" + bestAnt.f);
            } else {
                System.out.println("try " + loopTime + " time; this time no ant is all targets better than bestAnt"
                        + "; Global best ant target func value is:" + bestAnt.f);
            }
        } else {
            bestAntSameLoopTime++;
            System.out.println("try " + loopTime + " time; all ants dominated, no best ant"
                    +"; Global best ant target func value is:" + bestAnt.f);
        }

        // 1、只有非支配解的蚂蚁才能更新信息素，
        // 2、蚂蚁只更新地类改变了的格网的信息素，不是所有格网的信息素都更新
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (instance.getGrids()[i][j] != null) {
                    for (int l = 0; l < lu8.size(); l++) {
                        // 信息素挥发
                        ph.pheroVolatilize(new Position(i, j), l);
                        for (int k = 0; k < ants.size(); k++) {
                            // 如果蚂蚁在格网p处与原格网的土地利用类型不同，
                            // 则蚂蚁改变了这个格网的土地利用类型，会释放信息素
                            if (ants.get(k).getGrid(new Position(i, j)).dlbm4 != instance.getGrids()[i][j].dlbm4) {
                                ph.updatePheros(new Position(i, j), l, ants.get(k), loopTime);
                            }
                        }
                    }
                }

            }
        }
    }

    public int getBetterAnt() {
        if (bestAnt == null) {
            return 0;
        }
        int res = -1;
        for (int i = 0; i < ants.size(); i++) {
            if (Ant.CompareTargets(ants.get(i).target, bestAnt.target) == 1) {
                res = i;
                break;
            }
        }
        return res;
    }

    // 当前蚂蚁与所有解比较,
    // 如果当前蚂蚁支配某个解，则那个解删除，并将当前蚂蚁加入非支配解集，返回true
    // 如果当前蚂蚁被某个解支配，则当前蚂蚁被支配，删除当前蚂蚁， 返回false
    // 如果当前蚂蚁不支配任何解，与不被任何解支配，则作为新的非支配解加入解集，与情况一类似，返回true
    public boolean isAntDominatedByOthers(Ant a) {
        HashMap<String, Double> tar = a.target;
        boolean res = true;
        for (Map.Entry<Integer, ArrayList<HashMap<String, Double>>> e : tars.entrySet()) {
            // 第i次迭代中蚁群的所有目标
            Iterator<HashMap<String, Double>> loop = e.getValue().iterator();
            while (loop.hasNext()) {
                // 如果tar是某个目标的非支配解，则把那个目标删除，同时返回true，
                // 由于tar可能是多个目标的非支配解，故需要遍历所有的Pareto解集
                int t = Ant.CompareTargets(tar, loop.next());
                // t == 1 :当前蚂蚁为非支配解；t == -1:当前蚂蚁被支配；t == 0:无法确定支配关系
                if (t == 1) {
                    loop.remove();
                } else if (t == -1) {
                    res = false;
                    return res;
                } else {
                    continue;
                }
            }
        }
        return res;
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
