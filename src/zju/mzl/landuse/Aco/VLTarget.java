package zju.mzl.landuse.Aco;

import java.util.HashMap;
import java.util.Map;

/**
 * Value Target
 * 价值相关目标，每个地类代表一种价值
 * Created by mzl on 16-12-5.
 */
public class VLTarget extends Target {
    public VLTarget() {
        this.suits = new HashMap<>();
    }
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
        if (gd != null && this.getSuits().get(type) != 0) {
            return this.getSuits().get(type);
        } else {
            return 0;
        }
    }

    @Override
    public double EtaConsiderLuComp(Position p, int type, Grid grids[][]) {
        Grid gd = grids[p.x][p.y];
        if (gd != null && this.getSuits().get(type) != 0) {
            return  this.getSuit() * this.getSuits().get(type)
                    + this.getComp() * luComp(p, type, grids);
        } else {
            return 0;
        }
    }

    // EtaConsiderLuComp 和 EtaNotConsiderLuComp
    @Override
    public double eta(Position p, int type, Grid grids[][]) {
        if(this.getLuType() == 4) {
            type = Utils.lu8tolu4(type);
        }
        return this.isConsiderLuComp() == true
                ? EtaConsiderLuComp(p, type, grids)
                : EtaNotConsiderLuComp(p, type, grids);
    }

    @Override
    public double targetVal(Grid[][] olds, Ant a) {
        return super.targetVal(olds, a);
    }

    @Override
    public double targetVal2(Grid[][] olds, Ant a) {
        return super.targetVal2(olds, a);
    }

    public Map<Integer, Double> getSuits() {
        return suits;
    }

    // 不同地类的价值或因子
    private Map<Integer, Double> suits;
}
