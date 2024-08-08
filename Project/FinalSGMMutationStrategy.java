package Project;

import HAL.GridsAndAgents.SphericalAgent2D;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Gui.OpenGL2DWindow;
import HAL.Tools.FileIO;
import HAL.Tools.Internal.Gaussian;
import HAL.Rand;

import java.lang.Math;
import java.util.ArrayList;

import static HAL.Util.*;

class Cell4 extends SphericalAgent2D<Cell4, FinalSGMMutationStrategy>{
    int type;
    double resistance;
    double forceSum;
    public void Init(int color, double resistance) {
        this.type = color;
        this.resistance = resistance + G.totalResistance;
        if (type == FinalSGMMutationStrategy.SGM_PACC) {
            this.radius = 0.15;
        } else if (type == FinalSGMMutationStrategy.SGM_ANEU) {
            this.radius = 0.1;
        } else if (type == FinalSGMMutationStrategy.ET_PACC) {
            this.radius = 0.15;
        } else if (type == FinalSGMMutationStrategy.ET_ANEU) {
            this.radius = 0.1;
        }
    }

    double ForceCalc(double overlap, Cell4 other) {
        if(overlap < 0) {
            return 0;
        }
        return G.FORCE_SCALER*overlap;
    }

    public void CalcMove(){
        forceSum = SumForces(.3, this::ForceCalc);
    }

    public boolean CanDivide(double div_bias,double inhib_weight){
        return G.rn.Double()<Math.tanh(div_bias-forceSum*inhib_weight);
    }

    public void Move() {
        ForceMove();
        ApplyFriction(G.FRICTION);
    }

