package ABM.sources;

import HAL.GridsAndAgents.SphericalAgent2D;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Gui.OpenGL2DWindow;
import HAL.Tools.FileIO;
import HAL.Tools.Internal.Gaussian;
import HAL.Rand;
import java.io.*;
import java.util.Properties;

import java.lang.Math;
import java.util.ArrayList;

import static HAL.Util.*;

class CellFinal extends SphericalAgent2D<CellFinal, ProjectContinuation> {

    int type;
    double resistance;
    double forceSum;

    public void Init(int color, double resistance) {
        this.type = color;
        this.resistance = resistance + G.totalResistance;
        if (type == ProjectContinuation.SGM_PACC) {
            this.radius = 0.15;
        } else if (type == ProjectContinuation.SGM_ANEU) {
            this.radius = 0.1;
        } else if (type == ProjectContinuation.ET_PACC) {
            this.radius = 0.15;
        } else if (type == ProjectContinuation.ET_ANEU) {
            this.radius = 0.1;
        } else {
            System.out.println("different cell");
        }
    }

    double ForceCalc(double overlap, CellFinal other) {
        if(overlap < 0) {
            return 0;
        }
        return G.FORCE_SCALER*overlap;
    }

    public void CalcMove(){
        forceSum = SumForces(.3, this::ForceCalc);
        //.3 is the largest distance apart that the cells can be (ie. the two largest radii added together)
    }

    public boolean CanDivide(double div_bias,double inhib_weight){
        return G.rn.Double()<Math.tanh(div_bias-forceSum*inhib_weight);
    }

    public void Move() {
        ForceMove();
        ApplyFriction(G.FRICTION);
    }

    public void Div() {
        if (type == ProjectContinuation.SGM_PACC) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(ProjectContinuation.SGM_PACC, resistance);
        } else if(type == ProjectContinuation.SGM_ANEU) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(ProjectContinuation.SGM_ANEU, resistance);
        } else if(type == ProjectContinuation.ET_PACC) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(ProjectContinuation.ET_PACC, resistance);
        } else if(type == ProjectContinuation.ET_ANEU) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(ProjectContinuation.ET_ANEU, resistance);
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
        if ((options < 0) || (ProjectContinuation.deathDueToDrug(ProjectContinuation.drugDose, resistance) > ProjectContinuation.fitnessThreshold)) {
            if (mutationChance < ProjectContinuation.firstMutationChance) {
                mutated = true;
            } else {
                mutated = false;
            }
        } else {
            if (mutationChance < ProjectContinuation.secondMutation) {
                mutated = true;
            } else {
                mutated = false;
            }
        }
        if (mutated) {
            if (G.rn.Double(1) > ProjectContinuation.favorability) {
                double resistanceAdded = G.rn.Double(100);
                resistance += resistanceAdded;
            }
        }
    }
}

public class ProjectContinuation extends AgentGrid2D<CellFinal> {
    static final int WHITE = RGB256(248, 255, 252);
    static final int ET_PACC = RGB256(155, 155, 235);
    static final int ET_ANEU = RGB256(18, 148, 144);
    static final int SGM_PACC = RGB256(5, 47, 95);
    static final int SGM_ANEU = RGB256(170,68,101);
    static int drugCYTOPLASM = RGB256(240,177,177);
    static int CYTOPLASM = RGB256(255,227,217);

    // properties
    public double FORCE_SCALER = 0;
    public double FRICTION = 0;
    public double PACC_DIV_BIAS = 0;
    public double ANEU_DIV_BIAS = 0;
    public double PACC_INHIB_WEIGHT = 0;
    public double ANEU_INHIB_WEIGHT = 0;
    public static int fitnessThreshold = 0;
    public static double firstMutationChance = 0;
    public static double secondMutation = 0;
    public static double favorability = 0;
    public static String distanceFileName = "";
    public static String populationFileName = "";

    Rand rn = new Rand(System.nanoTime());
    public double SGMPACCPosition = 0;
    public double SGMANEUPosition = 0;
    public double ETPACCPosition = 0;
    public double ETANEUPosition = 0;
    public double centerX = xDim/2;
    public double centerY = yDim/2;

    public static int time = 0;
    public static double drugDose = 0;

