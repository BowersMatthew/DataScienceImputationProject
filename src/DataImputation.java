import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * DataImputation.class 
 * a java class to read in an incomplete data set and complete it by 4 different imputation
 * methods. The results are then compared to the known correct values.
 * 
 * The method take the incomplete data in csv form as the first argument and the complete data as the second
 * 
 * @author Matthew Bowers
 *
 */
public class DataImputation {
	
	//static DataObject[] objects;
	static String[] featureNames;
	static double[][] data1, data2, correctData;
	static double[][] transData1, transData2;
	static int numObjects = 3587;
	static int numFeatures = 85;
	static File source1,source2,correct,impute4Mean,impute4MeanCon,impute4Hd,impute4HdCon;
	static File impute20Mean,impute20MeanCon,impute20Hd,impute20HdCon;
	static final int C = 2;
	static final int F = 3;
	static DecimalFormat df = new DecimalFormat("#.####");
	static double mae4Mean, mae20Mean, mae4ConMean, mae20ConMean, mae4hd, mae20hd, mae4Conhd, mae20Conhd;

	public static void main(String[] args) {
		instantiateFiles();
		instantiateArrays();
		System.out.println("reading 0.4% data");
		readData(source1, data1, transData1);
		System.out.println("reading 20% data");
		readData(source2, data2, transData2);
		System.out.println("reading correct data");
		readData(correct, correctData);
		System.out.println("Filling 0.4% with mean");
		mae4Mean = fillMean(transData1, data1, impute4Mean);
		System.out.println("Filling 20% with mean");
		mae20Mean = fillMean(transData2, data2, impute20Mean);
		System.out.println("Filling 0.4% with Conditional mean");
		mae4ConMean = fillMeanCon(transData1, data1, impute4MeanCon);
		System.out.println("Filling 20% with Conditional mean");
		mae20ConMean = fillMeanCon(transData2, data2, impute20MeanCon);
		System.out.println("Filling 0.4% with hotDeck");
		mae4hd = fillHotDeck(data1, impute4Hd, false);
		System.out.println("Filling 20% with hotDeck");
		mae20hd = fillHotDeck(data2, impute20Hd, false);
		System.out.println("Filling 0.4% with Conditional hotDeck");
		mae4Conhd = fillHotDeck(data1, impute4HdCon, true);
		System.out.println("Filling 20% with Conditional hotDeck");
		mae20Conhd = fillHotDeck(data2, impute20HdCon, true);
		
		System.out.println("MAE_004_mean = " + df.format(mae4Mean));
		System.out.println("MAE_004_mean_conditional = " + df.format(mae4ConMean));
		System.out.println("MAE_004_hd = " + df.format(mae4hd));
		System.out.println("MAE_004_hd_conditional = " + df.format(mae4Conhd));
		System.out.println("MAE_20_mean = " + df.format(mae20Mean));
		System.out.println("MAE_20_mean_conditional = " + df.format(mae20ConMean));
		System.out.println("MAE_20_hd = " + df.format(mae20hd));
		System.out.println("MAE_20_hd_conditional = " + df.format(mae20Conhd));
		
		System.out.println("Finished.");
	}

	/**
	 * writes complete data set to target file using unconditional hot deck imputation
	 * @param original double[][] the original data with missing values
	 * @param target File the target file
	 * @param isConditional indicates if the hotdeck should consider class 
	 */
	private static double fillHotDeck(double[][] original, File target, boolean isConditional) {
		double sum = 0;
		int missing = 0;
		int[] bestMatch = new int[numObjects];
		// fill bestMatch with -1 as marker
		for (int i = 0; i < numObjects; i++){
			bestMatch[i] = -1;
		}
		StringBuilder toWrite = new StringBuilder();
		fillHeadings(toWrite);
		for(int i = 0; i < numObjects; i++){
			for(int j = 0; j < numFeatures; j++){
				// value to be use to fill missing value
				double value = -1;
				// End of line Class C
				if(original[i][j] == 2){
					toWrite.append("C,\n");
				// End of line Class F
				}else if(original[i][j] == 3){
					toWrite.append("F,\n");
				// value is present write it
				}else if(original[i][j] != -1){
					toWrite.append(df.format(original[i][j])+",");
				// value missing fill it in
				}else{
					// nearest match not know find it
					if(bestMatch[i] == -1){
						//System.out.println("No known nearest match. Finding Match!");
						bestMatch[i] = findNearest(i, original, isConditional);						
						value = original[bestMatch[i]][j];
						
						//System.out.println(value);
					
					}
					// if current bestMatch does not have a value find a sub
					if( original[bestMatch[i]][j] == -1){
						//System.out.println("Best match has missing value!");
						int subMatch = findSub(i, j, original, isConditional);
						value = original[subMatch][j];
						//System.out.println(value);
					
					}
					// if the best match is known and has a value write it
					else{
						//System.out.println("Using know nearest match");
						value = original[bestMatch[i]][j];
						//System.out.println(value);
					}
					if(value == -1){
						System.out.println("Failed to find value!");
						System.exit(-1);
					}else{
						toWrite.append(df.format(value)+",");
						sum += Math.abs(value - correctData[i][j]);
						missing++;
					}
				}
			}
		}
		try{
			writeToFile(toWrite, target);
		}catch(IOException e){
			e.printStackTrace();
		}
		return sum/missing;
		
	}
	
