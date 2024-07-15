package ProjectDraft;

import HAL.GridsAndAgents.SphericalAgent2D;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Gui.OpenGL2DWindow;
import HAL.Tools.FileIO;
import HAL.Tools.Internal.Gaussian;
import HAL.Rand;

import java.lang.Math;
import java.sql.Array;
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
        this.resistance = resistance + TumorWithSGMandET.totalResistance;
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
            assert G != null;
            boolean mutated = G.rn.Bool();
             if (mutated) {
                double favorability = G.rn.Double(1);
                if (favorability < 0.9) {

                } else if (favorability > 0.9) {
                    double resistanceAdded = G.rn.Double(100);
                    resistance = TumorWithSGMandET.totalResistance + resistanceAdded;
                }
                else {

                }
            }
        } else if(((type == TumorWithSGMandET.SGM_PACC)||(type == TumorWithSGMandET.SGM_ANEU))
                &&(((options<0)||(TumorWithSGMandET.deathDueToDrug(TumorWithSGMandET.drugDose, TumorWithSGMandET.totalResistance) > TumorWithSGMandET.fitnessThresholdSGM)))) {
            while(resistance < TumorWithSGMandET.resistancethresholdSGM) {
                double favorability = G.rn.Double(1);
                 if (favorability > 0.9) {
                    double resistanceAdded = G.rn.Double(1000);
                    resistance = TumorWithSGMandET.totalResistance + resistanceAdded;
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
    public static int time = 0;
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

    public double facultativeToPACC(double drugDose, double totalResistance) {
        double facultative = 0.7*(drugDose/(100+totalResistance));
        return facultative;

    }

    public static double deathDueToDrug(double drugDose,  double totalResistance) {
        double death = drugDose/(100 + totalResistance);
        return death;
    }

    public static void main(String[] args) {
        OpenGL2DWindow.MakeMacCompatible(args);
        int x = 30, y = 30;
        TumorWithSGMandET model = new TumorWithSGMandET(x, y, "TumorData.csv");
        OpenGL2DWindow vis = new OpenGL2DWindow("Tumor With SGM and ET", 700, 700, x, y);
        model.Setup( 200, 5);
        int i = 0;
        while ((time < 100000)&&(!vis.IsClosed())) {
           /* while(time < 100){
                vis.TickPause(0);
                model.Draw(vis);
                model.StepCells();
                time++;
            }

            */
            while (time < 1000000) {
                drugDose = 200;
                vis.TickPause(0);
                model.Draw(vis);
                model.StepCells();
                time++;
            }

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
            double logistic = 0.6;
            double obligate = 0.02;
            double facultative = facultativeToPACC(drugDose, totalResistance);
            double death = deathDueToDrug(drugDose, totalResistance);
            double from = 0;
            double nothing = rn.Double(0.5);

        if(((cell.type == ET_ANEU)||(cell.type == SGM_ANEU))&&(cell.CanDivide(ANEU_DIV_BIAS,ANEU_INHIB_WEIGHT))) {
            double[] eventsAneu = {logistic, obligate, facultative, death, nothing};
            double[] eventPercentagesAneu = new double[eventsAneu.length];
            double sum = logistic + obligate + facultative + death + nothing;
            for(int i = 0; i< eventsAneu.length; i++) {
                eventPercentagesAneu[i] = (eventsAneu[i]/sum);
            }
            double[] eventProbabilitiesAneu = new double[eventsAneu.length];
            eventProbabilitiesAneu[0] = eventPercentagesAneu[0];
            for(int i = 1; i < eventsAneu.length; i++) {
                eventProbabilitiesAneu[i] = eventProbabilitiesAneu[i-1] + eventPercentagesAneu[i];
            }

            double r = rn.Double(1);
            if (r < eventProbabilitiesAneu[0]) {
                cell.Mutation();
                cell.Div();
            } else if((r < eventProbabilitiesAneu[1])||(r < eventProbabilitiesAneu[2])) {

                cell.Die();
                if(cell.type == ET_ANEU) {
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ET_PACC, totalResistance);
                } else {
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(SGM_PACC, totalResistance);
                }
            }  else if(r < eventProbabilitiesAneu[3]) {

                cell.Die();
            } else if(r < eventProbabilitiesAneu[4]) {

             }
        } else if(((cell.type == ET_PACC)||(cell.type == SGM_PACC))&&(cell.CanDivide(PACC_DIV_BIAS,PACC_INHIB_WEIGHT))) {
            from = 0.4;
            double[] eventsPACC = {from, nothing};
            double[] eventPercentagesPACC = new double[eventsPACC.length];
            double sum = from + nothing;
            for(int i = 0; i < eventsPACC.length; i++) {
                eventPercentagesPACC[i] = (eventsPACC[i]/sum);
            }
            double[] eventProbabilitiesPACC = new double[eventsPACC.length];
            eventProbabilitiesPACC[0] = eventPercentagesPACC[0];
            for(int i = 1; i < eventsPACC.length; i++) {
                eventProbabilitiesPACC[i] = eventProbabilitiesPACC[i-1] + eventPercentagesPACC[i];
            }
            double r = rn.Double(1);

                if(r < eventProbabilitiesPACC[0]) {

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

                } else if(r < eventProbabilitiesPACC[1]) {

                }
            }
        }
        if(out!=null){
            //if an output file has been generated, write to it
            RecordOut(out);
        }
    }



    public void RecordOut (FileIO writeHere){
        int ctPACCSGM = 0, ctPACCET = 0, ctAneuSGM = 0, ctAneuET = 0;
        for (Cell2 cell : this) {
            if (cell.type == ET_PACC) {
                ctPACCET++;
            } else if(cell.type == SGM_PACC){
                ctPACCSGM++;
            } else if(cell.type == ET_ANEU) {
                ctAneuET++;
            } else {
                ctAneuSGM++;
            }
        }
        writeHere.Write(time + "," + ctPACCET + "," + ctPACCSGM + "," + ctAneuET + "," + ctAneuSGM + "\n");

    }

}