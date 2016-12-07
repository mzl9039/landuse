package zju.mzl.landuse.Aco;

/**
 * 规划成本目标 Planning Costs
 * Created by mzl on 2016/11/20.
 */
public class PCTarget extends Target {
    @Override
    // 计算当前目标下土地利用强度
    public double luComp(Position p, int type, Grid grids[][]) {
        return super.luComp(p, type, grids);
    }

    @Override
    public double EtaNotConsiderLuComp(Position p, int type, Grid grids[][]) {
        Grid gd = grids[p.x][p.y];
        if (gd != null) {
            return this.getSuits()[Utils.lu8toIdx(gd.dlbm8)][Utils.lu8toIdx(type)];
        } else {
            return 0;
        }
    }

    @Override
    public double EtaConsiderLuComp(Position p, int type, Grid grids[][]) {
        Grid gd = grids[p.x][p.y];
        if (gd != null) {
            return  this.getSuit() * this.getSuits()[Utils.lu8toIdx(gd.dlbm8)][Utils.lu8toIdx(type)]
                    + this.getComp() * luComp(p, type, grids);
        } else {
            return 0;
        }
    }

    // 成本函数一般是越小越优,这里调用了super，要看super里会不会再调用这个对象里的
    // EtaConsiderLuComp 和 EtaNotConsiderLuComp
    @Override
    public double eta(Position p, int type, Grid grids[][]) {
        return this.isConsiderLuComp() == true
                ? EtaConsiderLuComp(p, type, grids)
                : EtaNotConsiderLuComp(p, type, grids);
    }

    @Override
    public double targetVal(Grid[][] olds, Ant a) {
        return super.targetVal(olds, a);
    }


    public double[][] getSuits() {
        return suits;
    }

    public void setSuits(double[][] suits) {
        this.suits = suits;
    }

    private double suits[][];
}
