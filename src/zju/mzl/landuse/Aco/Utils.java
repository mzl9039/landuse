package zju.mzl.landuse.Aco;

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
    // 建设用地不能转换为其它地类
    public static boolean canConvert(int from, int to, Grid grid) {
        if (from > 4) return false;
        if (from == 3 && to > 4) return false;
        if ((to == 1 || to == 2) && grid.slope >= 25) return false;
        return true;
    }

    public static double gridArea() {
        return distance * distance;
    }

    public static double distance = 0;
    public static double minFarmArea = 486750000;
    public static double maxConsArea = 439400000;
}
