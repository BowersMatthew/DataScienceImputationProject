import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * DataImputation.class 
 * a java class to read in an incomplete data set and complete it by 4 different imputation
 * methods. The results are then compared to the known correct values.
 * 
 * The method take the incomplete data in csv form as the first argument and the complete data as the second
 * 
 * @author matthew Bowers
 *
 */
public class DataImputation {
	
	//static DataObject[] objects;
	static String[] featureNames;
	static double[][] data1, data2, filled1, filled2;
	static double[][] transData1, transData2, transFilled1, transFilled2;
	static int numObjects;
	static int numFeatures;
	static File source1, source2, target, correct;

	public static void main(String[] args) {
		if(args.length != 5){
			System.out.println("Usage: java DataImputation [incomplete file1] [incomplete file2] [complete file] [# of objects]"
					+ " [# of Features]");
			System.exit(1);
		}
		try{
			source1 = new File(args[1]);
		}catch(Exception e){
			System.out.println("File Not Found: argument 1 should be the path the the first incomplete file");
		}
		try{
			source2 = new File(args[2]);
		}catch(Exception e){
			System.out.println("File Not Found: argument 2 should be the path the the second incomplete file");
		}
		try{
			correct = new File(args[1]);
		}catch(Exception e){
			System.out.println("File Not Found: argument 1 should be the path the the first incomplete file");
		}
		try{
			numObjects = Integer.parseInt(args[4]);
			//objects = new DataObject[numObjects];
		}catch(Exception e){
			System.out.println("argument 4 must be an integer indicating the number of objects in the file");
			System.exit(1);
		}
		try{
			numFeatures = Integer.parseInt(args[5]);
			//featureNames = new String[numFeatures];
		}catch(Exception e){
			System.out.println("argument 5 must be an integer indicating the number of features of each object");
			System.exit(1);
		}
		data1 = new double[numFeatures][numObjects];
		transData1 = new double[numObjects][numFeatures];
		data2 = new double[numFeatures][numObjects];
		transData2 = new double[numObjects][numFeatures];
		filled1 = new double[numFeatures][numObjects];
		transFilled1 = new double[numObjects][numFeatures];
		filled2 = new double[numFeatures][numObjects];
		transFilled2 = new double[numObjects][numFeatures];
		
		readData(source1, data1, transData1);
		readData(source2, data2, transData2);
		//for(int i = 0; i < numObjects; i++){
			//objects[i] = new DataObject(numFeatures);
		//}

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
			lineNum++;
			// Read in the rest of the values
			while((line = br.readLine()) !=null){
				values = line.split(",");
				for(int i = 0; i < numFeatures; i++){
					a[lineNum][i] = Double.parseDouble(values[i]);
					b[i][lineNum] = a[lineNum][i];
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
