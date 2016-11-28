package zju.mzl.landuse.Aco;

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
        this.input_target_path = this.file_path + "\\target";
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
        Target target = readLSETarget(opti_file_name);
        instance.getTargets().put(target.getName(), target);
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
                } else {
                    String[] gridInfo = line.split(",");
                    Grid gd = new Grid();
                    gd.objectid = Integer.parseInt(gridInfo[0]);
                    gd.area = Double.parseDouble(gridInfo[1]);
                    gd.slope = Double.parseDouble(gridInfo[2]);
                    gd.dlbm8 = Integer.parseInt(gridInfo[3]);
                    gd.constraint = Integer.parseInt(gridInfo[4]);
                    gd.dlbm4 = Integer.parseInt(gridInfo[9]);
                    gd.lat = Double.parseDouble(gridInfo[10]);
                    gd.lon = Double.parseDouble(gridInfo[11]);
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
        target.setType(4);      // 土地利用适宜性评价是4大类的
        target.setSuit(0.5);    // 设置土地利用适宜性与土地利用强度的权重
        return target;
    }

    public Target readTarget(String filename) throws IOException {
        Reader reader = null;
        BufferedReader bufferedReader = null;
        Target target = new SETarget();
        Suits targetSuits[][] = new Suits[this.gridLength][this.gridLength];
        try {
            reader = new InputStreamReader(new FileInputStream(filename), "GBK");
            bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            double lon, lat;
            while (line != null) {
                if (line.startsWith("EOF")) {
                    break;
                } else if (line.startsWith("TYPE")) {
                    // 根据类型确实目标类型
                    line = line.split("[:]")[1];
                    if (line == "SE") {
                        target = new SETarget();
                    } else if (line == "PC") {
                        target = new PCTarget();
                    }
                }else if (line.startsWith("NAME")) {
                    target.setName(line.split("[:]")[1]);
                } else if (line.startsWith("LAND_USE_TYPE")) {
                    target.setType(Integer.parseInt(line.split("[:]")[1]));
                } else if (line.startsWith("SUIT")) {
                    target.setSuit(Double.parseDouble(line.split("[:]")[1]));
                } else if (line.startsWith("DIS")) {
                } else {
                    String[] gridInfo = line.split("[,]");
                    Suits suits = new Suits();
                    suits.valMap.put(1, Double.parseDouble(gridInfo[5]));   // 农用地适宜度
                    suits.valMap.put(4, Double.parseDouble(gridInfo[6]));   // 林地适宜度
                    suits.valMap.put(3, Double.parseDouble(gridInfo[7]));   // 建设用地适宜度
                    suits.valMap.put(2, Double.parseDouble(gridInfo[8]));   // 绿地适宜度
                    lat = Double.parseDouble(gridInfo[10]);
                    lon = Double.parseDouble(gridInfo[11]);
                    Position p = geo2pos(lat, lon);
                    targetSuits[p.x][p.y] = suits;
                }
                line = bufferedReader.readLine();
            }
            target.setSuits(targetSuits);
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
                color = Color.yellow;   // 农用地
                break;
            case 2:
                color = Color.green;    // 绿地
                break;
            case 3:
                color = Color.magenta;  // 建设用地
                break;
            case 4:
                color = Color.cyan;     // 森林
                break;
            default:
                color = Color.white;
                break;
        }
        return color;
    }

    public void inputImage(Grid grids[][]) {
        this.generateImageByValues(grids, this.input_image);
    }

    public void generateImageByValues(Grid tours[][], String fileLocation) {
        int width = 5 * this.gridLength, height = 5 * this.gridLength;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D graphics2d = (Graphics2D)image.getGraphics();
        graphics2d.setBackground(Color.white);
        Color color = null;
        for (int i = 0; i < this.gridLength; i++) {
            for (int j = 0; j < this.gridLength; j++) {
                color = setColor(tours[i][j] == null ? 0 : tours[i][j].dlbm4);
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

    public Position geo2pos(double lat, double lon) {
        Position p = new Position(0, 0);
        p.x = (int)((lat - this.minLat) / this.distance);
        p.y = (int)((lon - this.minLon) / this.distance);
        return p;
    }

    public void printGrids(Grid grids[][], int row, int col, String filename) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        Writer writer = null;
        BufferedWriter bufferedWriter = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(filename), "GBK");
            bufferedWriter = new BufferedWriter(writer);
            HashMap<Integer, Integer> stat = Grid.statGrids(grids, row, col);
            for (Map.Entry<Integer, Integer> e : stat.entrySet()) {
                bufferedWriter.write("" + e.getKey() + " " + e.getValue());
                bufferedWriter.newLine();
            }
            bufferedWriter.newLine();
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    if (grids[i][j] != null) {
                        bufferedWriter.write("" + i + " " + j + " " + grids[i][j].dlbm8);
                        bufferedWriter.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bufferedWriter.close();
            writer.close();
        }

    }
}
