package ProjectDraft;

import HAL.GridsAndAgents.SphericalAgent2D;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Gui.OpenGL2DWindow;
import HAL.Tools.FileIO;
import HAL.Tools.Internal.Gaussian;
import HAL.Rand;

import java.lang.Math;
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
            this.radius = 0.57;
        } else if (type == TumorWithSGMandET.SGM_ANEU) {
            this.radius = 0.4;
        } else if (type == TumorWithSGMandET.ET_PACC) {
            this.radius = 0.57;
        } else if (type == TumorWithSGMandET.ET_ANEU) {
            this.radius = 0.4;
        }
    }

    double ForceCalc(double overlap, Cell2 other) {
        if(overlap < 0) {
            return 0;
        }
        return G.FORCE_SCALER*overlap;
    }

    public void CalcMove(){
        forceSum = SumForces(1.14, this::ForceCalc);
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
        double mutationChance = G.rn.Double(1);
        if((type == TumorWithSGMandET.ET_ANEU)||(type == TumorWithSGMandET.ET_PACC)) {
            boolean mutated;
            if((options < 0)||(TumorWithSGMandET.deathDueToDrug(TumorWithSGMandET.drugDose, resistance) > TumorWithSGMandET.fitnessThreshold)) {
                if(mutationChance < 0.7) {
                    mutated = true;
                } else {
                    mutated = false;
                }
            } else {
                if(mutationChance < 0.3) {
                    mutated = true;
                } else {
                    mutated = false;
                }
            }
             if (mutated) {
                double favorability = G.rn.Double(1);
                if (favorability > 0.9) {
                    double resistanceAdded = G.rn.Double(100);
                    resistance += resistanceAdded;
                }
            }
        } else if((type == TumorWithSGMandET.SGM_PACC)||(type == TumorWithSGMandET.SGM_ANEU)) {
            if((options>0)||(TumorWithSGMandET.deathDueToDrug(TumorWithSGMandET.drugDose, resistance) < TumorWithSGMandET.fitnessThreshold)) {
                boolean mutated = G.rn.Bool();
                if (mutated) {
                    double favorability = G.rn.Double(1);
                     if (favorability > 0.9) {
                        double resistanceAdded = G.rn.Double(100);
                        resistance += resistanceAdded;
                    }
                }
            } else {
                double resistanceThreshold = G.rn.Double( 1000);
                while(resistance < resistanceThreshold) {
                    double favorability = G.rn.Double(1);
                    if (favorability > 0.8) {
                        double resistanceAdded = G.rn.Double(1000);
                        resistance += resistanceAdded;
                    }
                }
            }
        }
    }
}

public class TumorWithSGMandET extends AgentGrid2D<Cell2> {

    static final int WHITE = RGB256(248, 255, 252);
    static final int ET_PACC = RGB256(155, 155, 235);
    static final int ET_ANEU = RGB256(18, 148, 144);
    static final int SGM_PACC = RGB256(5, 47, 95);
    static final int SGM_ANEU = RGB256(170,68,101);
    static int drugCYTOPLASM = RGB256(240,177,177);
    static int CYTOPLASM = RGB256(255,227,217);

