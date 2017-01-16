package zju.mzl.landuse.Aco;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

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

        System.out.println("reading file: " + opti_file_name + " ... ");

        Grid[][] grids = readGrid(opti_file_name);
        instance.setGrids(grids);
        int[] nums = new int[2];
        nums[0] = nums[1] = this.gridLength;
        instance.setNum(nums);
        // 土地利用强度目标
        Target luTarget = initLUTarget();
        instance.getTargets().put(luTarget.getName(), luTarget);
        // 读取其它的目标
        File path = new File(this.input_target_path);
        File files[] = path.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && files[i].toString().endsWith("json")) {
                    Target t = raedTarget2(files[i].toString());
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

    public ArrayList<Grid> readGridsCsv(String opti_file_name) throws IOException {
        ICsvBeanReader beanReader = null;
        ArrayList<Grid> gds = new ArrayList<>();
        try {
            beanReader = new CsvBeanReader(new FileReader(opti_file_name), CsvPreference.EXCEL_PREFERENCE);
            final String[] header = beanReader.getHeader(true);
            final CellProcessor[] processors = getProcessors();
            SrcGrid grid = null;
            ArrayList<SrcGrid> grids = new ArrayList<>();
            while ((grid = beanReader.read(SrcGrid.class, header, processors)) != null) {
                grids.add(grid);
                if (grid.Y < this.minLon) this.minLon = grid.Y;
                if (grid.X < this.minLat) this.minLat = grid.X;
                if (grid.Y > this.maxLon) this.maxLon = grid.Y;
                if (grid.X > this.maxLat) this.maxLat = grid.X;
            }
            grids.forEach(gd -> gds.add(Grid.fromSrcGrid(gd)));
        } finally {
            if (beanReader != null) {
                beanReader.close();
            }
            return gds;
        }
    }

    public Grid[][] readGrid(String opti_file_name) throws IOException {
        Grid grids[][] = null;
        try {
            if (this.distance != 200) {
                this.distance = 200;
                Utils.distance = this.distance;
                Utils.minFarmArea /= Utils.distance * Utils.distance;
                Utils.maxConsArea /= Utils.distance * Utils.distance;
            }
            ArrayList<Grid> gds = readGridsCsv(opti_file_name);

            this.gridLength = (int) ((this.maxLat - this.minLat) > (this.maxLon - this.minLon)
                    ? (this.maxLat - this.minLat) / this.distance
                    : (this.maxLon - this.minLon) / this.distance
            ) + 1;
            grids = new Grid[this.gridLength][this.gridLength];
            for (Grid g : gds) {
                Position p = geo2pos(g.lat, g.lon);
                g.x = p.x;
                g.y = p.y;
                grids[p.x][p.y] = g;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return grids;
    }

    public Target raedTarget2(String filename) throws IOException {
        String file = filename.substring(filename.lastIndexOf("\\") + 1);
        if (file.startsWith("SE")) {
            return readSETarget(filename);
        } else if (file.startsWith("PC")) {
            return readPCTarget(filename);
        } else if (file.startsWith("VL")) {
            return readVLTarget(filename);
        }
        return null;
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

    public void printChangedGridsImg(Grid olds[][], Grid tours[][], String filename) throws IOException {
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

    public ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper;
    }

    public void printAntTargets(HashMap<String, Double> targets, String filename) throws IOException {
        ObjectMapper objectMapper = getObjectMapper();
        objectMapper.writeValue(new File(filename), targets);
    }

    public void printAnt(Ant a, String filename) throws IOException {
        ObjectMapper objectMapper = getObjectMapper();
        objectMapper.writeValue(new File(filename), a);
    }

    public void printAntGrids(Grid[][] grids, String filename) throws IOException {
        ObjectMapper objectMapper = getObjectMapper();
        objectMapper.writeValue(new File(filename), grids);
    }

    public void printChangedGrids(int[][] changeGrids, String filename) throws IOException {
        ObjectMapper mapper = getObjectMapper();
        mapper.writeValue(new File(filename), changeGrids);
    }

    public SETarget readSETarget(String filename) throws IOException {
        ObjectMapper objectMapper = getObjectMapper();
        SETarget target = objectMapper.readValue(new File(filename), SETarget.class);
        return target;
    }

    public PCTarget readPCTarget(String filename) throws IOException {
        ObjectMapper objectMapper = getObjectMapper();
        PCTarget target = objectMapper.readValue(new File(filename), PCTarget.class);
        return target;
    }

    public VLTarget readVLTarget(String filename) throws IOException {
        ObjectMapper objectMapper = getObjectMapper();
        VLTarget target = objectMapper.readValue(new File(filename), VLTarget.class);
        return target;
    }

    public void printSETarget(SETarget se, String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.writeValue(new File(filename), se);
    }

    public void printPCTarget(PCTarget se, String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.writeValue(new File(filename), se);
    }

    public void printVLTarget(VLTarget se, String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.writeValue(new File(filename), se);
    }

    public void printTargets(HashMap<Integer, ArrayList<HashMap<String, Double>>> targets, String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.writeValue(new File(filename), targets);
    }

    public static CellProcessor[] getProcessors() {
        final CellProcessor[] processors = new CellProcessor[] {
                new ParseInt(),
                new ParseDouble(),
                new ParseDouble(),
                new ParseInt(),
                new ParseInt(),
                new ParseDouble(),
                new ParseDouble(),
                new ParseDouble(),
                new ParseDouble(),
                new ParseInt(),
                new ParseDouble(),
                new ParseDouble(),
                new ParseDouble(),
                new ParseDouble()
        };
        return processors;
    }


}
