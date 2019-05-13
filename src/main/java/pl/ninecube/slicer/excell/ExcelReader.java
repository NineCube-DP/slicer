package pl.ninecube.slicer.excell;

import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pl.ninecube.slicer.excell.exception.ErrorOpenExcellFileException;
import pl.ninecube.slicer.excell.exception.ExcelException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ExcelReader {

    private Workbook workbook;

    private Sheet sheet;

    public void openFile(Path filePath) throws FileNotFoundException, ErrorOpenExcellFileException {

        FileInputStream file = new FileInputStream(filePath.toFile());
        workbook = openExcellFile(filePath, file);
    }

    private Workbook openExcellFile(Path filePath, FileInputStream file) throws ErrorOpenExcellFileException {

        try {
            return new XSSFWorkbook(file);
        } catch (IOException e) {
            throw new ErrorOpenExcellFileException("Error opening file : " + filePath.getFileName());
        }
    }

    public Cell readCell(int x, int y) throws ExcelException {

        if (sheet.getRow(y) == null){
            throw new ExcelException("Row not found");
        }

        if (sheet.getRow(y).getCell(x) == null){
            throw new ExcelException("Cell not found");
        }

        return sheet.getRow(y).getCell(x);
    }

    public void readFromSheet(String name) throws ExcelException {

        if (name == null || name.isEmpty()) {
            throw new ExcelException("Wrong sheet name");
        }
        sheet = workbook.getSheet(name);
    }

    public List<String> getAllSheets(){

        List<String> sheetNames = new ArrayList<>();
        int numberOfSheets = workbook.getNumberOfSheets();

        for (int sheetNr = 0 ; sheetNr < numberOfSheets; sheetNr++){
            sheetNames.add(workbook.getSheetName(sheetNr));
        }

        return sheetNames;
    }

    public static void main(String[] args) throws FileNotFoundException, ErrorOpenExcellFileException, ExcelException {

        ExcelReader.openFile(Paths.get("src/main/resources/Orders.xlsx"));
        ExcelReader.readFromSheet("Dane Aplikacji");
        String stringCellValue = ExcelReader.readCell(0, 1).getStringCellValue();

        System.out.println(stringCellValue);

        System.out.println(ExcelReader.getAllSheets());
    }

}
