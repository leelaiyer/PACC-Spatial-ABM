package ProjectDraft;

import HAL.GridsAndAgents.SphericalAgent2D;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Gui.OpenGL2DWindow;
import HAL.Tools.FileIO;
import HAL.Tools.Internal.Gaussian;
import HAL.Rand;

import java.lang.Math;
import java.sql.Time;
import java.util.ArrayList;

import static HAL.Util.*;

class Cell2 extends SphericalAgent2D<Cell2, TumorWithSGMandET>{
    int type;
    double resistance;
    double forceSum;
    public void Init(int color, double resistance) {
        this.type = color;
        this.resistance = resistance + G.totalResistance;
        if (type == TumorWithSGMandET.SGM_PACC) {
            this.radius = 0.5;
        } else if (type == TumorWithSGMandET.SGM_ANEU) {
            this.radius = 0.25;
        } else if (type == TumorWithSGMandET.ET_PACC) {
            this.radius = 0.5;
        } else if (type == TumorWithSGMandET.ET_ANEU) {
            this.radius = 0.25;
        }
    }

    double ForceCalc(double overlap, Cell2 other) {
        if(overlap < 0) {
            return 0;
        }
        return G.FORCE_SCALER*overlap;
    }

    public void CalcMove(){
        forceSum = SumForces(G.RADIUS*2, this::ForceCalc);
    }

    public boolean CanDivide(double div_bias,double inhib_weight){
        return G.rn.Double()<Math.tanh(div_bias-forceSum*inhib_weight);
    }

    public void Move() {
        ForceMove();
        ApplyFriction(G.FRICTION);
    }

    public void Div() {
        if (type == TumorWithSGMandET.SGM_PACC) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(TumorWithSGMandET.SGM_PACC, resistance);
        } else if(type == TumorWithSGMandET.SGM_ANEU) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(TumorWithSGMandET.SGM_ANEU, resistance);
        } else if(type == TumorWithSGMandET.ET_PACC) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(TumorWithSGMandET.ET_PACC, resistance);
        } else if(type == TumorWithSGMandET.ET_ANEU) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(TumorWithSGMandET.ET_ANEU, resistance);
        }
    }

    public void Die() {
        Dispose();
    }



    public void MutationET() {
        if (TumorWithSGMandET.deathDueToDrug(TumorWithSGMandET.drugDose, TumorWithSGMandET.ET_AneuPop, TumorWithSGMandET.totalResistance) > TumorWithSGMandET.fitnessThresholdET) {
            boolean mutated = G.rn.Bool();
            if (!mutated) {

            } else if (mutated) {
                double favorability = G.rn.Double(1);
                if (favorability < 0.9) {

                } else if (favorability > 0.9) {
                    double resistanceAdded = G.rn.Double(1);
                    resistance = G.totalResistance + resistanceAdded;
                }
            }
        } else {

        }
    }

    public void MutationSGM() {
        int [] neighborhood = CircleHood(false,radius);
        int options = MapEmptyHood(neighborhood);
        if ((options>0)&&(TumorWithSGMandET.deathDueToDrug(TumorWithSGMandET.drugDose, TumorWithSGMandET.ET_AneuPop, TumorWithSGMandET.totalResistance) < TumorWithSGMandET.fitnessThresholdSGM)){
            MutationET();
        } else {
            while(resistance < TumorWithSGMandET.resistancethresholdSGM) {
                double favorability = G.rn.Double(1);
                if (favorability < 0.9) {

                } else if (favorability > 0.9) {
                    double resistanceAdded = G.rn.Double(1);
                    resistance = G.totalResistance + resistanceAdded;
                }
            }
        }
    }
}

public class TumorWithSGMandET extends AgentGrid2D<Cell2> {

    static final int WHITE = RGB256(248, 255, 252);
    static final int ET_PACC = RGB256(60, 179, 113);
    static final int ET_ANEU = RGB256(65, 105, 225);
    static final int SGM_PACC = RGB256(249, 42, 130);
    static final int SGM_ANEU = RGB256(238,108,77);
    static int CYTOPLASM = RGB256(255,228,225);
    double RADIUS = 0.5;
    double FORCE_SCALER = 0.25;
    double FRICTION = 0.6;
    double PACC_DIV_BIAS = 0.01;
    double ANEU_DIV_BIAS = 0.02;
    double PACC_INHIB_WEIGHT = 0.02;
    double ANEU_INHIB_WEIGHT = 0.05;
    public static int totalPACCPop = 0;
    public static int totalAneuPop = 0;
    public static int ET_PACCPop = 0;
    public static int ET_AneuPop = 0;
    public static int SGM_PACCPop = 0;
    public static int SGM_AneuPop = 0;
    public static int drugDose = 0;
    public static int fitnessThresholdET = 0;
    public static int fitnessThresholdSGM = 0;
    public static int resistancethresholdSGM = 0;
    ArrayList<Cell2> neighborList = new ArrayList<>();
    ArrayList<double[]> neighborInfo = new ArrayList<>();
    double[] divCoordStorage = new double[2];
    Rand rn = new Rand(System.nanoTime());
    Gaussian gaussian = new Gaussian();
    static double totalResistance = 0;
    FileIO out;