    public void Div() {
        if (type == FinalSGMMutationStrategy.SGM_PACC) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(FinalSGMMutationStrategy.SGM_PACC, resistance);
        } else if(type == FinalSGMMutationStrategy.SGM_ANEU) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(FinalSGMMutationStrategy.SGM_ANEU, resistance);
        } else if(type == FinalSGMMutationStrategy.ET_PACC) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(FinalSGMMutationStrategy.ET_PACC, resistance);
        } else if(type == FinalSGMMutationStrategy.ET_ANEU) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(FinalSGMMutationStrategy.ET_ANEU, resistance);
        }
    }

    public void Die() {
        Dispose();
    }

    public void Mutation() {
        int[] neighborhood = CircleHood(false, radius);
        int options = MapEmptyHood(neighborhood);
        double mutationChance = G.rn.Double(1);
        boolean mutated;
        if ((options < 0) || (FinalSGMMutationStrategy.deathDueToDrug(FinalSGMMutationStrategy.drugDose, resistance) > FinalSGMMutationStrategy.fitnessThreshold)) {
            if (mutationChance < 0.7) {
                mutated = true;
            } else {
                mutated = false;
            }
        } else {
            if (mutationChance < 0.3) {
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
    }
}

public class FinalSGMMutationStrategy extends AgentGrid2D<Cell4> {

    static final int WHITE = RGB256(248, 255, 252);
    static final int ET_PACC = RGB256(155, 155, 235);
    static final int ET_ANEU = RGB256(18, 148, 144);
    static final int SGM_PACC = RGB256(5, 47, 95);
    static final int SGM_ANEU = RGB256(170,68,101);
    static int drugCYTOPLASM = RGB256(240,177,177);
    static int CYTOPLASM = RGB256(255,227,217);

    Rand rn = new Rand(System.nanoTime());
    public double SGMPACCPosition = 0;
    public double SGMANEUPosition = 0;
    public double ETPACCPosition = 0;
    public double ETANEUPosition = 0;
    public double centerX = xDim/2;
    public double centerY = yDim/2;
    double FORCE_SCALER = .25;
    double FRICTION = 0.5;
    double PACC_DIV_BIAS = 0.02;
    double ANEU_DIV_BIAS = 0.01;
    double PACC_INHIB_WEIGHT = 0.05;
    double ANEU_INHIB_WEIGHT = 0.02;
    public static int time = 0;
    public static double drugDose = 0;
    public static int fitnessThreshold = 50;
    ArrayList<Cell4> neighborList = new ArrayList<>();
    ArrayList<double[]> neighborInfo = new ArrayList<>();
    double[] divCoordStorage = new double[2];
    Rand r3 = new Rand(0);
    Gaussian gaussian = new Gaussian();
    static double totalResistance = 0;
    FileIO out;
    FileIO out2;


    public FinalSGMMutationStrategy(int x, int y, String outFileName, String outFileName2) {
        super(x, y, Cell4.class, true, true);
        out = new FileIO(outFileName, "w");
        out2 = new FileIO(outFileName2,"w");

    }

    public double facultativeToPACC(double drugDose, double totalResistance) {
        return 0.7*(drugDose/(100+totalResistance));
    }

    public static double deathDueToDrug(double drugDose,  double totalResistance) {
        return drugDose/(100 + totalResistance);
    }

    public static void main(String[] args) {
        OpenGL2DWindow.MakeMacCompatible(args);
        int x = 30, y = 30;
        FinalSGMMutationStrategy model = new FinalSGMMutationStrategy(x, y, "IntermittentTherapyHighDist1.csv", "IntermittentTherapyHighPop1.csv");
        OpenGL2DWindow vis = new OpenGL2DWindow("SGM and ET Tumor", 700, 700, x, y);
        model.Setup( 200, 2);
        while ((time < 100000)&&(!vis.IsClosed())) {
                while(time < 200) {
                    System.out.println("time: " + time);
                    drugDose = 0;
                    CYTOPLASM = RGB256(255, 228, 225);
                    vis.TickPause(0);
                    model.Draw(vis);
                    model.StepCells();
                    model.cellDistanceMethod();
                    time++;
                } while(time < 2000) {
                    drugDose = 500;
                    CYTOPLASM = drugCYTOPLASM;
                    vis.TickPause(0);
                    model.Draw(vis);
                    model.StepCells();
                    model.cellDistanceMethod();
                    time++;
                    System.out.println("time: " + time);
            }
            while(time < 2500) {
                drugDose = 0;
                CYTOPLASM = RGB256(255, 228, 225);
                vis.TickPause(0);
                model.Draw(vis);
                model.StepCells();
                model.cellDistanceMethod();
                time++;
                System.out.println("time: " + time);
            }
        }
        if ((model.out != null)||(model.out2 != null)) {
            model.out.Close();
            model.out2.Close();
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
        for (Cell4 cell : this) {
            vis.Circle(cell.Xpt(),cell.Ypt(),cell.radius, CYTOPLASM);
        }
        for (Cell4 cell : this) {
            vis.Circle(cell.Xpt(), cell.Ypt(), cell.radius / 3, cell.type);
        }
        vis.Update();
    }
    public void cellDistanceMethod () {
        int SGMPACCPop = 0, ETPACCPop = 0, SGMAneuPop = 0, ETAneuPop = 0;
        for(Cell4 cell : this) {
            if(cell.type == SGM_PACC){
                SGMPACCPop++;
            } else if(cell.type == ET_PACC) {
                ETPACCPop++;
            } else if(cell.type == SGM_ANEU) {
                SGMAneuPop++;
            } else if(cell.type == ET_ANEU)  {
                ETAneuPop++;
            }
        }

        for(Cell4 cell : this) {
            if(cell.type == SGM_PACC) {
                double cellDistance = Dist(cell.Xpt(),cell.Ypt(),centerX,centerY);
                SGMPACCPosition += cellDistance;
            } else if(cell.type == ET_PACC)  {
                double cellDistance = Dist(cell.Xpt(),cell.Ypt(),centerX,centerY);
                ETPACCPosition += cellDistance;
            } else if(cell.type == SGM_ANEU) {
                double cellDistance = Dist(cell.Xpt(),cell.Ypt(),centerX,centerY);
                SGMANEUPosition += cellDistance;
            } else if(cell.type == ET_ANEU) {
                double cellDistance = Dist(cell.Xpt(),cell.Ypt(),centerX,centerY);
                ETANEUPosition += cellDistance;
            }
        }
        SGMPACCPosition = SGMPACCPosition/SGMPACCPop;
        ETPACCPosition = ETPACCPosition/ETPACCPop;
        SGMANEUPosition = SGMANEUPosition/SGMAneuPop;
        ETANEUPosition = ETANEUPosition/ETAneuPop;

    }

    public void StepCells() {

        for(Cell4 cell : this){
            cell.CalcMove();
        } for(Cell4 cell : this) {
            cell.Move();
        }
        for (Cell4 cell : this) {
            double logistic = 0.6;
            double obligate = 0.02;
            double facultative = facultativeToPACC(drugDose, cell.resistance);
            double death = deathDueToDrug(drugDose, cell.resistance);
            double depoly = 0.6;
            double nothing = rn.Double(0.5);

            if(((cell.type == ET_ANEU)||(cell.type == SGM_ANEU))&&(cell.CanDivide(ANEU_DIV_BIAS,ANEU_INHIB_WEIGHT))) {
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
                    if(cell.type == ET_PACC) {
                        cell.Mutation();
                        cell.Die();
                        NewAgentPT(cell.Xpt(), cell.Ypt()).Init(ET_ANEU, cell.resistance);
                        double r1 = rn.Double(1);
                        if(r1 < 0.25) {
                            if(cell.Xpt() + 0.1 < xDim - 0.1) {
                                NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Ypt() + 0.1 < yDim - 0.1){
                                NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.1 > xDim + 0.1){
                                NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if(cell.Ypt() - 0.1 > yDim + 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(ET_ANEU, cell.resistance);
                            }
                        } else if(r1 < .5) {
                            if(cell.Ypt() - 0.1 > yDim + 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt()-0.1).Init(ET_ANEU, cell.resistance);
                            } else if(cell.Xpt() + 0.1 < xDim - 0.1) {
                                NewAgentPT(cell.Xpt()+0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Ypt() + 0.1 < yDim - 0.1){
                                NewAgentPT(cell.Xpt(), cell.Ypt()+0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.1 > xDim + 0.1){
                                NewAgentPT(cell.Xpt()-0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            }
                        } else if(r1 < .75) {
                             if (cell.Ypt() + 0.1 < yDim - 0.1){
                                NewAgentPT(cell.Xpt(), cell.Ypt()+0.1).Init(ET_ANEU, cell.resistance);
                            } else if(cell.Ypt() - 0.1 > yDim + 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt()-0.1).Init(ET_ANEU, cell.resistance);
                            } else if(cell.Xpt() + 0.1 < xDim - 0.1) {
                                NewAgentPT(cell.Xpt()+0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.1 > xDim + 0.1){
                                NewAgentPT(cell.Xpt()-0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            }
                        } else {
                            if(cell.Ypt() - 0.1 > yDim + 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt()-0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Ypt() + 0.1 < yDim - 0.1){
                                NewAgentPT(cell.Xpt(), cell.Ypt()+0.1).Init(ET_ANEU, cell.resistance);
                            }  else if(cell.Xpt() + 0.1 < xDim - 0.1) {
                                NewAgentPT(cell.Xpt()+0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.1 > xDim + 0.1){
                                NewAgentPT(cell.Xpt()-0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            }
                        }

                    } else if(cell.type == SGM_PACC) {
                            cell.Mutation();
                            if(drugDose > 0) {
                                double resistanceThreshold = 50;
                                if (cell.resistance > resistanceThreshold) {
                                    cell.Die();
                                    NewAgentPT(cell.Xpt(), cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    double r1 = rn.Double(1);
                                    if (r1 < 0.25) {
                                        if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                            NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                            NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                            NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                            NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(SGM_ANEU, cell.resistance);
                                        }
                                    } else if (r1 < .5) {
                                        if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                            NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                            NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                            NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                            NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                        }
                                    } else if (r1 < .75) {
                                        if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                            NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                            NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                            NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                            NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                        }
                                    } else {
                                        if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                            NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                            NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                            NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                        } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                            NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                        }
                                    }
                                }
                            } else {
                                cell.Die();
                                NewAgentPT(cell.Xpt(), cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                double r1 = rn.Double(1);
                                if (r1 < 0.25) {
                                    if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                        NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                        NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                        NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                        NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(SGM_ANEU, cell.resistance);
                                    }
                                } else if (r1 < .5) {
                                    if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                        NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                        NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                        NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                        NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    }
                                } else if (r1 < .75) {
                                    if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                        NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                        NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                        NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                        NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    }
                                } else {
                                    if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                        NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                        NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                        NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                        NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        RecordOutSize(out, out2);
    }

    public void RecordOutSize (FileIO writeHere, FileIO writeHere1){
        int ctPACCSGM = 0, ctPACCET = 0, ctAneuSGM = 0, ctAneuET = 0;

        for (Cell4 cell : this) {
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
        writeHere.Write(time + "," + ETPACCPosition + "," + SGMPACCPosition + "," + ETANEUPosition + "," + SGMANEUPosition + "\n");
        writeHere1.Write(time + "," + ctPACCET + "," + ctPACCSGM + "," + ctAneuET + "," + ctAneuSGM + "\n");


    }
}