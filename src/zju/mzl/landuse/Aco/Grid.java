package zju.mzl.landuse.Aco;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by mzl on 2016/11/17.
 */
public class Grid {
    int objectid;
    double area;            // 格网面积
    double slope;           // 坡度值
    int dlbm8;
    int dlbm4;
    int constraint;
    double lon, lat;
    int x, y;

    public Grid() {}

    public Grid copy() {
        Grid grid = new Grid();
        grid.objectid = this.objectid;
        grid.area = this.area;
        grid.slope = this.slope;
        grid.dlbm8 = this.dlbm8;
        grid.dlbm4 = this.dlbm4;
        grid.constraint = this.constraint;
        grid.lon = this.lon;
        grid.lat = this.lat;
        grid.x = this.x;
        grid.y = this.y;
        return grid;
    }

    public static Grid[][] gridsClone(Grid grids[][], int row, int col) {
        Grid gds[][] = new Grid[row][col];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (grids[i][j] != null) {
                    gds[i][j] = grids[i][j].copy();
                }
            }
        }
        return gds;
    }

    public static HashMap<Integer, Integer> statGrids(Grid grids[][], int row, int col) {
        int lu8[] = {1, 2, 3, 4, 10, 11, 12, 20};
        HashMap<Integer, Integer> stat = new HashMap<>();
        for (int i = 0; i < lu8.length; i++) {
            stat.put(lu8[i], 0);
        }
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (grids[i][j] != null) {
                    int type = grids[i][j].dlbm8;
                    stat.replace(type, stat.get(type) + 1);
                }
            }
        }
        return stat;
    }
}
