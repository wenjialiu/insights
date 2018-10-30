package insightdata;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VisaApplication {
	
	private static final String SOC_NAME = "SOC_NAME";
	private static final String SOC_CODE = "SOC_CODE";
	private static final String CASE_STATUS = "CASE_STATUS";
	private static final String WORKSITE_STATE = "WORKSITE_STATE";
	
	private static final String TOP_OCCUPATION_COUNT_TITLE = "TOP_OCCUPATIONS;NUMBER_CERTIFIED_APPLICATIONS;PERCENTAGE\n";
	private static final String TOP_STATES_COUNT_TITLE = "TOP_STATES;NUMBER_CERTIFIED_APPLICATIONS;PERCENTAGE\n";
	
	private static final String CASE_CERTIFIED = "CERTIFIED";
	private static final String CASE_WITHDRAWN = "WITHDRAWN";
	private static final String CASE_CERTIFIED_WITHDRAWN = "CERTIFIED-WITHDRAWN";
	private static final String CASE_DENIED = "DENIED";
	
	private static String sourceInputPath;
	private static String destOccupationOutputPath;
	private static String destStateOutputPath;
	private static File sourceFile;
	private static BufferedWriter writer;
	
	private static StringBuilder sb = new StringBuilder();
	private static boolean alreadyGotTotalIndex = false;
	private static int indexLineLength = 0;
	private static int lineLength = 0;
	private static boolean certifiedLine = false;
	
	private static int indexOfOccupationName;
	private static int indexOfOccupationCode;
	private static int indexOfCaseStatus;
	private static int indexOfWorksiteStates;
	private static int totalCaseCertified = 0;
	private static String lineOccupationName = "";
	private static String lineOccupationCode = "";
	private static String lineStates = "";
	
	private static Set<String> visaStatus = new HashSet<>();
	private static Map<String, Integer> occupationCountMap = new HashMap<>();
	private static Map<String, String> occupationNameKeyCodeValue = new HashMap<>();
	private static Map<String, Integer> statesCountMap = new HashMap<>();
	
	/*
	 * This method is to read csv file one line by one line.
	 * Firstly read the index line, and save the variables' indices we need,
	 * Then read the line for 
	 */
	public static void readAndSaveInMaps() throws FileNotFoundException{
		visaStatus.add(CASE_CERTIFIED);
		visaStatus.add(CASE_WITHDRAWN);
		visaStatus.add(CASE_CERTIFIED_WITHDRAWN);
		visaStatus.add(CASE_DENIED);
		sourceFile = new File(sourceInputPath);
		try {
			Scanner sc = new Scanner(sourceFile);
			String line = "";
			try {
				while(sc.hasNext()) {
					line = sc.next();
					if(!alreadyGotTotalIndex) {
						getIndexLength(line);
					}
					else {
						parseDataAndSaveInMap(line);
					}
				}
				if(certifiedLine) {
					saveCertifiedApplicationData();
				}
			}
			finally {
				sc.close();
			}
		}catch (FileNotFoundException e) {
			throw new FileNotFoundException("ERROR: Inputfile does not exist.");
		}
	}
	
	/*
	 * This method is to parse index string to get variables' indices until 
	 * get to the quote which is a start of a person's application record.
	 */
	public static void getIndexLength(String line) {
		/*
		 * Check if the string starting with a quote.
		 * If it is, "alreadyGotTotalIndex" is true now because 
		 * we do not need to get into this method any more.
		 */
		if(line.length() > 0 && Character.isDigit(line.charAt(0))) {
			String last = sb.toString().trim();
			if(last.length() > 0) {
				saveIndices(last);
				indexLineLength++;
			}
			sb.setLength(0);
			alreadyGotTotalIndex = true;
			parseDataAndSaveInMap(line);
			return ;
		}
		for(int i = 0; i < line.length(); i++) {
			if(line.charAt(i) == ';') {
				String indexName = sb.toString();
				saveIndices(indexName);
				indexLineLength++;
				sb.setLength(0);
			}
			else {
				sb.append(line.charAt(i));
			}
		}
		/*
		 * Need to append a space here because StringBuilder need to 
		 * connect with the next line with a space
		 */
		if(sb.length() > 0) sb.append(" ");
	}
	
	//This method is to save variables' indices
	public static void saveIndices(String indexName) {
		if(indexName.equals(SOC_NAME)) {
			indexOfOccupationName = indexLineLength;
		}
		else if(indexName.equals(SOC_CODE)) {
			indexOfOccupationCode = indexLineLength;
		}
		else if(indexName.equals(CASE_STATUS)) {
			indexOfCaseStatus = indexLineLength;
		}
		else if(indexName.equals(WORKSITE_STATE)) {
			indexOfWorksiteStates = indexLineLength;
		}
	}
	
	/*
	 * This method is used to parse every application record.
	 */
	public static void parseDataAndSaveInMap(String line) {
		for(int i = 0 ;i < line.length(); i++) {
			if(line.charAt(i) == ';') {
				String word = sb.toString();
				saveIndexData(word);
				lineLength++;
				sb.setLength(0);
			}
			else {
				if(line.charAt(i) != '\"') sb.append(line.charAt(i));
			}
		}
		if(sb.length() > 0) sb.append(" ");
	}
	
	
	/* 
	 * After traversing all characters in the application,
	 * save data in matching map: occupationCountMap and statesCountMap.
	 * We use SOC_CODE as occupationCountMap's key and certified application counts as value.
	 * We do not use SOC_NAME as a key because SOC_CODE is made up of numbers, 
	 * it is more stable than string names, but we still need a map(occupationNameKeyCodeValue) to match them.
	 * We use WORKSITE_STATE as statesCountMap's key and certified application counts as value.
	 */
	public static void saveCertifiedApplicationData() {
		if(!occupationNameKeyCodeValue.containsKey(lineOccupationCode)) {
			occupationNameKeyCodeValue.put(lineOccupationCode, lineOccupationName);
		}
		if(!occupationCountMap.containsKey(lineOccupationCode)) {
			occupationCountMap.put(lineOccupationCode, 1);
		}
		else {
			occupationCountMap.put(lineOccupationCode, occupationCountMap.get(lineOccupationCode)+1);
		}
		if(!statesCountMap.containsKey(lineStates)){
			statesCountMap.put(lineStates, 1);
		}
		else {
			statesCountMap.put(lineStates, statesCountMap.get(lineStates)+1);
		}
	}
	
	/*
	 * if the index is one of the specific variables' index, save it
	 * "certifiedLine = true" means the last application is valid, which means
	 * we need to count. And continue counting the new application.
	 */
	public static void saveIndexData(String word) {
		if(visaStatus.contains(word)) {
			if(certifiedLine) {
				saveCertifiedApplicationData();
				certifiedLine = false;
			}
			lineLength = indexOfCaseStatus;
		}
		if(lineLength == indexOfCaseStatus && word.equals(CASE_CERTIFIED)) {
			totalCaseCertified++;
			certifiedLine = true;
		}
		else if(lineLength == indexOfOccupationName) {
			lineOccupationName = word;
		}
		else if(lineLength == indexOfOccupationCode) {
			lineOccupationCode = word;
		}
		else if(lineLength == indexOfWorksiteStates) {
			lineStates = word;
		}
	}
	
	/*
	 * This method used 10-size min-heap to store certified applications, 
	 * if there is a tie, sort reverse alphabetically by SOC_NAME.
	 * Output reversely to get the top-10 certified applications.
	 */
	public static void sortOccupationData() throws IOException {
		PriorityQueue<String> pqSortByOccupation = new PriorityQueue<>(new Comparator<String>(){
			@Override
			public int compare(String s1, String s2) {
				//in case string s1 or s2 are not in maps
				if(!occupationCountMap.containsKey(s1) || !occupationCountMap.containsKey(s2)) {
					return 0;
				}
				if(!occupationNameKeyCodeValue.containsKey(s1) || !occupationNameKeyCodeValue.containsKey(s2)) {
					return 0;
				}
				if(occupationCountMap.get(s1) != occupationCountMap.get(s2)) {
					return occupationCountMap.get(s1) - occupationCountMap.get(s2);
				}
				else {
					return occupationNameKeyCodeValue.get(s2).compareTo(occupationNameKeyCodeValue.get(s1));
				}
			}
		});
		//maintain a 10-size min-heap
		for(String occupation:occupationCountMap.keySet()) {
			pqSortByOccupation.offer(occupation);
			if(pqSortByOccupation.size() > 10) {
				pqSortByOccupation.poll();
			}
		}
		//add reversely
		List<String> occupations = new ArrayList<>();
		while(!pqSortByOccupation.isEmpty()) {
			occupations.add(0, pqSortByOccupation.poll());
		}
		//output top 10 occupations
		writer = new BufferedWriter(new FileWriter(destOccupationOutputPath));
		writer.write(TOP_OCCUPATION_COUNT_TITLE);
		for(String occupationId:occupations) {
			String occupationName = occupationNameKeyCodeValue.get(occupationId);
			int occupationCaseCertified = occupationCountMap.get(occupationId);
			double percentage = (double)occupationCaseCertified*100/totalCaseCertified;
			String output = occupationName + ";" + occupationCaseCertified + ";" + String.format("%.1f", percentage) + "%\n";
			writer.write(output);
		}
		writer.close();
	}
	
	/*
	 * This method used 10-size min-heap to store certified applications, 
	 * if there is a tie, sort reverse alphabetically by WORKSITE_STATE.
	 * Output reversely to get the top-10 certified applications.
	 */
	public static void sortStatesData() throws IOException {
		PriorityQueue<String> pqSortByStates = new PriorityQueue<>(new Comparator<String>(){
			@Override
			public int compare(String s1, String s2) {
				//in case s1 or s2 not in map
				if(!statesCountMap.containsKey(s1) || !statesCountMap.containsKey(s2)) {
					return 0;
				}
				if(statesCountMap.get(s1) != statesCountMap.get(s2)) {
					return statesCountMap.get(s1) - statesCountMap.get(s2);
				}
				else {
					return s2.compareTo(s1);
				}
			}
		});
		//maintain 10-size min-heap
		for(String state:statesCountMap.keySet()) {
			pqSortByStates.offer(state);
			if(pqSortByStates.size() > 10) {
				pqSortByStates.poll();
			}
		}
		
		//add reversely
		List<String> states = new ArrayList<>();
		while(!pqSortByStates.isEmpty()) {
			states.add(0, pqSortByStates.poll());
		}
		//output top 10 states
		writer = new BufferedWriter(new FileWriter(destStateOutputPath));
		writer.write(TOP_STATES_COUNT_TITLE);
		for(String state:states) {
			int statesCaseCertified = statesCountMap.get(state);
			double percentage = (double)statesCaseCertified*100/totalCaseCertified;
			String output = state + ";" + statesCaseCertified + ";" + String.format("%.1f", percentage) + "%\n";
			writer.write(output);
		}
		writer.close();
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length != 3) {
			throw new IllegalArgumentException("ERROR: Missing one or more aommand arguments.");
		}
		try {
			sourceInputPath = args[0];
			destOccupationOutputPath = args[1];
			destStateOutputPath = args[2];
			readAndSaveInMaps();
			sortOccupationData();
			sortStatesData();
		}
		catch(IllegalArgumentException e) {
			System.out.println("ERROR: Missing one or more aommand arguments.");
			System.exit(1);
		}
		catch(FileNotFoundException e) {
			System.out.println("ERROR: Cannot find files.");
			System.exit(1);
		}
	}
}