    public TumorWithSGMandET(int x, int y, String outFileName) {
        super(x, y, Cell2.class, true, true);
        out = new FileIO(outFileName, "w");
    }

    public static double logisticGrowth(double aneuPop, double PACCPop) {
        double logistic = 0.6*aneuPop*(10000 - aneuPop - PACCPop)/10000;
        if(logistic < 0){
            return 0;
        } else {
            return logistic;
        }
    }

    public static double obligateToPACC(double aneuPop) {
        double obligate = 0.02 * aneuPop;
        return obligate;
    }

    public static double facultativeToPACC(double aneuPop, double drugResistance) {
        double facultative = 0.7 * aneuPop * (1/(100+drugResistance));
        return facultative;
    }

    public static double fromPACC(double PACCPop) {
        double fromPACC = 0.4 * PACCPop;
        return fromPACC;
    }

    public static double deathDueToDrug(double drugDose, double aneuPop, double totalResistance) {
        double death = aneuPop * (drugDose/(100 + totalResistance));
        return death;
    }

    public static void main(String[] args) {
        OpenGL2DWindow.MakeMacCompatible(args);
        int x = 30, y = 30;
        TumorWithSGMandET model = new TumorWithSGMandET(x, y, "PopOut.csv");
        OpenGL2DWindow vis = new OpenGL2DWindow("Tumor With SGM and ET", 750, 750, x, y);
        model.Setup( 200, 5);
        int i = 0;
        while ((i<1000)&&(!vis.IsClosed())) {
            vis.TickPause(10);
            model.Draw(vis);
            model.StepCells(vis);
            i++;
        }
        if (model.out != null) {
            model.out.Close();
        }
        vis.Close();
    }

    public void Setup(double initPop, double initRadius) {
        for (int i = 0; i < initPop; i++) {
            double cellType = rn.Double(1);
            rn.RandomPointInCircle(initRadius, divCoordStorage);
            if(cellType < 0.1) {
                NewAgentPT(divCoordStorage[0] + xDim / 2.0, divCoordStorage[1] + yDim / 2.0).Init(ET_PACC, totalResistance);
            } else if(cellType < 0.2) {
                NewAgentPT(divCoordStorage[0] + xDim / 2.0, divCoordStorage[1] + yDim / 2.0).Init(SGM_PACC, totalResistance);
            } else if(cellType < 0.6) {
                NewAgentPT(divCoordStorage[0] + xDim / 2.0, divCoordStorage[1] + yDim / 2.0).Init(SGM_ANEU, totalResistance);
            } else {
                NewAgentPT(divCoordStorage[0] + xDim / 2.0, divCoordStorage[1] + yDim / 2.0).Init(ET_ANEU, totalResistance);
            }
        }
    }

    public void Draw(OpenGL2DWindow vis) {
        vis.Clear(WHITE);
        for (Cell2 cell : this) {
            vis.Circle(cell.Xpt(),cell.Ypt(),cell.radius,CYTOPLASM);
        }
        for (Cell2 cell : this) {
            vis.Circle(cell.Xpt(), cell.Ypt(), cell.radius / 3, cell.type);
        }
        vis.Update();
    }

