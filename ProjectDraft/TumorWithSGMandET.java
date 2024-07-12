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
import java.util.Arrays;

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

    public void Mutation() {
        int [] neighborhood = CircleHood(false,radius);
        int options = MapEmptyHood(neighborhood);
        if((type == TumorWithSGMandET.ET_PACC)||(type == TumorWithSGMandET.ET_ANEU)
        ||(((type==TumorWithSGMandET.SGM_ANEU)||(type == TumorWithSGMandET.SGM_PACC))&&
                (((options>0) ||(TumorWithSGMandET.deathDueToDrug(TumorWithSGMandET.drugDose, TumorWithSGMandET.totalResistance) < TumorWithSGMandET.fitnessThresholdSGM))))) {
            boolean mutated = G.rn.Bool();
            if (!mutated) {

            } else if (mutated) {
                double favorability = G.rn.Double(1);
                if (favorability < 0.9) {

                } else if (favorability > 0.9) {
                    double resistanceAdded = G.rn.Double(100);
                    resistance = G.totalResistance + resistanceAdded;
                }
                else {

                }
            }
        } else if(((type == TumorWithSGMandET.SGM_PACC)||(type == TumorWithSGMandET.SGM_ANEU))
                &&(((options<0)||(TumorWithSGMandET.deathDueToDrug(TumorWithSGMandET.drugDose, TumorWithSGMandET.totalResistance) > TumorWithSGMandET.fitnessThresholdSGM)))) {
            while(resistance < TumorWithSGMandET.resistancethresholdSGM) {
                double favorability = G.rn.Double(1);
                if (favorability < 0.9) {

                } else if (favorability > 0.9) {
                    double resistanceAdded = G.rn.Double(100);
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
    public static int fitnessThresholdSGM = 100;
    public static int resistancethresholdSGM = 100000;
    ArrayList<Cell2> neighborList = new ArrayList<>();
    ArrayList<double[]> neighborInfo = new ArrayList<>();
    double[] divCoordStorage = new double[2];
    Rand rn = new Rand(System.nanoTime());
    Rand r3 = new Rand(0);
    Gaussian gaussian = new Gaussian();
    static double totalResistance = 0;
    FileIO out;

    public TumorWithSGMandET(int x, int y, String outFileName) {
        super(x, y, Cell2.class, true, true);
        out = new FileIO(outFileName, "w");
    }

    public double facultativeToPACC(double drugDose, double drugResistance) {
        double facultative = drugDose/(100+drugResistance);
        return facultative;

    }

    public static double deathDueToDrug(double drugDose,  double totalResistance) {
        double death = drugDose/(100 + totalResistance);
        return death;
    }

    public static void main(String[] args) {
        OpenGL2DWindow.MakeMacCompatible(args);
        int x = 30, y = 30;
        TumorWithSGMandET model = new TumorWithSGMandET(x, y, "PopOut.csv");
        OpenGL2DWindow vis = new OpenGL2DWindow("Tumor With SGM and ET", 700, 700, x, y);
        model.Setup( 200, 5);
        int i = 0;
        while ((i<100000)&&(!vis.IsClosed())) {
            vis.TickPause(0);
            model.Draw(vis);
            model.StepCells();

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

    public void StepCells() {

        for(Cell2 cell : this){
            cell.CalcMove();
        } for(Cell2 cell : this) {
            cell.Move();
        }
        for (Cell2 cell : this) {
            double logistic;
            double obligate;
            double facultative = 0;
            double death;
            double from;
            if((cell.type == SGM_PACC)||(cell.type == ET_PACC)) {
                logistic = 0;
                obligate = 0;
                facultative = 0;
                death = 0;
                from = 0.4;
            } else {
                from = 0;
                logistic = 0.6;
                obligate = 0.02;
                facultative = facultativeToPACC(drugDose, totalResistance);
                death = deathDueToDrug(drugDose, totalResistance);
            }
            double[] eventProbabilities = {logistic,
                    obligate, facultative,
                    from, death};

            double sum = 0;
            for(int i = 0; i < eventProbabilities.length; i++){
                sum = sum + eventProbabilities[i];
            }
            double[] eventPercentages = new double[eventProbabilities.length];
            for(int w = 0; w < eventPercentages.length; w++) {
                eventPercentages[w] = (eventProbabilities[w]/sum);
            }
            double[] events = new double[eventPercentages.length];
            events[0] = eventPercentages[0];
            for(int e = 1; e < eventPercentages.length; e++) {
                events[e] = events[e-1] + eventPercentages[e];
            }

            double r = rn.Double(1);
            double r2 = r3.Double(1);
            System.out.println("event percentages: " + Arrays.toString(eventPercentages));
            System.out.println("events: " + Arrays.toString(events));
            System.out.println("random: " + r);

        if(((cell.type == ET_ANEU)||(cell.type == SGM_ANEU))&&(cell.CanDivide(ANEU_DIV_BIAS,ANEU_INHIB_WEIGHT))) {
            if (r < 0.6) {
                cell.Mutation();
                cell.Div();
            } else if(((r2 < obligate)&&(eventPercentages[1] != 0))||((r < events[2])&&(eventPercentages[2] != 0))) {
                    cell.Die();
                    if(cell.type == ET_ANEU) {
                        NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ET_PACC, totalResistance);
                    } else {
                        NewAgentPT(cell.Xpt(),cell.Ypt()).Init(SGM_PACC, totalResistance);
                    }
                }  else if((r < events[4])&&(eventPercentages[4] != 0)) {
                     cell.Die();
                 }
            } if(((cell.type == ET_PACC)||(cell.type == SGM_PACC))&&(cell.CanDivide(PACC_DIV_BIAS,PACC_INHIB_WEIGHT))) {
                if((r < 0.4)&&(eventPercentages[3] != 0)) {
                    cell.Mutation();
                    cell.Die();
                    if(cell.type == ET_PACC) {
                        NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ET_ANEU, totalResistance);
                        if(cell.Xpt()+0.5 < xDim-0.5) {
                            NewAgentPT(cell.Xpt()+0.5, cell.Ypt()).Init(ET_ANEU, totalResistance);
                        } else if (cell.Ypt()+0.5 < yDim - 0.5){
                            NewAgentPT(cell.Xpt(), cell.Ypt()+0.5).Init(ET_ANEU, totalResistance);
                        } else if (cell.Xpt()-0.5 > xDim +0.5){
                            NewAgentPT(cell.Xpt()-0.5, cell.Ypt()).Init(ET_ANEU, totalResistance);
                        } else if(cell.Ypt() -0.5 > yDim +0.5) {
                            NewAgentPT(cell.Xpt(), cell.Ypt()-0.5).Init(ET_ANEU, totalResistance);
                        } else {

                        }
                    } else if(cell.type == SGM_PACC) {
                        System.out.println("from");
                        NewAgentPT(cell.Xpt(),cell.Ypt()).Init(SGM_ANEU, totalResistance);
                        if(cell.Xpt()+0.5 < xDim-0.5) {
                            NewAgentPT(cell.Xpt()+0.5, cell.Ypt()).Init(SGM_ANEU, totalResistance);
                        } else if (cell.Ypt()+0.5 < yDim - 0.5){
                            NewAgentPT(cell.Xpt(), cell.Ypt()+0.5).Init(SGM_ANEU, totalResistance);
                        } else if (cell.Xpt()-0.5 > xDim +0.5){
                            NewAgentPT(cell.Xpt()-0.5, cell.Ypt()).Init(SGM_ANEU, totalResistance);
                        } else if(cell.Ypt() -0.5 > yDim +0.5) {
                            NewAgentPT(cell.Xpt(), cell.Ypt()-0.5).Init(SGM_ANEU, totalResistance);
                        } else {

                        }
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