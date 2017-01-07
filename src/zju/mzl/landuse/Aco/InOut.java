package zju.mzl.landuse.Aco;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSON;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mzl on 2016/11/17.
 */
public class InOut {

    public final String PROG_ID_STR = "ACO optimization algorithms for the landuse";
    public String NAME = "";
    public double minLon = 1E10;     // 最小经度值
    public double minLat = 1E10;      // 最小纬度值
    public double maxLon = 0;      // 最大经度值
    public double maxLat = 0;      // 最大纬度值
    public double distance = 0;             // 格网连长（米）
    public int gridLength = 0;                  // 格网矩阵一排的格网数

    private Map<String, BufferedWriter> writer = new HashMap<String, BufferedWriter>();

    public String file_path = "";
    public String input_file_name = "";
    public String input_target_path = "";
    public String input_image = "";
    public String output_dir = "";

    public BufferedImage image;

    public InOut(String opti_file_name) throws IOException {
        if (opti_file_name == null) {
            System.err.println("文件" + opti_file_name + "不存在，退出");
            System.exit(1);
        }

        if (!new File(opti_file_name).canRead()) {
            System.err.println("文件" + opti_file_name + "不可读，退出");
            System.exit(1);
        }
        this.input_file_name = opti_file_name;
        // 从全路径文件名，获取文件名（不含路径和后缀）
        File file = new File(opti_file_name);
        this.NAME = file.getName().split("[.]")[0];
        // 从文件名获取格网长度（如200米）
        this.distance = Integer.parseInt(this.NAME.split("_")[2]);
        this.file_path = file.getParent();
        this.input_target_path = this.file_path + "\\targets";
        this.output_dir = this.file_path + "\\output";
        this.input_image = this.file_path + "\\input_" + this.NAME + ".jpg";
    }

