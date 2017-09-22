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
	static double[][] data1, data2, filled1, filled2, correctData;
	static double[][] transData1, transData2;
	static int numObjects = 3587;
	static int numFeatures = 85;
	static File source1,source2,correct,impute4Mean,impute4MeanCon,impute4Hd,impute4HdCon;
	static File impute20Mean,impute20MeanCon,impute20Hd,impute20HdCon;
	static final int C = 2;
	static final int F = 3;
	static DecimalFormat df = new DecimalFormat("#.#####");

	public static void main(String[] args) {
		instantiateFiles();
		instantiateArrays();
		System.out.println("reading 0.4% data");
		readData(source1, data1, transData1);
		System.out.println("reading 20% data");
		readData(source2, data2, transData2);
		System.out.println("reading correct data");
		readData(correct, correctData);
		System.out.println("copying 0.4% data");
		System.out.println("copying 20% data");
		//System.out.println(data1[3][7]);
		//System.out.println(transData1[7][3]);
		//System.out.println(filled1[3][7]);
		System.out.println("Filling 0.4% with mean");
		fillMean(transData1, data1, impute4Mean);
		System.out.println("Filling 20% with mean");
		fillMean(transData2, data2, impute20Mean);
		System.out.println("Filling 0.4% with Conditional mean");
		fillMeanCon(transData1, data1, impute4MeanCon);
		fillMeanCon(transData2, data2, impute20MeanCon);
		
		System.out.println("Finished.");
	}

	private static void fillMeanCon(double[][] trans, double[][] original, File target) {
		double totalC, totalF;
		int countC, countF;
		double[] averagesC = new double[numFeatures - 1];
		double[] averagesF = new double[numFeatures - 1];
		StringBuilder toWrite = new StringBuilder();
		fillHeadings(toWrite);
		//fillAverages(averagesC, averagesF);
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
		for(int i = 0; i < numObjects; i++){
			//if(i == numObjects - 1){System.out.println("Last object: value: " + original[i][0]);}
			for(int j = 0; j < numFeatures; j++){
				// skip complete values
				//System.out.println(original[i][j]);
				if(original[i][j] != -1){
					if(j < numFeatures - 1){
						toWrite.append(df.format(original[i][j]));
						toWrite.append(",");
					}else{
						//System.out.println("In last column! Row: " + i);
						if((int)original[i][j] == C){
							toWrite.append("C\n");
						}else if((int)original[i][j] == F){
							toWrite.append("F\n");
						}
					}
				// fill missing values with correct average
				}else{
									
					if((int)original[i][numFeatures-1] == C){
						toWrite.append(df.format(averagesC[j])+",");
					}else{
						toWrite.append(df.format(averagesF[j])+",");
					}
				}
			}
			//System.out.println(toWrite.toString());	
		}
		try {
			writeToFile(toWrite, target);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void fillMean(double[][] trans, double[][] original, File target) {
		copyArray(original, filled1);
		//System.out.println("In fillMean: " + original[3][7]);
		
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
			//if(i == numObjects - 1){System.out.println("Last object: value: " + original[i][0]);}
			for(int j = 0; j < numFeatures; j++){
				// skip complete values
				//System.out.println(original[i][j]);
				if(original[i][j] != -1){
					if(j < numFeatures - 1){
						toWrite.append(df.format(original[i][j]));
						toWrite.append(",");
					}else{
						//System.out.println("In last column! Row: " + i);
						if((int)original[i][j] == C){
							toWrite.append("C\n");
						}else if((int)original[i][j] == F){
							toWrite.append("F\n");
						}
					}
					//continue;
				}else{
					filled1[i][j] = averages[j];
					if(j < numFeatures - 1){
						toWrite.append(df.format(filled1[i][j]));
						toWrite.append(",");
					}else{
						//System.out.println("In last column! Row: " + i);
						if((int)filled1[i][j] == C){
							//System.out.println(filled1[i][j]);
							toWrite.append("C\n");
						}else if((int)filled1[i][j] == F){
							toWrite.append("F\n");
						}
					}
				}
			}
			//System.out.println(toWrite.toString());	
		}
		try {
			writeToFile(toWrite, target);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeToFile(StringBuilder toWrite, File target) throws IOException{
		FileWriter fw = new FileWriter(target);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(toWrite.toString());
		bw.close();
	}

	private static void fillHeadings(StringBuilder toWrite) {
		for(int i =0; i < numFeatures; i++){
			if(i == numFeatures - 1){
				toWrite.append(featureNames[i] + "\n");
			}else{
				toWrite.append(featureNames[i] + ",");
			}
		}
	}

	private static void copyArray(double[][] a, double[][] b) {
		for(int i = 0; i < a.length; i++){
			for(int j = 0; j < a[i].length; j++){
				b[i][j] = a[i][j];
			}
		}	
	}

	private static void instantiateArrays() {
		data1 = new double[numObjects][numFeatures];
		transData1 = new double[numFeatures][numObjects];
		data2 = new double[numObjects][numFeatures];
		transData2 = new double[numFeatures][numObjects];
		filled1 = new double[numObjects][numFeatures];
		filled2 = new double[numObjects][numFeatures];
		correctData = new double[numObjects][numFeatures];
		featureNames = new String[numFeatures];
		
	}

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
			//lineNum++;
			// Read in the rest of the values
			while((line = br.readLine()) !=null){
				values = line.split(",");
				for(int i = 0; i < numFeatures; i++){
					try {
						//System.out.println(values[i]);
						if(i == numFeatures - 1){
							//System.out.println("Reading last column. Value: " + values[i]);
						}
						a[lineNum][i] = Double.parseDouble(values[i]);
						//System.out.println(data1[lineNum][i]);
					} catch (NumberFormatException e) {
						if(values[i].equals("C")){
							//System.out.println("Writing C to column: " + i);
							a[lineNum][i] = C;
						}if(values[i].equals("F")){
							a[lineNum][i] = F;
						}if(values[i].equals("?")){
							a[lineNum][i] = -1;;
						}
					}
					b[i][lineNum] = a[lineNum][i];
					//System.out.println("a: " + a[lineNum][i] + " b: " + b[i][lineNum]);
				}
				lineNum++;
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("source1 not found, make sure your path is correct");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
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
	
	/*static class DataObject{
		private int[] values;
		
		
		 * Constructor
		 * @param i - the number of features in the data set
		 *//*
		private DataObject(int i){
			values = new int[i];
		}
		
		public int[] getValues(){
			return values;
		}*/
	//}
}