    public void StepCells(OpenGL2DWindow vis) {
        ET_PACCPop = 0;
        ET_AneuPop = 0;
        SGM_PACCPop = 0;
        SGM_AneuPop = 0;
        for (Cell2 cell : this) {
            if (cell.type == ET_PACC) {
                ET_PACCPop++;
            } else if (cell.type == ET_ANEU){
                ET_AneuPop++;
            } else if (cell.type == SGM_PACC) {
                SGM_PACCPop++;
            } else if (cell.type == SGM_ANEU) {
                SGM_AneuPop++;
            }
        }
        totalAneuPop = SGM_AneuPop + ET_AneuPop;
        totalPACCPop = SGM_PACCPop + ET_PACCPop;

        System.out.println("ET_PACCPop: " + ET_PACCPop);
        System.out.println("ET_AneuPop: " + ET_AneuPop);

        System.out.println("SGM_PACCPop: " + SGM_PACCPop);
        System.out.println("SGM_AneuPop: " + SGM_AneuPop);
        System.out.println(" ");
        for(Cell2 cell : this){
            cell.CalcMove();
        }
        for (Cell2 cell : this) {
            cell.Move();
            double[] eventProbabilities = {logisticGrowth(totalAneuPop, totalPACCPop),
                    obligateToPACC(totalAneuPop), facultativeToPACC(totalAneuPop, totalResistance),
                    fromPACC(totalPACCPop), deathDueToDrug(0, totalAneuPop, totalResistance)};
            double sum = 0;
            for(int i = 0; i < eventProbabilities.length; i++){
                sum = sum + eventProbabilities[i];
            }
            double[] eventPercentages = new double[eventProbabilities.length];
            for(int w = 0; w < eventPercentages.length; w++) {
                eventPercentages[w] = (eventProbabilities[w]/sum);
            }
            double r = rn.Double(1);
            if ((cell.type == ET_ANEU) && (cell.CanDivide(ANEU_DIV_BIAS, ANEU_INHIB_WEIGHT))) {
                if (r < eventPercentages[4]) {
                    cell.Die();
                } else if((r < eventPercentages[1])||(r < eventPercentages[2])){
                    cell.Die();
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ET_PACC, totalResistance);
                } else if(r < eventPercentages[0]){
                    cell.MutationET();
                    cell.Div();
                } else{

                }
            } else if ((cell.type == ET_PACC) && (cell.CanDivide(PACC_DIV_BIAS, PACC_INHIB_WEIGHT))) {
                if(r < eventPercentages[3]) {
                    cell.MutationET();
                    cell.Die();
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ET_ANEU, totalResistance);
                    if(cell.Xpt()+0.5 < xDim-0.5){
                        NewAgentPT(cell.Xpt()+0.5, cell.Ypt()).Init(ET_ANEU, totalResistance);
                    } else if (cell.Ypt()+0.5 < yDim - 0.5){
                        NewAgentPT(cell.Xpt(), cell.Ypt()+0.5).Init(ET_ANEU, totalResistance);
                    } else if (cell.Xpt()-0.5 > xDim +0.5){
                        NewAgentPT(cell.Xpt()-0.5, cell.Ypt()).Init(ET_ANEU, totalResistance);
                    } else if(cell.Ypt() -0.5 > yDim +0.5) {
                        NewAgentPT(cell.Xpt(), cell.Ypt()-0.5).Init(ET_ANEU, totalResistance);
                    } else {

                    }
                }
            } if ((cell.type == SGM_ANEU) && (cell.CanDivide(ANEU_DIV_BIAS, ANEU_INHIB_WEIGHT))) {
                if (r < eventPercentages[4]) {
                    cell.Die();
                } else if((r < eventPercentages[1])||(r < eventPercentages[2])) {
                    cell.Die();
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(SGM_PACC, totalResistance);
                } else if(r < eventPercentages[0]){
                    cell.MutationSGM();
                    cell.Div();
                } else {

                }
            } else if ((cell.type == SGM_PACC) && (cell.CanDivide(PACC_DIV_BIAS, PACC_INHIB_WEIGHT))) {
                if(r < eventPercentages[3]) {
                    cell.MutationSGM();
                    cell.Die();
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(SGM_ANEU, totalResistance);
                    if(cell.Xpt()+0.5 < xDim-0.5) {
                        NewAgentPT(cell.Xpt()+0.5, cell.Ypt()).Init(SGM_ANEU, totalResistance);
                    } else if (cell.Ypt()+0.5 < yDim - 0.5) {
                        NewAgentPT(cell.Xpt(), cell.Ypt()+0.5).Init(SGM_ANEU, totalResistance);
                    } else if (cell.Xpt()-0.5 > xDim +0.5) {
                        NewAgentPT(cell.Xpt()-0.5, cell.Ypt()).Init(SGM_ANEU, totalResistance);
                    } else if(cell.Ypt() -0.5 > yDim +0.5) {
                        NewAgentPT(cell.Xpt(), cell.Ypt()-0.5).Init(SGM_ANEU, totalResistance);
                    } else {

                    }
                }
            }
        }
        if(out!=null){
            //if an output file has been generated, write to it
            RecordOut(out);
        }
    }

    public void RecordOut (FileIO writeHere){
        int ctPACC = 0, ctAneu = 0;
        for (Cell2 cell : this) {
            if (cell.type == ET_PACC) {
                ctPACC++;
            } else {
                ctAneu++;
            }
        }
        writeHere.Write(ctAneu + "," + ctPACC + "\n");
    }
}