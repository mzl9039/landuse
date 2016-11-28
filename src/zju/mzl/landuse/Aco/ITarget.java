package zju.mzl.landuse.Aco;

/**
 * Created by mzl on 2016/11/19.
 */
public interface ITarget {

    double init_pher = 0.5;
    double rho = 0.5;

    double luComp(Position p, int type, Grid grids[][]);

    // 局部目标函数方法
    double eta(Position p, int type, Grid grids[][]);

    double tau(Position p, int type, Pheromone ph);

    double targetVal(Grid olds[][], Ant a);
}
