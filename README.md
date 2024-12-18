# Spatial Agent Based Model (ABM) of Polyaneuploid Cancer Cells (PACCs)
This project is an ABM studying the spatial effects of two different evolutionary strategies. The aim of this project was to create a stochastic agent based, state structured model of PACCs and aneuploid cells, with two subpopulations that use different strategies. Through creating state-structured populations, the cells are able to fluidly transition between the two states - PACC and aneuploid - without being considered as two different species in the simulation.
The simulation is constructed with the Hybrid Automata Library (HAL) as a dependency.

## Table of Contents
- [Installation](#installation)
- [Simulation](#simulation)
- [Parameters](#parameters)

## Installation
1. Clone the repo using web URL
2. Download HAL Library here: https://halloworld.org
3. Add HAL as a module to the project
4. The code for the model is housed in the sources package, titled "ProjectContinuation"

## Simulation
Like I mentioned above, in the simulation, there are two different evolutionary strategies. The first is called evolutionary triage (ET) and the second is called self-genetic modification (SGM). If you would like to learn more about these strategies, please refer to this paper: [(https://pmc.ncbi.nlm.nih.gov/articles/PMC9338039/)]. 

The model includes four different cell types, with the color of their nuclei corresponding to their type: 
1. Green: PACCs under ET
2. Turquoise: Aneuploid cells under ET
3. Black: PACCs under SGM
4. Red: Aneuploid cells under SGM

Each individual cell has its own properties (ie. resistance, ability to divide, etc.) along with parameters connected to its type (ie. radius, color, etc.) These parameters can be altered in the config.properties file.

## Parameters
In the resources file, there is a default set of parameters that can be altered in the config.properties file. In order to load the parameters correctly, the environment variable, "propertiesFilePath" must be the file path of config.properties.


