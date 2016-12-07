package zju.mzl.landuse.Aco;

/**
 * Transform target
 * 与成本函数类似，只规定原始类型转为目标类型的转换系数，没有格网位置差异带来的系数的不同
 * 求最大目标函数
 * Created by mzl on 16-12-5.
 */
public class TFTarget extends Target {
    // 计算当前目标下土地利用强度
    @Override
    public double luComp(Position p, int type, Grid grids[][]) {
        return super.luComp(p, type, grids);
    }

    // 计算当前目标在格网 m,n 位置处的 局部目标函数值，
    // 注意局部目标函数值是一个邮权重suit 和 comp ，以及当前适宜度和土地利用强度共同计算的结果
    // 如果不需要计算土地利用强度，则suit和comp就不需要了，将suit设为1也可以
    @Override
    public double EtaNotConsiderLuComp(Position p, int type, Grid grids[][]) {
        Grid gd = grids[p.x][p.y];
        int from = Utils.lu4toIdx(gd.dlbm4), to = Utils.lu4toIdx(type);
        if (gd != null && this.getSuits()[from][to] != 0) {
            return this.getSuits()[from][to];
        } else {
            return 0;
        }
    }

    @Override
    public double EtaConsiderLuComp(Position p, int type, Grid grids[][]) {
        Grid gd = grids[p.x][p.y];
        int from = Utils.lu4toIdx(gd.dlbm4), to = Utils.lu4toIdx(type);
        if (gd != null && this.getSuits()[from][to] != 0) {
            return  this.getSuit() * this.getSuits()[from][to]
                    + this.getComp() * luComp(p, type, grids);
        } else {
            return 0;
        }
    }

    // 成本函数一般是越小越优,这里调用了super，要看super里会不会再调用这个对象里的
    // EtaConsiderLuComp 和 EtaNotConsiderLuComp
    @Override
    public double eta(Position p, int type, Grid grids[][]) {
        if(this.getLuType() == 4) {
            type = Utils.lu8tolu4(type);
            // 如果是建设用地，则不进行转换，即转换概率为0
            if (type == 3 && grids[p.x][p.y].dlbm8 != 12) {
                return 0;
            }
        }
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