    public Problem read_opti() throws IOException {
        String opti_file_name = this.input_file_name;
        Problem instance = new Problem();

        if (opti_file_name == null) {
            System.exit(1);
        }

        System.out.println("正在读取文件" + opti_file_name + " ... ");

        Grid[][] grids = readGrid(opti_file_name);
        instance.setGrids(grids);
        int[] nums = new int[2];
        nums[0] = nums[1] = this.gridLength;
        instance.setNum(nums);
        // 土地适宜性目标
        //Target target = readLSETarget(opti_file_name);
        //instance.getTargets().put(target.getName(), target);
        // 最小规划成本目标
        // TODO 测试仅适宜度目标时的效果
        Target mpcTarget = initMPCTarget();
        instance.getTargets().put(mpcTarget.getName(), mpcTarget);
        // 土地利用强度目标
        Target luTarget = initLUTarget();
        instance.getTargets().put(luTarget.getName(), luTarget);
        // 读取其它的目标
        File path = new File(this.input_target_path);
        File files[] = path.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    // 严格来说，这里应该要可以区分出来目标的类型，要不然就是在目标文件里区分类型
                    Target t = readTarget(files[i].toString());
                    instance.getTargets().put(t.getName(), t);
                }
            }
        }
        return instance;
    }

    public Target initLUTarget() {
        Target target = new LUTarget();
        target.setName("LU");
        target.setLuType(4);
        target.considerLuComp = false;
        target.setType("LU");
        target.setSuit(0);
        return target;
    }

    // 初始化最小规划成本目标
    public Target initMPCTarget() {
        Target target = new PCTarget();
        target.setName("MPC");  // 最小规划成本
        target.setLuType(8);
        target.considerLuComp = true;
        target.setType("PC");
        target.setSuit(0.5);
        // 一共八大类，所有是八
        double t[][] = new double[8][8];
        // 0-7：耕 园 林 草 交 水 未 城
        t[0][1] = 0.7;  t[0][2] = 0.7;  t[0][3] = 0.5;  t[0][4] = 0.1;
        t[0][5] = 0.5;  t[0][7] = 0.1;
        t[1][0] = 0.7;  t[1][2] = 0.9;  t[1][3] = 0.9;  t[1][4] = 0.9;
        t[1][5] = 0.7;  t[1][7] = 0.3;
        t[2][0] = 0.9;  t[2][1] = 0.9;  t[2][3] = 0.5;  t[2][4] = 0.9;
        t[2][5] = 0.9;  t[2][7] = 0.9;
        t[3][0] = 0.1;  t[3][1] = 0.3;  t[3][2] = 0.7;  t[3][4] = 0.1;
        t[3][5] = 0.5;  t[3][7] = 0.1;
        t[6][0] = 0.3;  t[6][1] = 0.9;  t[6][2] = 0.7;  t[6][3] = 0.9;
        t[6][4] = 0.1;  t[6][5] = 0.3;  t[6][7] = 0.1;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (t[i][j] == 0) {
                    if (i != j) t[i][j] = 0.9999999999;    // 很高的成本，不鼓励转换，取一个非常非常接近1的数
                        // 目前采用 1-t[i][j]的方式求最大值，故，本身转换成本为0，转换后为1，符合逻辑
                    else t[i][j] = 0;
                }
            }
        }
        ((PCTarget)target).setSuits(t);
        return target;
    }

    public Grid[][] readGrid(String opti_file_name) throws IOException {
        Grid grids[][] = null;
        Reader reader = null;
        BufferedReader bufferedReader = null;
        ArrayList<Grid> gds = new ArrayList<>();
        try {
            reader = new InputStreamReader(new FileInputStream(opti_file_name), "GBK");
            bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            // 读取除适宜性以外的所有信息
            while (line != null) {
                if (line.startsWith("EOF")) {
                    break;
                } else if (line.startsWith("DIS")) {
                    this.distance = Integer.parseInt(line.split(",")[1]);
                    Utils.distance = this.distance;
                    Utils.minFarmArea /= Utils.distance * Utils.distance;
                    Utils.maxConsArea /= Utils.distance * Utils.distance;
                } else {
                    String[] gridInfo = line.split(",");
                    Grid gd = new Grid();
                    gd.objectid = Integer.parseInt(gridInfo[0]);
                    gd.area = Double.parseDouble(gridInfo[1]);
                    gd.slope = Double.parseDouble(gridInfo[2]);
                    gd.dlbm8 = Integer.parseInt(gridInfo[3]);
                    gd.constraint = Integer.parseInt(gridInfo[4]);
                    gd.dlbm4 = Integer.parseInt(gridInfo[9]);
                    gd.encourageFactor = Double.parseDouble(gridInfo[10]) / 100;
                    gd.height = Double.parseDouble(gridInfo[11]);
                    gd.lat = Double.parseDouble(gridInfo[12]);
                    gd.lon = Double.parseDouble(gridInfo[13]);
                    if (gd.lon < this.minLon) this.minLon = gd.lon;
                    if (gd.lat < this.minLat) this.minLat = gd.lat;
                    if (gd.lon > this.maxLon) this.maxLon = gd.lon;
                    if (gd.lat > this.maxLat) this.maxLat = gd.lat;
                    gds.add(gd);
                }
                line = bufferedReader.readLine();
            }

            this.gridLength = (int) ((this.maxLat - this.minLat) > (this.maxLon - this.minLon)
                    ? (this.maxLat - this.minLat) / this.distance
                    : (this.maxLon - this.minLon) / this.distance
            ) + 1;
            grids = new Grid[this.gridLength][this.gridLength];
            for (Grid g : gds) {
                Position p = geo2pos(g.lat, g.lon);
                grids[p.x][p.y] = g;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bufferedReader.close();
            reader.close();
        }
        return grids;
    }

    public Target readLSETarget(String opti_file_name) throws IOException {
        Target target = readTarget(opti_file_name);
        target.setName("LSE");  // 土地利用适宜性评价：landuse suitability evaluation
        target.setLuType(4);      // 土地利用适宜性评价是4大类的
        target.setSuit(0.5);    // 设置土地利用适宜性与土地利用强度的权重
        target.setType("SE");      // 适宜性目标
        return target;
    }

    // TODO 最小规划成本，只支持8大类
    // 强烈建议：土地利用类型一定要统一
    public Target readTarget(String filename) throws IOException {
        Reader reader = null;
        BufferedReader bufferedReader = null;
        Target target = null;
        try {
            reader = new InputStreamReader(new FileInputStream(filename), "UTF-8");
            bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            double lon, lat;
            while (line != null) {
                if (line.startsWith("EOF")) {
                    break;
                } else if (line.startsWith("TYPE")) {
                    // 根据类型确实目标类型,type:1,2,3 分别对应 适宜度、转换成本、价值因子
                    line = line.split("[,]")[1];
                    if (line.equals("SE")) {
                        target = new SETarget();
                        target.setType("SE");
                        Suits targetSuits[][] = new Suits[this.gridLength][this.gridLength];
                        ((SETarget)target).setSuits(targetSuits);
                    } else if (line.equals("PC")) {
                        target = new PCTarget();
                        target.setType("PC");
                    } else if (line.equals("VL")) {
                        target = new VLTarget();
                        target.setType("VL");
                    }
                }else if (line.startsWith("NAME")) {
                    target.setName(line.split("[,]")[1]);
                } else if (line.startsWith("LAND_USE_TYPE")) {
                    target.setLuType(Integer.parseInt(line.split("[,]")[1]));
                } else if (line.startsWith("SUIT")) {
                    target.setSuit(Double.parseDouble(line.split("[,]")[1]));
                } else if (line.startsWith("DESC")) {
                    target.setDescription(line.split("[,]")[1]);
                } else if (line.startsWith("CONS_LU")) {
                    int a = Integer.parseInt(line.split("[,]")[1]);
                    target.considerLuComp = a == 0 ? false : true;
                } else {
                    if (target.getType() == "SE") {
                        // 对适宜度类目标的解析
                        String[] gridInfo = line.split("[,]");
                        Suits suits = new Suits();
                        suits.valMap.put(1, Double.parseDouble(gridInfo[5]) / 100);   // 农用地适宜度
                        suits.valMap.put(4, Double.parseDouble(gridInfo[6]) / 100);   // 林地适宜度
                        suits.valMap.put(3, Double.parseDouble(gridInfo[7]) / 100);   // 建设用地适宜度
                        suits.valMap.put(2, Double.parseDouble(gridInfo[8]) / 100);   // 绿地适宜度
                        lat = Double.parseDouble(gridInfo[12]);
                        lon = Double.parseDouble(gridInfo[13]);
                        Position p = geo2pos(lat, lon);
                        ((SETarget)target).getSuits()[p.x][p.y] = suits;
                    } else if (target.getType() == "PC") {
                        // 对规划成本等转移因子类目标的解析
                        String changeSuit[] = line.split("[,]");
                        if (target.getLuType() == 8) {
                            ((PCTarget) target).getSuits()
                                    [Utils.lu8toIdx(Integer.parseInt(changeSuit[0]))]
                                    [Utils.lu8toIdx(Integer.parseInt(changeSuit[1]))] =
                                    Double.parseDouble(changeSuit[2]);
                        } else if (target.getLuType() == 4) {
                            ((PCTarget) target).getSuits()
                                    [Utils.lu4toIdx(Integer.parseInt(changeSuit[0]))]
                                    [Utils.lu4toIdx(Integer.parseInt(changeSuit[1]))] =
                                    Double.parseDouble(changeSuit[2]);
                        }
                    } else if (target.getType() == "VL") {
                        // 对价值类目标的解析
                        String typeSuit[] = line.split("[,]");
                        ((VLTarget)target).getSuits().put(Integer.parseInt(typeSuit[0]), Double.parseDouble(typeSuit[1]));
                    }
                }
                line = bufferedReader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bufferedReader.close();
            reader.close();
        }
        return target;
    }

    public void createImage(String fileLocation) {
        try {
            File file = new File(fileLocation);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ImageIO.write(image, "JPEG", bos);
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Color setColor (int type) {
        Color color = null;
        switch (type) {
            case 1:
                color = Color.yellow;   // 耕地
                break;
            case 2:
                color = Color.cyan;     // 园地
                break;
            case 3:
                color = Color.green;    // 林地
                break;
            case 4:
                color = Color.green; // 草地
                break;
            case 10:
                color = Color.darkGray;// 交通
                break;
            case 11:
                color = Color.blue;     // 水利用地
                break;
            case 12:
                color = Color.black;    // 未利用地
                break;
            case 20:
                color = Color.magenta;  // 村镇及工矿用地
                break;
            default:
                color = Color.white;
                break;
        }
        return color;
    }

    public void inputImage(Grid grids[][]) {
        this.printAntGridsImage(grids, this.input_image);
    }

    public void printAntGridsImage(Grid tours[][], String fileLocation) {
        int width = 5 * this.gridLength, height = 5 * this.gridLength;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D graphics2d = (Graphics2D)image.getGraphics();
        graphics2d.setBackground(Color.white);
        Color color = null;
        for (int i = 0; i < this.gridLength; i++) {
            for (int j = 0; j < this.gridLength; j++) {
                color = setColor(tours[i][j] == null ? 0 : tours[i][j].dlbm8);
                graphics2d.setColor(color);
                graphics2d.fillRect(5*j, 5*i, 5, 5);
            }
        }
        if (image != null) {
            graphics2d.drawImage(image, 0, 0, null);
            graphics2d.dispose();
            createImage(fileLocation);
        }
    }

    public void printChangedGrids(Grid olds[][], Grid tours[][], String filename) throws IOException {
        int width = 5 * this.gridLength, height = 5 * this.gridLength;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D graphics2d = (Graphics2D)image.getGraphics();
        graphics2d.setBackground(Color.white);
        Color color = null;
        for (int i = 0; i < this.gridLength; i++) {
            for (int j = 0; j < this.gridLength; j++) {
                if (tours[i][j] == null || tours[i][j].dlbm8 == olds[i][j].dlbm8) {
                    color = setColor(0);
                } else {
                    color = setColor(tours[i][j].dlbm8);
                }
                graphics2d.setColor(color);
                graphics2d.fillRect(5*j, 5*i, 5, 5);
            }
        }
        if (image != null) {
            graphics2d.drawImage(image, 0, 0, null);
            graphics2d.dispose();
            createImage(filename);
        }
    }

    public Position geo2pos(double lat, double lon) {
        Position p = new Position(0, 0);
        p.x = (int)((lat - this.minLat) / this.distance);
        p.y = (int)((lon - this.minLon) / this.distance);
        return p;
    }

    public void printAnt(Ant a, String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        String val = objectMapper.writeValueAsString(a);
        objectMapper.writeValue(new File(filename), val);
    }

    public void printTargets(ArrayList<ArrayList<HashMap<String, Double>>> targets, String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        String val = objectMapper.writeValueAsString(targets);
        objectMapper.writeValue(new File(filename), val);
    }
}
