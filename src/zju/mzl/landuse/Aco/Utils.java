package zju.mzl.landuse.Aco;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mzl on 2016/11/17.
 */
public class Utils {
    public static double dtrunc(double x) {
        int k;

        k = (int) x;
        x = (double) k;
        return x;
    }

    public static int random(int min, int max) {
        return (int) (1 + Math.random() * (max - min + 1));
    }

    public static String pos2str(Position p) {
        return "" + p.x + "_" + p.y;
    }

    public static Position str2pos(String s) {
        String strs[] = s.split("_");
        return new Position(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]));
    }

    public static int lu8toIdx(int type) {
        switch (type) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            case 10:
                return 4;
            case 11:
                return 5;
            case 12:
                return 6;
            case 20:
                return 7;
            default:
                return 0;
        }
    }

    public static Map<Integer, Double> initLu8Map() {
        Map<Integer, Double> lu8 = new HashMap<>();
        lu8.put(1, 0.0);
        lu8.put(2, 0.0);
        lu8.put(3, 0.0);
        lu8.put(4, 0.0);
        lu8.put(10, 0.0);
        lu8.put(11, 0.0);
        lu8.put(12, 0.0);
        lu8.put(20, 0.0);
        return lu8;
    }

    public static int lu4toIdx(int type) {
        switch (type) {
            case 1:
                return 0;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 1;
            default:
                return -1;
        }
    }

    public static int Idxtolu4(int idx) {
        switch (idx) {
            case 0:
                return 1;
            case 2:
                return 3;
            case 3:
                return 4;
            case 1:
                return 2;
            default:
                return -1;
        }
    }

    public static int mpcLu4toIdx(int lu4, int lu8) {
        switch (lu4) {
            case 1:
                return 0;
            case 2:
                return 2;
            case 3:
                return lu8 != 12 ? 3 : 4;
            case 4:
                return 1;
            default:
                return -1;
        }
    }

    public static int idxtolu8(int idx) {
        switch (idx) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 10;
            case 5:
                return 11;
            case 6:
                return 12;
            case 7:
                return 20;
            default:
                return 20;
        }
    }

    public static int lu4tolu8(int dlbm4) {
        switch (dlbm4) {
            case 1:
                return 1;
            case 2:
                return 4;
            case 3:
                return 20;
            case 4:
                return 3;
            default:
                return 20;
        }
    }

    public static int lu8tolu4(int dlbm8) {
        switch (dlbm8) {
            case 1:
            case 2:
                return 1;
            case 3:
                return 4;
            case 4:
                return 2;
            case 10:
            case 11:
            case 12:
            case 20:
                return 3;
            // 如果找不到类型，则默认为建设用地
            default:
                return 0;
        }
    }

    // type是土地利用8大类的类型，只有耕地、园地、林地、草地可以转换为其它地类
    // 除未利用地的建设用地不能转换为其它地类
    // 未利用地只能转为建设用地
    public static boolean canConvert(int from, int to, Grid grid) {
        // 添加自己的限制条件
        if (Utils.lu8tolu4(to) == 3 && to != 20) return false;
        // 若当前为林地，要转为农用地，且坡度大于10,则不做转化
        if (from == 3 && to <= 2 && grid.slope >= 10) return false;
        // TODO： 原来程序有错，所以人为加了限制，若要转为林地或园地，且坡度小于3,则不做转化
        if ((to == 3 || to == 2) && grid.slope < 3) return false;
        if (Utils.lu8tolu4(to) == 3 && grid.height > 100) return false;

        if (grid.constrain != 0) return false;
        // 除未利用地外的建设用地不能转为其它类型
        if (from > 4 && from != 12) return false;
        // 坡度大于25度时，不能转为耕地
        if ((to == 1 || to == 2) && grid.slope >= 25) return false;
        // 任何类型都不能转为未利用地
        if (to == 12) return false;
        return true;
    }

    public static double gridArea() {
        return distance * distance;
    }

    public static int[] mapToArray(HashMap<Integer, String> lu) {
        int lus[] = new int[lu.size()], idx = 0;
        for (Map.Entry<Integer, String> e : lu.entrySet()) {
            lus[idx] = e.getKey();
            idx++;
        }
        return lus;
    }

    /*** 复制单个文件
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ( (byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        }
        catch (Exception e) {
            System.out.println("there is sth. wrong with moving file");
            e.printStackTrace();
        }
    }

    public static double distance = 0;
    public static double minFarmArea = 486750000;
    public static double maxConsArea = 439400000;
    public static double maxGrassArea = 960000;     // 草地不超过24个格网
}
