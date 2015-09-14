package org.archive.a1.search.tabu;
/**
 *
 * @author http://voidException.weebly.com
 * Use this code at your own risk ;)
 */
public class TabuSearch {

    public static int[] getBestNeighbour(TabuList tabuList,
            TSPEnvironment tspEnviromnet,
            int[] initSolution) {


        int[] bestSol = new int[initSolution.length]; //this is the best Solution So Far
        System.arraycopy(initSolution, 0, bestSol, 0, bestSol.length);
        int bestCost = tspEnviromnet.getObjectiveFunctionValue(initSolution);
        int city1 = 0;
        int city2 = 0;
        boolean firstNeighbor = true;

        for (int i = 1; i < bestSol.length - 1; i++) {
            for (int j = 2; j < bestSol.length - 1; j++) {
                if (i == j) {
                    continue;
                }

                int[] newBestSol = new int[bestSol.length]; //this is the best Solution So Far
                System.arraycopy(bestSol, 0, newBestSol, 0, newBestSol.length);

                newBestSol = swapOperator(i, j, initSolution); //Try swapping cities i and j
                // , maybe we get a bettersolution
                int newBestCost = tspEnviromnet.getObjectiveFunctionValue(newBestSol);



                if ((newBestCost > bestCost || firstNeighbor) && tabuList.tabuList[i][j] == 0) { //if better move found, store it
                    firstNeighbor = false;
                    city1 = i;
                    city2 = j;
                    System.arraycopy(newBestSol, 0, bestSol, 0, newBestSol.length);
                    bestCost = newBestCost;
                }


            }
        }

        if (city1 != 0) {
            tabuList.decrementTabu();
            tabuList.tabuMove(city1, city2);
        }
        return bestSol;


    }

    //swaps two cities
    public static int[] swapOperator(int city1, int city2, int[] solution) {
        int temp = solution[city1];
        solution[city1] = solution[city2];
        solution[city2] = temp;
        return solution;
    }

    public static void main(String[] args) {

        TSPEnvironment tspEnvironment = new TSPEnvironment();

        tspEnvironment.distances = //Distance matrix, 5x5, used to represent distances
                new int[][]{{0, 1, 3, 4, 5},
                            {1, 0, 1, 4, 8},
                            {3, 1, 0, 5, 1},
                            {4, 4, 5, 0, 2},
                            {5, 8, 1, 2, 0}};
        //Between cities. 0,1 represents distance between cities 0 and 1, and so on.

        int[] currSolution = new int[]{0, 1, 2, 3, 4, 0};   //initial solution
        //city numbers start from 0
        // the first and last cities' positions do not change

        int numberOfIterations = 100;
        int tabuLength = 10;
        TabuList tabuList = new TabuList(tabuLength);

        int[] bestSol = new int[currSolution.length]; //this is the best Solution So Far
        System.arraycopy(currSolution, 0, bestSol, 0, bestSol.length);
        int bestCost = tspEnvironment.getObjectiveFunctionValue(bestSol);

        for (int i = 0; i < numberOfIterations; i++) { // perform iterations here

            currSolution = TabuSearch.getBestNeighbour(tabuList, tspEnvironment, currSolution);
            //printSolution(currSolution);
            int currCost = tspEnvironment.getObjectiveFunctionValue(currSolution);

            //System.out.println("Current best cost = " + tspEnvironment.getObjectiveFunctionValue(currSolution));

            if (currCost < bestCost) {
                System.arraycopy(currSolution, 0, bestSol, 0, bestSol.length);
                bestCost = currCost;
            }
        }

        System.out.println("Search done! \nBest Solution cost found = " + bestCost + "\nBest Solution :");

        printSolution(bestSol);



    }

    public static void printSolution(int[] solution) {
        for (int i = 0; i < solution.length; i++) {
            System.out.print(solution[i] + " ");
        }
        System.out.println();
    }
}