	/**
	 * returns the row index of the object which is nearest to the target object 
	 * using the class identifier as a numeric value. Being in a different class results in the 
	 * distance for the class attribute to be 1.
	 * @param rowToMatch int the row index of the object with a missing value
	 * @param original double[][] the 2D array holding the original data
	 * @param isConditional boolean indicates if class should be considered
	 * @return int the row index of the first closest object
	 */
	private static int findNearest(int rowToMatch, double[][] original, boolean isConditional){
		int best = -1;
		double minDistance = Double.MAX_VALUE;
		// move through each row
		for(int j = 0; j < numObjects; j++){
			double runTotal = 0;
			// if we get to the row we are trying to match skip it
			// or if the row is missing the value we are trying to replace
			if (j == rowToMatch || (isConditional && 
					original[rowToMatch][numFeatures-1] != original[j][numFeatures-1])){
				continue;
			}else{
				// sum up distances
				for(int k = 0; k < numFeatures; k++){
					// if either value is missing add the max distance of 1
					if( original[rowToMatch][k] == -1 || original[j][k] == -1){
						runTotal += 1;
					}else{
					runTotal += Math.pow(original[rowToMatch][k] - original[j][k] , 2);
					}
				}
			}
			// check if the current row is the closest so far
			
			if(Math.sqrt(runTotal) < minDistance){
				minDistance = Math.sqrt(runTotal);
				best = j;
			}
		}
		return best;
	}
	
	/**
	 * Finds the row index of the nearest object which has a value for the target attribute
	 * @param rowToMatch int index of the object with a missing attribute value
	 * @param missingValue int  index of the missing attribute
	 * @param original double[][] the 2D array holding the original data
	 * @param isConditional boolean indicates if class should be considered
	 * @return index int of nearest object
	 */
	private static int findSub(int rowToMatch, int missingValue, double[][] original, boolean isConditional){
		int best = -1;
		double minDistance = Double.MAX_VALUE;
		// move through each row
		for(int j = 0; j < numObjects; j++){
			double runTotal = 0;
			// if we get to the row we are trying to match skip it
			// or if the row is missing the value we are trying to replace
			if (j == rowToMatch || original[j][missingValue] == -1 || 
					(isConditional && original[rowToMatch][numFeatures - 1] != original[j][numFeatures - 1])){
				continue;
			}else{
				// sum up distances
				for(int k = 0; k < numFeatures - 1; k++){
					// if either value is missing add the max distance of 1
					if( original[rowToMatch][k] == -1 || original[j][k] == -1){
						runTotal += 1;
					}else{
					runTotal += Math.pow(original[rowToMatch][k] - original[j][k] , 2);
					}
				}
			}
			// check if the current row is the closest so far
			
			if(Math.sqrt(runTotal) < minDistance){
				minDistance = Math.sqrt(runTotal);
				best = j;
			}
		}
		// check for the possibility that no member of the class has a value for the attribute
		// if so try again with and unconditional search
		if(isConditional && best == -1){
			System.out.println("no match found in class searching all values");
				best = findSub(rowToMatch, missingValue, original, !isConditional);
			}
			if(best == -1){
				System.out.printf("Could not find a match!");
			}
		return best;
	}