    ArrayList<CellFinal> neighborList = new ArrayList<>();
    ArrayList<double[]> neighborInfo = new ArrayList<>();
    double[] divCoordStorage = new double[2];
    Rand r3 = new Rand(0);
    Gaussian gaussian = new Gaussian();
    static double totalResistance = 0;

    Properties props;
    FileIO out;
    FileIO out2;

    public ProjectContinuation(int x, int y, Properties props) {
        super(x, y, CellFinal.class, true, true);

        this.props = props;
        out = new FileIO(props.getProperty("distanceFileName"), "w");
        out2 = new FileIO(props.getProperty("populationFileName"),"w");

        FORCE_SCALER = Double.parseDouble(props.getProperty("FORCE_SCALER"));
        FRICTION = Double.parseDouble(props.getProperty("FRICTION"));
        PACC_DIV_BIAS = Double.parseDouble(props.getProperty("PACC_DIV_BIAS"));
        ANEU_DIV_BIAS = Double.parseDouble(props.getProperty("ANEU_DIV_BIAS"));
        PACC_INHIB_WEIGHT = Double.parseDouble(props.getProperty("PACC_INHIB_WEIGHT"));
        ANEU_INHIB_WEIGHT = Double.parseDouble(props.getProperty("ANEU_INHIB_WEIGHT"));
        fitnessThreshold = Integer.parseInt(props.getProperty("fitnessThreshold"));
        firstMutationChance = Double.parseDouble(props.getProperty("firstMutationChance"));
        secondMutation = Double.parseDouble(props.getProperty("secondMutationChance"));
        favorability = Double.parseDouble(props.getProperty("favorability"));

    }

    public double facultativeToPACC(double facultativeParameter, double drugDose, double totalResistance) {
        return facultativeParameter*(drugDose/(100+totalResistance));
    }

    public static double deathDueToDrug(double drugDose,  double totalResistance) {
        return drugDose/(100 + totalResistance);
    }

