package zju.mzl.landuse.Aco;

/**
 * 适宜性评价相关的目标函数
 * Created by mzl on 2016/11/20.
 */
public class SETarget extends Target {
    // 计算当前目标下土地利用强度
    @Override
    public double luComp(Position p, int type, Grid grids[][]) {
        return super.luComp(p, type, grids);
    }

    // 计算当前目标在格网 m,n 位置处的 局部目标函数值，
    // 注意局部目标函数值是一个邮权重suit 和 comp ，以及当前适宜度和土地利用强度共同计算的结果
    // 如果不需要计算土地利用强度，则suit和comp就不需要了，将suit设为1也可以
    @Override
    public double eta(Position p, int type, Grid grids[][]) {
        return super.eta(p, type, grids);
    }

    @Override
    public double tau(Position p, int type, Pheromone ph) {
        return super.tau(p, type, ph);
    }

    @Override
    public double targetVal(Grid[][] olds, Ant a) {
        return super.targetVal(olds, a);
    }

    @Override
    public double targetVal2(Grid[][] olds, Ant a) {
        return super.targetVal2(olds, a);
    }

    public double EtaNotConsiderLuComp(Position p, int type, Grid grids[][]) {
        if (grids[p.x][p.y] != null) {
            return this.getSuits()[p.x][p.y].valMap.get(type);
        } else {
            return 0;
        }
    }

    public double EtaConsiderLuComp(Position p, int type, Grid grids[][]) {
        try {
            if (grids[p.x][p.y] != null) {
                return this.getSuit() * this.getSuits()[p.x][p.y].valMap.get(type)
                        + this.getComp() * luComp(p, type, grids);
            } else {
                return 0;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.err.println(p.x + " " + p.y);
        }
        return 0;
    }

    public Suits[][] getSuits() {
        return suits;
    }

    public void setSuits(Suits[][] suits) {
        this.suits = suits;
    }

    private Suits[][] suits;
}