	/**
	 * writes complete data to target file filling missing values with conditional mean
	 * @param trans double[][] original data transposed for faster access
	 * @param original double[][] original data with missing values
	 * @param target File the array will be written to this file
	 */
	private static double fillMeanCon(double[][] trans, double[][] original, File target) {
		double mae = 0;
		double[] averagesC = new double[numFeatures - 1];
		double[] averagesF = new double[numFeatures - 1];
		StringBuilder toWrite = new StringBuilder();
		fillHeadings(toWrite);
		fillAverages(averagesC, averagesF, trans, original);
		mae = fillAveragesFromMean(averagesC, averagesF, original, toWrite);
		try {
			writeToFile(toWrite, target);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mae;
	}

	/**
	 * Fills string builder with known values and fills missing values from averages
	 * @param averagesC known averages for class C objects
	 * @param averagesF known averages for class F objects
	 * @param original the original data
	 * @param toWrite StringBuilder
	 */
	private static double fillAveragesFromMean(double[] averagesC, double[] averagesF, double[][] original,
			StringBuilder toWrite) {
		int missing = 0;
		double sum = 0;
		for(int i = 0; i < numObjects; i++){
			//if(i == numObjects - 1){System.out.println("Last object: value: " + original[i][0]);}
			for(int j = 0; j < numFeatures; j++){
				// write complete values to string builder
				if(original[i][j] != -1){
					// write attribute values to string builder
					if(j < numFeatures - 1){
						toWrite.append(df.format(original[i][j]));
						toWrite.append(",");
					}
					// in last column write the correct letter to string builder
					else{
						if((int)original[i][j] == C){
							toWrite.append("C\n");
						}else if((int)original[i][j] == F){
							toWrite.append("F\n");
						}
					}
				// fill missing values with correct average
				}else{
					missing++;
					if((int)original[i][numFeatures-1] == C){
						toWrite.append(df.format(averagesC[j])+",");
						sum += Math.abs(averagesC[j] - correctData[i][j]);
					}else{
						toWrite.append(df.format(averagesF[j])+",");
						sum += Math.abs(averagesF[j] - correctData[i][j]);
					}
				}
			}	
		}
		return sum/missing;
	}

	/**
	 * Calculates and fills the arrays of averages for each attribute for each class
	 * @param averagesC
	 * @param averagesF
	 * @param trans
	 * @param original
	 */
	private static void fillAverages(double[] averagesC, double[] averagesF, double[][] trans, double[][] original) {
		double totalC, totalF;
		int countC, countF;
		for(int i = 0; i < numFeatures - 1; i++){
			totalC = 0;
			totalF = 0;
			countC = 0;
			countF = 0;
			for(int j = 0; j < numObjects; j++){
				if(trans[i][j] == -1){
					continue;
				}else{
					if(original[j][numFeatures -1 ] == C){
						totalC += trans[i][j];
						countC++;
					}else{
						totalF += trans[i][j];
						countF++;
					}
				}
			}
			averagesC[i] = totalC/countC;
			averagesF[i] = totalF/countF;
		}
		
	}

	/**
	 * fills all missing values with the average value for the attribute
	 * @param trans the original data transposed for faster access
	 * @param original the original data
	 * @param target the file where the output will be written
	 */
	private static double fillMean(double[][] trans, double[][] original, File target) {
		
		double sum = 0;
		int missing = 0;
		
		// calculate average of every column using the transpose matrix
		double[] averages = new double[numFeatures];
		double total;
		StringBuilder toWrite = new StringBuilder();
		fillHeadings(toWrite);
		int count;
		for(int i = 0; i < numFeatures; i++){
			total = 0;
			count = 0;
			for(int j = 0; j < numObjects; j++){
				if(trans[i][j] == -1){
					continue;
				}else{
					total+=trans[i][j];
					count++;
				}
			}
			averages[i] = total/count;
		}
		for(int i = 0; i < numObjects; i++){
			for(int j = 0; j < numFeatures; j++){
				// skip complete values
				if(original[i][j] != -1){
					// the value is not the last column
					if(j < numFeatures - 1){
						toWrite.append(df.format(original[i][j]));
						toWrite.append(",");
					}
					// in the lasts column write a letter instead of a number
					else{
						if((int)original[i][j] == C){
							toWrite.append("C\n");
						}else if((int)original[i][j] == F){
							toWrite.append("F\n");
						}
					}
				}
				// If the value was missing use the average for that column
				else{
					missing++;
					sum += Math.abs(averages[j]-correctData[i][j]);
					toWrite.append(df.format(averages[j]));
					toWrite.append(",");
				}
			}	
		}
		try {
			writeToFile(toWrite, target);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sum/missing;
	}

	/**
	 * Writes the string builder to the appropriate file on disk
	 * @param toWrite StringBuilder which is buffering the data to be written
	 * @param target File file where the data is to be written 
	 * @throws IOException Exception handling is a thing
	 */
	private static void writeToFile(StringBuilder toWrite, File target) throws IOException{
		FileWriter fw = new FileWriter(target);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(toWrite.toString());
		bw.close();
	}

	/**
	 * appends the column headings into the StringBuilder
	 * @param toWrite StringBuilder which is buffering the data to be written
	 */
	private static void fillHeadings(StringBuilder toWrite) {
		for(int i =0; i < numFeatures; i++){
			if(i == numFeatures - 1){
				toWrite.append(featureNames[i] + "\n");
			}else{
				toWrite.append(featureNames[i] + ",");
			}
		}
	}

/*	private static void copyArray(double[][] a, double[][] b) {
		for(int i = 0; i < a.length; i++){
			for(int j = 0; j < a[i].length; j++){
				b[i][j] = a[i][j];
			}
		}	
	}*/

	/**
	 * instantiate the array to hold all of the data
	 */
	private static void instantiateArrays() {
		data1 = new double[numObjects][numFeatures];
		transData1 = new double[numFeatures][numObjects];
		data2 = new double[numObjects][numFeatures];
		transData2 = new double[numFeatures][numObjects];
		correctData = new double[numObjects][numFeatures];
		featureNames = new String[numFeatures];
		
	}

	/**
	 * instantiate the many files we need
	 */
	private static void instantiateFiles() {
		source1 = new File("assignment2_dataset_missing004.csv"); 
		source2 = new File("assignment2_dataset_missing20.csv"); 
		correct = new File("assignment2_dataset_complete.csv");
		impute4Mean = new File("V00801365_a2_missing004_imputed_mean.csv");
		impute4MeanCon = new File("V00801365_a2_missing004_imputed_mean_conditional.csv");
		impute4Hd = new File("V00801365_a2_missing004_imputed_hd.csv");
		impute4HdCon = new File("V00801365_a2_missing004_hd_conditional.csv");
		impute20Mean = new File("V00801365_a2_missing20_imputed_mean.csv");
		impute20MeanCon = new File("V00801365_a2_missing20_imputed_mean_conditional.csv");
		impute20Hd = new File("V00801365_a2_missing20_imputed_hd.csv");
		impute20HdCon = new File("V00801365_a2_missing20_imputed_hd_conditional.csv");
	}

	/**
	 * reads data from source file into an array and a transposed array
	 * @param source File file containing the csv data
	 * @param a double[][] target array for the data
	 * @param b double[][] target array for the transposed data
	 */
	static void readData(File source, double[][] a, double[][] b){
		FileReader reader;
		String[] values = new String[numFeatures];
		String line;
		int lineNum = 0;
		try {
			reader = new FileReader(source);
			BufferedReader br = new BufferedReader(reader);
			
			// Read feature names from the first row
			values = br.readLine().split(",");
			for(int i = 0; i < numFeatures; i++){
				featureNames[i] = values[i]; 
			}
			// Read in the rest of the values
			while((line = br.readLine()) !=null){
				values = line.split(",");
				for(int i = 0; i < numFeatures; i++){
					// attempt to parse a double from the current string
					try {
						a[lineNum][i] = Double.parseDouble(values[i]);
					} 
					// if the parse fails we have a missing value or are in the last column
					catch (NumberFormatException e) {
						// assign value for C
						if(values[i].equals("C")){
							a[lineNum][i] = C;
						}
						// assign value for F
						if(values[i].equals("F")){
							a[lineNum][i] = F;
						}
						// change missing values to -1 so we can find them later
						if(values[i].equals("?")){
							a[lineNum][i] = -1;;
						}
					}
					// fill transposed table
					b[i][lineNum] = a[lineNum][i];
				}
				// move to the next row of the table
				lineNum++;
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("source1 not found, make sure your path is correct");
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * read in complete data set no need to transpose
	 * @param source File source of the data in csv format
	 * @param a double[][] target for parsed data
	 */
	static void readData(File source, double[][] a){
		FileReader reader;
		String[] values = new String[numFeatures];
		String line;
		int lineNum = 0;
		try {
			reader = new FileReader(source);
			BufferedReader br = new BufferedReader(reader);
			
			// Read feature names from the first row
			values = br.readLine().split(",");
			for(int i = 0; i < numFeatures; i++){
				featureNames[i] = values[i]; 
			}
			//lineNum++;
			// Read in the rest of the values
			while((line = br.readLine()) !=null){
				values = line.split(",");
				for(int i = 0; i < numFeatures; i++){
					try {
						a[lineNum][i] = Double.parseDouble(values[i]);
					} catch (NumberFormatException e) {
						if(values[i].equals("C")){
							a[lineNum][i] = C;
						}if(values[i].equals("F")){
							a[lineNum][i] = F;
						}if(values[i].equals("?")){
							a[lineNum][i] = -1;;
						}
					}
				}
				lineNum++;
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("source1 not found, make sure your path in arg1 is correct");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
}
