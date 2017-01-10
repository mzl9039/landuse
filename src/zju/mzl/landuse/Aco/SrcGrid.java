package zju.mzl.landuse.Aco;

/**
 * Created by mzl on 2017/1/10.
 */
public class SrcGrid {
    public int getOBJECTID() {
        return OBJECTID;
    }

    public void setOBJECTID(int OBJECTID) {
        this.OBJECTID = OBJECTID;
    }

    public double getShape_Area() {
        return Shape_Area;
    }

    public void setShape_Area(double shape_Area) {
        Shape_Area = shape_Area;
    }

    public double getSlope() {
        return Slope;
    }

    public void setSlope(double slope) {
        Slope = slope;
    }

    public int getDLBM_8() {
        return DLBM_8;
    }

    public void setDLBM_8(int DLBM_8) {
        this.DLBM_8 = DLBM_8;
    }

    public int getDLBM_4() {
        return DLBM_4;
    }

    public void setDLBM_4(int DLBM_4) {
        this.DLBM_4 = DLBM_4;
    }

    public int getConstrain() {
        return Constrain;
    }

    public void setConstrain(int constrain) {
        Constrain = constrain;
    }

    public double getEncourageF() {
        return encourageF;
    }

    public void setEncourageF(double encourageF) {
        this.encourageF = encourageF;
    }

    public double getHeight() {
        return Height;
    }

    public void setHeight(double height) {
        Height = height;
    }

    public double getX() {
        return X;
    }

    public void setX(double x) {
        X = x;
    }

    public double getY() {
        return Y;
    }

    public void setY(double y) {
        Y = y;
    }

    public double getSuit_Crop() {
        return Suit_Crop;
    }

    public void setSuit_Crop(double suit_Crop) {
        Suit_Crop = suit_Crop;
    }

    public double getSuit_Fores() {
        return Suit_Fores;
    }

    public void setSuit_Fores(double suit_Fores) {
        Suit_Fores = suit_Fores;
    }

    public double getSuit_Const() {
        return Suit_Const;
    }

    public void setSuit_Const(double suit_Const) {
        Suit_Const = suit_Const;
    }

    public double getSuit_Green() {
        return Suit_Green;
    }

    public void setSuit_Green(double suit_Green) {
        Suit_Green = suit_Green;
    }

    public int OBJECTID;
    public double Shape_Area;      // 格网面积
    public double Slope;           // 坡度值
    public int DLBM_8;
    public int DLBM_4;
    public int Constrain;
    public double encourageF;
    public double Height;
    public double X, Y;
    public double Suit_Crop;
    public double Suit_Fores;
    public double Suit_Const;
    public double Suit_Green;
}