    public static void main(String[] args) {
        String[] args2 = new String[0];
        OpenGL2DWindow.MakeMacCompatible(args2);
        int x = 30, y = 30;

        try {
            String configFilePath = args[0]; //System.getenv("propertiesFilePath");
            FileInputStream propsInput = new FileInputStream(configFilePath);
            Properties props = new Properties();
            props.load(propsInput);

            ProjectContinuation model = new ProjectContinuation(x, y, props);
            OpenGL2DWindow vis = new OpenGL2DWindow ("Spatial SGM and ET Model", 700, 700, x, y);

            model.Setup( 200, 2);
            while ((time < 100000)&&(!vis.IsClosed())) {
                while(time < 200) {
                    System.out.println("time: " + time);
                    drugDose = 0;
                    CYTOPLASM = RGB256(255, 228, 225);
                    vis.TickPause(0);
                    model.Draw(vis);
                    model.StepCells(props);
                    model.cellDistanceMethod();
                    time++;
                } while(time < 2000) {
                    drugDose = 500;
                    CYTOPLASM = drugCYTOPLASM;
                    vis.TickPause(0);
                    model.Draw(vis);
                    model.StepCells(props);
                    model.cellDistanceMethod();
                    time++;
                    System.out.println("time: " + time);
                }
                while(time < 2500) {
                    drugDose = 0;
                    CYTOPLASM = RGB256(255, 228, 225);
                    vis.TickPause(0);
                    model.Draw(vis);
                    model.StepCells(props);
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
        } catch (FileNotFoundException e){
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        for (CellFinal cell : this) {
            vis.Circle(cell.Xpt(),cell.Ypt(),cell.radius, CYTOPLASM);
        }
        for (CellFinal cell : this) {
            vis.Circle(cell.Xpt(), cell.Ypt(), cell.radius / 3, cell.type);
        }
        vis.Update();
    }
    public void cellDistanceMethod () {
        int SGMPACCPop = 0, ETPACCPop = 0, SGMAneuPop = 0, ETAneuPop = 0;
        for(CellFinal cell : this) {
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

        for(CellFinal cell : this) {
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

    public void StepCells(Properties props) {
        double logistic = Double.parseDouble(props.getProperty("logistic"));
        double obligate = Double.parseDouble(props.getProperty("obligate"));
        double facultativeParameter = Double.parseDouble(props.getProperty("facultative"));
        double depoly = Double.parseDouble(props.getProperty("depoly"));
        double nothing = (1-logistic) * (1-obligate) * (1-facultativeParameter) * (1-depoly);
        double resistanceThreshold = Double.parseDouble(props.getProperty("resistanceThreshold"));

        for(CellFinal cell : this){
            cell.CalcMove();
        } for(CellFinal cell : this) {
            cell.Move();
        }
        for (CellFinal cell : this) {
            double facultative = facultativeToPACC(facultativeParameter, drugDose, cell.resistance);
            double death = deathDueToDrug(drugDose, cell.resistance);
            if (((cell.type == ET_ANEU) || (cell.type == SGM_ANEU)) && (cell.CanDivide(ANEU_DIV_BIAS, ANEU_INHIB_WEIGHT))) {
                double[] eventsAneu = {logistic, death, obligate, facultative, nothing};
                double[] eventPercentagesAneu = new double[eventsAneu.length];
                double sum = logistic + obligate + facultative + death + nothing;
                for (int i = 0; i < eventsAneu.length; i++) {
                    eventPercentagesAneu[i] = (eventsAneu[i] / sum);
                }
                double[] eventProbabilitiesAneu = new double[eventsAneu.length];
                eventProbabilitiesAneu[0] = eventPercentagesAneu[0];
                for (int i = 1; i < eventsAneu.length; i++) {
                    eventProbabilitiesAneu[i] = eventProbabilitiesAneu[i - 1] + eventPercentagesAneu[i];
                }
                double r = rn.Double(1);

                if (r < eventProbabilitiesAneu[0]) {
                    cell.Mutation();
                    cell.Div();
                } else if ((r < eventProbabilitiesAneu[1]) && (eventsAneu[1] != 0)) {
                    cell.Die();
                } else if ((r < eventProbabilitiesAneu[2]) || (r < eventProbabilitiesAneu[3])) {
                    cell.Die();
                    if (cell.type == ET_ANEU) {
                        NewAgentPT(cell.Xpt(), cell.Ypt()).Init(ET_PACC, cell.resistance);
                    } else {
                        NewAgentPT(cell.Xpt(), cell.Ypt()).Init(SGM_PACC, cell.resistance);
                    }
                } else if (r < eventProbabilitiesAneu[4]) {
                }
            } else if (((cell.type == ET_PACC) || (cell.type == SGM_PACC)) && (cell.CanDivide(PACC_DIV_BIAS, PACC_INHIB_WEIGHT))) {
                nothing = (1-depoly);
                double[] eventsPACC = {depoly, nothing};
                double[] eventPercentagesPACC = new double[eventsPACC.length];
                double sum = depoly + nothing;
                for (int i = 0; i < eventsPACC.length; i++) {
                    eventPercentagesPACC[i] = (eventsPACC[i] / sum);
                }
                double[] eventProbabilitiesPACC = new double[eventsPACC.length];
                eventProbabilitiesPACC[0] = eventPercentagesPACC[0];
                for (int i = 1; i < eventsPACC.length; i++) {
                    eventProbabilitiesPACC[i] = eventProbabilitiesPACC[i - 1] + eventPercentagesPACC[i];
                }
                double r = rn.Double(1);
                if (r < eventProbabilitiesPACC[0]) {
                    if (cell.type == ET_PACC) {
                        cell.Mutation();
                        cell.Die();
                        NewAgentPT(cell.Xpt(), cell.Ypt()).Init(ET_ANEU, cell.resistance);
                        double r1 = rn.Double(1);
                        if (r1 < 0.25) {
                            if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(ET_ANEU, cell.resistance);
                            }
                        } else if (r1 < .5) {
                            if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            }
                        } else if (r1 < .75) {
                            if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            }
                        } else {
                            if (cell.Ypt() - 0.1 > yDim + 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() - 0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Ypt() + 0.1 < yDim - 0.1) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() + 0.1).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() + 0.1 < xDim - 0.1) {
                                NewAgentPT(cell.Xpt() + 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.1 > xDim + 0.1) {
                                NewAgentPT(cell.Xpt() - 0.1, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                            }
                        }

                    } else if (cell.type == SGM_PACC) {
                        cell.Mutation();
                        if (drugDose > 0) {
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

        for (CellFinal cell : this) {
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