    Rand rn = new Rand(System.nanoTime());
    double FORCE_SCALER = .25;
    double FRICTION = .4;
    double PACC_DIV_BIAS = 0.02;
    double ANEU_DIV_BIAS = 0.01;
    double PACC_INHIB_WEIGHT = 0.05;
    double ANEU_INHIB_WEIGHT = 0.02;
    public static int time = 0;
    public static double drugDose = 0;
    public static int fitnessThreshold = 50;
    ArrayList<Cell2> neighborList = new ArrayList<>();
    ArrayList<double[]> neighborInfo = new ArrayList<>();
    double[] divCoordStorage = new double[2];
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
                while(time < 300) {
                    drugDose = 0;
                    vis.TickPause(0);
                    model.Draw(vis);
                    model.StepCells();
                    time++;
                }
                while(time < 5000) {
                    drugDose = 250;
                    CYTOPLASM = drugCYTOPLASM;
                    vis.TickPause(0);
                    model.Draw(vis);
                    model.StepCells();
                    time++;
                } while(time < 10000) {
                drugDose = 0;
                CYTOPLASM = RGB256(255,228,225);
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
            if(cellType < 0.05) {
                NewAgentPT(divCoordStorage[0] + xDim / 2.0, divCoordStorage[1] + yDim / 2.0).Init(ET_PACC, totalResistance);
            } else if(cellType < 0.1) {
                NewAgentPT(divCoordStorage[0] + xDim / 2.0, divCoordStorage[1] + yDim / 2.0).Init(SGM_PACC, totalResistance);
            } else if(cellType < 0.55) {
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
            double facultative = facultativeToPACC(drugDose, cell.resistance);
            double death = deathDueToDrug(drugDose, cell.resistance);
            double depoly = 0.4;
            double nothing = rn.Double(0.5);

        if(((cell.type == ET_ANEU)||(cell.type == SGM_ANEU))&&(cell.CanDivide(ANEU_DIV_BIAS,ANEU_INHIB_WEIGHT))) {
            if(cell.type == ET_ANEU) {
                System.out.println("et aneu resistance = " + cell.resistance);
            } else {
                System.out.println("sgm aneu resistance = " + cell.resistance);
            }
            double[] eventsAneu = {logistic, death, obligate, facultative, nothing};
            double[] eventPercentagesAneu = new double[eventsAneu.length];
            double sum = logistic + obligate + facultative + death + nothing;
            for(int i = 0; i < eventsAneu.length; i++) {
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
            } else if((r < eventProbabilitiesAneu[1])&&(eventsAneu[1] != 0)) {
                 cell.Die();
             } else if((r < eventProbabilitiesAneu[2])||(r < eventProbabilitiesAneu[3])) {
                cell.Die();
                if(cell.type == ET_ANEU) {
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ET_PACC, cell.resistance);
                } else {
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(SGM_PACC, cell.resistance);
                }
            } else if(r < eventProbabilitiesAneu[4]) {
             }
        } else if(((cell.type == ET_PACC)||(cell.type == SGM_PACC))&&(cell.CanDivide(PACC_DIV_BIAS,PACC_INHIB_WEIGHT))) {
            double[] eventsPACC = {depoly, nothing};
            double[] eventPercentagesPACC = new double[eventsPACC.length];
            double sum = depoly + nothing;
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
                        NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ET_ANEU, cell.resistance);
                        if(cell.Xpt()+0.5 < xDim - 0.5) {
                            NewAgentPT(cell.Xpt()+0.5, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                        } else if (cell.Ypt()+0.5 < yDim - 0.5){
                            NewAgentPT(cell.Xpt(), cell.Ypt()+0.5).Init(ET_ANEU, cell.resistance);
                        } else if (cell.Xpt()-0.5 > xDim + 0.5){
                            NewAgentPT(cell.Xpt()-0.5, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                        } else if(cell.Ypt() -0.5 > yDim + 0.5) {
                            NewAgentPT(cell.Xpt(), cell.Ypt()-0.5).Init(ET_ANEU, cell.resistance);
                        }
                    } else if(cell.type == SGM_PACC) {
                        NewAgentPT(cell.Xpt(),cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                        if(cell.Xpt()+0.5 < xDim - 0.5) {
                            NewAgentPT(cell.Xpt()+0.5, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                        } else if (cell.Ypt()+0.5 < yDim - 0.5){
                            NewAgentPT(cell.Xpt(), cell.Ypt()+0.5).Init(SGM_ANEU, cell.resistance);
                        } else if (cell.Xpt()-0.5 > xDim + 0.5){
                            NewAgentPT(cell.Xpt()-0.5, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                        } else if(cell.Ypt() -0.5 > yDim + 0.5) {
                            NewAgentPT(cell.Xpt(), cell.Ypt()-0.5).Init(SGM_ANEU, cell.resistance);
                        }
                    }

                } else if(r < eventProbabilitiesPACC[1]) {

                }
            }
        }
        if(out!=null){
            //if an output file has been generated, write to it
            RecordOutSize(out);
        }
    }

    public void RecordOutSize (FileIO writeHere){
        int ctPACCSGM = 0, ctPACCET = 0, ctAneuSGM = 0, ctAneuET = 0;
        int resistancePACCET = 0, resistancePACCSGM = 0, resistanceAneuET = 0, resistanceAneuSGM = 0;

        for (Cell2 cell : this) {
            if (cell.type == ET_PACC) {
                ctPACCET++;
                resistancePACCET++;
            } else if(cell.type == SGM_PACC){
                ctPACCSGM++;
                resistancePACCSGM++;
            } else if(cell.type == ET_ANEU) {
                ctAneuET++;
                resistanceAneuET++;
            } else {
                ctAneuSGM++;
                resistanceAneuSGM++;
            }
        }
        writeHere.Write(time + " , " + ctPACCET + ", " + ctPACCSGM + ", " + ctAneuET + ", " + ctAneuSGM + "\n");
    }
}